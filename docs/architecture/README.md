## Architecture Decision Records

This directory documents significant architectural decisions for the i-love-shopping platform.

### ADR Template

Each ADR should follow this format:

```
# ADR-XXX: Title

## Status
Proposed | Accepted | Deprecated | Superseded

## Context
What is the issue that we're seeing that is motivating this decision or change?

## Decision
What is the change that we're proposing and/or doing?

## Consequences
What becomes easier or more difficult to do because of this change?
```

### Index

| ADR | Title | Status |
|-----|-------|--------|
| [001](./001-use-spring-boot.md) | Use Spring Boot 3.x | Accepted |
| [002](./002-use-postgresql.md) | Use PostgreSQL as primary database | Accepted |
| [003](./003-use-jwt-auth.md) | Use JWT for stateless authentication | Accepted |
| [004](./004-use-mpesa.md) | Integrate M-Pesa Daraja for payments | Accepted |
| [005](./005-use-docker.md) | Containerize with Docker | Accepted |
| [006](./006-use-flyway.md) | Use Flyway for database migrations | Accepted |
| [007](./007-use-redis.md) | Use Redis for caching and sessions | Accepted |
| [008](./008-modular-monolith.md) | Modular monolith architecture | Accepted |

---

*ADRs are immutable once accepted. To change a decision, create a new ADR that supersedes the old one.*