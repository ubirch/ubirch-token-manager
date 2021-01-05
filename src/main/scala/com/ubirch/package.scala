package com

import com.ubirch.models.TokenClaim

import scala.util.control.NoStackTrace

package object ubirch {

  /**
    * Represents Generic Top Level Exception for the Service System
    * @param message Represents the error message.
    */

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

}
