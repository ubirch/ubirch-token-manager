#!/bin/bash

local=$1
remote_host="https://token.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

echo "=> host: $host"

curl -s -X POST -H "content-type: application/json" -d @verificationRequest.json $host/api/tokens/v2/verify | jq .
