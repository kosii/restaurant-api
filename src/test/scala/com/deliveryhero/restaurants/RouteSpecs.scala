package com.deliveryhero.restaurants

import java.util.UUID

import akka.http.scaladsl.model.{ContentTypes, HttpHeader, StatusCodes}
import akka.http.scaladsl.server.{MalformedRequestContentRejection, RequestEntityExpectedRejection}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.deliveryhero.restaurants.models.Restaurant
import com.deliveryhero.restaurants.models.Restaurant.RestaurantFactory
import com.deliveryhero.restaurants.repositories.RestaurantRepositoryComponent
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future

class RouteSpecs extends FlatSpec with Matchers with ScalatestRouteTest {
  "POST to /restaurants" should "reject missing body" in {
    val routeWithMockedReposityComponent = routeBuilder(mock(classOf[RestaurantRepositoryComponent]))
    Post("/restaurants") ~> routeWithMockedReposityComponent ~> check {
      rejection should be (RequestEntityExpectedRejection)
    }
  }

  it should "reject empty json body" in {
    val routeWithMockedReposityComponent = routeBuilder(mock(classOf[RestaurantRepositoryComponent]))

    Post("/restaurants").withEntity(ContentTypes.`application/json`, """""") ~> routeWithMockedReposityComponent ~> check {
      rejection shouldEqual RequestEntityExpectedRejection
    }
  }

  it should "reject malformed json body" in {
    val routeWithMockedReposityComponent = routeBuilder(mock(classOf[RestaurantRepositoryComponent]))

    Post("/restaurants").withEntity(ContentTypes.`application/json`, """{"lol": """) ~> routeWithMockedReposityComponent ~> check {
      rejection should matchPattern {
        case MalformedRequestContentRejection(_, _) =>
      }
    }
  }

  it should "accept correctly formed json body" in {
    val mockedRepositoryComponent = mock(classOf[RestaurantRepositoryComponent])
    val restaurant = mock(classOf[Restaurant])
    val uuid = UUID.randomUUID()
    when(restaurant.id).thenReturn(uuid)
    when(mockedRepositoryComponent.createRestaurant(any())).thenReturn(Future.successful(restaurant))
    val routeWithMockedReposityComponent = routeBuilder(mockedRepositoryComponent)
    val body =
      """
        |{
        |  "name":"Le Petit Poucet",
        |	 "phoneNumber":"+33 1 47 38 61 85",
        |	 "cuisines":["french", "traditional"],
        |	 "address":"4 Rond-Point Claude Monet, 92300 Levallois-Perret, France",
        |	 "description":"Le Petit Poucet est une institution sur l’île de la Jatte. Le bâtiment de style californien, sa belle cheminée et son bois exotique vous séduisent d’emblée. Et si vous regardiez couler la Seine depuis la vaste terrasse chauffée l’hiver ?"
        |}
      """.stripMargin

    Post("/restaurants").withEntity(ContentTypes.`application/json`, body) ~> routeWithMockedReposityComponent ~> check {
      status should be (StatusCodes.Created)
      header("Location") should matchPattern {
        case Some(header: HttpHeader) if header.value().endsWith(uuid.toString) =>
      }
    }
  }

  "GET to /restaurants" should "return the complete list of the restaurants" in {
    val mockedRepositoryComponent = mock(classOf[RestaurantRepositoryComponent])
    val restaurant = Restaurant(UUID.randomUUID(), "", "", Nil, "", "")
    val restaurants = List(restaurant, restaurant, restaurant)
    when(mockedRepositoryComponent.getRestaurants).thenReturn(Future.successful(restaurants))
    val routeWithMockedReposityComponent = routeBuilder(mockedRepositoryComponent)

    Get("/restaurants") ~> routeWithMockedReposityComponent ~> check {
      import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
      import io.circe.generic.auto._

      status should be (StatusCodes.OK)
      entityAs[List[Restaurant]] should be (restaurants)
    }
  }

  "GET a given restaurant" should "return NotFound when it does no exist" in {
    val mockedRepositoryComponent = mock(classOf[RestaurantRepositoryComponent])
    val uuid = UUID.randomUUID()
    val routeWithMockedReposityComponent = routeBuilder(mockedRepositoryComponent)

    when(mockedRepositoryComponent.getRestaurant(any[UUID])).thenReturn(Future.successful { None })

    Get(s"/restaurants/${uuid}") ~> routeWithMockedReposityComponent ~> check {
      status should be (StatusCodes.NotFound)
    }
  }

  it should "return OK when it does exist" in {
    val mockedRepositoryComponent = mock(classOf[RestaurantRepositoryComponent])
    val uuid = UUID.randomUUID()
    val restaurant = Restaurant(uuid, "", "", Nil, "", "")
    val routeWithMockedReposityComponent = routeBuilder(mockedRepositoryComponent)

    when(mockedRepositoryComponent.getRestaurant(any[UUID])).thenAnswer((invocation: InvocationOnMock) => Future.successful {
      if (invocation.getArgument(0).asInstanceOf[UUID].equals(uuid)) {
        Some(restaurant)
      } else {
        None
      }
    })

    Get(s"/restaurants/${UUID.randomUUID()}") ~> routeWithMockedReposityComponent ~> check {
      status should be (StatusCodes.NotFound)
    }

    Get(s"/restaurants/${uuid}") ~> routeWithMockedReposityComponent ~> check {
      import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
      import io.circe.generic.auto._

      status should be (StatusCodes.OK)
      entityAs[Restaurant] should be (restaurant)
    }
  }

