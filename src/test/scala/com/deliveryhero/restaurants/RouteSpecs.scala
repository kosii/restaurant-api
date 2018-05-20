package com.deliveryhero.restaurants

import java.util.UUID

import akka.http.scaladsl.model.{ContentTypes, HttpHeader, StatusCodes}
import akka.http.scaladsl.server.{MalformedRequestContentRejection, RequestEntityExpectedRejection}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.deliveryhero.restaurants.models.Restaurant
import com.deliveryhero.restaurants.repositories.restaurants.RestaurantRepositoryComponent
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
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
}
