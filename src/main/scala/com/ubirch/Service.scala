package com.ubirch

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.services.jwt.PublicKeyPoolService
import com.ubirch.services.rest.RestService
import monix.eval.Task
import monix.execution.Scheduler
import javax.inject.{ Inject, Singleton }

import com.ubirch.services.state.SecretKeyPoolService

/**
  * Represents a bootable service object that starts the system
  */
@Singleton
class Service @Inject() (restService: RestService, publicKeyPoolService: PublicKeyPoolService, secretKeyPoolService: SecretKeyPoolService)(implicit scheduler: Scheduler) extends LazyLogging {

  def start(): Unit = {

    (for {
      _ <- publicKeyPoolService.init
      _ <- secretKeyPoolService.init
      _ <- Task.delay(restService.start())
    } yield ()).onErrorRecover {
      case e: Exception => logger.error("error_starting", e)
    }.runToFuture

    val cd = new CountDownLatch(1)
    cd.await()
  }

}

object Service extends Boot(List(new Binder)) {
  def main(args: Array[String]): Unit = * {
    get[Service].start()
  }
}
