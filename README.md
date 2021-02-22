# Ubirch Token Manager

This service knows about jwt tokens.

1. [Getting Started](#steps-to-prepare-a-request)
2. [Token Claim Object](#token-claim-object)
3. [Create Verification Token for Devices](#create-a-verification-token-for-specific-devices)
4. [Create Verification Token with Wildcard](#create-a-verification-token-for-specific-devices)
5. [List Your Tokens](#list-your-tokens)
6. [Delete A Token](#delete-a-token)
7. [Available Scopes](#available-scopes)
8. [Verification Token Claims](#verification-token-claims)
9. [Keycloak and Responses](#keycloak-token-and-responses)
10. [Verifying an Ubirch JWT Token (JWK)](#verifying-an-ubirch-jwt-token)
11. [A Light SDK](#a-light-sdk)    
12. [Swagger](#swagger)
13. [Some Workflows](#some-workflows)

## Steps to prepare a request

1. Get your keycloak token.
2. Prepare the data object - when needed (creation).
3. Prepare the request and send.
4. Have fun.

## Token Claim Object

![Data Model](DataModel.png)

**Fields**

_`tenantId`_: it is the keycloak id of the logged in user.
 
_`purpose`_: it is a description for the token.  Min characters are 6

_`targetIdentities`_: it is a list of device ids that belong to the user. It supports a list of specific devices or the wildcard *.
If it is meant as wildcard, the field should be sent as `["*"]`.

_`targetGroups`_: it is a list of uuids that correspond to identity groups or a list of the names of the groups.

_`expiration`_: the number of seconds after which the token will be considered expired.
That is to say: 'X seconds from now', where X == expiration AND now == the current time calculated on the server.
If not set, it will not expire.

_`notBefore`_: the number of seconds after which the token should be considered valid. 
That is to say: 'X seconds from now', where X == notBefore AND now == the current time calculated on the server.

_`originDomains`_: list of domains from which the calls will be accepted from when verifying.

_`scopes`_: list of available scopes: "upp:anchor", "upp:verify", "thing:create", "thing:getinfo", "user:getinfo"

**Mandatory Fields**

* tenantId (uuid as string)
* purpose (string) :: min characters are 6
* targetIdentities (array of uuid as string) | (["*"])
* targetGroups (array of uuids as string) | (array of group names)
* originDomains (array of urls from which a verification can originate)
* scopes (array of at least one scope)

**Option Fields** 

* expiration (number or null) in seconds
* notBefore (number of null) in seconds

Set as null or don't send the fields.

NOTE: Don't send `targetIdentities` and `targetGroups`. Send only one at a time.  

## Create a Verification Token for Specific Devices. 

#### Keycloak Token

```json
token=`curl -s -d "client_id=ubirch-2.0-user-access" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASS" -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID" $keycloak | jq -r .access_token`
```

#### Data object

```json
{
  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
  "purpose":"King Dude - Concert",
  "targetIdentities":["840b7e21-03e9-4de7-bb31-0b9524f3b500"],
  "expiration": 6311390400,
  "notBefore":null,
  "originDomains": ["http://verification.dev.ubirch.com"],
  "scopes": ["upp:verify"]
}

```

#### Post Request

```shell script
curl -s -X POST \
    -H "authorization: bearer ${token}" \
    -H "content-type: application/json" \
    -d @createVerificationToken.json \
    "${host}/api/tokens/v1/create" | jq .
```

#### Post Response

```json
{
  "version": "1.0",
  "ok": true,
  "data": {
    "id": "407f29ab-ac74-42c7-81e9-107a53f3de36",
    "jwtClaim": {
      "content": "{\"purpose\":\"King Dude - Concert\",\"target_identities\":[\"840b7e21-03e9-4de7-bb31-0b9524f3b500\"],\"origin_domains\":[\"http://verification.dev.ubirch.com\"],\"scopes\":[\"upp:verify\"]}",
      "issuer": "https://token.dev.ubirch.com",
      "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "audience": [
        "https://verify.dev.ubirch.com"
      ],
      "expiration": 7924869463,
      "issuedAt": 1613479063,
      "jwtId": "407f29ab-ac74-42c7-81e9-107a53f3de36"
    },
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MjQ4Njk0NjMsImlhdCI6MTYxMzQ3OTA2MywianRpIjoiNDA3ZjI5YWItYWM3NC00MmM3LTgxZTktMTA3YTUzZjNkZTM2IiwicHVycG9zZSI6IktpbmcgRHVkZSAtIENvbmNlcnQiLCJ0YXJnZXRfaWRlbnRpdGllcyI6WyI4NDBiN2UyMS0wM2U5LTRkZTctYmIzMS0wYjk1MjRmM2I1MDAiXSwib3JpZ2luX2RvbWFpbnMiOlsiaHR0cDovL3ZlcmlmaWNhdGlvbi5kZXYudWJpcmNoLmNvbSJdLCJzY29wZXMiOlsidXBwOnZlcmlmeSJdfQ.sSelvdqPV82dJTPaLxw6bJAH9XlnO8YK8cG8WPwlQWfhI_pjw0qJX0DHLUzK3awT2LtAa-8nkJr_cUoo5XCMHg"
  }
}
```

## Create a Verification Token with Wildcard. 

#### Keycloak Token

```json
token=`curl -s -d "client_id=ubirch-2.0-user-access" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASS" -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID" $keycloak | jq -r .access_token`
```

#### Data object

```json
{
  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
  "purpose":"King Dude - Concert",
  "targetIdentities":["*"],
  "expiration": 6311390400,
  "notBefore":null,
  "originDomains": ["http://verification.dev.ubirch.com"],
  "scopes": ["upp:verify"]
}
```

#### Post Request

```shell script
curl -s -X POST \
    -H "authorization: bearer ${token}" \
    -H "content-type: application/json" \
    -d @createVerificationToken.json \
    "${host}/api/tokens/v1/create" | jq .
```

#### Post Response

```json
{
  "version": "1.0",
  "ok": true,
  "data": {
    "id": "740eefa9-5566-49df-839d-70416e57aa96",
    "jwtClaim": {
      "content": "{\"purpose\":\"King Dude - Concert\",\"target_identities\":[\"*\"],\"origin_domains\":[\"http://verification.dev.ubirch.com\"],\"scopes\":[\"upp:verify\"]}",
      "issuer": "https://token.dev.ubirch.com",
      "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "audience": [
        "https://verify.dev.ubirch.com"
      ],
      "expiration": 7924869565,
      "issuedAt": 1613479165,
      "jwtId": "740eefa9-5566-49df-839d-70416e57aa96"
    },
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MjQ4Njk1NjUsImlhdCI6MTYxMzQ3OTE2NSwianRpIjoiNzQwZWVmYTktNTU2Ni00OWRmLTgzOWQtNzA0MTZlNTdhYTk2IiwicHVycG9zZSI6IktpbmcgRHVkZSAtIENvbmNlcnQiLCJ0YXJnZXRfaWRlbnRpdGllcyI6WyIqIl0sIm9yaWdpbl9kb21haW5zIjpbImh0dHA6Ly92ZXJpZmljYXRpb24uZGV2LnViaXJjaC5jb20iXSwic2NvcGVzIjpbInVwcDp2ZXJpZnkiXX0.OfZqlftYl0kMflOKDm39fXneJ3R1aWLXB4WaJhPVl4W3F4koCzIoXFupB-9U2xwCCl7eA6uTy8E7fGNm9aFWVg"
  }
}
```

## Verification Token Claims

This token has the following header:

```json
{
  "typ": "JWT",
  "alg": "ES256"
}
```

```json
{
  "iss": "https://token.dev.ubirch.com",
  "sub": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
  "aud": "https://verify.dev.ubirch.com",
  "exp": 7924869463,
  "iat": 1613479063,
  "jti": "407f29ab-ac74-42c7-81e9-107a53f3de36",
  "purpose": "King Dude - Concert",
  "target_identities": [
    "840b7e21-03e9-4de7-bb31-0b9524f3b500"
  ],
  "origin_domains": [
    "http://verification.dev.ubirch.com"
  ],
  "scopes": [
    "upp:verify"
  ]
}
```

```
Where 
    'iss' is Principal Entity that signs/issues the token: The Token Manager.
    'sub' is the purpose or subject for this token: The tenantId/UserId from Keycloak 
    'aud' target entity: The target systems: Verfication Service, Niomon, or Thing API.  
    'exp' is the expiration time
    'iat' is the initial time
    'jti' is a unique uuid id for the token
    'purpose' is a description of the main usage for this token, like a concert or artist show
    'target_identities': it is the entities for which the subject can perform the action on the target audience system
    'scopes' is set of actions allowed per resource for this token.
```

## List your Tokens 

#### Keycloak Token

```json
token=`curl -s -d "client_id=ubirch-2.0-user-access" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASS" -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID" $keycloak | jq -r .access_token`
```

#### Get Request

```shell script
curl -s -X GET \
    -H "authorization: bearer ${token}" \
    -H "content-type: application/json" \
    "${host}/api/tokens/v1" | jq .
```

#### List Response

```json
{
  "version":"1.0",
  "ok":true,
  "data":[
    {
      "id":"163e22a2-bbd6-4536-a8f6-db0356c67a07",
      "ownerId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "tokenValue":"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MTc4MDcwNzUsImlhdCI6MTYwNjQxNjY3NSwianRpIjoiMTYzZTIyYTItYmJkNi00NTM2LWE4ZjYtZGIwMzU2YzY3YTA3IiwicHVycG9zZSI6IktpbmcgRHVkZSAtIENvbmNlcnQiLCJ0YXJnZXRfaWRlbnRpdGllcyI6WyI4NDBiN2UyMS0wM2U5LTRkZTctYmIzMS0wYjk1MjRmM2I1MDAiXSwicm9sZSI6InZlcmlmaWVyIn0.GIv9n3C6nEEnHZlHMZa_saaENv51MeWH1586UBQUP8GlwMcjbWU6mTXe3LRXjTLHRJXpuGrH5fcNe3fLC7MqzA",
      "category":"verification",
      "createdAt":"2020-11-26T18:51:15.700Z"
    },
    {
      "id":"2b7df15a-e170-4e5b-be7a-d548a3330d73",
      "ownerId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "tokenValue":"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiIiLCJzdWIiOiIiLCJhdWQiOiIiLCJpYXQiOjE2MDQ0MTU1NjQsImp0aSI6IjJiN2RmMTVhLWUxNzAtNGU1Yi1iZTdhLWQ1NDhhMzMzMGQ3MyIsIm93bmVySWQiOiI5NjM5OTVlZC1jZTEyLTRlYTUtODlkYy1iMTgxNzAxZDFkN2IifQ.Fk3Cqr3QzIpbBkE8NVY_m8LVVfwxTuOtDja4F7jpWu26YlH0v2wh1hg7iv9o9_hAchK3qc7LtyI43lhUA0nkIg",
      "category":"generic",
      "createdAt":"2020-11-03T14:59:24.323Z"
    }
  ]
}
```

## Delete a Token 

#### Keycloak Token

```json
token=`curl -s -d "client_id=ubirch-2.0-user-access" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASS" -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID" $keycloak | jq -r .access_token`
```

#### Token

```json
tokenId=UUID for the token id
```

#### Delete Request

```shell script
curl -s -X DELETE \
    -H "authorization: bearer ${token}" \
    -H "content-type: application/json" \
    "${host}/api/tokens/v1/${tokenId}" | jq .
```

### Keycloak Token and Responses
 
In order for any request be received and executed, the initiator must provide proof it has been granted with the required permissions. 
In order to do so, its request must contain an Authorization header. 

#### The Header

```
Authorization: <type> <token>

where 
  <type> is Bearer
  <token> is the JWT token for the current logged in user. This token originates from Keycloak.
``` 
  
#### The Responses

```
The <response> codes could be:

1. <200 OK>           When the system found a proper verification.
2. <400 Badrequest>   When the incoming data has not been properly parsed or accepted.            
3. <403 Forbidden>    When the token is invalid.
4. <401 Unauthorized> When no Authorization header is found in the request.
                      In this case, the response will contain the following header 
                      WWW-Authenticate: <type> realm=<realm>
                      
                      where <type> is Bearer and
                           <realm> is "Ubirch Token Service"
5. <500 Internal Server Error> When an internal error happened from which it is not possible to recover.
```


## Available Scopes

The scopes have this definition:

`[RESOURCE]:[ACTION]`

RESOURCE: Represents the entity in the system that needs to be regulated. For example: DEVICE or USER or UPP
ACTION: Represents the verb/action that is regulated for the given resource. For example: verify or anchor

```shell script
curl -s -X GET $host/api/tokens/v1/scopes | jq .
```

**upp:anchor**  :: it allows anchoring upps

**upp:verify**  :: it allows verifying upps

**thing:create**:: it allows creating/registering devices or things.

**thing:getinfo**:: it allows querying info about a device or thing.

**user:getinfo**:: it allows querying info about a user.

This call returns a json object whose data field is an array of scopes.

```json
{
  "version": "1.0",
  "ok": true,
  "data": [
    "upp:anchor",
    "upp:verify",
    "thing:create",
    "thing:getinfo",
    "user:getinfo"
  ]
}
```

## Verifying an Ubirch JWT Token

The Ubirch Token Manager offers an endpoint that can be used to retrieve the public key for the tokens in order to be able to verify the generated token.
This endpoint doesn't require of a access token. 

```shell script
curl -s -X GET $host/api/tokens/v1/jwk | jq .
```

This call returns a json object whose data field is the public key. This public key is in JWK format.

## A Light SDK

In order to facilitate the integration of some of the most important functions of the Token Manager when verifying Token in other Services, a light SDK has been included in the project.

The interface offers these basic operations:

* **decodeAndVerify** ->  It allows a basic verification. It verifies that the token is well-built, that its standard claims are checked. I
* **getClaims** -> It the same as the previous operation but basically performing the verification on the header as it is.
* **externalStateVerify**: Task[Boolean] -> Depending on the kinds of claims, there are some that require an external verification, this operations starts a verification against the Token Manager. Useful for groups and revocation claims.

## Swagger

Visit https://token.dev.ubirch.com/docs on your browser to see the swagger docs.

## Some Workflows

[Token workflows](token_verification_workflows.md)

[Token Scenarios](token_scenarios.md)
