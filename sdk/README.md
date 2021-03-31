# A Light SDK

In order to facilitate the integration of some of the most important functions of the Token Manager when verifying Token in other Services, a light SDK has been included in the project.

## The interface offers these basic operations:

* **decodeAndVerify** ->  It allows a basic verification. It verifies that the token is well-built, that its standard claims are checked. I
* **getClaims** -> It the same as the previous operation but basically performing the verification on the header as it is.
* **externalStateVerify**: -> Depending on the kinds of claims, there are some that require an external verification, these operations starts a verification against the Token Manager. Useful for groups and revocation claims.

        <dependency>
            <groupId>com.ubirch</groupId>
            <artifactId>ubirch-token-sdk</artifactId>
            <version>0.6.5-SNAPSHOT</version>
        </dependency>

Note that every system that might use the Light SDK and that performs externalities has to be explicitly known to the token manager. Every client has to configure their secret.

## Configurations Example:

```
token {
  env: "dev"
  tokenPublicKey: "2e09fc73de8b067c4c38292e8d683ed3abaef220c9b8a6b85935a055359139a70f17b2e76543518a113fba84863db6060bb0224fc45104ca0ac8a8279b0d744a"
  issuer: "https://token."${token.env}".ubirch.com"
  audience: "https://verify."${token.env}".ubirch.com"
  tokenManager: token.issuer
  scopes: ["thing:create"],
  secret: "judgDg3jaCDM-QwKFZpTiEcXnLdbbGEdzcO57/yoIVGOpixXfeKGLDdg="
}
```
