# Features

## Features

<img src="docs/images/mpesa-proof.jpg" width="300" align="right" alt="M-Pesa B2C Payment Proof">

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
- ✅ Weight & dimensions
- ✅ Full-text search on name & description
- ✅ Faceted filtering (category, brand, price, stock, sale)
- ✅ Sorting (relevance, price, newest, rating)
- ✅ Search suggestions/autocomplete
- ✅ Similar products recommendations
- ✅ Offset pagination

### Shopping Cart
- ✅ User carts (authenticated)
- ✅ Guest carts (session-based)
- ✅ Quantity validation against stock
- ✅ Price snapshots (protects against price changes)
- ✅ Variant support
- ✅ Cart merging on login

### Orders & Checkout
- ✅ Single-page checkout (shipping/billing addresses)
- ✅ Address book with default addresses
- ✅ Tax calculation (configurable rate)
- ✅ Shipping calculation (free over threshold)
- ✅ Order number generation (prefix + timestamp + random)
- ✅ Order status workflow (PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED, plus EXPIRED/CANCELLED/REFUNDED)
- ✅ Order cancellation (before processing)
- ✅ Order history with pagination

### M-Pesa Payments (African markets only)

- ✅ C2B Payments
- ✅ STK Push initiation
- ✅ Callback processing (success/failure/timeout)
- ✅ Payment status polling
- ✅ Retry failed payments
- ✅ Payment metadata storage

### Card Payments (Stripe)
- ✅ Stripe PaymentIntent create + confirm (test/live mode)
- ✅ Stripe webhook handling (`payment_intent.succeeded`, `payment_intent.payment_failed`)
- ✅ Test card: Stripe `4242 4242 4242 4242`
- ✅ No card data ever touches the server - PCI-friendly tokenized flow
- ✅ Failure scenarios: declined cards, gateway errors, invalid payment IDs

### Messaging & Async Processing
- ✅ RabbitMQ topic exchange `order.events` with durable queues
- ✅ `order.created` published at checkout, `order.paid` on successful payment, `order.cancelled` on cancellation
- ✅ Consumer updates order state (PENDING → CONFIRMED) and triggers confirmation emails
- ✅ Retry with exponential backoff on the listener container

### Frontend (Next.js)
- ✅ Home page with featured products and category tiles
- ✅ Product listing with faceted filters (category, brand, price, stock, sale), sorting and pagination
- ✅ Product detail page with image gallery, similar products and reviews
- ✅ Cart page with real-time totals, quantity updates and free-shipping threshold
- ✅ Single-page checkout: address form + payment method selection (M-Pesa, Stripe card)
- ✅ Order success page and order history
- ✅ Auth modal: sign-in/register tabs, 2FA code step, Google/GitHub buttons, forgot-password link
- ✅ Auth pages: forgot password, reset password, verify email, OAuth callback
- ✅ Account area: profile, addresses book, password change
- ✅ Admin area (role-gated): dashboard, orders, products, categories, brands
- ✅ Search autocomplete in the header, responsive layout, toast notifications
- ✅ Loading skeletons, empty states and inline error states throughout

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

## Beyond the brief

These go beyond the core requirements - added for real-world polish:

### Payments
- **Two payment rails at checkout** - M-Pesa Daraja (mobile money, **only in supported African markets**: Kenya, Tanzania, DRC, Mozambique, Lesotho, Ghana, Ethiopia) and card via Stripe (testable worldwide). Both selectable in a single payment step. (Flutterwave/Airtel entries were removed: enum-only placeholders with no implementation. Stripe covers Visa/Mastercard worldwide including Kenya; M-Pesa covers mobile money.)
- **Real M-Pesa Daraja** - STK push via Safaricom sandbox/production. Configure keys in `.env`. Callback polling fallback for local dev.
- **STK expiry watchdog** - Safaricom phone prompts last ~60–120s. A scheduled job (`MPESA_STK_TIMEOUT_SECONDS`, default 120) auto-marks unanswered STK sessions FAILED so orders never get stuck.
- **Pay-later invoices** - every unpaid order triggers a payable invoice email (`/checkout?retry=ORDER-NUMBER`); fresh invoices are re-sent on every failed/expired payment. Customers can leave mid-payment and resume anytime.
- **Unpaid order self-service** - customers can retry payment (PENDING/EXPIRED/CANCELLED with stock re-check), cancel (restores stock + cart), or permanently delete unpaid orders from their account.
- **Client-side card validation** - Luhn check, expiry and CVV validation happen in the browser before any request; card data is never sent to or stored on our servers.

