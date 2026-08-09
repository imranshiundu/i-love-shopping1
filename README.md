# i-love-shopping

B2C E-commerce Platform for the Kenyan market, built with Spring Boot 3, PostgreSQL, and M-Pesa Daraja API integration.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Entity Relationship Diagram](#entity-relationship-diagram)
- [Technology Stack](#technology-stack)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Docker Deployment](#docker-deployment)
- [Project Structure](#project-structure)
- [Security](#security)
- [License](#license)

## Overview

i-love-shopping is a full-featured B2C e-commerce platform designed for the Kenyan market. It provides a complete shopping experience from product discovery to checkout with M-Pesa integration, user authentication with 2FA support, and admin management capabilities.

### Key Highlights

- **M-Pesa Daraja Integration** - Full STK Push payment flow with callback handling
- **JWT Authentication** - Access/refresh tokens with rotation and revocation
- **Two-Factor Authentication** - TOTP-based 2FA with Google Authenticator support
- **CAPTCHA Protection** - Google reCAPTCHA v3 integration
- **OAuth2 Login** - Google and GitHub OAuth2 support
- **Product Catalog** - Categories, brands, faceted search, filtering, sorting
- **Shopping Cart** - Session and user-based carts with stock validation
- **Order Management** - Complete checkout flow with order tracking
- **Admin Dashboard** - Product, category, brand, order management

## Architecture

The application follows a modular monolith architecture with clean separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                      Spring Boot Application                 │
├─────────────────────────────────────────────────────────────┤
│  Controllers  │  Services  │  Repositories  │  Entities      │
├─────────────────────────────────────────────────────────────┤
│              Security Layer (JWT, OAuth2, 2FA)              │
├─────────────────────────────────────────────────────────────┤
│  PostgreSQL  │  Redis  │  M-Pesa Daraja API  │  Email (SMTP)│
└─────────────────────────────────────────────────────────────┘
```

### Module Structure

| Module | Responsibility |
|--------|----------------|
| `auth` | User registration, login, JWT tokens, OAuth2, 2FA, CAPTCHA |
| `catalog` | Products, categories, brands, search, filtering |
| `cart` | Shopping cart management |
| `orders` | Checkout, order management, order history |
| `payments` | M-Pesa STK Push, callbacks, payment status |
| `users` | Profile management, addresses, password changes |
| `reviews` | Product reviews and ratings |

## Entity Relationship Diagram

```mermaid
erDiagram
    USER ||--o{ SESSION : has
    USER ||--o{ REFRESH_TOKEN : has
    USER ||--o{ ADDRESS : has
    USER ||--o{ CART : has
    USER ||--o{ ORDER : places
    USER ||--o{ REVIEW : writes
    USER {
        uuid id PK
        string email UK
        string password_hash
        string name
        string avatar
        datetime email_verified
        string two_factor_secret
        boolean two_factor_enabled
        string[] roles
        datetime created_at
        datetime updated_at
    }

    SESSION {
        uuid id PK
        uuid user_id FK
        string refresh_token_hash UK
        string user_agent
        string ip
        datetime expires_at
        datetime revoked_at
        datetime created_at
    }

    REFRESH_TOKEN {
        uuid id PK
        string token_hash UK
        uuid user_id FK
        datetime expires_at
        datetime revoked_at
        datetime created_at
    }

    CATEGORY ||--o{ CATEGORY : "parent"
    CATEGORY ||--o{ PRODUCT : contains
    CATEGORY {
        uuid id PK
        string name
        string slug UK
        string description
        string image
        integer sort_order
        uuid parent_id FK
        datetime created_at
        datetime updated_at
    }

    BRAND ||--o{ PRODUCT : has
    BRAND {
        uuid id PK
        string name
        string slug UK
        string logo
        string description
        datetime created_at
        datetime updated_at
    }

    PRODUCT ||--o{ PRODUCT_IMAGE : has
    PRODUCT ||--o{ CART_ITEM : "in cart"
    PRODUCT ||--o{ ORDER_ITEM : "in order"
    PRODUCT ||--o{ REVIEW : receives
    PRODUCT {
        uuid id PK
        string name
        string slug UK
        string description
        decimal price
        decimal compare_at_price
        string sku UK
        integer stock
        decimal weight
        jsonb dimensions
        boolean is_active
        uuid category_id FK
        uuid brand_id FK
        datetime created_at
        datetime updated_at
    }

    PRODUCT_IMAGE {
        uuid id PK
        uuid product_id FK
        string url
        string alt
        integer sort_order
        datetime created_at
    }

    CART ||--o{ CART_ITEM : contains
    CART {
        uuid id PK
        uuid user_id UK FK
        string session_id UK
        datetime created_at
        datetime updated_at
    }

    CART_ITEM {
        uuid id PK
        uuid cart_id FK
        uuid product_id FK
        string variant_id
        integer quantity
        decimal price_snapshot
        datetime created_at
        datetime updated_at
    }

    ORDER ||--o{ ORDER_ITEM : contains
    ORDER ||--o{ PAYMENT : has
    ORDER {
        uuid id PK
        string number UK
        uuid user_id FK
        enum status
        decimal subtotal
        decimal tax
        decimal shipping
        decimal total
        string currency
        jsonb shipping_address
        jsonb billing_address
        string notes
        datetime created_at
        datetime updated_at
    }

    ORDER_ITEM {
        uuid id PK
        uuid order_id FK
        uuid product_id FK
        string variant_id
        string name
        decimal price
        integer quantity
        decimal total
        datetime created_at
    }

    PAYMENT {
        uuid id PK
        uuid order_id FK
        enum provider
        string provider_id
        decimal amount
        string currency
        enum status
        jsonb metadata
        jsonb callback_data
        datetime created_at
        datetime updated_at
    }

    ADDRESS {
        uuid id PK
        uuid user_id FK
        enum type
        string name
        string line1
        string line2
        string city
        string state
        string postal_code
        string country
        string phone
        boolean is_default
        datetime created_at
        datetime updated_at
    }

    REVIEW {
        uuid id PK
        uuid product_id FK
        uuid user_id FK
        integer rating
        string title
        string content
        boolean is_verified_purchase
        datetime created_at
        datetime updated_at
    }
```

## Technology Stack

### Backend
- **Java 21** - Language
- **Spring Boot 3.2.x** - Framework
- **Spring Security 6** - Authentication & Authorization
- **Spring Data JPA** - Database ORM
- **Spring Web** - REST API
- **Spring Mail** - Email sending
- **Spring Actuator** - Health checks & metrics
- **Flyway** - Database migrations
- **Hibernate** - JPA Provider
- **PostgreSQL 16** - Primary Database
- **Redis 7** - Caching & Sessions
- **JJWT (0.12.5)** - JWT Token handling
- **M-Pesa Daraja API** - Mobile payments
- **Google reCAPTCHA** - Bot protection
- **MapStruct** - Object mapping
- **Lombok** - Boilerplate reduction
- **Testcontainers** - Integration testing

### Build & Deployment
- **Maven** - Build tool
- **Docker** - Containerization
- **Docker Compose** - Local development
- **Jib** - Container image building

## Features

### Authentication & Security
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

### Product Catalog
- ✅ Hierarchical categories with unlimited depth
- ✅ Brand management
- ✅ Product CRUD with images
- ✅ Stock tracking with atomic decrement/increment
- ✅ Sale pricing with compare-at price
- ✅ Weight & dimensions (metric/imperial)
- ✅ Full-text search on name & description
- ✅ Faceted filtering (category, brand, price, stock, sale)
- ✅ Sorting (relevance, price, newest, rating)
- ✅ Search suggestions/autocomplete
- ✅ Similar products recommendations
- ✅ Pagination with cursor support

### Shopping Cart
- ✅ User carts (authenticated)
- ✅ Guest carts (session-based)
- ✅ Quantity validation against stock
- ✅ Price snapshots (protects against price changes)
- ✅ Variant support
- ✅ Cart merging on login

### Orders & Checkout
- ✅ Multi-step checkout (shipping/billing addresses)
- ✅ Address book with default addresses
- ✅ Tax calculation (configurable rate)
- ✅ Shipping calculation (free over threshold)
- ✅ Order number generation (prefix + timestamp + random)
- ✅ Order status workflow (PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED)
- ✅ Order cancellation (before processing)
- ✅ Order history with pagination

### M-Pesa Payments
- ✅ STK Push initiation
- ✅ Callback processing (success/failure/timeout)
- ✅ Payment status polling
- ✅ Retry failed payments
- ✅ Payment metadata storage

### User Management
- ✅ Profile updates (name, avatar, email)
- ✅ Email change with re-verification
- ✅ Password change with session invalidation
- ✅ Address book (shipping/billing)
- ✅ Default address per type
- ✅ Order history with totals
- ✅ Product reviews (1-5 stars, verified purchase badge)

### Admin Features
- ✅ Role-based access control (USER, ADMIN, MODERATOR)
- ✅ Product/Category/Brand management
- ✅ Order status updates
- ✅ User management

## Prerequisites

- **Java 21** (JDK)
- **Maven 3.9+**
- **PostgreSQL 16** (or Docker)
- **Redis 7** (or Docker)
- **Docker & Docker Compose** (optional, for containerized deployment)

### External Services (Required for Full Functionality)
- **M-Pesa Daraja API** credentials (Consumer Key, Secret, Shortcode, Passkey)
- **Google reCAPTCHA** (Site Key, Secret Key)
- **Google OAuth2** (Client ID, Secret)
- **GitHub OAuth2** (Client ID, Secret)
- **SMTP Server** for emails (Mailhog for development)

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/i-love-shopping.git
cd i-love-shopping
```

### 2. Configure Environment

Copy the example environment file and configure:

```bash
cp backend/.env.example backend/.env
```

Edit `backend/.env` with your credentials:

```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/iloveshopping
DATABASE_USER=iloveshopping
DATABASE_PASSWORD=secure_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT (generate with: openssl rand -base64 32)
JWT_ACCESS_SECRET=your-access-secret-32-chars-minimum
JWT_REFRESH_SECRET=your-refresh-secret-32-chars-minimum

# reCAPTCHA
RECAPTCHA_SECRET_KEY=your-recaptcha-secret
RECAPTCHA_SITE_KEY=your-recaptcha-site-key

# OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@yourdomain.com

# M-Pesa Daraja (Sandbox)
MPESA_CONSUMER_KEY=your-consumer-key
MPESA_CONSUMER_SECRET=your-consumer-secret
MPESA_SHORTCODE=174379
MPESA_PASSKEY=your-passkey
MPESA_CALLBACK_URL=https://yourdomain.com/api/v1/orders/payments/mpesa/callback
```

### 3. Run with Docker Compose (Recommended)

```bash
docker-compose -f docker/docker-compose.yml up -d
```

This starts:
- PostgreSQL on port 5432
- Redis on port 6379
- Mailhog on ports 1025 (SMTP) / 8025 (Web UI)
- Spring Boot API on port 8080

### 4. Run Locally (Without Docker)

```bash
# Start dependencies
docker-compose -f docker/docker-compose.yml up -d postgres redis mailhog

# Run the application
cd backend
./mvnw spring-boot:run
```

The API will be available at: `http://localhost:8080/api/v1`

### 5. Verify Installation

```bash
# Health check
curl http://localhost:8080/api/v1/health

# API Documentation (Swagger UI)
open http://localhost:8080/api/v1/docs
```

## Configuration

### Application Profiles

| Profile | Description |
|---------|-------------|
| `development` | Local development with H2/PostgreSQL |
| `test` | Testcontainers-based integration tests |
| `docker` | Docker deployment with container networking |

### Key Configuration Properties

```yaml
# Database
spring.datasource.url: jdbc:postgresql://localhost:5432/iloveshopping
spring.datasource.username: iloveshopping
spring.datasource.password: ${DATABASE_PASSWORD}

# JWT
security.jwt.access-secret: ${JWT_ACCESS_SECRET}
security.jwt.refresh-secret: ${JWT_REFRESH_SECRET}
security.jwt.access-expiry-minutes: 15
security.jwt.refresh-expiry-days: 7

# M-Pesa
mpesa.environment: sandbox
mpesa.consumer-key: ${MPESA_CONSUMER_KEY}
mpesa.consumer-secret: ${MPESA_CONSUMER_SECRET}
mpesa.shortcode: ${MPESA_SHORTCODE}
mpesa.passkey: ${MPESA_PASSKEY}
mpesa.callback-url: ${MPESA_CALLBACK_URL}

# Rate Limiting
security.rate-limit.auth-requests-per-minute: 10
security.rate-limit.api-requests-per-minute: 100
```

## API Documentation

### Interactive Documentation

Swagger UI is available at: `http://localhost:8080/api/v1/docs`

### Core Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/auth/register` | Register new user | No |
| `POST` | `/auth/login` | Login user | No |
| `POST` | `/auth/refresh` | Refresh access token | No |
| `POST` | `/auth/logout` | Logout & revoke session | Yes |
| `POST` | `/auth/forgot-password` | Request password reset | No |
| `POST` | `/auth/reset-password` | Reset password with token | No |
| `GET` | `/auth/verify-email` | Verify email with token | No |
| `POST` | `/auth/2fa/setup` | Get 2FA secret & QR code | Yes |
| `POST` | `/auth/2fa/enable` | Enable 2FA | Yes |
| `GET` | `/categories` | List all categories | No |
| `GET` | `/categories/{slug}` | Get category by slug | No |
| `GET` | `/brands` | List all brands | No |
| `GET` | `/products` | Search & filter products | No |
| `GET` | `/products/{slug}` | Get product details | No |
| `GET` | `/products/search/suggestions` | Search autocomplete | No |
| `GET` | `/cart` | Get current cart | Yes |
| `POST` | `/cart/items` | Add item to cart | Yes |
| `PATCH` | `/cart/items/{id}` | Update cart item | Yes |
| `DELETE` | `/cart/items/{id}` | Remove cart item | Yes |
| `POST` | `/orders/checkout` | Checkout (create order) | Yes |
| `GET` | `/orders` | List user orders | Yes |
| `GET` | `/orders/{number}` | Get order details | Yes |
| `POST` | `/orders/{number}/cancel` | Cancel order | Yes |
| `POST` | `/orders/payments/mpesa/stk-push` | Initiate M-Pesa payment | Yes |
| `GET` | `/user/profile` | Get user profile | Yes |
| `PUT` | `/user/profile` | Update profile | Yes |
| `POST` | `/user/password` | Change password | Yes |
| `GET` | `/user/addresses` | List addresses | Yes |
| `POST` | `/user/addresses` | Add address | Yes |

### Example Requests

#### Register User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "name": "John Doe",
    "captchaToken": "recaptcha-token-from-frontend"
  }'
```

#### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

#### Search Products

```bash
curl "http://localhost:8080/api/v1/products?query=ceramic&minPrice=10&maxPrice=100&sortBy=price_asc&page=0&size=20"
```

#### Add to Cart

```bash
curl -X POST http://localhost:8080/api/v1/cart/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access-token>" \
  -d '{
    "productId": "uuid-of-product",
    "quantity": 2
  }'
```

#### Checkout

```bash
curl -X POST http://localhost:8080/api/v1/orders/checkout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access-token>" \
  -d '{
    "shippingAddress": {
      "name": "John Doe",
      "line1": "123 Main St",
      "city": "Nairobi",
      "state": "Nairobi County",
      "postalCode": "00100",
      "country": "KE",
      "phone": "+254700000000"
    },
    "billingAddress": {
      "name": "John Doe",
      "line1": "123 Main St",
      "city": "Nairobi",
      "state": "Nairobi County",
      "postalCode": "00100",
      "country": "KE"
    }
  }'
```

#### Initiate M-Pesa Payment

```bash
curl -X POST http://localhost:8080/api/v1/orders/payments/mpesa/stk-push \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access-token>" \
  -d '{
    "orderId": "uuid-of-order",
    "amount": "4720.00",
    "phoneNumber": "254712345678",
    "accountReference": "ORD-12345",
    "transactionDesc": "Payment for order ORD-12345"
  }'
```

## Testing

### Run All Tests

```bash
cd backend
./mvnw test
```

### Run Specific Test Classes

```bash
# Unit tests
./mvnw test -Dtest=JwtServiceTest
./mvnw test -Dtest=ProductTest
./mvnw test -Dtest=AuthValidationTest
./mvnw test -Dtest=SecurityTest

# With coverage
./mvnw test jacoco:report
```

### Test Reports

- **Surefire Reports**: `backend/target/surefire-reports/`
- **JaCoCo Coverage**: `backend/target/site/jacoco/index.html`

### Test Categories

| Test Class | Category | Description |
|------------|----------|-------------|
| `JwtServiceTest` | Unit | JWT token generation, validation, expiry |
| `AuthValidationTest` | Unit | Input validation for auth DTOs |
| `ProductTest` | Unit | Product entity business logic |
| `SecurityTest` | Unit | SQL injection, XSS, path traversal detection |
| `HealthCheckTest` | Unit | Health check response structure |

## Docker Deployment

### Build Images

```bash
# Build backend image
cd backend
./mvnw compile jib:dockerBuild -Dimage=iloveshopping/backend:latest

# Or build with Docker directly
docker build -t iloveshopping/backend:latest -f Dockerfile .
```

### Deploy with Docker Compose

```bash
# Production deployment
docker-compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d
```

### Environment-Specific Compose Files

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Development (with Mailhog) |
| `docker-compose.prod.yml` | Production (no Mailhog, production configs) |

### Health Checks

```bash
# Check service health
curl http://localhost:8080/api/v1/health

# Detailed health
curl http://localhost:8080/api/v1/health/detailed

# Kubernetes probes
curl http://localhost:8080/api/v1/ready
curl http://localhost:8080/api/v1/live
```

## Project Structure

```
i-love-shopping/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/iloveshopping/
│   │   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── exception/       # Custom exceptions
│   │   │   │   ├── repository/      # Spring Data repositories
│   │   │   │   ├── security/        # JWT, OAuth2, security config
│   │   │   │   ├── service/         # Business logic
│   │   │   │   └── util/            # Utility classes
│   │   │   └── resources/
│   │   │       ├── db/migration/    # Flyway SQL migrations
│   │   │       ├── application.yml  # Main configuration
│   │   │       └── templates/       # Thymeleaf email templates
│   │   └── test/
│   │       └── java/...             # Unit & integration tests
│   ├── Dockerfile
│   ├── pom.xml
│   └── .env.example
├── docker/
│   ├── docker-compose.yml
│   └── docker-compose.prod.yml
├── .gitignore
└── README.md
```

## Security

### Implemented Security Measures

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

### Security Checklist for Production

- [ ] Rotate JWT secrets regularly
- [ ] Use HTTPS only (configure TLS termination)
- [ ] Set secure cookie flags (`Secure`, `HttpOnly`, `SameSite=Strict`)
- [ ] Configure proper CSP for your frontend domain
- [ ] Enable audit logging for sensitive operations
- [ ] Set up WAF rules for API endpoints
- [ ] Regular dependency vulnerability scanning (`mvn dependency-check`)
- [ ] Configure proper CORS origins (no wildcards)

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

---

**Built with ❤️ for the Kenyan e-commerce ecosystem**