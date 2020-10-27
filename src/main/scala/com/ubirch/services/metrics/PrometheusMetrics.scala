package com.ubirch.services.metrics

import java.net.BindException

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.PrometheusConfPaths
import com.ubirch.services.lifeCycle.Lifecycle
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports
import javax.inject._

import scala.annotation.tailrec
import scala.concurrent.Future

/**
  * Represents a component for starting the Prometheus Server
  * @param config the configuration object
  * @param lifecycle the life cycle tool
  */
@Singleton
class PrometheusMetrics @Inject() (config: Config, lifecycle: Lifecycle) extends PrometheusConfPaths with LazyLogging {

  val port: Int = config.getInt(PORT)

  logger.debug("Creating Prometheus Server on Port[{}]", port)

  val server: HTTPServer = PrometheusMetricsHelper.create(port)

  lifecycle.addStopHook { () =>
    logger.info("Shutting down Prometheus")
    Future.successful(server.stop())
  }

}

object PrometheusMetricsHelper extends LazyLogging {

  final private val maxAttempts = 3

  def create(port: Int): HTTPServer = {
    DefaultExports.initialize()

    @tailrec
    def go(attempts: Int, port: Int): HTTPServer = {
      try {
        new HTTPServer(port)
      } catch {
        case e: BindException =>
          val newPort = port + new scala.util.Random().nextInt(50)
          logger.debug("Attempt[{}], Port[{}] is busy, trying Port[{}]", attempts, port, newPort)
          if (attempts == maxAttempts) {
            throw e
          } else {
            go(attempts + 1, newPort)
          }
      }
    }

    val server = go(0, port)
    logger.debug(s"You can visit http://localhost:${server.getPort}/ to check the metrics.")
    server
  }

}
