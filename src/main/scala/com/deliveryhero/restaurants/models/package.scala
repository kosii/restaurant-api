package com.deliveryhero.restaurants

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.UUID

import com.sksamuel.avro4s._

package object models {
  case class Restaurant(id: UUID, name: String, phoneNumber: String, cuisines: List[String], address: String, description: String)

  object Restaurant {
    type RestaurantFactory = (UUID => Restaurant)
  }

  trait Serialization[T] {
    def serialize(o: T): Array[Byte]
    def deserialize(bytes: Array[Byte]): Option[T]
  }

  implicit def avroDeserializer[T: SchemaFor : ToRecord : FromRecord]: Serialization[T] = new Serialization[T] {
    override def serialize(o: T): Array[Byte] = {
      val baos = new ByteArrayOutputStream()
      val output = AvroOutputStream.binary[T](baos)
      output.write(o)
      output.close()
      baos.toByteArray
    }

    override def deserialize(bytes: Array[Byte]): Option[T] = {
      val in = new ByteArrayInputStream(bytes)
      val input = AvroInputStream.binary[T](in)
      val result = input.iterator.toSeq
      result.headOption
    }
  }
}
