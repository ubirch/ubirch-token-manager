package com.ubirch.controllers

import javax.inject._
import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, NativeSwaggerBase, Swagger}

/**
  *  Represents the Resource Controller that allows to serve public files: The Swagger UI.
  * @param swagger Represents the Swagger Engine.
  */
@Singleton
class ResourcesController @Inject() (val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

object RestApiInfo extends ApiInfo(
  "Token Manager",
  "These are the available endpoints for querying the Token Service. For more information drop me an email at carlos.sanchez at ubirch.com",
  "https://ubirch.de",
  "carlos.sanchez@ubirch.com",
  "Apache License, Version 2.0",
  "https://www.apache.org/licenses/LICENSE-2.0"
)
