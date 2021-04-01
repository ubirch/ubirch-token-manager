#!/bin/bash

local=$1
remote_host="https://token.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

signature="This is signature"

echo "=> host: $host"

curl -s -X POST -H "X-Ubirch-Signature: signature" -H "content-type: application/json" -d @bootstrap.json $host/api/tokens/v1/bootstrap | jq .
