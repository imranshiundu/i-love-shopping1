# Deployment

### Option 1 — Everything in Docker

```bash
# Run from the project root (i-love-shopping/)
docker compose -f docker/docker-compose.yml up
```

This starts six services: `postgres`, `redis`, `mailhog`, `rabbitmq`, `api` and `frontend`. The first build can take several minutes. Press `Ctrl+C` to stop all services.

## Docker Deployment

> For local development, use the [Getting Started (Development)](#getting-started-development) instructions instead. This section covers building the images and the production deployment.

The repository ships two compose files:

| File | Purpose |
|------|---------|
| `docker/docker-compose.yml` | **Development** - PostgreSQL, Redis, Mailhog, RabbitMQ, the API and the Next.js frontend with sensible dev defaults |
| `docker/docker-compose.prod.yml` | **Production** - adds Nginx reverse proxy, env-based secrets, no Mailhog |

### Build Images

```bash
# Backend image (Jib)
cd backend
./mvnw compile jib:dockerBuild -Dimage=iloveshopping/backend:latest

# Or build with Docker directly
docker build -t iloveshopping/backend:latest -f Dockerfile .

# Frontend image (multi-stage Node build)
docker build -t iloveshopping/frontend:latest -f frontend/Dockerfile frontend/
```

### Deploy with Docker Compose

```bash
# Production deployment
docker compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d
```

### Docker Commands on Different Operating Systems

#### Linux/macOS

```bash
# Build and start all services
docker compose -f docker/docker-compose.yml up -d

# View logs
docker compose -f docker/docker-compose.yml logs -f api

# Stop services
docker compose -f docker/docker-compose.yml down

# Build production images
docker compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml build
```

#### Windows (PowerShell)

```powershell
# Build and start all services
docker compose -f docker\docker-compose.yml up -d

# View logs
docker compose -f docker\docker-compose.yml logs -f api

# Stop services
docker compose -f docker\docker-compose.yml down

# Build production images
docker compose -f docker\docker-compose.yml -f docker\docker-compose.prod.yml build
```

#### Windows (Command Prompt)

```cmd
REM Build and start all services
docker compose -f docker\docker-compose.yml up -d

REM View logs
docker compose -f docker\docker-compose.yml logs -f api

REM Stop services
docker compose -f docker\docker-compose.yml down

REM Build production images
docker compose -f docker\docker-compose.yml -f docker\docker-compose.prod.yml build
```

### Health Checks

```bash
# Check service health
curl http://localhost:8080/api/v1/health

# Detailed health
curl http://localhost:8080/api/v1/health/detailed

# Kubernetes probes
curl http://localhost:8080/api/v1/ready
curl http://localhost:8080/api/v1/live
```
