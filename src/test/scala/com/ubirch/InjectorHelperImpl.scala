package com.ubirch
import java.security.Key

import com.google.inject.binder.ScopedBindingBuilder
import com.typesafe.config.Config
import com.ubirch.crypto.utils.Curve
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey }
import com.ubirch.services.jwt.{ DefaultPublicKeyPoolService, PublicKeyDiscoveryService, PublicKeyPoolService, TokenCreationService }
import javax.inject.{ Inject, Provider, Singleton }
import monix.eval.Task

@Singleton
class FakeDefaultPublicKeyPoolService @Inject() (privKey: PrivKey, config: Config, publicKeyDiscoveryService: PublicKeyDiscoveryService)
  extends DefaultPublicKeyPoolService(config, publicKeyDiscoveryService) {

  override def getKeyFromDiscoveryService(kid: String): Task[Option[Key]] = Task {
    kid match {
      case "PSJ-ZQWx9EPztQowhNbET0rZwTYraqi6uDbxJwy4n3E" => Some(privKey.getPublicKey)
    }
  }
}

@Singleton
class KeyPairProvider extends Provider[PrivKey] {
  val privKey = GeneratorKeyFactory.getPrivKey(Curve.PRIME256V1)
  override def get(): PrivKey = privKey
}

case class FakeToken(value: String) {
  def prepare: String = "bearer " + value
}

object FakeToken {

  val header =
    """
      |{
      |  "alg": "ES256",
      |  "typ": "JWT",
      |  "kid": "PSJ-ZQWx9EPztQowhNbET0rZwTYraqi6uDbxJwy4n3E"
      |}""".stripMargin

  val admin =
    """
      |{
      |  "exp": 1718338181,
      |  "iat": 1604336381,
      |  "jti": "2fb1c61d-2113-4b8e-9432-97c28c697b98",
      |  "iss": "https://id.dev.ubirch.com/auth/realms/ubirch-default-realm",
      |  "aud": "account",
      |  "sub": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      |  "typ": "Bearer",
      |  "azp": "ubirch-2.0-user-access",
      |  "session_state": "f334122a-4693-4826-a2c0-546391886eda",
      |  "acr": "1",
      |  "allowed-origins": [
      |    "http://localhost:9101",
      |    "https://console.dev.ubirch.com"
      |  ],
      |  "realm_access": {
      |    "roles": [
      |      "offline_access",
      |      "ADMIN",
      |      "uma_authorization",
      |      "USER"
      |    ]
      |  },
      |  "resource_access": {
      |    "account": {
      |      "roles": [
      |        "manage-account",
      |        "manage-account-links",
      |        "view-profile"
      |      ]
      |    }
      |  },
      |  "scope": "fav_color profile email",
      |  "email_verified": true,
      |  "fav_fruit": [
      |    "/OWN_DEVICES_carlos.sanchez@ubirch.com"
      |  ],
      |  "name": "Carlos Sanchez",
      |  "preferred_username": "carlos.sanchez@ubirch.com",
      |  "given_name": "Carlos",
      |  "family_name": "Sanchez",
      |  "email": "carlos.sanchez@ubirch.com"
      |}""".stripMargin

  val user =
    """
      |{
      |  "exp": 1718338181,
      |  "iat": 1604336381,
      |  "jti": "2fb1c61d-2113-4b8e-9432-97c28c697b98",
      |  "iss": "https://id.dev.ubirch.com/auth/realms/ubirch-default-realm",
      |  "aud": "account",
      |  "sub": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      |  "typ": "Bearer",
      |  "azp": "ubirch-2.0-user-access",
      |  "session_state": "f334122a-4693-4826-a2c0-546391886eda",
      |  "acr": "1",
      |  "allowed-origins": [
      |    "http://localhost:9101",
      |    "https://console.dev.ubirch.com"
      |  ],
      |  "realm_access": {
      |    "roles": [
      |      "offline_access",
      |      "uma_authorization",
      |      "USER"
      |    ]
      |  },
      |  "resource_access": {
      |    "account": {
      |      "roles": [
      |        "manage-account",
      |        "manage-account-links",
      |        "view-profile"
      |      ]
      |    }
      |  },
      |  "scope": "fav_color profile email",
      |  "email_verified": true,
      |  "fav_fruit": [
      |    "/OWN_DEVICES_carlos.sanchez@ubirch.com"
      |  ],
      |  "name": "Carlos Sanchez",
      |  "preferred_username": "carlos.sanchez@ubirch.com",
      |  "given_name": "Carlos",
      |  "family_name": "Sanchez",
      |  "email": "carlos.sanchez@ubirch.com"
      |}""".stripMargin

}

@Singleton
class FakeTokenCreator @Inject() (val privKey: PrivKey, tokenCreationService: TokenCreationService) {

  def fakeToken(header: String, token: String): FakeToken = {
    FakeToken(
      tokenCreationService.encode(header, token, privKey)
        .getOrElse(throw new Exception("Error Creating Token"))
    )
  }

  val user: FakeToken = fakeToken(FakeToken.header, FakeToken.user)
  val admin: FakeToken = fakeToken(FakeToken.header, FakeToken.admin)

}

class InjectorHelperImpl() extends InjectorHelper(List(new Binder {
  override def PublicKeyPoolService: ScopedBindingBuilder = {
    bind(classOf[PublicKeyPoolService]).to(classOf[FakeDefaultPublicKeyPoolService])
  }

  override def configure(): Unit = {
    super.configure()
    bind(classOf[PrivKey]).toProvider(classOf[KeyPairProvider])
  }
}))
