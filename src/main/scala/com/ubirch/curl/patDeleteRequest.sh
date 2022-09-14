#!/bin/bash

local=$1
remote_host="https://token.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

echo "=> host: $host"

curl -s -X DELETE -H "content-type: application/json" -H "X-Ubirch-Signature: x3CDsjXG5BFx-pp1iUNSlunbXeq4KkBJcyC65MiGWXhnBcjCV/M3vyx0=" -H "X-Ubirch-Timestamp: 1663102593678" -d @patDeleteRequest.json $host/api/tokens/v1/pat | jq .
