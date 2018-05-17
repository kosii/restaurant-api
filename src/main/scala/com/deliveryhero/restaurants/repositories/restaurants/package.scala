package com.deliveryhero.restaurants.repositories

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.UUID

import com.deliveryhero.restaurants.models.Restaurant
import com.deliveryhero.restaurants.models.Restaurant.RestaurantFactory
import com.sksamuel.avro4s._
import org.iq80.leveldb.Options

import scala.concurrent.{ExecutionContext, Future}

package object restaurants {

  /**
    * The component responsible for the storage subsystem.
    */
  trait RestaurantRepositoryComponent {
    /**
      * @return The complete list of the Restaurant stored with the actual implementation
      */
    def getRestaurants: Future[List[Restaurant]]

    /**
      * Return the restaurant defined by it's id
      * @param uuid The id of the restaurant to return
      * @return
      */
    def getRestaurant(uuid: UUID): Future[Option[Restaurant]]
    def createRestaurant(restaurantFactory: RestaurantFactory): Future[Restaurant]

    /**
      * Delete the restaurant defined by it's id
      * @param uuid The id of the restaurant to be deleted
      * @return The
      */
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

  class LevelDbRestaurantRepositoryComponent(implicit ec: ExecutionContext) extends RestaurantRepositoryComponent {

    implicit val schema = AvroSchema[Restaurant]
    implicit val schemaFor = SchemaFor[Restaurant]
    implicit val toRecord = ToRecord[Restaurant]


    def serialize(restaurant: Restaurant): Array[Byte] = {
      val baos = new ByteArrayOutputStream()
      val output = AvroOutputStream.binary[Restaurant](baos)
      output.write(restaurant)
      output.close()
      baos.toByteArray
    }

    def deserialize(bytes: Array[Byte]): Option[Restaurant] = {
      val in = new ByteArrayInputStream(bytes)
      val input = AvroInputStream.binary[Restaurant](in)
      val result = input.iterator.toSeq
      result.headOption
    }


    import org.iq80.leveldb._
    import org.fusesource.leveldbjni.JniDBFactory._
    import java.io._

    import org.iq80.leveldb.DB

    val options = new Options
    options.createIfMissing(true)
    val db: DB = factory.open(new File("restaurants"), options)

    override def getRestaurants: Future[List[Restaurant]] = Future {
      val iterator = db.iterator()
      iterator.seekToFirst()
      var list = List[Option[Restaurant]]()
      while (iterator.hasNext) {
        val next = iterator.next()
        list :+= deserialize(next.getValue)
      }
      list.flatten
    }

    override def getRestaurant(uuid: UUID): Future[Option[Restaurant]] = Future {
      deserialize(db.get(bytes(uuid.toString)))
    }

    override def createRestaurant(restaurantFactory: RestaurantFactory): Future[Restaurant] = Future {
      val uuid = UUID.randomUUID()
      val restaurant = restaurantFactory(uuid)
      db.put(bytes(uuid.toString), serialize(restaurant))
      restaurant
    }

    override def deleteRestaurant(uuid: UUID): Future[Option[Restaurant]] = for {
        restaurant <- getRestaurant(uuid)
      } yield {
        restaurant.foreach(_ => db.delete(bytes(uuid.toString)))
        restaurant
      }

    override def updateRestaurant(restaurant: Restaurant): Future[Unit] = Future {
      db.put(bytes(restaurant.id.toString), serialize(restaurant))
    }
  }
}
