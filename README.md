# i-love-shopping

B2C e-commerce platform for the Kenyan market - Next.js 14 storefront, Spring Boot 3 API, PostgreSQL, Redis, RabbitMQ, M-Pesa Daraja and Stripe.

## Overview

i-love-shopping is a full-featured B2C e-commerce platform designed for the Kenyan market. It provides a complete shopping experience from product discovery to checkout with M-Pesa integration, user authentication with 2FA support, and admin management capabilities.

### Key Highlights

- **M-Pesa Daraja Integration** - Full STK Push payment flow with callback handling (African markets only)
- **JWT Authentication** - Access/refresh tokens with rotation and revocation
- **Two-Factor Authentication** - TOTP-based 2FA with Google Authenticator support
- **CAPTCHA Protection** - Google reCAPTCHA v3 integration
- **OAuth2 Login** - Google and GitHub OAuth2 support
- **Product Catalog** - Categories, brands, faceted search, filtering, sorting
- **Shopping Cart** - Session and user-based carts with stock validation
- **Order Management** - Complete checkout flow with order tracking
- **Admin Dashboard** - Product, category, brand, order management

## Quick start

```bash
git clone https://gitea.kood.tech/imranshiundu/i-love-shopping1.git
cd i-love-shopping

# One command: checks prerequisites, installs what is missing,
# creates .env, starts everything (interactive menu)
bash start.sh            # Linux / macOS / Git Bash
start.cmd                # Windows

# Or fully automated (Docker dependencies + local API & frontend)
bash start.sh --auto
```

Everything runs in the foreground - press `Ctrl+C` to stop, `bash start.sh --stop` cleans up.

Alternative: run everything in Docker or start each service by hand - see [docs/deployment.md](docs/deployment.md) and [docs/configuration.md](docs/configuration.md).

## What runs where

| Service | Address | Notes |
|---------|---------|-------|
| Frontend (Next.js) | `http://localhost:3000` | React UI - product browsing, cart, checkout, admin |
| Spring Boot API | `http://localhost:8080/api/v1` | REST API |
| Swagger UI | `http://localhost:8080/api/v1/docs` | Interactive API docs |
| PostgreSQL | `localhost:5433` | Database (container maps 5433 → 5432) |
| Redis | `localhost:6380` | Cache (container maps 6380 → 6379) |
| RabbitMQ | `localhost:5672` / `http://localhost:15672` | Message queue / Management UI (`iloveshopping` / `iloveshopping`) |
| Mailhog SMTP | `localhost:1025` | Catches all outgoing emails |
| Mailhog Web UI | `http://localhost:8025` | Read emails sent by the app |

## Verify it works

```bash
# API health check
curl http://localhost:8080/api/v1/health

# Frontend is up
curl -o /dev/null -w "%{http_code}\n" http://localhost:3000/

# Interactive API documentation
# Open http://localhost:8080/api/v1/docs in your browser
```

### Test Accounts

These accounts are seeded by Flyway migrations and are ready to use:

| Role | Email | Password | What you can do |
|------|-------|----------|-----------------|
| **Administrator** | `admin@iloveshopping.com` | `Admin123!` | Everything, plus `/admin`: dashboard analytics, orders lifecycle, product CRUD, offers engine, customers |
| **Customer** | `user@iloveshopping.com` | `User123!` | Browse, buy, review, manage profile and addresses |

You can also register a brand-new account via the **Create account** button in the
header (a modal — verification emails land in Mailhog during development),
or go through the entire shopping, checkout and payment journey as a
**guest** - no account needed until after you have paid.

> These credentials are for local development and reviewer environments only.
> Never ship seeded passwords to production.

Then open **http://localhost:3000** in your browser and:

1. Browse products, filter by category/brand/price on `/products`
2. Switch the display currency from the globe icon in the header
3. Add items to the cart and watch totals update in real time
4. Log in as `admin@iloveshopping.com` / `Admin123!` and place an order through checkout with the card option (needs Stripe test keys — see [Payment test keys](#payment-test-keys-sandbox))
5. Watch the order flip from PENDING to CONFIRMED in `/account/orders`, then check the confirmation email at http://localhost:8025 (Mailhog)
6. Visit `/admin` for the dashboard, orders, products, categories and brands management

## Documentation

| Document | Contents |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Architecture, module structure, ERD, technology stack, project layout |
| [docs/features.md](docs/features.md) | Full feature list + everything built beyond the brief |
| [docs/payments.md](docs/payments.md) | M-Pesa + Stripe: sandbox test keys, test payments, going live |
| [docs/security.md](docs/security.md) | Auth, 2FA, encryption at rest, rate limiting, production checklist |
| [docs/configuration.md](docs/configuration.md) | Prerequisites, external services/tokens, all environment variables |
| [docs/deployment.md](docs/deployment.md) | Docker (dev + prod compose), Nginx + self-signed TLS, health checks |
| [docs/testing.md](docs/testing.md) | Test suites, categories, k6 load tests, performance analysis |
| [docs/api.md](docs/api.md) | Swagger UI + core endpoints with example requests |
| [SETUP-PAYMENTS.md](SETUP-PAYMENTS.md) | Deep-dive payment setup with real API calls |

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [M-Pesa Daraja API](https://developer.safaricom.co.ke/)
- [Google reCAPTCHA](https://www.google.com/recaptcha/)
- [JJWT](https://github.com/jwtk/jjwt)
- [MapStruct](https://mapstruct.org/)
- [Testcontainers](https://testcontainers.com/)
- [Flyway](https://flywaydb.org/)
