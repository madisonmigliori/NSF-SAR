#!/bin/bash

command -v jq >/dev/null 2>&1 || {
  echo "jq is not installed. Please install it to parse JSON responses."
  exit 1
}

echo "This script connects to your RAG backend, ingests GitHub repos, and lets you chat with them!"

function wait_for_server() {
  echo "Building the backend..."
  cd rag || { echo "Could not find 'rag' directory"; exit 1; }

  mvn clean package -DskipTests || { echo "Build failed"; exit 1; }

  echo "Starting the Spring Boot app..."
  java -jar target/rag-0.0.1-SNAPSHOT.jar &

  # Get PID to kill later
  SERVER_PID=$!

  echo "Waiting for server to become healthy..."
  until [[ "$(curl -s http://localhost:8080/actuator/health | jq -r .status)" == "UP" ]]; do
    echo "Waiting for server to be healthy..."
    sleep 5
  done

  echo "Server is up and running!"
}

command -v jq >/dev/null 2>&1 || {
  echo "jq is not installed. Please install it to parse JSON responses."
  exit 1
}

# echo "This script connects to your RAG backend, ingests GitHub repos, and lets you chat with them!"

# function wait_for_server() {
#   echo "Checking if server is running..."
#   docker compose up -d > /dev/null 2>&1

#   until [[ "$(curl -s http://localhost:8080/actuator/health | jq -r .status)" == "UP" ]]; do
#     echo "Waiting for server to be healthy..."
#     sleep 5
#   done

#   echo "Server is up and running!"
# }

wait_for_server

# List of repo URLs to test
repoUrls=(
  "https://github.com/anilallewar/microservices-basics-spring-boot.git"
  "https://github.com/apssouza22/java-microservice.git"
  "https://github.com/callistaenterprise/blog-microservices.git"
  "https://github.com/ewolff/microservice.git"
  "https://github.com/ewolff/microservice-kafka.git"
  "https://github.com/fernandoabcampos/spring-netflix-oss-microservices.git"
  "https://github.com/georgwittberger/apache-spring-boot-microservice-example.git"
  "https://github.com/jferrater/Tap-And-Eat-MicroServices.git"
  "https://github.com/koushikkothagal/spring-boot-microservices-workshop.git"
  "https://github.com/mdeket/spring-cloud-movie-recommendation.git"
  "https://github.com/mudigal-technologies/microservices-sample.git"
  "https://github.com/piomin/sample-spring-oauth2-microservices.git"
  "https://github.com/rohitghatol/spring-boot-microservices.git"
  "https://github.com/shabbirdwd53/Springboot-Microservice.git"
  "https://github.com/spring-petclinic/spring-petclinic-microservices.git"
  "https://github.com/sqshq/piggymetrics.git"
  "https://github.com/yidongnan/spring-cloud-netflix-example.git"
  "https://github.com/FudanSELab/train-ticket.git"
  "https://github.com/delimitrou/DeathStarBench.git"
  "https://github.com/daxnet/we-text.git"
  "https://github.com/hiejulia/warehouse-microservice.git"
  "https://github.com/mohamed-abdo/vehicle-tracking-microservices.git"
  "https://github.com/Vanilla-Java/Microservices.git"
  "https://github.com/FudanSELab/train-ticket.git"
  "https://github.com/DescartesResearch/TeaStore.git"
  "https://github.com/yun19830206/CloudShop-MicroService-Architecture.git"
  "https://github.com/jferrater/Tap-And-Eat-MicroServices.git"
  "https://github.com/LandRover/StaffjoyV2.git"
  "https://github.com/Staffjoy/v2.git"
  "https://github.com/sivaprasadreddy/spring-boot-microservices-series.git"
  "https://github.com/oktadev/spring-boot-microservices-example.git"
  "https://github.com/aws-samples/amazon-ecs-java-microservices.git"
  "https://github.com/spring-petclinic/spring-petclinic-microservices.git"
  "https://github.com/paulc4/microservices-demo.git"
  "https://github.com/zpng/spring-cloud-microservice-examples.git"
  "https://github.com/microservices-demo/microservices-demo.git"
  "https://github.com/microservices-demo/user.git"
  "https://github.com/microservices-demo/microservices-demo.github.io.git"
  "https://github.com/microservices-demo/front-end.git"
  "https://github.com/microservices-demo/payment.git"
  "https://github.com/microservices-demo/orders.git"
  "https://github.com/sitewhere/sitewhere.git"
  "https://github.com/JoeCao/qbike.git"
  "https://github.com/instana/robot-shop.git"
  "https://github.com/callistaenterprise/blog-microservices.git"
  "https://github.com/EdwinVW/pitstop.git"
  "https://github.com/sqshq/piggymetrics.git"
  "https://github.com/nginxinc/mra-ingenious.git"
  "https://github.com/microsoft/PartsUnlimitedMRPmicro.git"
  "https://github.com/yidongnan/spring-cloud-netflix-example.git"
  "https://github.com/aspnet/MusicStore.git"
  "https://github.com/bishion/microService.git"
  "https://github.com/mdeket/spring-cloud-movie-recommendation.git"  
  "https://github.com/bishion/microService.git"
  "https://github.com/ewolff/microservice-kubernetes.git"
  "https://github.com/ewolff/microservice.git"
  "https://github.com/bishion/microService.git"
  "https://github.com/senecajs/ramanujan.git"
  "https://github.com/idugalic/micro-company.git"
  "https://github.com/Microservice-API-Patterns/LakesideMutual.git"
  "https://github.com/TheDigitalNinja/million-song-library.git"
  "https://github.com/jaegertracing/jaeger.git"
  "https://github.com/GoogleCloudPlatform/microservices-demo.git"
  "https://github.com/kbastani/spring-boot-graph-processing-example.git"
  "https://github.com/xJREB/research-modifiability-pattern-experiment.git"
  "https://github.com/microservices-patterns/ftgo-application.git"
  "https://github.com/william-tran/freddys-bbq.git"
  "https://github.com/dotnet-architecture/eShopOnContainers.git"
  "https://github.com/gfawcett22/EnterprisePlanner.git"
  ""
)

for repoUrl in "${repoUrls[@]}"; do
  echo ""
  echo "Processing repository: $repoUrl"

  if [[ -z "$repoUrl" || ! "$repoUrl" =~ \.git$ ]]; then
    echo "Invalid URL: $repoUrl. Skipping..."
    continue
  fi

  repoId=$(echo "$repoUrl" | sed -E 's|.*/([^/]+)/([^/]+)\.git|\1-\2|')
  if [[ -z "$repoId" ]]; then
    echo "Could not extract repo ID from: $repoUrl. Skipping..."
    continue
  fi

  echo "Repo ID extracted as: $repoId"
  encodedUrl=$(jq -rn --arg url "$repoUrl" '$url|@uri')

  echo "Ingesting the repository..."
  response=$(curl -s -X POST "http://localhost:8080/api/repos/ingest?gitUrl=$encodedUrl")

  if echo "$response" | jq empty >/dev/null 2>&1; then
    echo "$response" | jq -r '.message // .status // "Ingestion request sent."'
  else
    echo "$response"
  fi

  echo "Running architecture analysis..."
  analysisResponse=$(curl -s -X POST http://localhost:8080/api/chat/analyze \
    -H "Content-Type: application/json" \
    -d "{\"url\": \"$repoUrl\"}")

  echo "$analysisResponse" | jq .

done