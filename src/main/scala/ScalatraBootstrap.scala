import com.ubirch.Service
import com.ubirch.controllers.{ InfoController, ResourcesController, TokenControllerV1, TokenControllerV2 }
import org.scalatra.LifeCycle

import javax.servlet.ServletContext

/**
  * Represents the configuration of controllers
  */
class ScalatraBootstrap extends LifeCycle {

  lazy val infoController: InfoController = Service.get[InfoController]
  lazy val tokenControllerV1: TokenControllerV1 = Service.get[TokenControllerV1]
  lazy val tokenControllerV2: TokenControllerV2 = Service.get[TokenControllerV2]
  lazy val resourceController: ResourcesController = Service.get[ResourcesController]

  override def init(context: ServletContext): Unit = {

    context.setInitParameter("org.scalatra.cors.preflightMaxAge", "5")
    context.setInitParameter("org.scalatra.cors.allowCredentials", "false")

    context.mount(
      handler = infoController,
      urlPattern = "/",
      name = "Info"
    )
    context.mount(
      handler = tokenControllerV1,
      urlPattern = "/api/tokens/v1",
      name = "Tokens V1"
    )
    context.mount(
      handler = tokenControllerV2,
      urlPattern = "/api/tokens/v2",
      name = "Tokens V2"
    )
    context.mount(
      handler = resourceController,
      urlPattern = "/api-docs",
      name = "Resources"
    )
  }
}

