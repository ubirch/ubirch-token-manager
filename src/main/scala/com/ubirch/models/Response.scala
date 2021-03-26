package com.ubirch.models

/**
  * Represents a simple Response object. Used for HTTP responses.
  */
abstract class Response[T] {
  val version: Symbol
  val ok: T
}
object Response {
  final val v1 = Symbol("1.0")
  final val v2 = Symbol("2.0")
}

trait WithVersion {
  val version: Symbol
}

/**
  *  Represents an Error Response.
  * @param version the version of the response
  * @param ok the status of the response. true or false
  * @param errorType the error type
  * @param errorMessage the message for the response
  */
case class NOK(version: Symbol, ok: Boolean, errorType: Symbol, errorMessage: String) extends Response[Boolean]

/**
  * Companion object for the NOK response
  */
object NOK {

  final val SERVER_ERROR = 'ServerError
  final val PARSING_ERROR = 'ParsingError
  final val NO_ROUTE_FOUND_ERROR = 'NoRouteFound
  final val DELETE_ERROR = 'TokenDeleteError
  final val TOKEN_CREATION_ERROR = 'TokenCreationError
  final val TOKEN_LISTING_ERROR = 'TokenListingError
  final val AUTHENTICATION_ERROR = 'AuthenticationError

  def apply(version: Symbol, errorType: Symbol, errorMessage: String): NOK = new NOK(version, ok = false, errorType, errorMessage)

  def serverError(version: Symbol, errorMessage: String): NOK = NOK(version, SERVER_ERROR, errorMessage)
  def parsingError(version: Symbol, errorMessage: String): NOK = NOK(version, PARSING_ERROR, errorMessage)
  def noRouteFound(version: Symbol, errorMessage: String): NOK = NOK(version, NO_ROUTE_FOUND_ERROR, errorMessage)
  def tokenDeleteError(version: Symbol, errorMessage: String): NOK = NOK(version, DELETE_ERROR, errorMessage)
  def tokenCreationError(version: Symbol, errorMessage: String): NOK = NOK(version, TOKEN_CREATION_ERROR, errorMessage)
  def tokenListingError(version: Symbol, errorMessage: String): NOK = NOK(version, TOKEN_LISTING_ERROR, errorMessage)
  def authenticationError(version: Symbol, errorMessage: String): NOK = NOK(version, AUTHENTICATION_ERROR, errorMessage)

}

case class Good(version: Symbol, ok: Boolean, data: Any) extends Response[Boolean]
object Good {
  def apply(version: Symbol, data: Any): Good = new Good(version, ok = true, data)
}

