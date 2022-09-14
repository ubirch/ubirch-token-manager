package com.ubirch.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object HashUtils {
  def sha256WithSalt(string: String, salt: Array[Byte]): String = {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(salt)
    md.digest(string.getBytes(StandardCharsets.UTF_8)).map("%02x".format(_)).mkString
  }
}
