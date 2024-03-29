# Ubirch Token Manager

This service is in charge of creating and managing purposed tokens. A purposed token is a token that may have a hybrid nature. It is a self-verifiable token and reference token. This nature depends on the types of claims that might be included in its definition. This service allows defining purposes and scopes for the corresponding tokens. As a convenience mechanism to integrate verification on the systems that required the tokens, a light sdk is included.

1. [Getting Started](#steps-to-prepare-a-request)
2. [Token Claim Object](#token-claim-object)
3. [Create Verification Token for Devices](#create-a-verification-token-for-specific-devices)
4. [Create Verification Token with Wildcard](#create-a-verification-token-for-specific-devices)
5. [Create Bootstrap Token for a Tenant Group](#create-a-bootstrap-token-for-a-tenant-group)
6. [Using a Bootstrap Token](#using-a-bootstrap-token)
7. [List Your Tokens](#list-your-tokens)
8. [Delete A Token](#delete-a-token)
9. [Available Scopes](#available-scopes)
10. [Verification Token Claims](#verification-token-claims)
11. [Keycloak and Responses](#keycloak-token-and-responses)
12. [Verifying an Ubirch JWT Token (JWK)](#verifying-an-ubirch-jwt-token)
13. [A Light SDK](#a-light-sdk)    
14. [Swagger](#swagger)
15. [Workflows](#workflows)

## Steps to prepare a request

1. Get your keycloak token.
2. Prepare the data object - when needed (creation of token).
3. Prepare the request and send.
4. Have fun.

## Token Claim Object

This object is meant to be used in the creation of purposed claims. A purposed claim is an access token that has a purpose and contains scopes.

![Data Model](assets/DataModel.png)

**Fields**

_`tenantId`_: it is the keycloak id of the logged-in user.
 
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

_`scopes`_: list of available scopes: "upp:anchor", "upp:verify", "thing:create", "thing:getinfo", "thing:bootstrap", "user:getinfo"

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

## Create a Verification Token for Specific Devices. 

#### Keycloak Token

This is a command that can be used to get a keycloak token. Note that you will need the username, password and client secret for the corresponding keycloak instance.

```
token=`curl -s -d "client_id=ubirch-2.0-user-access" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASS" -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID" $keycloak | jq -r .access_token`
```

#### Data object

```json
{
  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
  "purpose":"King Dude - Concert",
  "targetIdentities":["e21552f8-0353-41e3-b86e-0d3e92935d46"],
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
    "${host}/api/tokens/v2/create" | jq .
```

#### Post Response

```json
{
  "version": "2.0.0",
  "ok": true,
  "data": {
    "id": "726aa795-a2dc-4354-9559-41a2c86615a1",
    "jwtClaim": {
      "content": "{\"scp\":[\"upp:verify\"],\"pur\":\"King Dude - Concert\",\"tgp\":[],\"tid\":[\"e21552f8-0353-41e3-b86e-0d3e92935d46\"],\"ord\":[\"http://verification.dev.ubirch.com\"]}",
      "issuer": "https://token.dev.ubirch.com",
      "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "audience": [
        "https://verify.dev.ubirch.com"
      ],
      "expiration": 7927995641,
      "issuedAt": 1616605241,
      "jwtId": "726aa795-a2dc-4354-9559-41a2c86615a1"
    },
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5Mjc5OTU2NDEsImlhdCI6MTYxNjYwNTI0MSwianRpIjoiNzI2YWE3OTUtYTJkYy00MzU0LTk1NTktNDFhMmM4NjYxNWExIiwic2NwIjpbInVwcDp2ZXJpZnkiXSwicHVyIjoiS2luZyBEdWRlIC0gQ29uY2VydCIsInRncCI6W10sInRpZCI6WyJlMjE1NTJmOC0wMzUzLTQxZTMtYjg2ZS0wZDNlOTI5MzVkNDYiXSwib3JkIjpbImh0dHA6Ly92ZXJpZmljYXRpb24uZGV2LnViaXJjaC5jb20iXX0.etW9msaDVae1SC_BJTA0H6Hqo0m2inKi4SYKjOmJgqbPEmlWxtDvd0MP1rf5GYpfGqizbH3uUgRxESS6qqXSTQ"
  }
}
```

## Create a Verification Token with Wildcard. 

#### Keycloak Token

```
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
    "${host}/api/tokens/v2/create" | jq .
```

#### Post Response

```json
{
  "version": "2.0.0",
  "ok": true,
  "data": {
    "id": "92d60695-e6a2-4c6c-8173-6f8f1496dbb3",
    "jwtClaim": {
      "content": "{\"scp\":[\"upp:verify\"],\"pur\":\"King Dude - Concert\",\"tgp\":[],\"tid\":[\"*\"],\"ord\":[\"http://verification.dev.ubirch.com\"]}",
      "issuer": "https://token.dev.ubirch.com",
      "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "audience": [
        "https://verify.dev.ubirch.com"
      ],
      "expiration": 7927995850,
      "issuedAt": 1616605450,
      "jwtId": "92d60695-e6a2-4c6c-8173-6f8f1496dbb3"
    },
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5Mjc5OTU4NTAsImlhdCI6MTYxNjYwNTQ1MCwianRpIjoiOTJkNjA2OTUtZTZhMi00YzZjLTgxNzMtNmY4ZjE0OTZkYmIzIiwic2NwIjpbInVwcDp2ZXJpZnkiXSwicHVyIjoiS2luZyBEdWRlIC0gQ29uY2VydCIsInRncCI6W10sInRpZCI6WyIqIl0sIm9yZCI6WyJodHRwOi8vdmVyaWZpY2F0aW9uLmRldi51YmlyY2guY29tIl19.-zcsKmqc0dimFfZO71l-eBnNufbUIRxykOIGyHsRFPmGVShYRwAZaH6CxtIPL60uvtokfyNumbNGIg7LILRiFA"
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
  "pur": "King Dude - Concert",
  "tid": ["840b7e21-03e9-4de7-bb31-0b9524f3b500"],
  "tgp": [],
  "ord": ["http://verification.dev.ubirch.com"],
  "scp": ["upp:verify"]
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
    'pur' is a description of the main usage for this token, like a concert or artist show
    'tid': it is the entities for which the subject can perform the action on the target audience system
    'tgp': it is the groups (keycloak groups) that can be used to aggregate devices.
    'scp' is set of actions allowed per resource for this token.
```

## Create a Bootstrap Token for a Tenant Group

#### Keycloak Token

```
token=`curl -s -d "client_id=ubirch-2.0-user-access" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASS" -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID" $keycloak | jq -r .access_token`
```

#### Data object

```json
{
  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
  "purpose":"Kitchen_Carlos",
  "targetIdentities":[],
  "targetGroups": ["d6e525c0-41e2-4a77-925c-4d6ea4fb8431"],
  "expiration": 6311390400,
  "notBefore":null,
  "originDomains": [],
  "scopes": ["thing:bootstrap"]
}
```

#### Post Request

```shell script
curl -s -X POST \
    -H "authorization: bearer ${token}" \
    -H "content-type: application/json" \
    -d @createBootstrapToken.json \
    "${host}/api/tokens/v2/create" | jq .
```

#### Post Response

```json
{
  "version": "2.0.0",
  "ok": true,
  "data": {
    "id": "820b619b-1bab-432e-bb49-8a985f236fe6",
    "jwtClaim": {
      "content": "{\"scp\":[\"thing:bootstrap\"],\"pur\":\"Kitchen_Carlos\",\"tgp\":[\"d6e525c0-41e2-4a77-925c-4d6ea4fb8431\"],\"tid\":[],\"ord\":[]}",
      "issuer": "https://token.dev.ubirch.com",
      "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "audience": [
        "https://token.dev.ubirch.com"
      ],
      "expiration": 7928862861,
      "issuedAt": 1617472461,
      "jwtId": "820b619b-1bab-432e-bb49-8a985f236fe6"
    },
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly90b2tlbi5kZXYudWJpcmNoLmNvbSIsImV4cCI6NzkyODg2Mjg2MSwiaWF0IjoxNjE3NDcyNDYxLCJqdGkiOiI4MjBiNjE5Yi0xYmFiLTQzMmUtYmI0OS04YTk4NWYyMzZmZTYiLCJzY3AiOlsidGhpbmc6Ym9vdHN0cmFwIl0sInB1ciI6IktpdGNoZW5fQ2FybG9zIiwidGdwIjpbImQ2ZTUyNWMwLTQxZTItNGE3Ny05MjVjLTRkNmVhNGZiODQzMSJdLCJ0aWQiOltdLCJvcmQiOltdfQ.WJhq1ldCYBmzBIg_UeuZsTXALCfwFl5pLPh3s4NakDEN4ZGEuEjejxvR_8d5PaEz4Bjgv6J0ZGU9qy-OyhrAvQ"
  }
}
```

## Using a Bootstrap Token

In order to use a bootstrap token, an existing public key is assumed to have already been registered on The Ubirch Identity Service.

#### Data object

```json
{"token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly90b2tlbi5kZXYudWJpcmNoLmNvbSIsImV4cCI6NzkyODg2Mjg2MSwiaWF0IjoxNjE3NDcyNDYxLCJqdGkiOiI4MjBiNjE5Yi0xYmFiLTQzMmUtYmI0OS04YTk4NWYyMzZmZTYiLCJzY3AiOlsidGhpbmc6Ym9vdHN0cmFwIl0sInB1ciI6IktpdGNoZW5fQ2FybG9zIiwidGdwIjpbImQ2ZTUyNWMwLTQxZTItNGE3Ny05MjVjLTRkNmVhNGZiODQzMSJdLCJ0aWQiOltdLCJvcmQiOltdfQ.WJhq1ldCYBmzBIg_UeuZsTXALCfwFl5pLPh3s4NakDEN4ZGEuEjejxvR_8d5PaEz4Bjgv6J0ZGU9qy-OyhrAvQ","identity":"d7a81058-ae97-4178-80ed-71aed46e88fa"}
```

### X-Ubirch-Signature Header

The _Data Object_ should be signed with a valid **ECDSA** or **EDDSA** key. The Json object is assumed to be assigned in a compact fashion. 

The _signature_ is calculated like this:

1. The _SHA-512 digest_ is calculated over the _Data Object_. See previous point.
2. The digest is signed with the private key.
3. The _base64_ string representation is calculated over the signature.
4. This encoded signature put in the header.

#### Post Request

```shell script
curl -s -X POST -H "X-Ubirch-Signature: $signature" -H "content-type: application/json" -d @bootstrap.json $host/api/tokens/v2/bootstrap | jq .
```

#### Post Response

The data object has three fields, registration, anchoring and verification. The registration token has an expiration of 15 mins. By registration of the device, the device is assigned to the claimed group. Note that in order to anchor the device has to be registered.

```json
{
  "version": "2.0.0",
  "ok": true,
  "data": {
    "registration": {
      "id": "006479e6-fb0b-40ef-ad17-e012913f4469",
      "jwtClaim": {
        "content": "{\"scp\":[\"thing:create\"],\"pur\":\"Kitchen_Carlos\",\"tgp\":[\"d6e525c0-41e2-4a77-925c-4d6ea4fb8431\"],\"tid\":[\"d7a81058-ae97-4178-80ed-71aed46e88fa\"],\"ord\":[]}",
        "issuer": "https://token.dev.ubirch.com",
        "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
        "audience": [
          "https://api.console.dev.ubirch.com"
        ],
        "expiration": 1617476571,
        "issuedAt": 1617475671,
        "jwtId": "006479e6-fb0b-40ef-ad17-e012913f4469"
      },
      "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly9hcGkuY29uc29sZS5kZXYudWJpcmNoLmNvbSIsImV4cCI6MTYxNzQ3NjU3MSwiaWF0IjoxNjE3NDc1NjcxLCJqdGkiOiIwMDY0NzllNi1mYjBiLTQwZWYtYWQxNy1lMDEyOTEzZjQ0NjkiLCJzY3AiOlsidGhpbmc6Y3JlYXRlIl0sInB1ciI6IktpdGNoZW5fQ2FybG9zIiwidGdwIjpbImQ2ZTUyNWMwLTQxZTItNGE3Ny05MjVjLTRkNmVhNGZiODQzMSJdLCJ0aWQiOlsiZDdhODEwNTgtYWU5Ny00MTc4LTgwZWQtNzFhZWQ0NmU4OGZhIl0sIm9yZCI6W119.x2VoQ34i1gC9pc_Q-d6T5RwBMAUPH7NvgTX1ZGq0dBnHkBjvW9yXFGa8Jwxn-VLXM_r6CVhON_eg_1h1fnPdiw"
    },
    "anchoring": {
      "id": "fe38223a-2bc8-48cc-ae1d-e2245283c968",
      "jwtClaim": {
        "content": "{\"scp\":[\"upp:anchor\"],\"pur\":\"Kitchen_Carlos\",\"tgp\":[\"d6e525c0-41e2-4a77-925c-4d6ea4fb8431\"],\"tid\":[\"d7a81058-ae97-4178-80ed-71aed46e88fa\"],\"ord\":[]}",
        "issuer": "https://token.dev.ubirch.com",
        "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
        "audience": [
          "https://niomon.dev.ubirch.com"
        ],
        "issuedAt": 1617475671,
        "jwtId": "fe38223a-2bc8-48cc-ae1d-e2245283c968"
      },
      "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly9uaW9tb24uZGV2LnViaXJjaC5jb20iLCJpYXQiOjE2MTc0NzU2NzEsImp0aSI6ImZlMzgyMjNhLTJiYzgtNDhjYy1hZTFkLWUyMjQ1MjgzYzk2OCIsInNjcCI6WyJ1cHA6YW5jaG9yIl0sInB1ciI6IktpdGNoZW5fQ2FybG9zIiwidGdwIjpbImQ2ZTUyNWMwLTQxZTItNGE3Ny05MjVjLTRkNmVhNGZiODQzMSJdLCJ0aWQiOlsiZDdhODEwNTgtYWU5Ny00MTc4LTgwZWQtNzFhZWQ0NmU4OGZhIl0sIm9yZCI6W119.X6NVe63gWo25Gd_h0fjO0okpLuV6V3-Qd87LsTXC2pIXn_WoDPg8rNwcKHqXUeR2OQBh79Vr6cxIUX2CmJP1RA"
    },
    "verification": {
      "id": "74087a7f-efc9-4bb1-b781-326306629bf7",
      "jwtClaim": {
        "content": "{\"scp\":[\"upp:verify\"],\"pur\":\"Kitchen_Carlos\",\"tgp\":[\"d6e525c0-41e2-4a77-925c-4d6ea4fb8431\"],\"tid\":[\"d7a81058-ae97-4178-80ed-71aed46e88fa\"],\"ord\":[]}",
        "issuer": "https://token.dev.ubirch.com",
        "subject": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
        "audience": [
          "https://verify.dev.ubirch.com"
        ],
        "issuedAt": 1617475671,
        "jwtId": "74087a7f-efc9-4bb1-b781-326306629bf7"
      },
      "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJpYXQiOjE2MTc0NzU2NzEsImp0aSI6Ijc0MDg3YTdmLWVmYzktNGJiMS1iNzgxLTMyNjMwNjYyOWJmNyIsInNjcCI6WyJ1cHA6dmVyaWZ5Il0sInB1ciI6IktpdGNoZW5fQ2FybG9zIiwidGdwIjpbImQ2ZTUyNWMwLTQxZTItNGE3Ny05MjVjLTRkNmVhNGZiODQzMSJdLCJ0aWQiOlsiZDdhODEwNTgtYWU5Ny00MTc4LTgwZWQtNzFhZWQ0NmU4OGZhIl0sIm9yZCI6W119.ucq7_c2r8qkuKx3r1kG8Z2msPLR6Er6kgCezJjeCWehsnao8l2WZ2jCbklb4i2KAFYcRut8y5qnBVz9GwglsXg"
    }
  }
}

```


## List your Tokens 

#### Keycloak Token

```
token=`curl -s -d "client_id=ubirch-2.0-user-access" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASS" -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID" $keycloak | jq -r .access_token`
```

#### Get Request

```shell script
curl -s -X GET \
    -H "authorization: bearer ${token}" \
    -H "content-type: application/json" \
    "${host}/api/tokens/v2" | jq .
```

#### List Response

```json
{
  "version": "2.0.0",
  "ok": true,
  "data": [
    {
      "id": "726aa795-a2dc-4354-9559-41a2c86615a1",
      "ownerId": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "tokenValue": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5Mjc5OTU2NDEsImlhdCI6MTYxNjYwNTI0MSwianRpIjoiNzI2YWE3OTUtYTJkYy00MzU0LTk1NTktNDFhMmM4NjYxNWExIiwic2NwIjpbInVwcDp2ZXJpZnkiXSwicHVyIjoiS2luZyBEdWRlIC0gQ29uY2VydCIsInRncCI6W10sInRpZCI6WyJlMjE1NTJmOC0wMzUzLTQxZTMtYjg2ZS0wZDNlOTI5MzVkNDYiXSwib3JkIjpbImh0dHA6Ly92ZXJpZmljYXRpb24uZGV2LnViaXJjaC5jb20iXX0.etW9msaDVae1SC_BJTA0H6Hqo0m2inKi4SYKjOmJgqbPEmlWxtDvd0MP1rf5GYpfGqizbH3uUgRxESS6qqXSTQ",
      "category": "purposed_claim",
      "createdAt": "2021-03-24T17:00:41.329Z"
    },
    {
      "id": "92d60695-e6a2-4c6c-8173-6f8f1496dbb3",
      "ownerId": "963995ed-ce12-4ea5-89dc-b181701d1d7b",
      "tokenValue": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly92ZXJpZnkuZGV2LnViaXJjaC5jb20iLCJleHAiOjc5Mjc5OTU4NTAsImlhdCI6MTYxNjYwNTQ1MCwianRpIjoiOTJkNjA2OTUtZTZhMi00YzZjLTgxNzMtNmY4ZjE0OTZkYmIzIiwic2NwIjpbInVwcDp2ZXJpZnkiXSwicHVyIjoiS2luZyBEdWRlIC0gQ29uY2VydCIsInRncCI6W10sInRpZCI6WyIqIl0sIm9yZCI6WyJodHRwOi8vdmVyaWZpY2F0aW9uLmRldi51YmlyY2guY29tIl19.-zcsKmqc0dimFfZO71l-eBnNufbUIRxykOIGyHsRFPmGVShYRwAZaH6CxtIPL60uvtokfyNumbNGIg7LILRiFA",
      "category": "purposed_claim",
      "createdAt": "2021-03-24T17:04:10.397Z"
    }
  ]
}
```

## Delete a Token 

#### Keycloak Token

```
token=`curl -s -d "client_id=ubirch-2.0-user-access" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASS" -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID" $keycloak | jq -r .access_token`
```

#### Token

```
tokenId=UUID for the token id
```

#### Delete Request

```shell script
curl -s -X DELETE \
    -H "authorization: bearer ${token}" \
    -H "content-type: application/json" \
    "${host}/api/tokens/v2/${tokenId}" | jq .
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
curl -s -X GET $host/api/tokens/v2/scopes | jq .
```

**upp:anchor**  :: it allows anchoring upps

**upp:verify**  :: it allows verifying upps

**thing:create**:: it allows creating/registering devices or things.

**thing:getinfo**:: it allows querying info about a device or thing.

**"thing:storedata"**:: it allows store data sets produced by a thing on the simple data service.

**user:getinfo**:: it allows querying info about a user.

**thing:bootstrap**:: it allows creating a bootstrap token that would permit creating devices, anchoring upps and 
verifying upps.

This call returns a json object whose data field is an array of scopes.

```json
{
  "version": "2.0.0",
  "ok": true,
  "data": [
    "upp:anchor",
    "upp:verify",
    "thing:create",
    "thing:getinfo",
    "thing:storedata",
    "thing:bootstrap",
    "user:getinfo"
  ]
}

```

## Verifying an Ubirch JWT Token

The Ubirch Token Manager offers an endpoint that can be used to retrieve the public key for the tokens in order to be able to verify the generated token.
This endpoint doesn't require of an access token. 

```shell script
curl -s -X GET $host/api/tokens/v2/jwk | jq .
```

This call returns a json object whose data field is the public key. This public key is in JWK format.

## A Light SDK

In order to facilitate the integration of some of the most important functions of the Token Manager when verifying Token in other Services, a light SDK has been included in the project.

The interface offers these basic operations:

* **decodeAndVerify** ->  It allows a basic verification. It verifies that the token is well-built, that its standard claims are checked. I
* **getClaims** -> It is same as the previous operation but basically performing the verification on the header as it is.
* **externalStateVerify**: -> Depending on the kinds of claims, there are some that require an external verification, these operations starts a verification against the Token Manager. Useful for groups and revocation claims.

        <dependency>
            <groupId>com.ubirch</groupId>
            <artifactId>ubirch-token-sdk</artifactId>
            <version>2.0.4-SNAPSHOT</version>
        </dependency>

Note that every system that might use the Light SDK and that performs externalities has to be explicitly known to the token manager. Every client has to configure their secret in their configuration file and the token manager must know of it.

### Secret

The secret for a client is a 2 part string. For example _`iHPDCXCTw1n0-Zcr1A/ZscwJWoi9oJK0XDOSnKJuDAfgSMLlV9hCIGSl8`_

Where the first part is 9 random bytes; and the second part is 33 random bytes. Both encoded in base64 and concatenated with "-".

You can create it by running 

```shell script
echo "$(dd if=/dev/urandom bs=9 count=1 | base64)-$(dd if=/dev/urandom bs=33 count=1 | base64)"
```

or running 

```shell script
mvn exec:java -Dexec.mainClass="com.ubirch.services.state.DefaultSecretKeyPoolService"
```

## Swagger

Visit https://token.dev.ubirch.com/docs on your browser to see the swagger docs.

## Workflows

### Token Creation
![Token Creation](assets/token_creation.png)

### Verification Token
![Verification Token](assets/token_verification_usage.png)

### Anchoring Token
![Anchoring Token](assets/token_anchoring_usage.png)

### Device Creation Token
![Device Creation Token](assets/token_device_creation_usage.png)

### Data Store Token
![Data Store Token](assets/token_store_data_usage.png)

[Token Scenarios](token_scenarios.md)
