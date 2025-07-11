#!/bin/bash

INPUT_FILE="repos.txt"
OUTPUT_FILE="batch_results.jsonl"

if [ ! -f "$INPUT_FILE" ]; then
  echo "Missing repos.txt"
  exit 1
fi

> "$OUTPUT_FILE" 

while read -r repo; do
  if [ -z "$repo" ]; then
    continue
  fi

  echo "Analyzing: $repo"
  echo "## Repository: $repo" >> "$OUTPUT_FILE"

  curl -s -X POST http://localhost:8080/api/chat/analyze \
    -H "Content-Type: application/json" \
    -d "{\"url\": \"$repo\"}" >> "$OUTPUT_FILE"

  echo -e "\n\n---\n" >> "$OUTPUT_FILE"
done < "$INPUT_FILE"
