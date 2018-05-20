package com.deliveryhero.restaurants.models

import java.util.UUID

import org.scalatest.{FlatSpec, FunSpec, Matchers}

class SerializationSpecs extends FlatSpec with Matchers {
  "avroDeserializer" should "correctly deserialize serialized Restaurant object" in {
    val restaurant = Restaurant(UUID.randomUUID(), "Le Petit Poucet", "+33 6 12 34 45 56", List("French"), "Ile de Jatte, Levallois", "")

    val ev = implicitly[Serialization[Restaurant]]

    ev.deserialize(ev.serialize(restaurant)) should matchPattern {
      case Some(`restaurant`) =>
    }
  }
}