### Security & reliability
- **Encryption at rest** - order shipping/billing addresses and payment metadata/callback records are encrypted with AES-256-GCM (`DATA_ENCRYPTION_KEY`). Raw database rows are ciphertext; authorised API readers receive transparently decrypted values.
- **Dead-letter queue** - RabbitMQ order queues dead-letter to `order.dead-letter` after exhausted retries, so failed messages are never silently dropped.
- **Per-IP rate limiting** on authentication and API traffic.
- **CORS lockdown** - the API echoes only explicitly configured origins (`CORS_ALLOWED_ORIGINS`); no wildcard-with-credentials.
- **JWT aligned to spec** - 15-minute access tokens, 7-day refresh tokens with single-use rotation and reuse detection.

### Auth & email extras (not in the brief)
- **Modal authentication** - sign-in/register live in a global modal (deep-linkable via `?auth=login&next=...`), including the 2FA code step and OAuth buttons.
- **OAuth end-to-end** - Google/GitHub buttons, `/oauth2/redirect` callback page, token handoff and guest-cart merge; providers stay hidden until you set keys on both sides.
- **GitHub-hosted product images** - catalogue imagery served from a dedicated CDN repo instead of placeholder services, so images survive redeploys.
- **Working email verification** - persisted single-use tokens (24h), `GET /auth/verify-email`, plus `POST /auth/resend-verification` which reuses a still-valid link.
- **Working password reset** - persisted single-use tokens (1h), session invalidation on reset, no account enumeration.
- **Real Gmail sender** - transactional mail goes through Gmail SMTP (`MAIL_*` in `.env`); MailHog remains a one-block dev toggle.
- **Payable invoice emails** - itemised invoice with a Pay-now link on every unpaid order and every failed/expired payment.
- **Printable invoice & receipt documents** - every order has a print-optimised invoice (`/account/orders/NUMBER/invoice`, with amount due and pay instructions); paid orders additionally get a receipt (`/account/orders/NUMBER/receipt`) with a PAID stamp and full payment details (provider, amount, reference, date). Both print to PDF straight from the browser.
- **Paid confirmation with payment details** - provider, amount paid and a formatted delivery address (never raw JSON or ciphertext).
- **2FA that actually verifies** - setup persists the QR secret; enable checks the code against it.
- **API slice tests** - MockMvc coverage for auth routing/validation alongside the unit suite.

### Storefront & admin experience
- **Multi-currency display** - KES base with USD, EUR, GBP, TZS, UGX and ZAR via a header switcher; rates are env-configurable (`NEXT_PUBLIC_CURRENCY_RATES`) and payments always settle in KES, stated clearly at checkout.
- **Admin analytics dashboard** - collected revenue vs awaiting-payment vs cancellations/refunds ("losses"), average order value, 7-day revenue chart, orders-by-status distribution, best sellers, low-stock watchlist and latest orders.
- **Offers engine** - launch bulk percentage promotions scoped to everything, a category or a brand, with live-offers tracking and one-click end-all.
- **User account dashboard** - KPI cards, recent orders, quick actions; profile and security live under Settings.
- **Guest checkout end-to-end** - visitors can buy without an account: enter an email in the delivery form and pay directly, no sign-up gate; orders attach to their session cart, sign-in stays one click away for saved addresses and order history.
- **Product image management in the admin form** - URL/alt rows with live preview, reorder and remove, sent as the images list on create and update; the first image becomes the card thumbnail. Stock accepts 0 (out of stock hides the buy button).
- **Honest payment status on confirmations** - the order confirmation page only claims money received when a payment record actually succeeded; manually confirmed orders show "Total due" and a pointer at the invoice email instead of a false "paid" badge.
- **Admin navigation complete** - Delivery options (/admin/shipping) and review moderation (/admin/reviews) are first-class sections in the sidebar, not hidden URLs.
- **Port-conflict-aware dev script** - every service auto-shifts to the next free port when defaults are taken. Docker daemon auto-starts on macOS, Windows and Linux if it is not already running.

### Images at rest & on the wire
- **Multi-size storage and serving** - uploads render thumb (256px), medium (512px) and full (1024px) variants served via `GET /api/v1/images/{size}/{file}`; storefront views fetch size-appropriate files (240px cart lines/preview/admin tables, 400px cards, 800px galleries) instead of re-using the original everywhere.
- **Uploads stay local** - the runtime `uploads/` directory is gitignored and excluded from Docker images via `.dockerignore`; image variants are reproducible from any upload.
