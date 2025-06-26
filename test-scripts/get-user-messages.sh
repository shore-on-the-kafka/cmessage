#!/bin/bash

# Usage: ./get-user-messages.sh <access_token>
# Example: ./get-user-messages.sh eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

ACCESS_TOKEN="$1"

if [ -z "$ACCESS_TOKEN" ]; then
  echo "Usage: $0 <access_token>"
  exit 1
fi

curl -X GET "http://localhost:8080/api/v1/messages" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/json"
