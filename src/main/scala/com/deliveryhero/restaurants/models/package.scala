package com.deliveryhero.restaurants

import java.util.UUID

package object models {
  case class Restaurant(id: UUID, name: String, phoneNumber: String, cuisines: List[String], address: String, description: String)

  object Restaurant {
    type RestaurantFactory = (UUID => Restaurant)
  }
}
