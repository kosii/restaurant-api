package com.deliveryhero.restaurants

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.deliveryhero.restaurants.repositories.LevelDbRestaurantRepositoryComponent
import org.iq80.leveldb.Options
import org.slf4j.LoggerFactory

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.{Failure, Success}

object Main extends App {
  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  val LOG = LoggerFactory.getLogger(this.getClass)

  import org.fusesource.leveldbjni.JniDBFactory._

  protected def waitForShutdownSignal(system: ActorSystem)(implicit ec: ExecutionContext): Future[Done] = {
    val promise = Promise[Done]()
    sys.addShutdownHook {
      promise.trySuccess(Done)
    }
    Future {
      blocking {
        if (StdIn.readLine("Press RETURN to stop...\n") != null)
          promise.trySuccess(Done)
      }
    }
    promise.future
  }

  val options = new Options
  options.createIfMissing(true)
  val db = factory.open(new java.io.File("restaurants"), options)

  val bindingFuture = Http().bindAndHandle(
    routeBuilder(new LevelDbRestaurantRepositoryComponent(db)) ~ path("v1" / "healthcheck") {
      get {
        complete(StatusCodes.OK)
      }
    }, "0.0.0.0", 8080)

  bindingFuture.onComplete {
    case Success(binding) ⇒
      LOG.info(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    case Failure(cause) ⇒
      LOG.error(s"Error starting the server ${cause.getMessage}", cause)
  }

  Await.ready(
    bindingFuture.flatMap(_ ⇒ waitForShutdownSignal(system)), // chaining both futures to fail fast
    Duration.Inf) // It's waiting forever because maybe there is never a shutdown signal

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(attempt ⇒ {
      LOG.info("Shutting down the server")
      materializer.shutdown()
      system.terminate()
      db.close()
    })
}
