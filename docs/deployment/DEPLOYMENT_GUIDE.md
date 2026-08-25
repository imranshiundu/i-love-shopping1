# Deployment Guide

This guide covers deploying i-love-shopping to various environments.

## Prerequisites

- Docker 24+ and Docker Compose 2+
- Domain name with DNS configured
- SSL certificates (Let's Encrypt recommended)
- M-Pesa Daraja production credentials
- SMTP server for emails

## Environment Variables

Create a `.env` file in the project root:

```bash
cp .env.example .env
# Edit .env with your production values
```

Required variables:
- `DATABASE_PASSWORD` - Strong PostgreSQL password
- `REDIS_PASSWORD` - Redis password
- `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` - RabbitMQ credentials
- `JWT_ACCESS_SECRET` - 32+ char random string
- `JWT_REFRESH_SECRET` - 32+ char random string
- `MPESA_*` - Production Daraja credentials
- `GOOGLE_CLIENT_SECRET` - OAuth2 secret
- `MAIL_PASSWORD` - SMTP app password
- `DATA_ENCRYPTION_KEY` - 32+ char random string; encrypts order addresses and payment records at rest
- `MPESA_SIMULATION_ENABLED` - keep `false` in production so STK push hits the real Daraja API
- Frontend build args: `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_APP_NAME`, and the rest of the `NEXT_PUBLIC_*` set (see README Configuration)

## Development Deployment

For a full development walkthrough (two options, prerequisites check, foreground run) use the setup script:

```bash
# Linux / macOS / Git Bash
bash start.sh

# Windows
start.cmd
```

Or manually:

```bash
# Start all services (option 1: API + frontend in Docker)
docker compose -f docker/docker-compose.yml up

# Or start only the dependencies, then run the API & frontend locally (option 2)
docker compose -f docker/docker-compose.yml up -d postgres redis mailhog rabbitmq
cd backend && ./mvnw spring-boot:run          # terminal 1
cd frontend && npm install && npm run dev     # terminal 2

# Run migrations (auto-run on startup)
docker compose -f docker/docker-compose.yml exec api ./mvnw flyway:migrate
```

Access:
- Frontend: http://localhost:3000
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/api/v1/docs
- Mailhog: http://localhost:8025
- RabbitMQ Management: http://localhost:15672 (`iloveshopping` / `iloveshopping`)

## Production Deployment

### 1. Prepare Server

```bash
# Ubuntu 22.04+
sudo apt update && sudo apt install -y docker.io docker-compose nginx certbot

# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker
```

### 2. Configure Nginx

```bash
# Copy nginx config
sudo cp docker/nginx/nginx.conf /etc/nginx/nginx.conf
sudo cp -r docker/nginx/ssl /etc/nginx/

# Obtain SSL certificates
sudo certbot --nginx -d yourdomain.com
```

### 3. Deploy

```bash
# Build and start
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d --build

# Verify
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml ps
curl -f https://yourdomain.com/api/v1/health
```

### 4. Database Setup

```bash
# Run migrations
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml exec api ./mvnw flyway:migrate

# Verify data
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml exec postgres psql -U iloveshopping -d iloveshopping -c "SELECT count(*) FROM products;"
```

## Monitoring & Maintenance

### Health Checks

```bash
# Basic health
curl https://yourdomain.com/api/v1/health

# Detailed health
curl https://yourdomain.com/api/v1/health/detailed

# Kubernetes probes
curl https://yourdomain.com/api/v1/ready
curl https://yourdomain.com/api/v1/live
```

### Logs

```bash
# All services
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml logs -f

# API only
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml logs -f api

# Nginx
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml logs -f nginx
```

### Backups

```bash
# Database backup
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml exec postgres pg_dump -U iloveshopping iloveshopping > backup_$(date +%Y%m%d).sql

# Restore
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml exec -T postgres psql -U iloveshopping iloveshopping < backup_20240115.sql
```

### Updates

```bash
# Pull latest code
git pull origin main

# Rebuild and deploy
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d --build --force-recreate

# Run migrations
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml exec api ./mvnw flyway:migrate
```

## Scaling

### Horizontal Scaling (API)

```yaml
# docker-compose.prod.yml
services:
  api:
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '1'
          memory: 1G
```

### Database Read Replicas

```yaml
# Add to docker-compose.prod.yml
  postgres-replica:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: iloveshopping
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
      POSTGRES_DB: iloveshopping
    command: postgres -c primary_conninfo='host=postgres port=5432 user=iloveshopping password=${DATABASE_PASSWORD}'
    depends_on:
      - postgres
```

## Troubleshooting

### API Won't Start

```bash
# Check logs
docker-compose logs api

# Common issues:
# - Database not ready: increase start_period in healthcheck
# - Migration failed: check Flyway logs
# - Port conflict: change SERVER_PORT
```

### Database Connection Issues

```bash
# Test connection
docker-compose exec api pg_isready -h postgres -U iloveshopping

# Check credentials
docker-compose exec api env | grep DATABASE
```

### SSL Certificate Issues

```bash
# Renew certificates
sudo certbot renew --dry-run
sudo certbot renew

# Reload nginx
docker-compose exec nginx nginx -s reload
```

### M-Pesa Callback Failures

```bash
# Verify callback URL is accessible
curl -X POST https://yourdomain.com/api/v1/orders/payments/mpesa/callback \
  -H "Content-Type: application/json" \
  -d '{"Body":{"stkCallback":{"CheckoutRequestID":"test","ResultCode":0}}}'

# Check nginx logs
docker-compose logs nginx | grep mpesa
```

## Security Checklist

- [ ] Strong passwords for all services
- [ ] JWT secrets rotated regularly
- [ ] HTTPS enforced (HSTS header)
- [ ] CORS restricted to frontend domain
- [ ] Rate limiting enabled
- [ ] Database not exposed publicly
- [ ] Redis password set
- [ ] Regular security updates (`apt update && apt upgrade`)
- [ ] Dependency vulnerability scanning (`mvn dependency-check`)
- [ ] Audit logs monitored

## Performance Tuning

### JVM Options

```yaml
# Add to docker-compose.prod.yml
environment:
  - JAVA_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Database

```sql
-- Add indexes for common queries
CREATE INDEX idx_products_category_brand ON products(category_id, brand_id);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
```

### Caching

```yaml
# Enable query caching in application.yml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          use_query_cache: true
```