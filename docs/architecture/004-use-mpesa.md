# ADR-004: Integrate M-Pesa Daraja for Payments

## Status
Accepted

## Context
The primary target market is Kenya, where M-Pesa is the dominant mobile payment platform with over 30 million active users. Supporting M-Pesa is essential for market adoption.

## Decision
We will integrate Safaricom's M-Pesa Daraja API using:
- STK Push for customer-initiated payments
- Callback URLs for asynchronous payment confirmation
- Sandbox environment for development/testing
- Production credentials for live transactions

## Consequences
- **Positive**: Access to Kenya's largest payment network
- **Positive**: Real-time payment confirmation via callbacks
- **Positive**: Familiar payment method for target users
- **Negative**: Dependency on Safaricom's API availability
- **Negative**: Sandbox limitations for testing edge cases
- **Negative**: API rate limits and timeout constraints
- **Mitigation**: Idempotent payment requests, retry logic, comprehensive logging