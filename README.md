# Ubirch Token Manager

This service knows about jwt tokens.

1. [Getting Started](#steps-to-prepare-a-request)
2. [TokenVerificationClaim Object](#token-verification-claim-object)
3. [Create Verification Token for Devices](#create-a-verification-token-for-specific-devices)
4. [Create Verification Token with Wildcard](#create-a-verification-token-for-specific-devices)
5. [List Your Tokens](#list-your-tokens)
6. [Delete A Token](#delete-a-token)
7. [Keycloak and Responses](#keycloak-token-and-responses)
8. [Swagger](#swagger)

## Steps to prepare a request

1. Get your keycloak token.
2. Prepare the data object - when needed (creation).
3. Prepare the request and send.

## Token Verification Claim Object

```json
{
  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
  "purpose":"King Dude - Concert",
  "targetIdentities":["840b7e21-03e9-4de7-bb31-0b9524f3b500"] | "*",
  "expiration": 6311390400,
  "notBefore":null
}
```

**Fields**

_tenantId_: it is the keycloak id of the logged in user.
 
_purpose_: it is a description for the token.  Min characters are 6

_targetIdentities_: it is a list of device ids that belong to the user. It supports a list of specific devices or the wildcard *.
If it is meant as wildcard, the field should be sent as a string.

_expiration_: the time in milliseconds after which the token will be considered expired. If not set, it will not expire.

_notBefore_: the time in milliseconds after which the token should be considered valid. 

**Mandatory Fields**

* tenantId (uuid as string)
* purpose (string) :: min characters are 6
* targetIdentities (array of uuid as string) | (*)

**Option Fields** 

* expiration (number or null)
* notBefore (number of null)

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
    "${host}/api/tokens/v1/verification/create" | jq .
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
    "${host}/api/tokens/v1/verification/create" | jq .
```

#### Post Response

```json
{
  "version": "1.0",
  "ok": true,
  "data": {
    "id": "6be6c7c0-4f15-40ba-9cb2-d066d4b81099",
    "jwtClaim": {
      "content": "{\"purpose\":\"King Dude - Concert\",\"target_identities\":\"*\",\"role\":\"verifier\"}",
      "issuer": "https://token.dev.ubirch.com",
      "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "audience": [
        "https://verify.dev.ubirch.com"
      ],
      "expiration": 7918388949,
      "issuedAt": 1606998549,
      "jwtId": "6be6c7c0-4f15-40ba-9cb2-d066d4b81099"
    },
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5MTgzODg5NDksImlhdCI6MTYwNjk5ODU0OSwianRpIjoiNmJlNmM3YzAtNGYxNS00MGJhLTljYjItZDA2NmQ0YjgxMDk5IiwicHVycG9zZSI6IktpbmcgRHVkZSAtIENvbmNlcnQiLCJ0YXJnZXRfaWRlbnRpdGllcyI6IioiLCJyb2xlIjoidmVyaWZpZXIifQ.-8nHxBoz71v4Vo9IGyg2A-iYKpsVYN4C5XgdC0D9jesMCxR7cYeufy4qe1QaHYGvZhO2tUBuiVVITeBFY4Az_Q"
  }
}

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

#### Keycloak Token and Responses
 
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

## Swagger

Visit https://token.dev.ubirch.com/docs on your browser to see the swagger docs.


