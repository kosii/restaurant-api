package com.deliveryhero.restaurants

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.deliveryhero.restaurants.repositories.restaurants.AsyncInmemoryRestaurantRepositoryComponent

import scala.io.StdIn
import scala.util.{Failure, Success}

object Main extends App {
  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val res = for {
    binding         <-  Http().bindAndHandle(routeBuilder(new AsyncInmemoryRestaurantRepositoryComponent), "localhost", 8080)
    _               =   println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    _               =   StdIn.readLine()
    done            <-  binding.unbind()
  } yield {
    ()
  }

  res onComplete {
    case Success(_) =>
      println("Application shut down successfully...")
    case Failure(ex) =>
      println(s"Application failed because of $ex")
  }

  res onComplete { _ =>
    materializer.shutdown()
    system.terminate()
  }
}
