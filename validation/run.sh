#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: ./run.sh <github_url>"
  exit 1
fi

curl -X POST http://localhost:8080/api/chat/analyze \
  -H "Content-Type: application/json" \
  -d "{\"url\": \"$1\"}"
