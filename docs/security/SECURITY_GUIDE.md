# Security Implementation Guide

This document details the security measures implemented in i-love-shopping.

## Authentication Security

### JWT Implementation
- **Algorithm**: HS256 (HMAC SHA-256)
- **Access Token Expiry**: 15 minutes
- **Refresh Token Expiry**: 7 days
- **Key Length**: 256 bits (32+ characters)
- **Token Rotation**: Single-use refresh tokens with automatic revocation
- **Revocation**: Database-backed blacklist with TTL

### Password Security
- **Hashing**: BCrypt with cost factor 12
- **Minimum Length**: 8 characters
- **Maximum Length**: 128 characters
- **No password reuse tracking** (can be added)

### Two-Factor Authentication (2FA)
- **Algorithm**: TOTP (RFC 6238)
- **Time Step**: 30 seconds
- **Code Digits**: 6
- **Secret Storage**: Base32 encoded in database
- **QR Code**: otpauth:// URI for authenticator apps
- **Backup Codes**: Not implemented (future enhancement)

### OAuth2 Integration
- **Providers**: Google, GitHub
- **Flow**: Authorization Code with PKCE
- **State Parameter**: CSRF protection
- **Token Storage**: Not stored (stateless)

### CAPTCHA
- **Provider**: Google reCAPTCHA v3
- **Score Threshold**: 0.5
- **Endpoints Protected**: Registration, Login (after failures)

## Authorization Security

### Role-Based Access Control (RBAC)
- **Roles**: USER, ADMIN, MODERATOR
- **Method-Level Security**: @PreAuthorize annotations
- **Endpoint Protection**: Role-based route guards

### Resource Ownership
- Users can only access their own: orders, cart, addresses, reviews
- Admins can access all resources
- Validation in service layer

## Data Protection

### Encryption at Rest
- **Database**: PostgreSQL with TLS
- **Secrets**: Environment variables (not in code)
- **Passwords**: BCrypt hashed
- **M-Pesa Credentials**: Environment variables

### Encryption in Transit
- **HTTPS**: TLS 1.2+ enforced
- **HSTS**: Strict Transport Security header
- **Internal Communication**: Docker network isolation

### Sensitive Data Handling
- **No logging** of passwords, tokens, PII
- **Masked** in logs: `password=***`, `token=***`
- **PII**: Email, name, phone, address stored securely

## Input Validation & Sanitization

### Request Validation
- **Bean Validation (JSR-380)**: @Valid on all DTOs
- **Custom Validators**: Business rule validation
- **Size Limits**: Prevent oversized payloads

### SQL Injection Prevention
- **JPA/Hibernate**: Parameterized queries
- **No Raw SQL**: Except Flyway migrations
- **Repository Methods**: Type-safe queries

### XSS Prevention
- **Content Security Policy**: Restrictive headers
- **Output Encoding**: Jackson JSON serialization
- **No HTML in API**: JSON-only responses

## Rate Limiting & DDoS Protection

### Endpoint Limits
- **Auth Endpoints**: 10 requests/minute
- **API Endpoints**: 100 requests/minute
- **Per IP**: Sliding window

### Implementation
- **Spring Boot Rate Limiter**: Token bucket
- **Redis Backend**: Distributed rate limiting
- **Custom Key Generator**: IP + endpoint

## Security Headers

```http
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'; frame-ancestors 'none';
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

## M-Pesa Security

### STK Push
- **Shortcode**: Registered business paybill
- **Passkey**: Base64 encoded for password generation
- **Callback Validation**: Verify checksum
- **Idempotency**: CheckoutRequestID tracking

### Callback Verification
```java
// Verify callback authenticity
String checksum = generateChecksum(callbackData, passkey);
if (!callbackChecksum.equals(checksum)) {
    throw new SecurityException("Invalid callback checksum");
}
```

## Audit Logging

### Events Logged
- User registration, login, logout
- Password changes, 2FA enable/disable
- Order creation, payment, cancellation
- Admin actions (user management, product changes)
- Failed login attempts, rate limit hits

### Log Format
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "INFO",
  "event": "USER_LOGIN",
  "userId": "uuid",
  "ip": "192.168.1.1",
  "userAgent": "Mozilla/5.0...",
  "success": true
}
```

## Vulnerability Management

### Dependency Scanning
```bash
# OWASP Dependency Check
mvn dependency-check:check

# GitHub Dependabot (configured in .github/dependabot.yml)
```

### Container Scanning
```bash
# Trivy scan
trivy image iloveshopping/backend:latest

# Docker Scout
docker scout cves iloveshopping/backend:latest
```

### Code Analysis
```bash
# SonarQube
mvn sonar:sonar

# SpotBugs
mvn spotbugs:check
```

## Incident Response

### Security Incident Types
1. **Data Breach**: Unauthorized data access
2. **Account Takeover**: Compromised credentials
3. **Payment Fraud**: M-Pesa callback manipulation
4. **DDoS**: Rate limit exhaustion
5. **Injection Attack**: SQL/XSS attempts

### Response Procedures
1. **Detect**: Monitoring alerts, user reports
2. **Contain**: Revoke tokens, block IPs, disable accounts
3. **Investigate**: Log analysis, root cause
4. **Remediate**: Patch, rotate secrets, deploy fixes
5. **Communicate**: Notify affected users, regulators
6. **Post-Mortem**: Document lessons learned

## Compliance Considerations

### GDPR (if applicable)
- **Data Minimization**: Collect only necessary data
- **Right to Access**: User data export endpoint
- **Right to Erasure**: Account deletion with data purge
- **Data Portability**: JSON export of user data
- **Privacy by Design**: Built into architecture

### PCI DSS (Payment Data)
- **No Card Storage**: M-Pesa handles card data
- **Tokenization**: M-Pesa transaction references
- **Network Segmentation**: Isolated payment processing

### Kenya Data Protection Act
- **Data Localization**: PostgreSQL in Kenya region
- **Consent**: Explicit for marketing communications
- **Breach Notification**: 72-hour requirement

## Security Testing Checklist

### Pre-Deployment
- [ ] Dependency vulnerability scan passes
- [ ] Container image scan passes
- [ ] SAST/DAST scans pass
- [ ] Penetration test completed
- [ ] Secrets scan (no hardcoded secrets)

### Runtime
- [ ] WAF rules configured
- [ ] Monitoring alerts configured
- [ ] Incident response plan documented
- [ ] Backup/restore tested
- [ ] SSL certificates valid (>30 days)

### Ongoing
- [ ] Monthly dependency updates
- [ ] Quarterly penetration tests
- [ ] Annual security audit
- [ ] Security training for team