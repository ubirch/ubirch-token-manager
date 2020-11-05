# Ubirch Token Manager

This service knows about jwt tokens.

## Steps to prepare a request

1. Get your keycloak token (*)
2. Prepare the data object - when needed (creation) - (**)
3. Prepare the request and send. (***)

## Create a Verification Token 

### Keycloak Token (*)

```json
token=`curl  -s  -d "client_id=ubirch-2.0-user-access"   -d "username=$TOKEN_USER"   -d "password=$TOKEN_PASS"   -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID"  $keycloak | jq -r .access_token`
```

### Data object (**)

```json
{
  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
  "purpose":"King Dude - Concert",
  "target_identity":"840b7e21-03e9-4de7-bb31-0b9524f3b500",
  "expiration": 6311390400,
  "notBefore":null
}
```

### Post Request (***)

```shell script
curl -s -X POST \
    -H "authorization: bearer ${token}" \
    -H "content-type: application/json" \
    -d @createVerificationToken.json \
    "${host}/api/tokens/v1/verification/create" | jq .
```

 

