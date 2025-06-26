#!/bin/bash

# Usage: ./send-user-message.sh <access_token> <receiver_id> <content>
# Example: ./send-user-message.sh eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... user123 "Hello!"

ACCESS_TOKEN="$1"
RECEIVER_ID="$2"
CONTENT="$3"

curl -X POST "http://localhost:8080/api/v1/messages" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "receiverId": "'"$RECEIVER_ID"'",
    "content": "'"$CONTENT"'"
  }'
