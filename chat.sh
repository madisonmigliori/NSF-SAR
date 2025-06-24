#!/bin/bash

# Function to check if server is running
function wait_for_server() {
  echo "🔍 Checking if server is running..."
  until curl -s --head http://localhost:8080/health | grep "200 OK" > /dev/null; do
    echo "⚙️  Server not running. Trying to start it with Docker..."
    docker compose up -d
    echo "⏳ Waiting for server to start..."
    sleep 5
  done
  echo "✅ Server is up and running!"
}

# Run server check
wait_for_server

# Ask for repo URL
while true; do
  read -rp "🔗 Enter GitHub repo URL: " repoUrl
  if [[ -z "$repoUrl" || ! "$repoUrl" =~ \.git$ ]]; then
    echo "❗ Please enter a valid GitHub URL ending in .git"
    continue
  fi

  repoId=$(echo "$repoUrl" | sed -E 's|.*/([^/]+)/([^/]+)\.git|\1-\2|')

  if [[ -z "$repoId" ]]; then
    echo "❗ Could not extract repo ID. Make sure the URL is like https://github.com/user/repo.git"
  else
    echo "📦 Repo ID extracted as: $repoId"
    break
  fi
done

# Chat loop
echo "💬 Ask your question (type /bye to exit):"
while true; do
  read -rp "> " question
  if [[ "$question" == "/bye" ]]; then
    echo "👋 Goodbye!"
    break
  fi

  curl -s -X POST http://localhost:8080/api/chat \
    -H "Content-Type: application/json" \
    -d "{\"repoId\": \"$repoId\", \"text\": \"$question\"}" \
    | jq -r '.text'
done
