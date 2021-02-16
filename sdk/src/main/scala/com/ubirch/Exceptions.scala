package com.ubirch

case class InvalidOtherClaims(message: String, value: String) extends Exception(message)
case class InvalidAllClaims(message: String, value: String) extends Exception(message)
case class InvalidSpecificClaim(message: String, value: String) extends Exception(message)
case class InvalidUUID(message: String, value: String) extends Exception(message)
case class InvalidOrigin(message: String, value: String) extends Exception(message)
