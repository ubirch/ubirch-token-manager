package com.ubirch.controllers

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ ApiInfo, ContactInfo, LicenseInfo, NativeSwaggerBase, Swagger }

import javax.inject._

/**
  *  Represents the Resource Controller that allows to serve public files: The Swagger UI.
  * @param swagger Represents the Swagger Engine.
  */
@Singleton
class ResourcesController @Inject() (val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

object RestApiInfo extends ApiInfo(
  "Token Manager",
  "These are the available endpoints for querying the Token Service. For more information drop me an email at carlos.sanchez at ubirch.com",
  "https://github.com/ubirch/ubirch-token-manager",
  ContactInfo("carlos sanchez", "https://ubirch.de", "carlos.sanchez@ubirch.com"),
  LicenseInfo("Apache License, Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0")
)
