package com.deliveryhero.restaurants.repositories

import java.util.UUID

import com.deliveryhero.restaurants.models.Restaurant
import com.deliveryhero.restaurants.models.Restaurant.RestaurantFactory
import org.iq80.leveldb.Options
import org.scalatest._

class LevelDbRestaurantRepositoryComponentSpecs extends AsyncFlatSpec with Matchers with OptionValues with Inside {
  import org.fusesource.leveldbjni.JniDBFactory._

  "LevelDbRestaurantRepositoryComponent" should "correctly return after creating an instance" in {
    val options = new Options
    options.createIfMissing(true)
    options.errorIfExists(true)

    val db = factory.open(new java.io.File("restaurant-test"), options)

    val repositoryComponent: RestaurantRepositoryComponent = new LevelDbRestaurantRepositoryComponent(db)

    val restaurantFactory: RestaurantFactory = { id =>
      Restaurant(id, "name", "0123456789", Nil, "", "desc")
    }

    val res = for {
      restaurant1 <- repositoryComponent.createRestaurant(restaurantFactory)
      restaurant2 <- repositoryComponent.getRestaurant(restaurant1.id)
    } yield {
      restaurant2.value should be (restaurant1)
    }

    res.onComplete { _ =>
      db.close()
      factory.destroy(new java.io.File("restaurant-test"), options)
    }
    res
  }

  it should "return None for a non-existing restaurant" in {
    val options = new Options
    options.createIfMissing(true)
    options.errorIfExists(true)

    val db = factory.open(new java.io.File("restaurant-test"), options)

    val repositoryComponent: RestaurantRepositoryComponent = new LevelDbRestaurantRepositoryComponent(db)

    val res = for {
      restaurant <- repositoryComponent.getRestaurant(UUID.randomUUID())
    } yield {
      restaurant should be (None)
    }

    res.onComplete { _ =>
      db.close()
      factory.destroy(new java.io.File("restaurant-test"), options)
    }
    res
  }

  it should "correctly return all previously created instance" in {
    val options = new Options
    options.createIfMissing(true)
    options.errorIfExists(true)

    val db = factory.open(new java.io.File("restaurant-test"), options)

    val repositoryComponent: RestaurantRepositoryComponent = new LevelDbRestaurantRepositoryComponent(db)

    val restaurantFactory: RestaurantFactory = { id =>
      Restaurant(id, "name", "0123456789", Nil, "", "desc")
    }

    val res = for {
      _ <- repositoryComponent.createRestaurant(restaurantFactory)
      _ <- repositoryComponent.createRestaurant(restaurantFactory)
      _ <- repositoryComponent.createRestaurant(restaurantFactory)
      restaurants <- repositoryComponent.getRestaurants
    } yield {
      restaurants.size should be (3)
    }

    res.onComplete { _ =>
      db.close()
      factory.destroy(new java.io.File("restaurant-test"), options)
    }
    res
  }

  it should "not return anymore a deleted restaurant" in {
    val options = new Options
    options.createIfMissing(true)
    options.errorIfExists(true)

    val db = factory.open(new java.io.File("restaurant-test"), options)

    val repositoryComponent: RestaurantRepositoryComponent = new LevelDbRestaurantRepositoryComponent(db)

    val restaurantFactory: RestaurantFactory = { id =>
      Restaurant(id, "name", "0123456789", Nil, "", "desc")
    }

    val res = for {
      restaurant1 <- repositoryComponent.createRestaurant(restaurantFactory)
      restaurant2 <- repositoryComponent.deleteRestaurant(restaurant1.id)
      restaurant3 <- repositoryComponent.getRestaurant(restaurant1.id)
    } yield {
      restaurant2.value should be (restaurant1)
      restaurant3 should be (None)
    }

    res.onComplete { _ =>
      db.close()
      factory.destroy(new java.io.File("restaurant-test"), options)
    }
    res
  }

  it should "correctly update already existing restaurant" in {
    val options = new Options
    options.createIfMissing(true)
    options.errorIfExists(true)

    val db = factory.open(new java.io.File("restaurant-test"), options)

    val repositoryComponent: RestaurantRepositoryComponent = new LevelDbRestaurantRepositoryComponent(db)

    val restaurantFactory: RestaurantFactory = { id =>
      Restaurant(id, "name", "0123456789", Nil, "", "desc")
    }

    val res = for {
      restaurant1 <- repositoryComponent.createRestaurant(restaurantFactory)
      _ <- repositoryComponent.updateOrCreateRestaurant(restaurant1.copy(name = "updatedName"))
      restaurant2 <- repositoryComponent.getRestaurant(restaurant1.id)
    } yield {
      inside(restaurant2) {
        case Some(Restaurant(id, name, phoneNumber, cuisines, address, description)) =>
          id should be (restaurant1.id)
          name should be ("updatedName")
          phoneNumber should be (restaurant1.phoneNumber)
          cuisines should be (restaurant1.cuisines)
          address should be (restaurant1.address)
          description should be (restaurant1.description)
      }
    }

    res.onComplete { _ =>
      db.close()
      factory.destroy(new java.io.File("restaurant-test"), options)
    }
    res
  }
}
