package com.ubirch.defaults

import javax.inject._

import com.ubirch.api.JsonConverterService
import org.json4s.native.JsonMethods.{ compact, render }
import org.json4s.native.Serialization.{ read, write }
import org.json4s.{ Formats, JValue }

/**
  * Represents a default internal service or component for managing Json.
  * @param formats Represents the json formats used for parsing.
  */
@Singleton
class DefaultJsonConverterService @Inject() (implicit formats: Formats) extends JsonConverterService {

  override def toString(value: JValue): String = compact(render(value))

  override def toString[T](t: T): Either[Exception, String] = {
    try {
      Right(write[T](t))
    } catch {
      case e: Exception =>
        Left(e)
    }
  }

  override def toJValue(value: String): Either[Exception, JValue] = {
    try {
      Right(read[JValue](value))
    } catch {
      case e: Exception =>
        Left(e)
    }
  }

  override def toJValue[T](obj: T): Either[Exception, JValue] = {
    for {
      s <- toString[T](obj)
      jv <- toJValue(s)
    } yield jv
  }

  override def as[T: Manifest](value: String): Either[Exception, T] = {
    try {
      Right(read[T](value))
    } catch {
      case e: Exception =>
        Left(e)
    }
  }

}
