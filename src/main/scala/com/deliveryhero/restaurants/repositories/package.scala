package com.deliveryhero.restaurants

import java.util.UUID

import com.deliveryhero.restaurants.models.Restaurant.RestaurantFactory
import com.deliveryhero.restaurants.models.{Restaurant, Serialization}
import org.iq80.leveldb.DB

import scala.concurrent.{ExecutionContext, Future}

package object repositories {

  /**
    * The component responsible for the storage
    */
  trait RestaurantRepositoryComponent {
    /**
      * @return The complete list of the Restaurant stored within the actual implementation
      */
    def getRestaurants: Future[List[Restaurant]]

    /**
      * Return the restaurant defined by its id
      * @param uuid The id of the restaurant to return
      * @return None, if there is no restaurant with the given id, or Some[Restaurant] otherwise
      */
    def getRestaurant(uuid: UUID): Future[Option[Restaurant]]

    /**
      * Create a new restaurant by assigning an ID to it
      * @param restaurantFactory a function of UUID => Restaurant
      * @return The restaurant created
      */
    def createRestaurant(restaurantFactory: RestaurantFactory): Future[Restaurant]

    /**
      * Delete the restaurant defined by it's id
      * @param uuid The id of the restaurant to be deleted
      * @return The
      */
    def deleteRestaurant(uuid: UUID): Future[Option[Restaurant]]

    /**
      * Replace or create a restaurant, depending if there is already a restaurant with `restaurant.id`
      * @param restaurant
      * @return
      */
    def updateOrCreateRestaurant(restaurant: Restaurant): Future[Unit]
  }

  class AsyncInmemoryRestaurantRepositoryComponent(var restaurants: Map[UUID, Restaurant] = Map()) extends RestaurantRepositoryComponent {
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
    override def updateOrCreateRestaurant(restaurant: Restaurant): Future[Unit] = Future.successful {
      restaurants += (restaurant.id -> restaurant)
    }
  }

  class LevelDbRestaurantRepositoryComponent(db: DB)(implicit ec: ExecutionContext, s: Serialization[Restaurant]) extends RestaurantRepositoryComponent {
    import org.fusesource.leveldbjni.JniDBFactory.bytes

    override def getRestaurants: Future[List[Restaurant]] = Future {
      val iterator = db.iterator()
      iterator.seekToFirst()
      var list = List[Restaurant]()
      while (iterator.hasNext) {
        val next = iterator.next()
        list :+= s.deserialize(next.getValue)
      }
      list
    }

    override def getRestaurant(uuid: UUID): Future[Option[Restaurant]] = Future {
      // db.get returns null, if the key is not present...
      val res = Option { db.get(bytes(uuid.toString)) }
      res.map(s.deserialize)
    }

    override def createRestaurant(restaurantFactory: RestaurantFactory): Future[Restaurant] = Future {
      val uuid = UUID.randomUUID()
      val restaurant = restaurantFactory(uuid)
      db.put(bytes(uuid.toString), s.serialize(restaurant))
      restaurant
    }

    override def deleteRestaurant(uuid: UUID): Future[Option[Restaurant]] = for {
        restaurant <- getRestaurant(uuid)
      } yield {
        // delete only if there is a restaurant with a given ID
        restaurant.foreach(_ => db.delete(bytes(uuid.toString)))
        restaurant
      }

    override def updateOrCreateRestaurant(restaurant: Restaurant): Future[Unit] = Future {
      db.put(bytes(restaurant.id.toString), s.serialize(restaurant))
    }
  }
}
