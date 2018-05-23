package com.deliveryhero.restaurants

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.deliveryhero.restaurants.repositories.LevelDbRestaurantRepositoryComponent
import org.iq80.leveldb.Options
import org.slf4j.LoggerFactory

import scala.io.StdIn
import scala.util.{Failure, Success}

object Main extends App {
  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  import org.fusesource.leveldbjni.JniDBFactory._

  val options = new Options
  options.createIfMissing(true)
  val db = factory.open(new java.io.File("restaurants"), options)

  val res = for {
    binding         <-  Http().bindAndHandle(
      routeBuilder(new LevelDbRestaurantRepositoryComponent(db)) ~ path("v1" / "healthcheck") {
        get {
          complete(StatusCodes.OK)
        }
      }, "localhost", 8080)
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
    db.close()
  }
}
