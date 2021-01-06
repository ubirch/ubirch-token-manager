package com

package object ubirch {

  def closableTry[A, B](resource: => A)(cleanup: A => Unit)(code: A => B): Either[Exception, B] = {
    try {
      val r = resource
      try { Right(code(r)) } finally { cleanup(r) }
    } catch { case e: Exception => Left(e) }
  }

}