  "PUT on a given restaurant's url" should "reject missing body" in {
    val routeWithMockedReposityComponent = routeBuilder(mock(classOf[RestaurantRepositoryComponent]))
    Put(s"/restaurants/${UUID.randomUUID()}") ~> routeWithMockedReposityComponent ~> check {
      rejection should be (RequestEntityExpectedRejection)
    }
  }

  it should "reject empty json body" in {
    val routeWithMockedReposityComponent = routeBuilder(mock(classOf[RestaurantRepositoryComponent]))

    Put(s"/restaurants/${UUID.randomUUID()}").withEntity(ContentTypes.`application/json`, """""") ~> routeWithMockedReposityComponent ~> check {
      rejection shouldEqual RequestEntityExpectedRejection
    }
  }

  it should "reject malformed json body" in {
    val routeWithMockedReposityComponent = routeBuilder(mock(classOf[RestaurantRepositoryComponent]))

    Put(s"/restaurants/${UUID.randomUUID()}").withEntity(ContentTypes.`application/json`, """{"lol": """) ~> routeWithMockedReposityComponent ~> check {
      rejection should matchPattern {
        case MalformedRequestContentRejection(_, _) =>
      }
    }
  }

  it should "call the correct repository method in case of correctly formatted json" in {
    val mockedRepositoryComponent = mock(classOf[RestaurantRepositoryComponent])
    val uuid = UUID.randomUUID()
    val routeWithMockedReposityComponent = routeBuilder(mockedRepositoryComponent)
    val body =
      """
        |{
        |  "name":"Le Petit Poucet",
        |	 "phoneNumber":"+33 1 47 38 61 85",
        |	 "cuisines":["french", "traditional"],
        |	 "address":"4 Rond-Point Claude Monet, 92300 Levallois-Perret, France",
        |	 "description":"Le Petit Poucet est une institution sur l’île de la Jatte. Le bâtiment de style californien, sa belle cheminée et son bois exotique vous séduisent d’emblée. Et si vous regardiez couler la Seine depuis la vaste terrasse chauffée l’hiver ?"
        |}
      """.stripMargin

    Put(s"/restaurants/${uuid}").withEntity(ContentTypes.`application/json`, body) ~> routeWithMockedReposityComponent ~> check {}
    verify(mockedRepositoryComponent, times(1)).updateOrCreateRestaurant(any())
  }

  it should "return with NoContent status code when restaurant was correctly updated or created" in {
    val mockedRepositoryComponent = mock(classOf[RestaurantRepositoryComponent])
    when(mockedRepositoryComponent.updateOrCreateRestaurant(any())).thenReturn(Future.successful(()))
    val uuid = UUID.randomUUID()
    val routeWithMockedReposityComponent = routeBuilder(mockedRepositoryComponent)
    val body =
      """
        |{
        |  "name":"Le Petit Poucet",
        |	 "phoneNumber":"+33 1 47 38 61 85",
        |	 "cuisines":["french", "traditional"],
        |	 "address":"4 Rond-Point Claude Monet, 92300 Levallois-Perret, France",
        |	 "description":"Le Petit Poucet est une institution sur l’île de la Jatte. Le bâtiment de style californien, sa belle cheminée et son bois exotique vous séduisent d’emblée. Et si vous regardiez couler la Seine depuis la vaste terrasse chauffée l’hiver ?"
        |}
      """.stripMargin
    import io.circe.generic.auto._, io.circe.parser._

    val restaurantFactory = decode[RestaurantFactory](body)
    val restaurant = restaurantFactory.right.get(uuid)

    Put(s"/restaurants/${uuid}").withEntity(ContentTypes.`application/json`, body) ~> routeWithMockedReposityComponent ~> check {
      verify(mockedRepositoryComponent, times(1)).updateOrCreateRestaurant(restaurant)
      status should be (StatusCodes.NoContent)
      header("Location") should be (None)
    }
  }

  "DELETE on a given restaurant's url" should "return NotFound for non-existing restaurant" in {
    val mockedRepositoryComponent = mock(classOf[RestaurantRepositoryComponent])
    val uuid = UUID.randomUUID()
    val restaurant = Restaurant(uuid, "", "", Nil, "", "")
    val routeWithMockedReposityComponent = routeBuilder(mockedRepositoryComponent)

    when(mockedRepositoryComponent.deleteRestaurant(any[UUID])).thenAnswer((invocation: InvocationOnMock) => Future.successful {
      if (invocation.getArgument(0).asInstanceOf[UUID].equals(uuid)) {
        Some(restaurant)
      } else {
        None
      }
    })

    Delete(s"/restaurants/${UUID.randomUUID()}") ~> routeWithMockedReposityComponent ~> check {
      status should be (StatusCodes.NotFound)
    }
  }

  it should "return NoContent when successfully deleted a restaurant" in {
    val mockedRepositoryComponent = mock(classOf[RestaurantRepositoryComponent])
    val uuid = UUID.randomUUID()
    val restaurant = Restaurant(uuid, "", "", Nil, "", "")
    val routeWithMockedReposityComponent = routeBuilder(mockedRepositoryComponent)

    when(mockedRepositoryComponent.deleteRestaurant(any[UUID])).thenAnswer((invocation: InvocationOnMock) => Future.successful {
      if (invocation.getArgument(0).asInstanceOf[UUID].equals(uuid)) {
        Some(restaurant)
      } else {
        None
      }
    })

    Delete(s"/restaurants/$uuid") ~> routeWithMockedReposityComponent ~> check {
      status should be (StatusCodes.NoContent)
    }
  }
}
