package com.ubirch

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.services.rest.RestService
import javax.inject.{ Inject, Singleton }

/**
  * Represents a bootable service object that starts the system
  */
@Singleton
class Service @Inject() (restService: RestService) extends LazyLogging {

  def start: Unit = {

    restService.start

    val cd = new CountDownLatch(1)
    cd.await()
  }

}

object Service extends Boot(List(new Binder)) {
  def main(args: Array[String]): Unit = * {
    get[Service].start
  }
}
