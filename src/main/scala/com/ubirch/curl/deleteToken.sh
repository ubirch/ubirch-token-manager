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

tokenId=87729510-cdbd-46f6-9ff1-da59fa73a1e8

curl -s -H "authorization: bearer $token" -X DELETE $host/api/tokens/v2/$tokenId | jq .
