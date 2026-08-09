# ADR-007: Use Redis for Caching and Sessions

## Status
Accepted

## Context
We need a high-performance, in-memory data store for:
- JWT token blacklisting (revocation)
- API response caching (product catalog, categories)
- Rate limiting counters
- Session storage for OAuth2 flows

## Decision
We will use Redis 7 with:
- TTL-based expiration for cache entries
- Redis Sentinel or clustering for production HA
- Connection pooling via Lettuce
- Spring Cache abstraction for declarative caching

## Consequences
- **Positive**: Sub-millisecond latency for cache operations
- **Positive**: Built-in TTL support for automatic cleanup
- **Positive**: Pub/Sub for distributed cache invalidation
- **Positive**: Persistence options (AOF/RDB) for durability
- **Negative**: Additional infrastructure component
- **Negative**: Memory-limited (plan capacity accordingly)
- **Mitigation**: Monitor memory usage, configure appropriate eviction policies