package com.ubirch.models

case class ExternalResponseData[T](status: Int, headers: Map[String, List[String]], body: T)
