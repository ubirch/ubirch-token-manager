package com.ubirch

import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, ExecutionContextExecutor, Future }

/**
  * Represents base for a convenient test
  */
trait TestBase
  extends AnyWordSpec
  with ScalaFutures
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with Matchers
  with Awaits
  with ExecutionContextsTests {}

trait ExecutionContextsTests {
  implicit lazy val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))
  implicit lazy val scheduler: Scheduler = monix.execution.Scheduler(ec)
}

trait Awaits {

  def await[T](future: Future[T]): T = await(future, Duration.Inf)

  def await[T](future: Future[T], atMost: Duration): T = Await.result(future, atMost)

  def await[T](observable: Observable[T], atMost: Duration)(implicit scheduler: Scheduler): Seq[T] = {
    val future = observable.foldLeftL(Nil: Seq[T])((a, b) => a ++ Seq(b)).runToFuture
    Await.result(future, atMost)
  }

  def await[T](task: Task[T], atMost: Duration)(implicit scheduler: Scheduler): T = {
    val future = task.runToFuture
    Await.result(future, atMost)
  }

}
