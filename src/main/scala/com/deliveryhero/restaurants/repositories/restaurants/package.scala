package com.deliveryhero.restaurants.repositories

import java.util.UUID

import com.deliveryhero.restaurants.models.Restaurant
import com.deliveryhero.restaurants.models.Restaurant.RestaurantFactory

import scala.concurrent.Future

package object restaurants {

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
}
