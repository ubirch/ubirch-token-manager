#!/bin/bash

local=$1
remote_host="https://token.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

signature="ow4t3n9Qjnyuqg1MHMBi/uU2lgeW1EU+0o6GX+quSJr4IlfY+GW+yT1Fw171VNM+pCZo7BBo5XuJns4mDIZdAQ=="

echo "=> host: $host"

curl -s -X POST -H "X-Ubirch-Signature: $signature" -H "content-type: application/json" -d @bootstrap.json $host/api/tokens/v2/bootstrap | jq .
