package com.ubirch.defaults

import com.ubirch.TestBase
import java.util.UUID

import scala.util.{ Failure, Success }

class TokenApiSpec extends TestBase {

  "Token API" must {

    "extract claims correctly" in {

      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL25pb21vbi5kZXYudWJpcmNoLmNvbSIsImh0dHBzOi8vdmVyaWZ5LmRldi51YmlyY2guY29tIl0sImV4cCI6NzkyODAwMDg1MiwiaWF0IjoxNjE2NjEwNDUyLCJqdGkiOiJmMTMwNmJhYi0zODIzLTRkYWUtYTIxZS03ZmJiMDFiNmI5ODciLCJzY3AiOlsidGhpbmc6Y3JlYXRlIiwidXBwOmFuY2hvciIsInVwcDp2ZXJpZnkiXSwicHVyIjoiS2luZyBEdWRlIC0gQ29uY2VydCIsInRncCI6W10sInRpZCI6WyI4NDBiN2UyMS0wM2U5LTRkZTctYmIzMS0wYjk1MjRmM2I1MDAiXSwib3JkIjpbImh0dHA6Ly92ZXJpZmljYXRpb24uZGV2LnViaXJjaC5jb20iXX0.0-CA-dhgbRjzWbCjX1e3B08bSiPDbeZfBDb85uJPf3rEuNNH6MeVk0RKt2MVq7DMYco_c5Wolf09wdKX8kRrIA"

      TokenApi.decodeAndVerify(token) match {
        case Success(claims) =>
          assert(claims.subject == "963995ed-ce12-4ea5-89dc-b181701d1d7b")
          assert(claims.audiences.length == 3)
          assert(claims.targetIdentities.isLeft)
          assert(!claims.hasMaybeGroups)
          assert(claims.issuer == "https://token.dev.ubirch.com")
          assert(claims.audiences == List("https://api.console.dev.ubirch.com", "https://niomon.dev.ubirch.com", "https://verify.dev.ubirch.com"))
          assert(claims.scopes == List("thing:create", "upp:anchor", "upp:verify"))
          assert(claims.validateIdentity(UUID.fromString("840b7e21-03e9-4de7-bb31-0b9524f3b500")).isSuccess)
          assert(claims.validateOrigin(Some("http://verification.dev.ubirch.com")).isSuccess)
          assert(claims.validatePurpose.filter(_ == "King Dude - Concert").isSuccess)
          assert(claims.hasScope("thing:create"))
          assert(claims.hasScopes)

        case Failure(exception) => fail(exception)
      }

    }

  }
}
