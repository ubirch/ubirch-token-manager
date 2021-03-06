package com.ubirch.defaults

import java.util.Date

import com.typesafe.config.Config
import com.ubirch.api.{ ExternalResponseData, ExternalStateGetter, HMAC }
import org.apache.http.HttpResponse
import org.apache.http.client.methods.{ HttpPost, HttpUriRequest }
import org.apache.http.impl.client.{ CloseableHttpClient, HttpClients }
import org.apache.http.util.EntityUtils

import javax.inject.{ Inject, Singleton }
import org.apache.http.entity.ByteArrayEntity

@Singleton
class DefaultExternalGetter @Inject() (config: Config, HMAC: HMAC) extends ExternalStateGetter {

  private val httpclient: CloseableHttpClient = HttpClients.createDefault

  private final val TOKEN_MANAGER_ENDPOINT: String = config.getString(Paths.TOKEN_SERVICE_PATH)

  def execute(request: HttpUriRequest): ExternalResponseData[Array[Byte]] = {
    httpclient.execute(request, (httpResponse: HttpResponse) =>
      ExternalResponseData(
        httpResponse.getStatusLine.getStatusCode,
        httpResponse.getAllHeaders.map(x => (x.getName, List(x.getValue))).toMap,
        EntityUtils.toByteArray(httpResponse.getEntity)
      ))
  }

  def calculateHmac(body: Array[Byte]): (String, String) = {
    val time = new Date()
    val hmac = HMAC.getHMAC(body, time)
    (time.getTime.toString, hmac)
  }

  override def verify(body: Array[Byte]): ExternalResponseData[Array[Byte]] = {
    val (time, hmac) = calculateHmac(body)
    val req = new HttpPost(TOKEN_MANAGER_ENDPOINT + "/api/tokens/v2/verify")
    req.setHeader("Content-Type", "application/json")
    req.setHeader("X-Ubirch-Timestamp", time)
    req.setHeader("X-Ubirch-Signature", hmac)
    req.setEntity(new ByteArrayEntity(body))
    execute(req)
  }

  sys.addShutdownHook(httpclient.close())

}
