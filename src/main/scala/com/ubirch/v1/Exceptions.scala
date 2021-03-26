package com.ubirch.v1

import com.ubirch.v1.models.TokenClaim

import scala.util.control.NoStackTrace

abstract class ServiceException(message: String) extends Exception(message) with NoStackTrace {
  val name: String = this.getClass.getCanonicalName
}

case class NoContactPointsException(message: String) extends ServiceException(message)
case class NoKeyspaceException(message: String) extends ServiceException(message)
case class InvalidConsistencyLevel(message: String) extends ServiceException(message)
case class InvalidContactPointsException(message: String) extends ServiceException(message)
case class StoringException(message: String, reason: String) extends ServiceException(message)
case class DeletingException(message: String, reason: String) extends ServiceException(message)

case class TokenEncodingException(message: String, tokenClaim: TokenClaim) extends ServiceException(message)

case class InvalidOtherClaims(message: String, value: String) extends ServiceException(message)
case class InvalidAllClaims(message: String, value: String) extends ServiceException(message)
case class InvalidSpecificClaim(message: String, value: String) extends ServiceException(message)
case class InvalidParamException(message: String, reason: String) extends ServiceException(message)
