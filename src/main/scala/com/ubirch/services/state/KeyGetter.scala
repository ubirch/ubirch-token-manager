package com.ubirch.services.state

import java.util.UUID

import com.typesafe.config.Config
import com.ubirch.ConfPaths.KeyServicePaths
import com.ubirch.models.ExternalResponseData
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.execution.{ExecutionProvider, SchedulerProvider}
import monix.eval.Task
import monix.execution.Scheduler
import org.apache.http.client.methods.HttpGet

import javax.inject.{Inject, Singleton}

trait KeyGetter {
  def byIdentityId(uuid: UUID): Task[ExternalResponseData[Array[Byte]]]
}

@Singleton
class DefaultKeyGetter @Inject() (config: Config, httpClient: HttpClient) extends KeyGetter {

  private final val DEVICE_GROUPS_ENDPOINT: String = config.getString(KeyServicePaths.KEY_BY_IDENTITY_ENDPOINT)

  override def byIdentityId(uuid: UUID): Task[ExternalResponseData[Array[Byte]]] = Task.delay {
    val req = new HttpGet(DEVICE_GROUPS_ENDPOINT + "/" + uuid.toString)
    req.setHeader("Content-Type", "application/json")
    httpClient.execute(req)
  }

}

object KeyGetterTest {

  import scala.concurrent.Await
  import scala.concurrent.duration._
  import scala.language.postfixOps

  def await[T](task: Task[T], atMost: Duration)(implicit scheduler: Scheduler): T = {
    val future = task.runToFuture
    Await.result(future, atMost)
  }

  def main(args: Array[String]): Unit = {

    val config = new ConfigProvider get()
    val e = new ExecutionProvider(config) get ()
    implicit val scheduler = new SchedulerProvider(e) get ()

    val client = new DefaultHttpClient()
    val keyGetter = new DefaultKeyGetter(config, client)


    val res = await(keyGetter.byIdentityId(UUID.fromString("9011a2de-8c69-45be-bc47-60fd58e121ce")), 5 seconds)
    println(res)
    println(new String(res.body))
  }


}
