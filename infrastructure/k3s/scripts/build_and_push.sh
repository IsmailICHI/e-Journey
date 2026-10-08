#!/bin/bash

DOCKERHUB_USER="ismailichi" # <-- Change this to your Docker Hub username

# Build, tag, and push inventory-db

docker build -t $DOCKERHUB_USER/inventory-db:latest srcs/postgres-db
docker push $DOCKERHUB_USER/inventory-db:latest

# Build, tag, and push billing-db
docker build -t $DOCKERHUB_USER/billing-db:latest srcs/postgres-db
docker push $DOCKERHUB_USER/billing-db:latest

# Build, tag, and push inventory-app
docker build -t $DOCKERHUB_USER/inventory-app:latest srcs/inventory-app
docker push $DOCKERHUB_USER/inventory-app:latest

# Build, tag, and push billing-app
docker build -t $DOCKERHUB_USER/billing-app:latest srcs/billing-app
docker push $DOCKERHUB_USER/billing-app:latest

# Build, tag, and push api-gateway-app
docker build -t $DOCKERHUB_USER/api-gateway-app:latest srcs/api-gateway-app
docker push $DOCKERHUB_USER/api-gateway-app:latest

# Build, tag, and push rabbitmq
docker build -t $DOCKERHUB_USER/rabbitmq:latest srcs/rabbitmq
docker push $DOCKERHUB_USER/rabbitmq:latest
