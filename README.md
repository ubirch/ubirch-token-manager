# Ubirch Token Manager

This service knows about jwt tokens.

1. [Getting Started](#steps-to-prepare-a-request)
2. [Token Claim Object](#token-claim-object)
3. [Create Verification Token for Devices](#create-a-verification-token-for-specific-devices)
4. [Create Verification Token with Wildcard](#create-a-verification-token-for-specific-devices)
5. [List Your Tokens](#list-your-tokens)
6. [Delete A Token](#delete-a-token)
7. [Verification Token Claims](#verification-token-claims)
8. [Keycloak and Responses](#keycloak-token-and-responses)
9. [Verifying an Ubirch JWT Token (JWK)](#verifying-an-ubirch-jwt-token)
10. [Swagger](#swagger)

## Steps to prepare a request

1. Get your keycloak token.
2. Prepare the data object - when needed (creation).
3. Prepare the request and send.

## Token Claim Object

![Data Model](DataModel.png)

**Fields**

_tenantId_: it is the keycloak id of the logged in user.
 
_purpose_: it is a description for the token.  Min characters are 6

_targetIdentities_: it is a list of device ids that belong to the user. It supports a list of specific devices or the wildcard *.
If it is meant as wildcard, the field should be sent as a string.

_expiration_: the number of seconds after which the token will be considered expired.
That is to say: 'X seconds from now', where X == expiration AND now == the current time calculated on the server.
If not set, it will not expire.

_notBefore_: the number of seconds after which the token should be considered valid. 
That is to say: 'X seconds from now', where X == notBefore AND now == the current time calculated on the server.

_originDomains_: list of domains from which the calls will be accepted from when verifying.

_scopes_: list of available scopes: "upp:anchor", "upp:verify", "thing:create"


**Mandatory Fields**

* tenantId (uuid as string)
* purpose (string) :: min characters are 6
* targetIdentities (array of uuid as string) | (*)
* originDomains (array of urls from which a verification can originate)

**Option Fields** 

* expiration (number or null) in seconds
* notBefore (number of null) in seconds

Set as null or don't send the fields

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
  "notBefore":null
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
    "id": "b9107002-9a60-4230-9b8a-a43b4317de1c",
    "jwtClaim": {
      "content": "{\"purpose\":\"King Dude - Concert\",\"target_identities\":[\"840b7e21-03e9-4de7-bb31-0b9524f3b500\"],\"role\":\"verifier\"}",
      "issuer": "https://token.dev.ubirch.com",
      "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "audience": [
        "https://verify.dev.ubirch.com"
      ],
      "expiration": 7918235892,
      "issuedAt": 1606845492,
      "jwtId": "b9107002-9a60-4230-9b8a-a43b4317de1c"
    },
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MTgyMzU4OTIsImlhdCI6MTYwNjg0NTQ5MiwianRpIjoiYjkxMDcwMDItOWE2MC00MjMwLTliOGEtYTQzYjQzMTdkZTFjIiwicHVycG9zZSI6IktpbmcgRHVkZSAtIENvbmNlcnQiLCJ0YXJnZXRfaWRlbnRpdGllcyI6WyI4NDBiN2UyMS0wM2U5LTRkZTctYmIzMS0wYjk1MjRmM2I1MDAiXSwicm9sZSI6InZlcmlmaWVyIn0.7OiXbsoZMtNE6OaanUat7beuW3vZeKrJ8_fkW1iOwXHPXewq_p4kanDKJEmQkd-dV8dg3IfbdCndnnM6jpCQdA"
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
  "targetIdentities": "*",
  "expiration": 6311390400,
  "notBefore":null
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
    "id": "2d105274-1cb3-45bc-9aed-b7c444d25f2f",
    "jwtClaim": {
      "content": "{\"purpose\":\"King Dude - Concert\",\"target_identities\":\"*\",\"role\":\"verifier\",\"scope\":\"ver\"}",
      "issuer": "https://token.dev.ubirch.com",
      "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "audience": [
        "https://verify.dev.ubirch.com"
      ],
      "expiration": 7921842955,
      "issuedAt": 1610452555,
      "jwtId": "2d105274-1cb3-45bc-9aed-b7c444d25f2f"
    },
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MjE4NDI5NTUsImlhdCI6MTYxMDQ1MjU1NSwianRpIjoiMmQxMDUyNzQtMWNiMy00NWJjLTlhZWQtYjdjNDQ0ZDI1ZjJmIiwicHVycG9zZSI6IktpbmcgRHVkZSAtIENvbmNlcnQiLCJ0YXJnZXRfaWRlbnRpdGllcyI6IioiLCJyb2xlIjoidmVyaWZpZXIiLCJzY29wZSI6InZlciJ9.AE5njTtbWGXDr-6hyn7UJYJgD10vGznp3vof2B8Bs77HzUqC42xfLDk0f0Fhcb8sXb61i32jxQj9fG0OulPVAg"
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
  "exp": 7921535473,
  "iat": 1610145073,
  "jti": "f359eaf1-2a91-4462-962b-5c85523bafad",
  "purpose": "Shark Tank - Mexico",
  "target_identities": [
    "7549acd8-91e1-4230-833a-2f386e09b96f"
  ],
  "role": "verifier",
  "scope": "ver"
}
```

```
Where 
    'iss' is Principal Entity that signs/issues the token: The Token Manager.
    'sub' is the purpose or subject for this token: The tenantId/UserId from Keycloak 
    'aud' target entity: The target system, in this token, the Verfication Service.  
    'exp' is the expiration time
    'iat' is the initial time
    'jti' is a unique uuid id for the token
    'purpose' is a description of the main usage for this token, like a concert or artist show
    'target_identities': it is the entities for which the subject can perform the action on the target audience system
    'role' is the role that was assigned to this token
    'scope' is the action allowed for this token.
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

## Verifying an Ubirch JWT Token

The Ubirch Token Manager offers an endpoint that can be used to retrieve the public key for the tokens in order to be able to verify the generated token.
This endpoint doesn't require of a access token. 

```shell script
curl -s -X GET $host/api/tokens/v1/jwk | jq .
```

This call returns a json object whose data field is the public key. This public key is in JWK format.


## Swagger

Visit https://token.dev.ubirch.com/docs on your browser to see the swagger docs.


