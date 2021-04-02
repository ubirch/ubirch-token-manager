#!/bin/bash

local=$1
remote_host="https://token.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

signature="XnzIs+NjXlVYANOvplFIApEbMCh1iuCSTmypupn+p2b37gT91ZAg//prZI4t11gS8jqBljo3O0ybqok4hiWHBg=="

echo "=> host: $host"

curl -s -X POST -H "X-Ubirch-Signature: $signature" -H "content-type: application/json" -d @bootstrap.json $host/api/tokens/v1/bootstrap | jq .
