# ADR-005: Containerize with Docker

## Status
Accepted

## Context
We need consistent, reproducible deployments across development, staging, and production environments. The application has multiple dependencies (PostgreSQL, Redis, Mailhog) that must be orchestrated.

## Decision
We will containerize the application using:
- Multi-stage Dockerfile (Maven build + JRE runtime)
- Docker Compose for local development
- Separate production compose with Nginx reverse proxy
- Jib for optimized production images

## Consequences
- **Positive**: Consistent environments across dev/staging/prod
- **Positive**: Easy onboarding for new developers
- **Positive**: Simplified CI/CD pipeline
- **Positive**: Dependency isolation
- **Negative**: Additional build step
- **Negative**: Image size considerations
- **Mitigation**: Multi-stage builds, .dockerignore, Jib optimization