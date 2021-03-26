package com.ubirch.util

import com.ubirch.models.WithVersion
import io.prometheus.client.Counter
import monix.execution.CancelableFuture

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

trait ServiceMetrics extends WithVersion {

  def service: String

  def successCounter: Counter

  def errorCounter: Counter

  def countWhen[T](method: String)(ft: T => Boolean)(cf: CancelableFuture[T])(implicit ec: ExecutionContext): CancelableFuture[T] = {

    def s(): Unit = successCounter.labels(service, method, version.name).inc()
    def f(): Unit = errorCounter.labels(service, method, version.name).inc()

    cf.onComplete {
      case Success(t) => if (ft(t)) s() else f()
      case Failure(_) => f()
    }
    cf
  }

  def count[T](method: String)(cf: CancelableFuture[T])(implicit ec: ExecutionContext): CancelableFuture[T] = {
    cf.onComplete {
      case Success(_) => successCounter.labels(service, method, version.name).inc()
      case Failure(_) => errorCounter.labels(service, method, version.name).inc()
    }
    cf
  }

}
