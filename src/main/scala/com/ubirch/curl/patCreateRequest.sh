#!/bin/bash

local=$1
remote_host="https://token.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

echo "=> host: $host"

curl -s -X POST -H "content-type: application/json" -H "X-Ubirch-Signature: x3CDsjXG5BFx-tS91J/mhVaqEVFsLZiYG83EEjOvB/iUryWAmcXYt/Ww=" -H "X-Ubirch-Timestamp: 1663102416131" -d @patCreateRequest.json $host/api/tokens/v1/pat | jq .
