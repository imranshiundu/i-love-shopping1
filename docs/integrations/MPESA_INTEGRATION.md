# M-Pesa Daraja API Integration Guide

This document details the M-Pesa Daraja API integration for STK Push payments.

## Overview

M-Pesa Daraja is Safaricom's API platform for integrating M-Pesa payments. We implement:
- **STK Push (Lipa Na M-Pesa Online)**: Customer enters PIN on phone
- **Callback Handling**: Asynchronous payment confirmation
- **Timeout Handling**: Payment expiration handling
- **Status Query**: Payment status polling

## Prerequisites

### Sandbox Credentials
1. Register at https://developer.safaricom.co.ke/
2. Create app, get Consumer Key & Secret
3. Use test shortcode: `174379`
4. Test passkey from sandbox

### Production Credentials
1. Complete Safaricom onboarding
2. Get production shortcode & passkey
3. Configure callback URLs with HTTPS

## Configuration

### Environment Variables
```bash
MPESA_CONSUMER_KEY=your_consumer_key
MPESA_CONSUMER_SECRET=your_consumer_secret
MPESA_SHORTCODE=174379
MPESA_PASSKEY=your_base64_passkey
MPESA_CALLBACK_URL=https://yourdomain.com/api/v1/orders/payments/mpesa/callback
MPESA_TIMEOUT_URL=https://yourdomain.com/api/v1/orders/payments/mpesa/timeout
MPESA_BASE_URL=https://sandbox.safaricom.co.ke  # or https://api.safaricom.co.ke for production
```

### Shortcode & Passkey
- **Shortcode**: Your business paybill/till number
- **Passkey**: Base64 encoded string from Safaricom
- **Password Generation**: `Base64(Shortcode + Passkey + Timestamp)`

## STK Push Flow

### 1. Initiate Payment
```bash
POST /api/v1/orders/payments/mpesa/stk-push
Content-Type: application/json
Authorization: Bearer <access_token>

{
  "phoneNumber": "254712345678",
  "amount": "4720.00",
  "orderId": "uuid-of-order",
  "accountReference": "ORD-12345",
  "transactionDesc": "Payment for order ORD-12345"
}
```

### 2. Response
```json
{
  "success": true,
  "data": {
    "merchantRequestId": "29115-34620561-1",
    "checkoutRequestId": "ws_CO_123456789",
    "responseCode": "0",
    "responseDescription": "Success. Request accepted for processing",
    "customerMessage": "Success. Request accepted for processing"
  }
}
```

### 3. Customer Experience
1. Customer receives STK Push prompt on phone
2. Enters M-Pesa PIN
3. Transaction processed
4. Callback sent to your endpoint

### 4. Callback Handling
```json
POST /api/v1/orders/payments/mpesa/callback
Content-Type: application/json

{
  "Body": {
    "stkCallback": {
      "MerchantRequestID": "29115-34620561-1",
      "CheckoutRequestID": "ws_CO_123456789",
      "ResultCode": 0,
      "ResultDesc": "The service request is processed successfully.",
      "CallbackMetadata": {
        "Item": [
          {"Name": "Amount", "Value": 4720.00},
          {"Name": "MpesaReceiptNumber", "Value": "NLJ7RT61SV"},
          {"Name": "Balance", "Value": 12345.00},
          {"Name": "TransactionDate", "Value": 20240115103000},
          {"Name": "PhoneNumber", "Value": 254712345678}
        ]
      }
    }
  }
}
```

### 5. Timeout Handling
```json
POST /api/v1/orders/payments/mpesa/timeout
Content-Type: application/json

{
  "CheckoutRequestID": "ws_CO_123456789",
  "ResultCode": 1,
  "ResultDesc": "Request timed out"
}
```

## Error Handling

### Common Response Codes
| Code | Description | Action |
|------|-------------|--------|
| 0 | Success | Process payment |
| 1 | Insufficient funds | Notify customer |
| 1032 | Cancelled by user | Mark order cancelled |
| 1037 | Timeout | Retry or cancel order |
| 2001 | Invalid phone number | Validate format |
| 4001 | Unsupported transaction | Check shortcode |

### Retry Logic
```java
// Retry up to 3 times with exponential backoff
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
public MpesaStkPushResponse initiateStkPush(MpesaStkPushRequest request) {
    // Implementation
}
```

## Testing

### Sandbox Test Numbers
| Phone Number | Behavior |
|--------------|----------|
| 254708374149 | Success (test) |
| 254708374150 | Insufficient funds |
| 254708374151 | Timeout |
| 254708374152 | Cancelled |

### Test Checklist
- [ ] Valid phone number format (254XXXXXXXXX)
- [ ] Amount matches order total
- [ ] Valid order ID
- [ ] Callback URL accessible (HTTPS)
- [ ] Idempotency (duplicate requests handled)
- [ ] Error responses handled gracefully

## Security Considerations

### Callback Validation
```java
// Verify callback authenticity
public boolean validateCallback(String checksum, String passkey, String data) {
    String expected = generateChecksum(data, passkey);
    return checksum.equals(expected);
}
```

### Rate Limiting
- M-Pesa API: 100 requests/minute
- Implement exponential backoff
- Cache access tokens (55 min TTL)

### Idempotency
- Track `CheckoutRequestID` in database
- Prevent duplicate payment processing
- Return existing result for duplicates

## Monitoring & Alerting

### Key Metrics
- **Success Rate**: Target > 95%
- **Average Latency**: < 10 seconds
- **Callback Success**: > 99%
- **Timeout Rate**: < 2%

### Alerts
- Success rate drops below 90%
- Callback failure rate > 1%
- Average latency > 15 seconds
- M-Pesa API errors > 5/minute

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| "Invalid signature" | Wrong passkey/shortcode | Verify credentials |
| "Invalid phone number" | Wrong format | Use 254XXXXXXXXX |
| "Callback not received" | URL not accessible | Check firewall, DNS, SSL |
| "Duplicate request" | Retry without idempotency | Track CheckoutRequestID |
| "Timeout" | Customer didn't enter PIN | Increase timeout, retry |

### Debug Checklist
1. Verify credentials in `.env`
2. Check callback URL accessibility
3. Verify SSL certificate valid
4. Check M-Pesa API status page
5. Review application logs
6. Test with sandbox numbers

## Production Checklist

- [ ] Production credentials configured
- [ ] Callback URLs use HTTPS
- [ ] SSL certificates valid
- [ ] Firewall allows M-Pesa IPs
- [ ] Rate limiting configured
- [ ] Idempotency implemented
- [ ] Monitoring alerts configured
- [ ] Rollback plan documented
- [ ] Team trained on incident response

## References

- [M-Pesa Daraja API Documentation](https://developer.safaricom.co.ke/docs)
- [STK Push API Spec](https://developer.safaricom.co.ke/docs#stk-push)
- [Callback Specification](https://developer.safaricom.co.ke/docs#callback)
- [Error Codes](https://developer.safaricom.co.ke/docs#error-codes)
- [Safaricom Developer Portal](https://developer.safaricom.co.ke/)