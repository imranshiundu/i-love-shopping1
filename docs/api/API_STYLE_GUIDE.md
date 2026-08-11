# i-love-shopping - API Style Guide

This document defines the coding standards and conventions for the REST API.

## General Principles

- **RESTful**: Follow REST conventions (nouns, not verbs)
- **Consistent**: Use consistent naming, formatting, and error handling
- **Versioned**: All endpoints under `/api/v1/`
- **Secure**: HTTPS only in production, proper authentication
- **Documented**: OpenAPI/Swagger documentation for all endpoints

## Naming Conventions

### Endpoints
- Use plural nouns: `/products`, `/categories`, `/orders`
- Use kebab-case for multi-word: `/product-images`, `/order-items`
- Nest related resources: `/categories/{slug}/products`

### HTTP Methods
| Method | Usage |
|--------|-------|
| GET    | Retrieve resource(s) |
| POST   | Create new resource |
| PUT    | Replace entire resource |
| PATCH  | Partial update |
| DELETE | Remove resource |

### Query Parameters
- Use snake_case: `page_size`, `sort_by`, `min_price`
- Pagination: `page` (0-indexed), `size`
- Sorting: `sort_by=price_asc`, `sort_by=created_desc`
- Filtering: `category=electronics`, `brand=nike`

### Request/Response Body
- Use camelCase for JSON fields
- Dates: ISO 8601 format (`2024-01-15T10:30:00Z`)
- Money: Decimal string (`"47.20"`) to avoid floating-point issues
- IDs: UUID strings

## Error Handling

### Standard Error Response (RFC 7807)
```json
{
  "success": false,
  "error": {
    "statusCode": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "details": {
      "email": "Invalid email format",
      "password": "Password must be at least 8 characters"
    },
    "path": "/api/v1/auth/register"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### HTTP Status Codes
| Code | Usage |
|------|-------|
| 200  | Success (GET, PUT, PATCH) |
| 201  | Created (POST) |
| 204  | No Content (DELETE) |
| 400  | Bad Request (validation) |
| 401  | Unauthorized (invalid/missing token) |
| 403  | Forbidden (insufficient permissions) |
| 404  | Not Found |
| 409  | Conflict (duplicate resource) |
| 422  | Unprocessable Entity (business logic) |
| 429  | Too Many Requests (rate limit) |
| 500  | Internal Server Error |

## Pagination

### Request
```
GET /api/v1/products?page=0&size=20&sort_by=price_asc
```

### Response
```json
{
  "success": true,
  "data": [...],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

## Filtering & Search

### Category Filter
```
GET /api/v1/products?categories=electronics,accessories
```

### Price Range
```
GET /api/v1/products?minPrice=100&maxPrice=500
```

### Text Search
```
GET /api/v1/products?query=iphone
```

### In Stock Only
```
GET /api/v1/products?inStockOnly=true
```

## Versioning

- URL versioning: `/api/v1/`
- Header versioning: `Accept: application/vnd.iloveshopping.v1+json`
- Deprecation notice: 6 months before removal
- Breaking changes = new version

## Security

- All endpoints require HTTPS in production
- JWT in Authorization header: `Bearer <token>`
- Refresh tokens in HttpOnly cookies
- CORS configured for frontend domain only
- Rate limiting on auth endpoints
- Input validation on all endpoints

## Documentation

- All endpoints documented with OpenAPI annotations
- Swagger UI available at `/docs`
- Examples for request/response bodies
- Error responses documented per endpoint