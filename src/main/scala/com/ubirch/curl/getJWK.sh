#!/bin/bash

local=$1
remote_host="https://token.dev.ubirch.com"
host="http://localhost:8081"
keycloak="https://id.dev.ubirch.com/auth/realms/ubirch-default-realm/protocol/openid-connect/token"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

echo "=> host: $host"

curl -s -X GET $host/api/tokens/v1/jwk | jq .
