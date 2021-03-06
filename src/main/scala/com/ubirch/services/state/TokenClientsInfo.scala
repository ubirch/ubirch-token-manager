package com.ubirch.services.state

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.services.formats.{ DefaultJsonConverterService, JsonConverterService, JsonFormatsProvider }
import java.io.{ FileNotFoundException, IOException }
import java.nio.file.{ Files, Paths }

import com.ubirch.ConfPaths.TokenClientsPaths
import com.ubirch.services.config.ConfigProvider

import javax.inject._
import scala.annotation.tailrec
import scala.collection.JavaConverters._

case class TokenClient(client: Symbol, secret: String) {
  override def toString: String = " client=" + client.name

  def secretParts: Array[String] = secret.split("-", 2)
  def secretPointer: String = secretParts.headOption.getOrElse("")
  def secretKey: String = secretParts.tail.headOption.getOrElse("")

}

trait TokenClientsInfo {
  val info: List[TokenClient]
}

@Singleton
class DefaultTokenClientsInfo @Inject() (config: Config, jsonConverterService: JsonConverterService) extends TokenClientsInfo with LazyLogging {

  final val TOKEN_SVC_CLIENT_NAME = "TOKEN_SVC_CLIENT_NAME_"
  final val TOKEN_SVC_CLIENT_SECRET = "TOKEN_SVC_CLIENT_SECRET_"

  final val info: List[TokenClient] = {
    lazy val file: String = config.getString(TokenClientsPaths.TOKEN_CLIENTS_FILE_PATH)
    lazy val isFileNull: Boolean = config.getIsNull(TokenClientsPaths.TOKEN_CLIENTS_FILE_PATH)
    if (isFileNull) {
      logger.info("clients_source={}", "ENV")
      loadInfoFromEnv(getEnv)
    } else {
      logger.info("clients_source={}", "FILE")
      loadInfoFromFile(file).map(x => toIdentities(x)).getOrElse(Nil)
    }
  }

  def getEnv: Map[String, String] = sys.env

  private def loadInfoFromEnv(envs: Map[String, String]): List[TokenClient] = {

    def getId(index: Int): Option[TokenClient] = for {
      role <- envs.get(TOKEN_SVC_CLIENT_NAME + index)
      pass <- envs.get(TOKEN_SVC_CLIENT_SECRET + index)
    } yield {
      TokenClient(Symbol(role), pass)
    }

    @tailrec
    def go(index: Int, identities: List[TokenClient]): List[TokenClient] = {
      getId(index) match {
        case Some(value) => go(index + 1, identities ++ List(value))
        case None => identities
      }
    }

    go(1, Nil)

  }

  private def loadInfoFromFile(file: String): Option[String] = {
    try {
      val path = Paths.get(file)

      if (!path.toFile.exists()) {
        throw new FileNotFoundException("file not found " + file)
      }

      val dispatch = Files.readAllLines(path).asScala
      val value = dispatch.mkString(" ")
      Some(value)

    } catch {
      case e: FileNotFoundException =>
        logger.error("Config clients info not found {}", e.getMessage)
        throw e
      case e: IOException =>
        logger.error("Error parsing into token clients info {}", e.getMessage)
        throw e
    }
  }

  private def toIdentities(data: String): List[TokenClient] = {

    jsonConverterService
      .as[List[TokenClient]](data)
      .map(data => {
        data.foreach { d =>
          if (d.client.name.isEmpty && d.client.name.length < 3) throw new IllegalArgumentException("Names can't be empty or has less than three letters")
          if (d.secret.isEmpty && d.secret.length < 3) throw new IllegalArgumentException("Client Secret can't be empty or has less than three letters")
        }
        if (data.isEmpty) throw new IllegalArgumentException("No valid Token Clients were found.")
        data
      })
      .fold(e => {
        logger.error("Error parsing into Clients {}", e.getMessage)
        throw new IllegalArgumentException("Error parsing token clients")
      }, data => data)

  }

}

object DefaultTokenClientsInfo {

  def main(args: Array[String]): Unit = {
    implicit val format = new JsonFormatsProvider().get()
    val di = new DefaultTokenClientsInfo(new ConfigProvider get (), new DefaultJsonConverterService {})
    println("Current token clients: \n" + di.info.zipWithIndex.map { case (d, i) => (i, d) }.mkString(", \n"))

  }
}
