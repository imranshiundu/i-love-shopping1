# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Dev scripts now auto-start Docker Desktop (macOS/Windows) or the Docker
  service (Linux) when the daemon is not running, instead of just erroring
- Dev scripts now auto-install Java 21, Maven and Node.js 18+ when missing
  (via Homebrew on macOS, Chocolatey on Windows, apt/dnf on Linux)
- `--auto` flag for unattended setup: runs option 2 with no prompts
- `--stop` flag to cleanly shut down all services and containers
- `.env` file auto-created from `.env.example` with generated JWT secrets
- Node.js version check tightened to 18+ (was too lenient)
- Multi-currency display: header selector with KES (base), USD, EUR, GBP,
  TZS, UGX and ZAR; rates configurable via NEXT_PUBLIC_CURRENCY_RATES;
  checkout states clearly that payments settle in KES
- Documented seeded test accounts in the README
- Flutterwave (create-payment/verify) and Airtel Money (initiate/confirm)
  simulated payment rails alongside Stripe and M-Pesa
- M-Pesa sandbox simulation mode so STK push completes locally
  (MPESA_SIMULATION_ENABLED, on by default)
- Encryption at rest: order addresses and payment metadata/callback data
  are AES-256-GCM encrypted with DATA_ENCRYPTION_KEY and decrypted
  transparently for authorised API readers
- RabbitMQ dead-letter queue for exhausted message retries
- Admin analytics dashboard (revenue, awaiting payment, losses, AOV,
  7-day chart, best sellers, low stock) and bulk offers engine
- User account dashboard with sidebar layout and a dedicated Settings page
- Guest checkout end to end, including anonymous M-Pesa/card payment
- Client-side card validation (Luhn, expiry, CVV) before card payments
- Cart recommendations based on cart contents; checkout address prefill
  for logged-in users; order history period filters
- Redesigned login/register/forgot-password screens with redirect support;
  restricted-area screen for non-admins opening /admin
- Smooth page and section transitions across storefront and admin

### Changed
- Payment events publish after transaction commit so queue consumers never
  race the database write

### Added
- Initial project structure with Spring Boot 3.2.x
- JWT authentication with access/refresh tokens
- M-Pesa Daraja STK Push payment integration
- TOTP-based Two-Factor Authentication
- Google reCAPTCHA v3 integration
- Google/GitHub OAuth2 login (enabled only when client IDs are configured)
- Product catalog with faceted search
- Shopping cart with stock validation
- Order management with checkout flow
- User profiles, addresses, reviews
- PostgreSQL + Flyway migrations
- Docker development and production configurations
- Nginx reverse proxy with rate limiting
- Comprehensive test suite (48 unit tests)
- Next.js 14 storefront: home, product listing/detail, cart, single-page checkout,
  auth pages, account area (profile/orders/addresses) and role-gated admin area
  (dashboard, orders, products, categories, brands)
- Frontend configuration fully driven by `NEXT_PUBLIC_*` environment variables via `src/lib/config.ts`
- Admin REST endpoints for orders/users plus catalog CRUD guarded by `@PreAuthorize`
- Simulated Stripe and PayPal payment endpoints (create/confirm/capture/webhook)
- RabbitMQ order event flow: checkout publishes `order.created`, successful payments publish
  `order.paid`, cancellations publish `order.cancelled`; a consumer moves orders PENDING to
  CONFIRMED and confirmation emails are sent on payment success
- Per-IP rate limiting filter registered in the security chain
- Thymeleaf email templates: verification, password reset, order confirmation, 2FA codes
- Multi-stage frontend Dockerfile and frontend service in docker-compose

### Changed
- RabbitMQ connection settings moved from `mpesa.rabbitmq` to `spring.rabbitmq`
- SMTP auth/starttls are now env-configurable (`MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`) so Mailhog works without credentials
- Dev setup script now includes RabbitMQ and the frontend; prerequisites check covers Node.js

### Fixed
- Circular dependency at startup between SecurityConfig, OAuth2 success handler and PasswordEncoder
  (PasswordEncoder extracted to its own configuration class)
- API failing to start when OAuth2 client IDs are absent (oauth2Login now conditional)
- Order status never leaving PENDING after a successful simulated payment
- Missing order confirmation emails after checkout/payment
- Search suggestions parameter mismatch between frontend (`q`) and backend (`query`)

### Removed
- N/A

## [1.0.0] - 2024-01-XX

### Added
- Initial release of i-love-shopping B2C e-commerce platform
- Core e-commerce functionality for Kenyan market
- M-Pesa Daraja API integration for mobile payments
- Complete authentication and authorization system
- Product catalog with categories, brands, search
- Shopping cart and order management
- Admin dashboard capabilities
- Docker containerization for deployment
- Comprehensive documentation and API specs

---

## Release Notes Format

### Added
- New features

### Changed
- Changes in existing functionality

### Fixed
- Bug fixes

### Removed
- Removed features

### Deprecated
- Soon-to-be removed features

### Security
- Security improvements