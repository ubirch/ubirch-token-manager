package com.ubirch.util

import java.security.MessageDigest

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.NoCurveException
import com.ubirch.crypto.{ PrivKey, PubKey }
import com.ubirch.crypto.utils.Curve

import scala.util.{ Failure, Success, Try }

object PublicKeyUtil extends LazyLogging {

  final val ECDSA_names = List("ecdsa-p256v1", "ECC_ECDSA", "ECDSA", "SHA256withECDSA", "SHA512withECDSA")
  final val EDDSA_names = List("ed25519-sha-512", "ECC_ED25519", "Ed25519", "1.3.101.112")

  final val ECDSA = ECDSA_names.headOption.getOrElse("CURVE WITH NO NAME")
  final val EDDSA = EDDSA_names.headOption.getOrElse("CURVE WITH NO NAME")

  def associateCurve(algorithm: String): Try[Curve] = {
    algorithm.toLowerCase match {
      case a if ECDSA_names.map(_.toLowerCase).contains(a) => Success(Curve.PRIME256V1)
      case a if EDDSA_names.map(_.toLowerCase).contains(a) => Success(Curve.Ed25519)
      case _ => Failure(NoCurveException(s"No matching curve for $algorithm"))
    }
  }

  def normalize(algorithm: String): Try[String] = {
    algorithm.toLowerCase match {
      case a if ECDSA_names.map(_.toLowerCase).contains(a) => Success("ecdsa-p256v1")
      case a if EDDSA_names.map(_.toLowerCase).contains(a) => Success("ECC_ED25519")
      case _ => Failure(NoCurveException(s"No matching curve for $algorithm"))
    }
  }

  def digestSHA512(privKey: PrivKey, data: Array[Byte]): Array[Byte] = {
    val digest: MessageDigest = MessageDigest.getInstance("SHA-512")
    digest.update(data)
    val dataToSign = digest.digest
    privKey.sign(dataToSign)
  }

  def verifySHA512(pubKey: PubKey, signed: Array[Byte], signature: Array[Byte]): Boolean = {
    val digest: MessageDigest = MessageDigest.getInstance("SHA-512")
    digest.update(signed)
    val dataToVerify = digest.digest
    pubKey.verify(dataToVerify, signature)
  }

}
