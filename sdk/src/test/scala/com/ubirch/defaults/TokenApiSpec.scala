package com.ubirch.defaults

import com.ubirch.TestBase

import java.util.UUID
import scala.util.{ Failure, Success }

class TokenApiSpec extends TestBase {

  "Token API" must {

    "extract claims correctly" in {

      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjpbImh0dHBzOi8vYXBpLmNvbnNvbGUuZGV2LnViaXJjaC5jb20iLCJodHRwczovL25pb21vbi5kZXYudWJpcmNoLmNvbSIsImh0dHBzOi8vdmVyaWZ5LmRldi51YmlyY2guY29tIl0sImV4cCI6NzkyNDg2MDQ2MywiaWF0IjoxNjEzNDcwMDYzLCJqdGkiOiIxM2Q5YTQ2NS0wYmUyLTQ4MzktOTUyYy02YWJmYTU4NzI3NGEiLCJwdXJwb3NlIjoiS2luZyBEdWRlIC0gQ29uY2VydCIsInRhcmdldF9pZGVudGl0aWVzIjpbIjg0MGI3ZTIxLTAzZTktNGRlNy1iYjMxLTBiOTUyNGYzYjUwMCJdLCJvcmlnaW5fZG9tYWlucyI6WyJodHRwOi8vdmVyaWZpY2F0aW9uLmRldi51YmlyY2guY29tIl0sInNjb3BlcyI6WyJ0aGluZzpjcmVhdGUiLCJ1cHA6YW5jaG9yIiwidXBwOnZlcmlmeSJdfQ.6Pf63EOJdgrPGbU0XHOnJZP1z4FLLLjUbvqEEj5x5ZrOHx1rIYJkg0AKcx6EO2Th1cAa9FEI7kAw12Rw8m_Drg"

      TokenApi.decodeAndVerify(token) match {
        case Success(claims) =>
          assert(claims.subject == "963995ed-ce12-4ea5-89dc-b181701d1d7b")
          assert(claims.audiences.length == 3)
          assert(claims.validateIdentity(UUID.fromString("840b7e21-03e9-4de7-bb31-0b9524f3b500")).isSuccess)
          assert(claims.validateOrigin(Some("http://verification.dev.ubirch.com")).isSuccess)
          assert(claims.validatePurpose.filter(_ == "King Dude - Concert").isSuccess)

        case Failure(exception: TokenSDKException) => fail(exception)
        case Failure(exception) => fail(exception)
      }

    }

  }
}
