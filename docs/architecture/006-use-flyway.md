# ADR-006: Use Flyway for Database Migrations

## Status
Accepted

## Context
We need a reliable way to manage database schema changes across environments. Manual SQL scripts are error-prone and difficult to track.

## Decision
We will use Flyway for database migrations with:
- Versioned SQL migration scripts (V1, V2, etc.)
- Automatic execution on application startup
- Checksum validation to prevent tampering
- Baseline support for existing databases

## Consequences
- **Positive**: Version-controlled, repeatable schema changes
- **Positive**: Automatic execution on deployment
- **Positive**: Rollback capability (with undo scripts)
- **Positive**: Team collaboration on schema changes
- **Negative**: Requires discipline to write backward-compatible migrations
- **Negative**: Cannot easily modify applied migrations
- **Mitigation**: Code review for migrations, test on staging first