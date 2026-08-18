# Contributing to i-love-shopping

Thank you for your interest in contributing to i-love-shopping! This document provides guidelines and instructions for contributing to this project.

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct. Please treat all contributors with respect and professionalism.

## How to Contribute

### 1. Fork the Repository

Fork the repository on Gitea/GitHub and clone your fork locally:

```bash
git clone https://gitea.kood.tech/imranshiundu/i-love-shopping1.git
cd i-love-shopping
```

### 2. Create a Branch

Create a feature branch for your changes:

```bash
git checkout -b feature/your-feature-name
```

Use descriptive branch names:
- `feature/add-product-reviews`
- `fix/cart-stock-validation`
- `docs/update-api-documentation`
- `refactor/auth-service`

### 3. Make Changes

Follow our coding standards:

- **Java**: Follow Google Java Style Guide
- **Spring Boot**: Use constructor injection, proper annotations
- **Database**: Use Flyway migrations for schema changes
- **Tests**: Write unit tests for new functionality (minimum 80% coverage)
- **Documentation**: Update README and API docs for new features

### 4. Run Tests

Ensure all tests pass before submitting:

```bash
cd backend
./mvnw clean test
```

### 5. Commit Changes

Write clear, conventional commit messages:

```
feat: add product search with faceted filtering
fix: resolve cart stock validation race condition
docs: update API documentation for order endpoints
refactor: extract payment service interface
test: add unit tests for M-Pesa callback handling
```

Format: `<type>: <subject>`

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`, `security`

### 6. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on Gitea/GitHub with:
- Clear title and description
- Reference any related issues
- Include screenshots for UI changes
- Ensure CI passes

## Development Setup

### Prerequisites

- Java 21
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- Docker & Docker Compose (for PostgreSQL, Redis, Mailhog)

### Quick Start (Recommended)

```bash
# Linux / macOS / Git Bash
bash scripts/dev.sh

# Windows (Command Prompt / PowerShell)
scripts\dev.cmd
```

### Running Manually

**Option 1 — Everything in Docker:**

```bash
docker compose -f docker/docker-compose.yml up
```

**Option 2 — Dependencies in Docker + local API:**

```bash
# Start dependencies (from project root)
docker compose -f docker/docker-compose.yml up -d postgres redis mailhog

# Run the API locally (from backend/ directory)
cd backend
export DATABASE_URL='jdbc:postgresql://localhost:5433/iloveshopping?stringtype=unspecified'
export DATABASE_USER=iloveshopping
export DATABASE_PASSWORD=iloveshopping
export REDIS_HOST=localhost
export REDIS_PORT=6380
export RECAPTCHA_SECRET_KEY=dev-test-secret
export JWT_ACCESS_SECRET=dev-access-secret-min-32-chars-long-for-test
export JWT_REFRESH_SECRET=dev-refresh-secret-min-32-chars-long-for-test
export MAIL_HOST=localhost
export MAIL_PORT=1025
./mvnw spring-boot:run
```

### Running Tests

```bash
cd backend
./mvnw test                    # Unit tests
./mvnw verify                  # Integration tests with Testcontainers
./mvnw jacoco:report           # Coverage report
```

## Code Review Process

1. All changes require at least one approval
2. CI must pass (tests, linting, security scans)
3. Maintainer merges after approval
4. Squash and merge preferred

## Reporting Issues

Use the issue tracker on Gitea/GitHub. Include:
- Clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, Java version, etc.)
- Screenshots if applicable

## Security Issues

Report security vulnerabilities privately to the maintainers. Do not open public issues for security problems.

## Questions?

Contact the maintainers or open a discussion on Gitea/GitHub.