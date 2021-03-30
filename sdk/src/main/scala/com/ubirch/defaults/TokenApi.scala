package com.ubirch.defaults

import com.ubirch.api.{ ExternalStateVerifier, TokenVerification }

object TokenApi extends TokenManagerImpl with Binding {
  override val tokenVerification: TokenVerification = injector.get[TokenVerification]
  override val externalStateVerifier: ExternalStateVerifier = injector.get[ExternalStateVerifier]
}
