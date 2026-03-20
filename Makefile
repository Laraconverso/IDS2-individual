.PHONY: help up up-detached down down-volumes logs logs-app logs-db stop restart ps \
        build db-only db-stop test test-docker test-clean maven-clean maven-package \
        maven-compile clean status version

help: ## Show available commands
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "%-20s %s\n", $$1, $$2}'

up: ## Build and run all services (DB + API)
	docker compose up --build

up-detached: ## Run services in background
	docker compose up --build -d

down: ## Stop and remove containers
	docker compose down

down-volumes: ## Stop and remove containers and volumes (deletes DB data)
	docker compose down -v

logs: ## View logs from all services
	docker compose logs -f

logs-app: ## View application logs only
	docker compose logs -f app

logs-db: ## View database logs only
	docker compose logs -f db

stop: ## Stop containers without removing
	docker compose stop

restart: ## Restart services
	docker compose restart

ps: ## Show container status
	docker compose ps

build: ## Build Docker image
	docker compose build

db-only: ## Run only the database
	docker compose up --build db

db-stop: ## Stop database
	docker compose stop db

test: ## Run tests locally (requires JDK 25+)
	cd eCommerce && ./mvnw test

test-clean: ## Clean and run tests
	cd eCommerce && ./mvnw clean test

maven-clean: ## Clean Maven artifacts
	cd eCommerce && ./mvnw clean

maven-package: ## Package application (create JAR)
	cd eCommerce && ./mvnw clean package -DskipTests

maven-compile: ## Compile code
	cd eCommerce && ./mvnw compile

clean: ## Remove containers, volumes, and prune images
	docker compose down -v
	docker system prune -f

status: ## Show service status
	docker compose ps

version: ## Show Java and Maven versions
	java -version
	cd eCommerce && ./mvnw --version

.DEFAULT_GOAL := help


