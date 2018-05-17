package com.deliveryhero.restaurants

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.deliveryhero.restaurants.Main.Restaurant.RestaurantFactory
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object Main extends App {
  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  case class Restaurant(id: UUID, name: String, phoneNumber: String, cuisines: List[String], address: String, description: String)

  object Restaurant {
    type RestaurantFactory = (UUID => Restaurant)
  }

  trait RestaurantRepositoryComponent {
    def getRestaurants: Future[List[Restaurant]]
    def getRestaurant(uuid: UUID): Future[Option[Restaurant]]
    def createRestaurant(restaurantFactory: RestaurantFactory): Future[Restaurant]
    def deleteRestaurant(uuid: UUID): Future[Option[Restaurant]]
    def updateRestaurant(restaurant: Restaurant): Future[Unit]
  }

  class AsyncInmemoryRestaurantRepositoryComponent extends RestaurantRepositoryComponent {
    var restaurants: Map[UUID, Restaurant] = Map(
      UUID.fromString("a4fc27ac-beaa-4ece-97b0-349b43127475") -> Restaurant(UUID.fromString("a4fc27ac-beaa-4ece-97b0-349b43127475"), "1", "", Nil, "", ""),
      UUID.fromString("a45c24fe-488d-44ce-af38-debca5eeba90") -> Restaurant(UUID.fromString("a45c24fe-488d-44ce-af38-debca5eeba90"), "2", "", Nil, "", ""),
      UUID.fromString("22acd04d-48c3-4bf9-830e-4d095f5b1613") -> Restaurant(UUID.fromString("22acd04d-48c3-4bf9-830e-4d095f5b1613"), "3", "", Nil, "", "")
    )

    override def getRestaurants: Future[List[Restaurant]] = Future.successful(restaurants.values.toList)
    override def getRestaurant(uuid: UUID): Future[Option[Restaurant]] = Future.successful(restaurants.get(uuid))
    override def createRestaurant(restaurantFactory: RestaurantFactory): Future[Restaurant] = Future.successful {
      val uuid = UUID.randomUUID()
      val restaurant = restaurantFactory(uuid)
      restaurants += (uuid -> restaurant)
      restaurant
    }
    override def deleteRestaurant(uuid: UUID): Future[Option[Restaurant]] = Future.successful {
      val res = restaurants.get(uuid)
      restaurants -= uuid
      res
    }
    override def updateRestaurant(restaurant: Restaurant): Future[Unit] = Future.successful {
      restaurants += (restaurant.id -> restaurant)
    }
  }

  def routeBuilder(restaurantRepositoryComponent: RestaurantRepositoryComponent): Route = {
    pathPrefix("restaurants") {
      pathEndOrSingleSlash {
        get {
          complete(restaurantRepositoryComponent.getRestaurants)
        } ~ post {
          extractUri { uri =>
            entity(as[RestaurantFactory]) { restaurantFactory =>
              val restaurantFuture = restaurantRepositoryComponent.createRestaurant(restaurantFactory)
              complete(restaurantFuture.map { restaurant =>
                (StatusCodes.Created, List(Location(Uri(uri + "/" + restaurant.id))), HttpEntity.Empty)
              })
            }
          }
        }
      } ~ path(JavaUUID) { restaurantID =>
        get {
          complete(restaurantRepositoryComponent.getRestaurant(restaurantID).map({
            case None =>
              val ev = implicitly[StatusCode => ToResponseMarshallable]
              ev(StatusCodes.NotFound)
            case Some(restaurant) =>
              val ev = implicitly[Restaurant => ToResponseMarshallable]
              ev(restaurant)
          }))
        } ~ put {
          entity(as[RestaurantFactory]) { restaurantFactory =>
            val restaurant = restaurantFactory(restaurantID)
            complete(restaurantRepositoryComponent.updateRestaurant(restaurant).map(_ => StatusCodes.NoContent))
          }
        } ~ delete {
          complete(
            restaurantRepositoryComponent.deleteRestaurant(restaurantID).map({
              case None => StatusCodes.NotFound
              case Some(_) => StatusCodes.NoContent
            })
          )
        }
      }
    }
  }

  val route = routeBuilder(new AsyncInmemoryRestaurantRepositoryComponent())

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
