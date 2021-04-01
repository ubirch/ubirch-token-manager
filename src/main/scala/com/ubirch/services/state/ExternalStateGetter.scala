package com.ubirch.services.state

import com.typesafe.config.Config
import com.ubirch.ConfPaths.ExternalStateGetterPaths
import com.ubirch.models.ExternalResponseData
import monix.eval.Task
import org.apache.http.client.methods.HttpGet

import javax.inject.{Inject, Singleton}

trait ExternalStateGetter {
  def getDeviceGroups(accessToken: String): Task[ExternalResponseData[Array[Byte]]]
  def getUserGroups(accessToken: String): Task[ExternalResponseData[Array[Byte]]]
}

@Singleton
class DefaultExternalGetter @Inject() (config: Config, httpClient: HttpClient) extends ExternalStateGetter {

  private final val DEVICE_GROUPS_ENDPOINT: String = config.getString(ExternalStateGetterPaths.DEVICE_GROUPS_ENDPOINT)
  private final val TENANT_GROUPS_ENDPOINT: String = config.getString(ExternalStateGetterPaths.TENANT_GROUPS_ENDPOINT)

  override def getDeviceGroups(accessToken: String): Task[ExternalResponseData[Array[Byte]]] = Task.delay {
    val req = new HttpGet(DEVICE_GROUPS_ENDPOINT)
    req.setHeader("Content-Type", "application/json")
    req.setHeader("Authorization", "bearer " + accessToken)
    httpClient.execute(req)
  }

  override def getUserGroups(accessToken: String): Task[ExternalResponseData[Array[Byte]]] = Task.delay {
    val req = new HttpGet(TENANT_GROUPS_ENDPOINT)
    req.setHeader("Content-Type", "application/json")
    req.setHeader("Authorization", "bearer " + accessToken)
    httpClient.execute(req)
  }

}
