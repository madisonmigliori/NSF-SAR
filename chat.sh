#!/bin/bash

command -v jq >/dev/null 2>&1 || {
  echo "jq is not installed. Please install it to parse JSON responses."
  exit 1
}

echo " This script connects to your RAG backend, ingests a GitHub repo, and lets you chat with it!"

function wait_for_server() {
  echo "Checking if server is running..."
  docker compose up -d > /dev/null 2>&1

  until [[ "$(curl -s http://localhost:8080/actuator/health | jq -r .status)" == "UP" ]]; do
    echo "Waiting for server to be healthy..."
    sleep 5
  done

  echo "Server is up and running!"
}

wait_for_server

while true; do
  read -rp "Enter GitHub repo URL: " repoUrl
  if [[ -z "$repoUrl" || ! "$repoUrl" =~ \.git$ ]]; then
    echo "Please enter a valid GitHub URL ending in .git"
    continue
  fi

  repoId=$(echo "$repoUrl" | sed -E 's|.*/([^/]+)/([^/]+)\.git|\1-\2|')
  if [[ -z "$repoId" ]]; then
    echo "Could not extract repo ID. Make sure the URL is like https://github.com/user/repo.git"
  else
    echo "Repo ID extracted as: $repoId"
    break
  fi
done


encodedUrl=$(jq -rn --arg url "$repoUrl" '$url|@uri')


echo "Ingesting the repository..."
response=$(curl -s -X POST "http://localhost:8080/api/repos/ingest?gitUrl=$encodedUrl")


if echo "$response" | jq empty >/dev/null 2>&1; then
  echo "$response" | jq -r '.message // .status // " Ingestion request sent."'
else
  echo "$response"
fi

# Optionally prompt user first:
# read -rp "Do you want to run architecture analysis now? (y/n): " analyzeConfirm
# if [[ "$analyzeConfirm" == "y" ]]; then
  echo "Running architecture analysis..."

  analysisResponse=$(curl -s -X POST http://localhost:8080/api/chat/analyze \
    -H "Content-Type: application/json" \
    -d "{\"url\": \"$repoUrl\"}")

  echo "$analysisResponse" | jq .
# fi



echo "Ask your question (type /bye to exit):"
while true; do
  read -rp "> " question
  if [[ "$question" == "/bye" ]]; then
    echo "Goodbye!"
    break
  fi



  curl -s -X POST http://localhost:8080/api/chat \
    -H "Content-Type: application/json" \
    -d "{\"repoId\": \"$repoId\", \"text\": \"$question\"}" \
    | jq -r '.text'
done
