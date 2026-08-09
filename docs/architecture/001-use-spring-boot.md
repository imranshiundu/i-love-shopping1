# ADR-001: Use Spring Boot 3.x

## Status
Accepted

## Context
We need a robust, production-ready Java framework for building a B2C e-commerce platform. The framework should provide:
- Dependency injection and inversion of control
- Built-in security, data access, and web modules
- Production-ready monitoring and health checks
- Strong ecosystem and community support
- Long-term support (LTS) releases

## Decision
We will use Spring Boot 3.2.x (based on Spring Framework 6.x) with Java 21.

## Consequences
- **Positive**: Excellent ecosystem, extensive documentation, strong community, built-in production features (Actuator), easy testing with Testcontainers
- **Positive**: Spring Security 6 provides modern authentication/authorization
- **Positive**: Spring Data JPA simplifies database access
- **Negative**: Larger memory footprint compared to micro-frameworks
- **Negative**: Learning curve for developers new to Spring
- **Mitigation**: Team training and code reviews to ensure proper Spring patterns