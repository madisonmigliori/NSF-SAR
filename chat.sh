
echo "Ask your question (type /bye to exit):"
while true; do
  read -rp "> " question
  if [[ "$question" == "/bye" ]]; then
    echo "Goodbye!"
    break
  fi

  curl -s -X POST http://localhost:8080/api/chat \
    -H "Content-Type: application/json" \
    -d "{\"repoId\": \"madisonmigliori-todo-app\", \"text\": \"$question\"}" \
    | jq -r '.text'
done
