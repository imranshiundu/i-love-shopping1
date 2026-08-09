# ADR-008: Modular Monolith Architecture

## Status
Accepted

## Context
We need to balance development simplicity with future scalability. A full microservices architecture adds significant operational complexity for a team of this size, while a traditional monolith can become unwieldy.

## Decision
We will implement a **modular monolith** with:
- Clear module boundaries (auth, catalog, cart, orders, payments, users, reviews)
- Separate packages for each module
- Shared kernel for common utilities (security, exceptions, DTOs)
- Database per module (logical separation via schemas/tables)
- Event-driven communication between modules (domain events)
- Future option to extract modules as microservices

## Consequences
- **Positive**: Single deployment unit, simpler operations
- **Positive**: Strong module boundaries prevent tight coupling
- **Positive**: Easy to extract modules later if needed
- **Positive**: Shared database simplifies transactions
- **Positive**: Simpler debugging and testing
- **Negative**: Single point of failure (mitigated by health checks, redundancy)
- **Negative**: Scaling requires scaling entire application
- **Negative**: Requires discipline to maintain module boundaries
- **Mitigation**: Code reviews, ArchUnit tests for architecture rules