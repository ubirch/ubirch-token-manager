#!/bin/bash

local=$1
remote_host="https://token.dev.ubirch.com"
host="http://localhost:8081"

if [ "$local" == "-r" ]
then
  host=$remote_host
fi

token="eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL3Rva2VuLmRldi51YmlyY2guY29tIiwic3ViIjoiOTYzOTk1ZWQtY2UxMi00ZWE1LTg5ZGMtYjE4MTcwMWQxZDdiIiwiYXVkIjoiaHR0cHM6Ly90b2tlbi5kZXYudWJpcmNoLmNvbSIsImV4cCI6NzkyODY3NjgzNiwiaWF0IjoxNjE3Mjg2NDM2LCJqdGkiOiJiZDc2MDE3NS01ODg4LTQ4MjUtYTExNi05MGQwMmNjYWRkMGUiLCJzY3AiOlsidGhpbmc6Ym9vdHN0cmFwIl0sInB1ciI6Ik1lZHdheSBMYWJvcmF0b3JpZXMiLCJ0Z3AiOlsiS2l0Y2hlbl9DYXJsb3MiXSwidGlkIjpbXSwib3JkIjpbXX0.S2OGWUTt6HFj0tByXwfEJRL1vfl5ctrU95QSLmYgFM7TWTY70dG7cO7RtU6y4KfIKaFiL-lg_Tvu3C98HTiVJg"

echo "=> host: $host"

curl -s -X POST -H "authorization: bearer $token" -H "content-type: application/json" -d @createBootstrapToken.json $host/api/tokens/v1/bootstrap | jq .
