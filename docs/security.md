# Security

## Authentication & Security
- ✅ Email/password registration with email verification
- ✅ JWT access tokens (15 min) + refresh tokens (7 days)
- ✅ Refresh token rotation (single-use)
- ✅ Token revocation (logout, password change, admin action)
- ✅ OAuth2 login (Google, GitHub)
- ✅ TOTP-based 2FA (Google Authenticator compatible)
- ✅ Google reCAPTCHA v3 on registration
- ✅ Password reset via email
- ✅ BCrypt password hashing (cost 12)
- ✅ CORS configuration
- ✅ Helmet-style security headers
- ✅ Rate limiting on auth endpoints

## Security & reliability
- **Encryption at rest** - order shipping/billing addresses and payment metadata/callback records are encrypted with AES-256-GCM (`DATA_ENCRYPTION_KEY`). Raw database rows are ciphertext; authorised API readers receive transparently decrypted values.
- **Dead-letter queue** - RabbitMQ order queues dead-letter to `order.dead-letter` after exhausted retries, so failed messages are never silently dropped.
- **Per-IP rate limiting** on authentication and API traffic.
- **CORS lockdown** - the API echoes only explicitly configured origins (`CORS_ALLOWED_ORIGINS`); no wildcard-with-credentials.
- **JWT aligned to spec** - 15-minute access tokens, 7-day refresh tokens with single-use rotation and reuse detection.

## Security

## Implemented Security Measures

- **JWT Tokens**: HS256 with 256-bit secrets, short-lived access tokens
- **Refresh Token Rotation**: Single-use, automatic revocation on reuse
- **Password Hashing**: BCrypt with cost factor 12
- **2FA**: TOTP (RFC 6238) with 30-second windows
- **CAPTCHA**: Google reCAPTCHA v3 score-based verification
- **CORS**: Configured allowed origins, credentials support
- **Rate Limiting**: Per-IP limits on auth endpoints
- **Security Headers**: CSP, X-Frame-Options, X-Content-Type-Options
- **Input Validation**: Bean Validation (JSR-380) on all DTOs
- **SQL Injection Prevention**: Parameterized queries via JPA
- **XSS Prevention**: Output encoding, CSP headers

## Security Checklist for Production

- [ ] Rotate JWT secrets regularly
- [ ] Use HTTPS only (configure TLS termination)
- [ ] Set secure cookie flags (`Secure`, `HttpOnly`, `SameSite=Strict`)
- [ ] Configure proper CSP for your frontend domain
- [ ] Enable audit logging for sensitive operations
- [ ] Set up WAF rules for API endpoints
- [ ] Regular dependency vulnerability scanning (`mvn dependency-check`)
- [ ] Configure proper CORS origins (no wildcards)
