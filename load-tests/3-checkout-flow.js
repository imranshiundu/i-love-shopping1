// Scenario 3 — User registration/login + completing the checkout process
// Mimics account creation, login, cart fill, checkout and order lookup.
// NOTE: run with raised rate limits so auth throttling (10/min per IP)
// does not skew results:
//   RATE_LIMIT_AUTH_PER_MINUTE=100000 RATE_LIMIT_API_PER_MINUTE=100000 ./start.sh
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const RUN_ID = Math.floor(Date.now() / 1000);

export const options = {
  scenarios: {
    checkout_flow: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 25 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(90)<2000'],
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.90'],          // 98% of transactions succeed at steady state
  },
};

export default function () {
  const unique = `${RUN_ID}-${Math.random().toString(36).substring(2, 10)}`;
  const cartSession = `co-${unique}`;
  const base = { headers: { 'Content-Type': 'application/json', 'X-Cart-Session': cartSession } };

  // 1. Register a fresh account (or fall back to the seeded test user)
  const regPayload = JSON.stringify({
    email: `loadtest-${unique}@example.com`,
    password: 'LoadTest123!',
    name: `Load Test ${unique}`,
    captchaToken: 'dev-test-secret',
  });
  const reg = http.post(`${BASE_URL}/auth/register`, regPayload, Object.assign({}, base, { tags: { name: 'POST /auth/register' } }));
  let token = null;
  if (check(reg, { 'register/login 200': r => r.status === 200 })) {
    token = reg.json().data.accessToken;
  } else {
    const login = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ email: 'user@iloveshopping.com', password: 'User123!' }),
      Object.assign({}, base, { tags: { name: 'POST /auth/login' } }),
    );
    if (check(login, { 'login 200': r => r.status === 200 })) {
      token = login.json().data.accessToken;
    }
  }
  if (!token) return;

  const authHeaders = Object.assign({}, base, { headers: Object.assign({}, base.headers, { Authorization: `Bearer ${token}` }) });

  // 2. Fill the cart
  const products = http.get(`${BASE_URL}/products?page=0&size=5`, { tags: { name: 'GET /products' } });
  const list = (products.json().data && products.json().data.products) || [];
  for (let i = 0; i < Math.min(2, list.length); i++) {
    http.post(
      `${BASE_URL}/cart/items`,
      JSON.stringify({ productId: list[i].id, quantity: 1 }),
      Object.assign({}, authHeaders, { tags: { name: 'POST /cart/items' } }),
    );
  }

  // 3. Checkout
  const address = {
    name: 'Load Test', line1: '123 Kimathi Street', city: 'Nairobi',
    state: 'Nairobi', postalCode: '00100', country: 'KE', phone: '254700000000',
    type: 'SHIPPING',
  };
  const checkout = http.post(
    `${BASE_URL}/orders/checkout`,
    JSON.stringify({ shippingAddress: address, billingAddress: Object.assign({}, address, { type: 'BILLING' }) }),
    Object.assign({}, authHeaders, { tags: { name: 'POST /orders/checkout' } }),
  );
  const checkoutOk = check(checkout, { 'checkout 200': r => r.status === 200 });
  let orderNumber = null;
  if (checkoutOk) {
    orderNumber = checkout.json().data.number;
  }

  // 4. View the order + profile
  if (orderNumber) {
    const order = http.get(`${BASE_URL}/orders/${orderNumber}`, Object.assign({}, authHeaders, { tags: { name: 'GET /orders/:number' } }));
    check(order, { 'order 200': r => r.status === 200 });
  }
  const profile = http.get(`${BASE_URL}/user/profile`, Object.assign({}, authHeaders, { tags: { name: 'GET /user/profile' } }));
  check(profile, { 'profile 200': r => r.status === 200 });

  sleep(1);
}
