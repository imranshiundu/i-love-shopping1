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
# creates .env (generating every required secret), shifts any busy
# port, and starts everything
bash start.sh            # Linux / macOS / Git Bash
start.cmd                # Windows

# With the observability stack (Grafana + Prometheus + Loki, alerts by email)
bash start.sh --observability

# Force a rebuild of the backend + frontend
bash start.sh --rebuild
```

Everything runs in the foreground - press `Ctrl+C` to stop, `bash start.sh --stop` cleans up (also stops the observability stack).

Alternative: run everything in Docker or start each service by hand - see [docs/deployment.md](docs/deployment.md) and [docs/configuration.md](docs/configuration.md).

Observability has its own repository and one-command startup - see [../observability/README.md](../observability/README.md) (or add `--observability` to the command above).

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

### Keys: required, optional, and how to update

`start.sh` generates every required secret on first boot — nothing to
do for a working demo. Optional keys unlock payments and integrations:

| Key (in `.env`) | Required | Without it | Update by |
|---|---|---|---|
| `DATA_ENCRYPTION_KEY` | **YES** | API refuses to start (fail-fast by design) | `openssl rand -base64 32`; generated automatically on first boot. Rotating invalidates old ciphertext — wipe volumes or keep the key |
| `JWT_ACCESS_SECRET` / `JWT_REFRESH_SECRET` | **YES** | API refuses to start | generated on first boot; rotate to force all users to re-login |
| `MPESA_CONSUMER_KEY/SECRET/PASSKEY` | optional | M-Pesa payments disabled | Daraja dashboard → restart |
| `STRIPE_SECRET_KEY` / `STRIPE_PUBLISHABLE_KEY` | optional | card payments disabled (test card 4242… works in test mode) | Stripe dashboard → restart |
| `RECAPTCHA_*` | optional | dev bypass keeps registration open | Google console → restart |
| `GOOGLE_*` / `GITHUB_*` | optional | social login buttons hidden | provider console → restart |
| `MAIL_*` | optional | auto-falls back to MailHog | any SMTP → restart |

**Update a key:** edit `.env`, restart the affected service
(`./start.sh --stop && ./start.sh`, or `docker compose -f docker/docker-compose.yml up -d --force-recreate api`).
The full key table lives in [docs/configuration.md](docs/configuration.md).

### Observability

The [observability repo](https://github.com/imranshiundu/observability)
monitors this platform: Prometheus + Loki + Grafana with four dashboards
(business, product, technical, security), email alerting through MailHog,
seed history and a traffic simulator. One command from that repo:

```bash
./start.sh    # clones and starts this platform automatically if missing
``

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
