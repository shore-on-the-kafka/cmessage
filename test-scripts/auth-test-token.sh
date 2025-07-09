#!/bin/bash

userId="$1"
name="$2"

curl -X GET "http://localhost:8080/api/v1/auth/test/token?userId=${userId}&name=${name}" \
  -H "Accept: application/json"
