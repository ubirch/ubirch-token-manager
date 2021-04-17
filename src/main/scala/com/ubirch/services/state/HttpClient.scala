package com.ubirch.services.state

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.models.ExternalResponseData
import com.ubirch.services.lifeCycle.Lifecycle
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.{ CloseableHttpClient, HttpClients }
import org.apache.http.util.EntityUtils
import javax.inject.{ Inject, Singleton }

import scala.concurrent.Future

trait HttpClient {
  def execute(request: HttpUriRequest): ExternalResponseData[Array[Byte]]
}

@Singleton
class DefaultHttpClient @Inject() (lifecycle: Lifecycle) extends HttpClient with LazyLogging {

  private val httpclient: CloseableHttpClient = HttpClients.createDefault

  def execute(request: HttpUriRequest): ExternalResponseData[Array[Byte]] = {
    httpclient.execute(request, (httpResponse: HttpResponse) =>
      ExternalResponseData(
        httpResponse.getStatusLine.getStatusCode,
        httpResponse.getAllHeaders.map(x => (x.getName, List(x.getValue))).toMap,
        EntityUtils.toByteArray(httpResponse.getEntity)
      ))
  }

  lifecycle.addStopHook { () =>
    logger.info("Shutting Http Client Service")
    Future.successful(httpclient.close())
  }

}
