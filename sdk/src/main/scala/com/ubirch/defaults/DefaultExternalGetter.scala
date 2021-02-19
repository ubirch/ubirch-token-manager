package com.ubirch.defaults

import com.typesafe.config.Config
import com.ubirch.api.{ ExternalResponseData, ExternalStateGetter }
import com.ubirch.utils.Paths
import org.apache.http.HttpResponse
import org.apache.http.client.methods.{ HttpPost, HttpUriRequest }
import org.apache.http.impl.client.{ CloseableHttpClient, HttpClients }
import org.apache.http.util.EntityUtils
import javax.inject.{ Inject, Singleton }

import org.apache.http.entity.ByteArrayEntity

@Singleton
class DefaultExternalGetter @Inject() (config: Config) extends ExternalStateGetter {

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

  override def verify(body: Array[Byte]): ExternalResponseData[Array[Byte]] = {
    val req = new HttpPost(TOKEN_MANAGER_ENDPOINT + "/api/tokens/v1/verify")
    req.setHeader("Content-Type", "application/json")
    req.setEntity(new ByteArrayEntity(body))
    execute(req)
  }

  sys.addShutdownHook(httpclient.close())

}
