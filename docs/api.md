# API Reference

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
| `GET` | `/auth/verify-email` | Verify email with token | No |
| `POST` | `/auth/resend-verification` | Resend verification email | No |
| `GET` | `/orders/{number}` | Get order details | Yes |
| `POST` | `/orders/{number}/cancel` | Cancel order | Yes |
| `DELETE` | `/orders/{number}` | Delete unpaid order | Yes |
| `POST` | `/orders/{number}/retry-payment` | Retry payment (re-opens unpaid order) | Yes |
| `POST` | `/orders/payments/mpesa/stk-push` | Initiate M-Pesa payment | Yes |
| `POST` | `/payments/stripe/create-intent` | Create Stripe PaymentIntent | Yes |
| `POST` | `/payments/stripe/confirm` | Confirm Stripe payment | Yes |
| `POST` | `/payments/stripe/webhook` | Stripe webhook handler | No |
| `GET` | `/products/{slug}/reviews` | List product reviews | No |
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

#### Pay an Order with Stripe Card

```bash
# 1. Create a payment intent
curl -X POST http://localhost:8080/api/v1/payments/stripe/create-intent \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access-token>" \
  -d '{"orderId": "uuid-of-order", "amount": 406.48, "currency": "kes"}'

# 2. Confirm it with the returned paymentIntentId.
#    On success the order moves PENDING -> CONFIRMED via RabbitMQ and a
#    confirmation email is sent (visible in Mailhog at http://localhost:8025).
curl -X POST http://localhost:8080/api/v1/payments/stripe/confirm \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access-token>" \
  -d '{"paymentIntentId": "pi_sim_xxxxxxxxxxxxxxxx"}'
```

> The payment is processed via the configured provider (Stripe). Set up webhooks for real-time status updates.
