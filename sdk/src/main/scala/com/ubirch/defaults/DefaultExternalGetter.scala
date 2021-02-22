package com.ubirch.defaults

import java.nio.charset.StandardCharsets
import java.util.Date

import com.typesafe.config.Config
import com.ubirch.api.{ ExternalResponseData, ExternalStateGetter, HMAC }
import org.apache.http.HttpResponse
import org.apache.http.client.methods.{ HttpPost, HttpUriRequest }
import org.apache.http.impl.client.{ CloseableHttpClient, HttpClients }
import org.apache.http.util.EntityUtils

import javax.inject.{ Inject, Singleton }
import org.apache.http.entity.ByteArrayEntity
import org.bouncycastle.util.Arrays

@Singleton
class DefaultExternalGetter @Inject() (config: Config, HMAC: HMAC) extends ExternalStateGetter {

  private val httpclient: CloseableHttpClient = HttpClients.createDefault

  private final val TOKEN_MANAGER_ENDPOINT: String = config.getString(Paths.VALID_ISSUER_PATH)

  def execute(request: HttpUriRequest): ExternalResponseData[Array[Byte]] = {
    httpclient.execute(request, (httpResponse: HttpResponse) =>
      ExternalResponseData(
        httpResponse.getStatusLine.getStatusCode,
        httpResponse.getAllHeaders.map(x => (x.getName, List(x.getValue))).toMap,
        EntityUtils.toByteArray(httpResponse.getEntity)
      ))
  }

  def calculateHmac(body: Array[Byte]): (String, String) = {
    val time = new Date().getTime.toString
    val hmac = HMAC.getHMAC(Arrays.concatenate(body, time.getBytes(StandardCharsets.UTF_8)))
    (time, hmac)
  }

  override def verify(body: Array[Byte]): ExternalResponseData[Array[Byte]] = {
    val (time, hmac) = calculateHmac(body)
    val req = new HttpPost(TOKEN_MANAGER_ENDPOINT + "/api/tokens/v1/verify")
    req.setHeader("Content-Type", "application/json")
    req.setHeader("X-Ubirch-Timestamp", time)
    req.setHeader("X-Ubirch-Signature", hmac)
    req.setEntity(new ByteArrayEntity(body))
    execute(req)
  }

  sys.addShutdownHook(httpclient.close())

}
