// Scenario 2 — Searching for products + filling the cart
// Mimics a shopper searching with suggestions, filtering results,
// and adding items to the cart (guest cart session).
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export const options = {
  scenarios: {
    search_and_cart: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 20,
      maxVUs: 80,
      stages: [
        { duration: '30s', target: 10 },   // 10 iterations/sec
        { duration: '1m', target: 15 },
        { duration: '30s', target: 5 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(90)<2000'],
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.95'],
  },
};

const SEARCH_TERMS = ['mug', 'blanket', 'candle', 'plant', 'linen'];

export default function () {
  const term = SEARCH_TERMS[Math.floor(Math.random() * SEARCH_TERMS.length)];
  const cartSession = `load-test-${Math.random().toString(36).substring(2, 12)}`;
  const params = { headers: { 'Content-Type': 'application/json', 'X-Cart-Session': cartSession } };

  // 1. Search with suggestions (quick search as you type)
  const suggestions = http.get(`${BASE_URL}/products/search/suggestions?query=${term}`, { tags: { name: 'GET suggestions' } });
  check(suggestions, { 'suggestions 200': r => r.status === 200 });

  // 2. Search results page (faceted)
  const results = http.get(`${BASE_URL}/products?query=${term}&page=0&size=12`, { tags: { name: 'GET /products?q=' } });
  check(results, { 'results 200': r => r.status === 200 });
  const body = results.json();
  const products = (body && body.data && body.data.products) || [];

  // 3. Add first two results to the cart
  for (let i = 0; i < Math.min(2, products.length); i++) {
    const add = http.post(
      `${BASE_URL}/cart/items`,
      JSON.stringify({ productId: products[i].id, quantity: 1 }),
      Object.assign({}, params, { tags: { name: 'POST /cart/items' } }),
    );
    check(add, { 'add to cart 200': r => r.status === 200 });
    sleep(0.3);
  }

  // 4. View the cart
  const cart = http.get(`${BASE_URL}/cart`, Object.assign({}, params, { tags: { name: 'GET /cart' } }));
  check(cart, { 'cart 200': r => r.status === 200 });

  sleep(1);
}
