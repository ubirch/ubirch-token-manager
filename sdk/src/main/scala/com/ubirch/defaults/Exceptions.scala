package com.ubirch.defaults

class TokenSDKException(message: String, value: String) extends Exception(message) {
  def getValue: String = value
}

case class InvalidOtherClaims(message: String, value: String) extends TokenSDKException(message, value)
case class InvalidAllClaims(message: String, value: String) extends TokenSDKException(message, value)
case class InvalidSpecificClaim(message: String, value: String) extends TokenSDKException(message, value)
case class InvalidUUID(message: String, value: String) extends TokenSDKException(message, value)
case class InvalidOrigin(message: String, value: String) extends TokenSDKException(message, value)
case class InvalidToken(message: String, value: String) extends TokenSDKException(message, value)
