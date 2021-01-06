package com.ubirch.controllers

import com.typesafe.config.Config
import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.controllers.concerns.{ ControllerBase, SwaggerElements }
import com.ubirch.models.{ Good, NOK }
import io.prometheus.client.Counter
import javax.inject._
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.{ Swagger, SwaggerSupportSyntax }

import scala.concurrent.ExecutionContext

/**
  * Represents a simple controller for the base path "/"
  * @param swagger Represents the Swagger Engine.
  * @param jFormats Represents the json formats for the system.
  */

@Singleton
class InfoController @Inject() (config: Config, val swagger: Swagger, jFormats: Formats)(implicit val executor: ExecutionContext, scheduler: Scheduler)
  extends ControllerBase {

  override protected val applicationDescription = "Info Controller"
  override protected implicit def jsonFormats: Formats = jFormats

  val service: String = config.getString(GenericConfPaths.NAME)

  val successCounter: Counter = Counter.build()
    .name("info_management_success")
    .help("Represents the number of info management successes")
    .labelNames("service", "method")
    .register()

  val errorCounter: Counter = Counter.build()
    .name("info_management_failures")
    .help("Represents the number of info management failures")
    .labelNames("service", "method")
    .register()

  val getSimpleCheck: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[String]("simpleCheck")
      summary "Welcome"
      description "Getting a hello from the system"
      tags SwaggerElements.TAG_WELCOME)

  get("/hola", operation(getSimpleCheck)) {
    asyncResult("hola") { _ => _ =>
      Task(hello)
    }
  }

  get("/hello", operation(getSimpleCheck)) {
    asyncResult("hello") { _ => _ =>
      Task(hello)
    }
  }

  get("/ping", operation(getSimpleCheck)) {
    asyncResult("ping") { _ => _ =>
      Task {
        Ok("pong")
      }
    }
  }

  get("/", operation(getSimpleCheck)) {
    asyncResult("root") { _ => _ =>
      Task {
        Ok(Good("Hallo, Hola, Hello, Salut, Hej, this is the Ubirch Token Manager."))
      }
    }
  }

  before() {
    contentType = formats("json")
  }

  notFound {
    asyncResult("not_found") { _ => _ =>
      Task {
        logger.info("controller=InfoController route_not_found={} query_string={}", requestPath, request.getQueryString)
        NotFound(NOK.noRouteFound(requestPath + " might exist in another universe"))
      }
    }
  }

  private def hello: ActionResult = {
    contentType = formats("txt")
    val data =
      """
        |From: spunk1111@aol.com (Spunk1111)
        |Newsgroups: alt.ascii-art
        |Subject: [PIC]   elephant...
        |Date: 24 Aug 1997 04:38:32 GMT
        |
        |              ___.-~"~-._   __....__
        |            .'    `    \ ~"~        ``-.
        |           /` _      )  `\              `\
        |          /`  a)    /     |               `\
        |         :`        /      |                 \
        |    <`-._|`  .-.  (      /   .            `;\\
        |     `-. `--'_.'-.;\___/'   .      .       | \\
        |  _     /:--`     |        /     /        .'  \\
        | ("\   /`/        |       '     '         /    :`;
        | `\'\_/`/         .\     /`~`=-.:        /     ``
        |   `._.'          /`\    |      `\      /(
        |                 /  /\   |        `Y   /  \
        |           jgs  J  /  Y  |         |  /`\  \
        |               /  |   |  |         |  |  |  |
        |              "---"  /___|        /___|  /__|
        |                     '\"\"\"         '\"\"\"  '\"\"\"
        |
        |------------------------------------------------
        |Thank you for visiting https://asciiart.website/
        |This ASCII pic can be found at
        |https://asciiart.website/index.php?art=animals/elephants
        |""".stripMargin
    Ok(data)
  }

}
