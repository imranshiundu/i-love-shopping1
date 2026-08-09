# ADR-002: Use PostgreSQL as Primary Database

## Status
Accepted

## Context
We need a reliable, ACID-compliant database for an e-commerce platform handling:
- User accounts and authentication data
- Product catalog with complex relationships
- Orders, payments, and financial transactions
- High read/write loads during peak periods
- Data integrity for financial records

## Decision
We will use PostgreSQL 16 as the primary database with Flyway for migrations.

## Consequences
- **Positive**: Full ACID compliance, robust transaction support
- **Positive**: Excellent JSON/JSONB support for flexible product attributes
- **Positive**: Advanced indexing (GIN, GiST, BRIN) for search performance
- **Positive**: Strong consistency for financial transactions
- **Positive**: Mature, battle-tested, widely supported
- **Negative**: Vertical scaling limitations (mitigated by read replicas)
- **Negative**: More complex setup than document databases