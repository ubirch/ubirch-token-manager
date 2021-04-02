package com.ubirch.services.state

import com.ubirch.models.ExternalResponseData
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.{ CloseableHttpClient, HttpClients }
import org.apache.http.util.EntityUtils

import javax.inject.Singleton

trait HttpClient {
  def execute(request: HttpUriRequest): ExternalResponseData[Array[Byte]]
}

@Singleton
class DefaultHttpClient extends HttpClient {

  private val httpclient: CloseableHttpClient = HttpClients.createDefault

  def execute(request: HttpUriRequest): ExternalResponseData[Array[Byte]] = {
    httpclient.execute(request, (httpResponse: HttpResponse) =>
      ExternalResponseData(
        httpResponse.getStatusLine.getStatusCode,
        httpResponse.getAllHeaders.map(x => (x.getName, List(x.getValue))).toMap,
        EntityUtils.toByteArray(httpResponse.getEntity)
      ))
  }

  sys.addShutdownHook(httpclient.close())

}
