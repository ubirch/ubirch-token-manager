package com.ubirch

import com.ubirch.models.TokenClaim

import scala.util.control.NoStackTrace

abstract class ServiceException(message: String) extends Exception(message) with NoStackTrace {
  val name: String = this.getClass.getCanonicalName
  def getReason: String = message
}

case class NoContactPointsException(message: String) extends ServiceException(message)
case class NoKeyspaceException(message: String) extends ServiceException(message)
case class InvalidConsistencyLevel(message: String) extends ServiceException(message)
case class InvalidContactPointsException(message: String) extends ServiceException(message)
case class StoringException(message: String, reason: String) extends ServiceException(message) {
  override def getReason: String = reason
}
case class DeletingException(message: String, reason: String) extends ServiceException(message) {
  override def getReason: String = reason
}
case class TokenEncodingException(message: String, tokenClaim: TokenClaim) extends ServiceException(message)
case class InvalidClaimException(message: String, reason: String) extends ServiceException(message) {
  override def getReason: String = reason
}
case class InvalidParamException(message: String, reason: String) extends ServiceException(message) {
  override def getReason: String = reason
}
case class StateVerifierException(message: String) extends ServiceException(message)
case class NoCurveException(message: String) extends ServiceException(message)
