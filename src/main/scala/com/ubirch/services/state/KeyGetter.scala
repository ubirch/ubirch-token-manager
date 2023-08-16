package com.ubirch.services.state

import java.nio.charset.StandardCharsets
import java.util.{ Base64, UUID }

import com.typesafe.config.Config
import com.ubirch.ConfPaths.KeyServicePaths
import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.models.ExternalResponseData
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.execution.{ ExecutionProvider, SchedulerProvider }
import com.ubirch.services.lifeCycle.DefaultLifecycle
import com.ubirch.util.PublicKeyUtil
import monix.eval.Task
import monix.execution.Scheduler
import org.apache.http.client.methods.HttpGet
import javax.inject.{ Inject, Singleton }

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

    val config = new ConfigProvider().get()
    val e = new ExecutionProvider(config).get()
    implicit val scheduler = new SchedulerProvider(e).get()

    val lifecycle = new DefaultLifecycle()
    val client = new DefaultHttpClient(lifecycle)
    val keyGetter = new DefaultKeyGetter(config, client)

    val res = await(keyGetter.byIdentityId(UUID.fromString("d7a81058-ae97-4178-80ed-71aed46e88fa")), 5 seconds)
    println(res)
    println(new String(res.body))
  }

}

object KeySignTest {

  import scala.concurrent.Await
  import scala.concurrent.duration._

  def await[T](task: Task[T], atMost: Duration)(implicit scheduler: Scheduler): T = {
    val future = task.runToFuture
    Await.result(future, atMost)
  }

  def main(args: Array[String]): Unit = {

    val privkey = GeneratorKeyFactory.getPrivKey(Base64.getDecoder.decode("L1qIoAosmLmaOh/W+a3YA4aJnWpoZ69NgRcE650T4hs="), PublicKeyUtil.associateCurve("ECC_ED25519").get)
    val data = PublicKeyUtil.signSHA512(privkey, """{"token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly90b2tlbi5kZXYudWJpcmNoLmNvbSIsImV4cCI6NzkyODg2Mjg2MSwiaWF0IjoxNjE3NDcyNDYxLCJqdGkiOiI4MjBiNjE5Yi0xYmFiLTQzMmUtYmI0OS04YTk4NWYyMzZmZTYiLCJzY3AiOlsidGhpbmc6Ym9vdHN0cmFwIl0sInB1ciI6IktpdGNoZW5fQ2FybG9zIiwidGdwIjpbImQ2ZTUyNWMwLTQxZTItNGE3Ny05MjVjLTRkNmVhNGZiODQzMSJdLCJ0aWQiOltdLCJvcmQiOltdfQ.WJhq1ldCYBmzBIg_UeuZsTXALCfwFl5pLPh3s4NakDEN4ZGEuEjejxvR_8d5PaEz4Bjgv6J0ZGU9qy-OyhrAvQ","identity":"d7a81058-ae97-4178-80ed-71aed46e88fa"}""".getBytes(StandardCharsets.UTF_8))

    println(Base64.getEncoder.encodeToString(data))

  }

}
