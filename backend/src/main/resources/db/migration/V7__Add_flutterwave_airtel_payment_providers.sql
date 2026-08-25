ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_provider_check;

ALTER TABLE payments ADD CONSTRAINT payments_provider_check
    CHECK (provider IN ('MPESA', 'STRIPE', 'PAYPAL', 'FLUTTERWAVE', 'AIRTEL_MONEY', 'BANK_TRANSFER'));
