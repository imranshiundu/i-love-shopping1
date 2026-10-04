# Architecture

The application follows a modular monolith architecture with clean separation of concerns:

```
┌──────────────────────┐      ┌────────────────────────────────────────────────┐
│   Next.js Frontend    │ HTTP │            Spring Boot Application              │
│   localhost:3000      │─────▶│                localhost:8080/api/v1            │
│  customer + admin UI  │ CORS ├────────────────────────────────────────────────┤
└──────────────────────┘      │  Controllers  │ Services  │ Repositories        │
                              ├────────────────────────────────────────────────┤
                              │     Security Layer (JWT, OAuth2, 2FA)           │
                              ├────────────────────────────────────────────────┤
                              │ PostgreSQL │ Redis │ RabbitMQ │ Email (SMTP)    │
                              └───────────────┬───────────────────┬─────────────┘
                                              │                   │
                                     ┌────────▼────────┐  ┌───────▼──────────┐
                                     │ M-Pesa Daraja   │  │ Stripe           │
                                      │ (sandbox)       │  │ Stripe cards     │
                                     └─────────────────┘  └──────────────────┘
```

Order state flows through RabbitMQ: checkout publishes `order.created`, successful payments publish `order.paid`, and a consumer updates the order to CONFIRMED and triggers the confirmation email.

### Module Structure

| Module | Responsibility |
|--------|----------------|
| `auth` | User registration, login, JWT tokens, OAuth2, 2FA, CAPTCHA |
| `catalog` | Products, categories, brands, search, filtering |
| `cart` | Shopping cart management |
| `orders` | Checkout, order management, order history |
| `payments` | M-Pesa STK Push, callbacks, payment status |
| `users` | Profile management, addresses, password changes |
| `reviews` | Product reviews, helpfulness votes and moderation |
| `review_votes` | One helpful vote per user per review |
| `shipping_methods` | Admin-managed delivery options (Standard/Express/Pickup) |

## Entity Relationship Diagram

```mermaid
erDiagram
    USER ||--o{ SESSION : has
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
        string email_verification_token
        datetime email_verification_expires_at
        string password_reset_token
        datetime password_reset_expires_at
        string two_factor_secret
        boolean two_factor_enabled
        string clerk_id UK
        string roles
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
        uuid user_id FK, UK
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
        integer helpful_count
        varchar status
        datetime created_at
        datetime updated_at
    }

    REVIEW ||--o{ REVIEW_VOTE : receives
    USER ||--o{ REVIEW_VOTE : casts
    REVIEW_VOTE {
        uuid id PK
        uuid review_id FK
        uuid user_id FK
        datetime created_at
    }

    ORDER ||--o{ SHIPPING_METHOD : uses
    SHIPPING_METHOD {
        uuid id PK
        string name
        string description
        decimal cost
        string estimated_days
        boolean active
        integer display_order
        datetime created_at
        datetime updated_at
    }
```

## Technology Stack

### Backend
- **Java 21** - Language
- **Spring Boot 3.3.x** - Framework
- **Spring Security 6** - Authentication & Authorization
- **Spring Data JPA** - Database ORM
- **Spring AMQP** - RabbitMQ integration for order events
- **Spring Web** - REST API
- **Spring Mail + Thymeleaf** - Transactional emails (verification, password reset, order confirmation, 2FA codes)
- **Flyway** - Database migrations
- **Hibernate** - JPA Provider
- **PostgreSQL 16** - Primary Database
- **Redis 7** - Caching & Sessions
- **RabbitMQ 3** - Message queue for order/payment events
- **JJWT (0.12.5)** - JWT Token handling
- **M-Pesa Daraja API** - Mobile payments (sandbox/production)
- **Stripe** - Card payments (test/live mode)
- **Stripe** - Card payments (test/live mode)
- **Google reCAPTCHA** - Bot protection
- **Lombok** - Boilerplate reduction

### Frontend
- **Next.js 14 (App Router)** - React framework
- **TypeScript** - Type safety
- **Tailwind CSS** - Styling with Outfit typeface
- **react-icons** - Icon set (no emojis)
- **react-hot-toast** - Notifications
- All runtime values are injected via `NEXT_PUBLIC_*` environment variables (see [Configuration](#configuration)) - nothing is hardcoded

### Build & Deployment
- **Maven** (with wrapper) - Backend build tool
- **Docker + Docker Compose** - Containerization for backend, frontend and all dependencies

## Project Structure

```
i-love-shopping/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/iloveshopping/
│   │   │   │   ├── config/          # Configuration (RabbitMQ, security beans, properties)
│   │   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── exception/       # Custom exceptions
│   │   │   │   ├── filter/          # Rate limiting filter
│   │   │   │   ├── messaging/       # RabbitMQ publisher & consumers
│   │   │   │   ├── repository/      # Spring Data repositories
│   │   │   │   ├── security/        # JWT, OAuth2, security config
│   │   │   │   ├── service/         # Business logic
│   │   │   │   ├── util/            # Utility classes
│   │   │   │   └── validation/      # Address + input validators
│   │   │   └── resources/
│   │   │       ├── db/migration/    # Flyway SQL migrations + seed data (V1..V13)
│   │   │       ├── application.yml  # Main configuration
│   │   │       └── templates/email/ # Thymeleaf emails (verification, reset, invoice, confirmation, 2FA)
│   │   └── test/
│   │       └── java/...             # Unit & integration tests
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd              # Maven wrapper (no global Maven needed)
│   └── .env.example
├── frontend/
│   ├── src/
│   │   ├── app/                     # Next.js App Router pages
│   │   │   ├── page.tsx             # Home
│   │   │   ├── products/            # Listing + detail pages
│   │   │   ├── cart/                # Cart
│   │   │   ├── checkout/            # Checkout + success page
│   │   │   ├── auth/                # Forgot/reset password, verify email (login+register are a global modal)
│   │   │   ├── account/             # Profile, orders (+invoice/receipt documents), addresses
│   │   │   ├── oauth2/redirect/     # OAuth callback (Google/GitHub token handoff)
│   │   │   └── admin/               # Admin dashboard (role-gated layout)
│   │   ├── components/              # Header, footer, UI primitives
│   │   ├── components/auth/         # Sign-in/register modal + host
│   │   ├── contexts/                # Auth + cart context
│   │   ├── services/                # API client, cart service
│   │   ├── lib/                     # config.ts (all env vars), captcha.ts, utils
│   │   └── types/                   # Shared TypeScript types
│   ├── Dockerfile                   # Multi-stage Node build
│   ├── next.config.js               # API rewrites + image allowlist from env
│   ├── tailwind.config.js           # Outfit font, primary palette, tinted shadows
│   └── .env.local                   # All NEXT_PUBLIC_* configuration (dev defaults)
├── docker/
│   ├── docker-compose.yml           # Dev compose (PostgreSQL, Redis, Mailhog, RabbitMQ, API, frontend)
│   ├── docker-compose.prod.yml      # Production compose (Nginx, no Mailhog)
│   └── nginx/                       # Nginx reverse proxy config
├── scripts/
│   ├── dev.sh                       # Dev setup script (Linux/macOS/Git Bash)
│   ├── dev.cmd                      # Dev setup script (Windows)
│   ├── check-secrets.sh             # Secret scanning helper
│   └── ...
├── start.sh / start.cmd             # One-command entry points (forward to scripts/dev.*)
├── docs/                            # Architecture, deployment, security & API docs
├── .gitignore
└── README.md
```
