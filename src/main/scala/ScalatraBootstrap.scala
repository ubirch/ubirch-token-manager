import com.ubirch.Service
import com.ubirch.controllers.{InfoController, ResourcesController, TokenController}
import javax.servlet.ServletContext
import org.scalatra.LifeCycle

/**
  * Represents the configuration of controllers
  */
class ScalatraBootstrap extends LifeCycle {

  lazy val infoController: InfoController = Service.get[InfoController]
  lazy val tokenController: TokenController = Service.get[TokenController]
  lazy val resourceController: ResourcesController = Service.get[ResourcesController]

  override def init(context: ServletContext) {

    context.setInitParameter("org.scalatra.cors.preflightMaxAge", "5")
    context.setInitParameter("org.scalatra.cors.allowCredentials", "false")

    context.mount(
      handler = infoController,
      urlPattern = "/",
      name = "Info"
    )
    context.mount(
      handler = tokenController,
      urlPattern = "/api/tokens",
      name = "Tokens"
    )
    context.mount(
      handler = resourceController,
      urlPattern = "/api-docs",
      name = "Resources"
    )
  }
}

