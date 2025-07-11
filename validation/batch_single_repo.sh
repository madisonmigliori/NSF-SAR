#!/bin/bash

REPO_URL="https://github.com/spring-petclinic/spring-petclinic-microservices.git"

for i in {1..50}; do
  echo "Run $i..."
  ./analyze.sh "$REPO_URL" >> repeated_results.jsonl
  echo -e "\n---\n" >> repeated_results.jsonl
done
