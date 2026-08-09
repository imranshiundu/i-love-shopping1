# ADR-003: Use JWT for Stateless Authentication

## Status
Accepted

## Context
We need a scalable authentication mechanism for a distributed e-commerce platform. Traditional session-based authentication requires sticky sessions or shared session stores, which complicates horizontal scaling.

## Decision
We will use JSON Web Tokens (JWT) with:
- Short-lived access tokens (15 minutes)
- Long-lived refresh tokens (7 days) with rotation
- HS256 signing algorithm with 256-bit secrets
- Token revocation via database blacklist

## Consequences
- **Positive**: Stateless, horizontally scalable
- **Positive**: No server-side session storage needed
- **Positive**: Works well with mobile clients and SPAs
- **Negative**: Cannot revoke access tokens immediately (mitigated by short expiry)
- **Negative**: Token size increases with claims (keep payload minimal)
- **Negative**: Secret rotation requires careful coordination
- **Mitigation**: Short access token lifetime, refresh token rotation, revocation list