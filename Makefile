# Makefile for i-love-shopping

.PHONY: help build test run-dev run-prod clean docker-build docker-up docker-down logs test-coverage

# Default target
help:
	@echo "i-love-shopping - Available commands:"
	@echo ""
	@echo "Build & Test:"
	@echo "  build              - Build the project (skip tests)"
	@echo "  test               - Run all unit tests"
	@echo "  test-coverage      - Run tests with coverage report"
	@echo "  test-integration   - Run integration tests with Testcontainers"
	@echo ""
	@echo "Development:"
	@echo "  run-dev            - Start development environment (PostgreSQL, Redis, Mailhog)"
	@echo "  run-api            - Run Spring Boot application locally"
	@echo "  stop-dev           - Stop development environment"
	@echo ""
	@echo "Docker:"
	@echo "  docker-build       - Build Docker images"
	@echo "  docker-up          - Start all services with Docker Compose"
	@echo "  docker-down        - Stop all services"
	@echo "  docker-logs        - View Docker logs"
	@echo ""
	@echo "Production:"
	@echo "  run-prod           - Start production environment"
	@echo "  docker-build-prod  - Build production Docker images"
	@echo ""
	@echo "Maintenance:"
	@echo "  clean              - Clean build artifacts"
	@echo "  db-migrate         - Run Flyway migrations"
	@echo "  db-seed            - Seed database with sample data"

# Build & Test
build:
	cd backend && ./mvnw clean compile -DskipTests

test:
	cd backend && ./mvnw test

test-coverage:
	cd backend && ./mvnw test jacoco:report

test-integration:
	cd backend && ./mvnw verify -DskipUnitTests

# Development
run-dev:
	docker-compose -f docker/docker-compose.yml up -d postgres redis mailhog
	@echo "Waiting for services to be ready..."
	@sleep 10
	@echo "Development services ready. Run 'make run-api' to start the API."

run-api:
	cd backend && ./mvnw spring-boot:run

stop-dev:
	docker-compose -f docker/docker-compose.yml down

# Docker
docker-build:
	docker-compose -f docker/docker-compose.yml build

docker-up:
	docker-compose -f docker/docker-compose.yml up -d

docker-down:
	docker-compose -f docker/docker-compose.yml down

docker-logs:
	docker-compose -f docker/docker-compose.yml logs -f

# Production
run-prod:
	docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d

docker-build-prod:
	docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml build

# Maintenance
clean:
	cd backend && ./mvnw clean
	docker-compose -f docker/docker-compose.yml down -v

db-migrate:
	cd backend && ./mvnw flyway:migrate

db-seed:
	cd backend && ./mvnw flyway:migrate -Dflyway.locations=classpath:db/migration
	@echo "Seed data is included in V2__Insert_seed_data.sql migration"