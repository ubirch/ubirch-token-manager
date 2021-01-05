#!/bin/bash

local=$1
remote_host="https://token.dev.ubirch.com"
host="http://localhost:8081"
keycloak="https://id.dev.ubirch.com/auth/realms/ubirch-default-realm/protocol/openid-connect/token"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

token=`curl  -s  -d "client_id=ubirch-2.0-user-access"   -d "username=$TOKEN_USER"   -d "password=$TOKEN_PASS"   -d "grant_type=password" -d "client_secret=$TOKEN_CLIENT_ID"  $keycloak | jq -r .access_token`

echo "=> host: $host"

id="faef71cc-2323-4b71-9bb1-e84ca348039c"

curl -s -H "authorization: bearer $token" -X GET $host/api/tokens/v1/$id | jq .
