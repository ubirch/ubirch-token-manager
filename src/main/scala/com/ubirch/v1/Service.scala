package com.ubirch.v1

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.v1.services.jwt.PublicKeyPoolService
import com.ubirch.v1.services.rest.RestService
import monix.eval.Task
import monix.execution.Scheduler
import javax.inject.{ Inject, Singleton }

/**
  * Represents a bootable service object that starts the system
  */
@Singleton
class Service @Inject() (restService: RestService, publicKeyPoolService: PublicKeyPoolService)(implicit scheduler: Scheduler) extends LazyLogging {

  def start(): Unit = {

    publicKeyPoolService.init.doOnFinish {
      case Some(e) =>
        Task.delay(logger.error("error_loading_keys", e))
      case None =>
        Task.delay(restService.start())
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
