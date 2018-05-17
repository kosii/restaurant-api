import java.util.UUID

import Main.Restaurant.RestaurantFactory
import akka.actor.ActorSystem
import akka.http.javadsl.model.headers.AccessControlAllowHeaders
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, ValidationRejection}
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.io.StdIn

object Main extends App {
  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  case class Restaurant(id: UUID, name: String, phoneNumber: String, cuisines: List[String], address: String, description: String)

  object Restaurant {
    type RestaurantFactory = (UUID => Restaurant)
  }

  var restaurants: Map[UUID, Restaurant] = Map(
    UUID.fromString("a4fc27ac-beaa-4ece-97b0-349b43127475") -> Restaurant(UUID.fromString("a4fc27ac-beaa-4ece-97b0-349b43127475"), "1", "", Nil, "", ""),
    UUID.fromString("a45c24fe-488d-44ce-af38-debca5eeba90") -> Restaurant(UUID.fromString("a45c24fe-488d-44ce-af38-debca5eeba90"), "2", "", Nil, "", ""),
    UUID.fromString("22acd04d-48c3-4bf9-830e-4d095f5b1613") -> Restaurant(UUID.fromString("22acd04d-48c3-4bf9-830e-4d095f5b1613"), "3", "", Nil, "", "")
  )

  println(restaurants)

  val route = pathPrefix("restaurants") {
    pathEndOrSingleSlash {
      get {
        complete(restaurants.values)
      } ~ post {
        entity(as[RestaurantFactory]) { restaurantFactory =>
          val newId = UUID.randomUUID()
          val restaurant = restaurantFactory(newId)
          restaurants += (newId -> restaurant)
          extractUri { uri =>
            complete(StatusCodes.Created, List(Location(Uri(uri + "/" + newId))), HttpEntity.Empty)
          }
        }
      }
    } ~ path(JavaUUID) { restaurantID =>
      get {
        complete(restaurants.get(restaurantID))
      } ~ put {
        entity(as[RestaurantFactory]) { restaurantFactory =>
          restaurants += (restaurantID -> restaurantFactory(restaurantID))
          complete(StatusCodes.NoContent)
        }
      } ~ delete {
        if (restaurants.contains(restaurantID)) {
          restaurants -= restaurantID
          complete(StatusCodes.NoContent)
        } else {
          complete(StatusCodes.NotFound)
        }
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
