package com.ubirch.controllers.concerns

import java.io.{ ByteArrayInputStream, FileOutputStream }
import java.nio.charset.StandardCharsets
import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.models.NOK
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import org.apache.commons.compress.utils.IOUtils
import org.json4s.JsonAST.JValue
import org.scalatra._
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.SwaggerSupport

import javax.servlet.http.{ HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse }
import javax.servlet.{ ReadListener, ServletInputStream }

import scala.util.Try
import scala.util.control.NoStackTrace

/**
  * Represents a customized ServletInputStream that allows to cache the body of a request.
  * This trait is very important to be able to re-consume the body in case of need.
  * @param cachedBody Represents the InputStream as bytes.
  * @param raw Represents the raw ServletInputStream
  */
class CachedBodyServletInputStream(cachedBody: Array[Byte], raw: ServletInputStream) extends ServletInputStream {

  private val cachedInputStream = new ByteArrayInputStream(cachedBody)

  override def isFinished: Boolean = cachedInputStream.available() == 0
  override def isReady: Boolean = true
  override def setReadListener(readListener: ReadListener): Unit = raw.setReadListener(readListener)

  override def read(): Int = cachedInputStream.read()
  override def read(b: Array[Byte]): Int = read(b, 0, b.length)
  override def read(b: Array[Byte], off: Int, len: Int): Int = cachedInputStream.read(b, off, len)

}

/***
 * Represents a customized HttpServletRequest that allows us to decorate the original object with extra info
 * or extra functionality.
 * Initially, it supports the re-consumption of the body stream
 * @param httpServletRequest Represents the original Request
 */
class ServiceRequest(httpServletRequest: HttpServletRequest) extends HttpServletRequestWrapper(httpServletRequest) {

  val cachedBody: Array[Byte] = IOUtils.toByteArray(httpServletRequest.getInputStream)

  override def getInputStream: ServletInputStream = {
    new CachedBodyServletInputStream(cachedBody, httpServletRequest.getInputStream)
  }
}

/**
  * Represents a Handler that creates the customized request.
  * It should be mixed it with the corresponding ScalatraServlet.
  */
trait RequestEnricher extends Handler {
  abstract override def handle(request: HttpServletRequest, res: HttpServletResponse): Unit = {
    super.handle(new ServiceRequest(request), res)
  }
}

/**
  * Represents the base for a controllers that supports the ServiceRequest
  * and adds helpers to handle async responses and body parsing and extraction.
  */
abstract class ControllerBase extends ScalatraServlet
  with RequestEnricher
  with FutureSupport
  with NativeJsonSupport
  with SwaggerSupport
  with CorsSupport
  with ServiceMetrics
  with LazyLogging {

  def asyncResult(name: String)(body: HttpServletRequest => HttpServletResponse => Task[ActionResult])(implicit request: HttpServletRequest, response: HttpServletResponse, scheduler: Scheduler): AsyncResult = {
    asyncResultCore(() => count(name)(actionResult(body).runToFuture))
  }

  private def asyncResultCore(body: () => CancelableFuture[ActionResult]): AsyncResult = {
    new AsyncResult() { override val is = body() }
  }

  private def actionResult(body: HttpServletRequest => HttpServletResponse => Task[ActionResult])(implicit request: HttpServletRequest, response: HttpServletResponse): Task[ActionResult] = {
    for {
      _ <- Task.delay(logRequestInfo)
      res <- Task.defer(body(request)(response))
        .onErrorHandle {
          case FailedExtractionException(_, rawBody, e) =>
            val msg = s"Couldn't parse [$rawBody] due to exception=${e.getClass.getCanonicalName} message=${e.getMessage}"
            logger.error(msg)
            BadRequest(NOK.parsingError(msg))
        }
        .onErrorHandle { e =>
          val name = e.getClass.getCanonicalName
          val cause = Try(e.getCause.getMessage).getOrElse(e.getMessage)
          logger.error("Error 0.1 ", e)
          logger.error("Error 0.1 exception={} message={}", name, cause)
          InternalServerError(NOK.serverError("Sorry, something happened"))

        }
    } yield {
      res
    }

  }

  private def logRequestInfo(implicit request: HttpServletRequest): Unit = {
    val path = request.getPathInfo
    val method = request.getMethod
    val headers = request.headers.toList.map { case (k, v) => k + ":" + v }.mkString(",")
    logger.info("Path[{}]:{} {}", method, path, headers)
  }

  case class ReadBody[T](extracted: T, asString: String)
  case class FailedExtractionException(message: String, body: String, throwable: Throwable) extends Exception(message, throwable) with NoStackTrace

  object ReadBody {

    def getBodyAsBytes(implicit request: HttpServletRequest): Try[(Array[Byte], String)] = for {
      bytes <- Try(IOUtils.toByteArray(request.getInputStream))
      bytesAsString <- Try(new String(bytes, StandardCharsets.UTF_8))
    } yield (bytes, bytesAsString)

    def getBodyAsString(implicit request: HttpServletRequest): Try[String] = Try(request.body)

    def store(bytes: Array[Byte]): Unit = {
      val date = new Date()
      val os = new FileOutputStream(s"src/main/scala/com/ubirch/curl/data_${date.getTime}.mpack")
      os.write(bytes)
      os.close()
    }

    def readJson[T: Manifest](transformF: JValue => JValue)(implicit request: HttpServletRequest): ReadBody[T] = {
      lazy val _body = getBodyAsString
      val parsed = for {
        body <- _body
        _ = logger.info("body={}", body)
        b <- Try(transformF(parsedBody).extract[T])
      } yield ReadBody(b, body)

      parsed.recover { case e => throw FailedExtractionException("Error Parsing", _body.getOrElse("No Body Found"), e) }.get
    }

  }

}
