package com.deliveryhero

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpEntity, StatusCode, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.deliveryhero.restaurants.models.Restaurant
import com.deliveryhero.restaurants.models.Restaurant.RestaurantFactory
import com.deliveryhero.restaurants.repositories.RestaurantRepositoryComponent
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext

package object restaurants {
  def routeBuilder(restaurantRepositoryComponent: RestaurantRepositoryComponent)(implicit ec: ExecutionContext): Route = {
    pathPrefix("restaurants") {
      pathEndOrSingleSlash {
        get {
          complete(restaurantRepositoryComponent.getRestaurants)
        } ~ post {
          extractMatchedPath {
            path =>
              entity(as[RestaurantFactory]) { restaurantFactory =>
              val restaurantFuture = restaurantRepositoryComponent.createRestaurant(restaurantFactory)
              complete(restaurantFuture.map { restaurant =>
                (StatusCodes.Created, List(Location(Uri.from(path = (path / restaurant.id.toString).toString()))), HttpEntity.Empty)
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
            complete(restaurantRepositoryComponent.updateOrCreateRestaurant(restaurant).map(_ => StatusCodes.NoContent))
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
}
