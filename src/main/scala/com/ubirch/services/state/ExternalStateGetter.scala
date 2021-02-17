package com.ubirch.services.state

import com.typesafe.config.Config
import com.ubirch.ConfPaths.ExternalStateGetterPaths
import com.ubirch.models.ExternalResponseData
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{ CloseableHttpClient, HttpClients }
import org.apache.http.util.EntityUtils

import javax.inject.{ Inject, Singleton }

trait ExternalStateGetter {
  def getDeviceGroups(accessToken: String): ExternalResponseData[Array[Byte]]
}

@Singleton
class DefaultExternalGetter @Inject() (config: Config) extends ExternalStateGetter {

  private val httpclient: CloseableHttpClient = HttpClients.createDefault

  private final val DEVICE_GROUPS_ENDPOINT: String = config.getString(ExternalStateGetterPaths.DEVICE_GROUPS_ENDPOINT)

  override def getDeviceGroups(accessToken: String): ExternalResponseData[Array[Byte]] = {

    val req = new HttpGet(DEVICE_GROUPS_ENDPOINT)
    req.setHeader("Content-Type", "application/json")
    req.setHeader("Authorization", "bearer " + accessToken)

    httpclient.execute(req, (httpResponse: HttpResponse) =>
      ExternalResponseData(
        httpResponse.getStatusLine.getStatusCode,
        httpResponse.getAllHeaders.map(x => (x.getName, List(x.getValue))).toMap,
        EntityUtils.toByteArray(httpResponse.getEntity)
      ))
  }

  sys.addShutdownHook(httpclient.close())

}
