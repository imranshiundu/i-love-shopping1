// Scenario 1 — Browsing product catalogs
// Mimics a shopper landing on the storefront and exploring:
// home page -> product listing -> filters -> product detail pages.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const FRONTEND_URL = __ENV.FRONTEND_URL || 'http://localhost:3000';

const PEAK = parseInt(__ENV.PEAK || '50', 10);

export const options = {
  scenarios: {
    browsing: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: Math.round(PEAK * 0.4) },
        { duration: '1m', target: PEAK },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(90)<2000'],            // 90% of requests within 2s
    http_req_failed: ['rate<0.05'],               // error rate below 5%
    checks: ['rate>0.95'],                        // 95%+ of checks pass
  },
};

export default function () {
  // 1. Home page (frontend render)
  const home = http.get(`${FRONTEND_URL}/`, { tags: { name: 'GET / (frontend)' } });
  check(home, { 'home 200': r => r.status === 200 });

  // 2. Featured products (API)
  const featured = http.get(`${BASE_URL}/products?page=0&size=8&sortBy=newest`, { tags: { name: 'GET /products featured' } });
  check(featured, { 'featured 200': r => r.status === 200 });
  const featuredBody = featured.json();
  const products = (featuredBody && featuredBody.data && featuredBody.data.products) || [];

  // 3. Categories
  const categories = http.get(`${BASE_URL}/categories`, { tags: { name: 'GET /categories' } });
  check(categories, { 'categories 200': r => r.status === 200 });

  // 4. Product detail pages (2 random-ish picks)
  if (products.length > 0) {
    for (let i = 0; i < Math.min(2, products.length); i++) {
      const slug = products[(i * 3) % products.length].slug;
      const detail = http.get(`${BASE_URL}/products/${slug}`, { tags: { name: 'GET /products/:slug' } });
      check(detail, { 'detail 200': r => r.status === 200 });
      const reviews = http.get(`${BASE_URL}/products/${slug}/reviews?page=0&sortBy=helpful`, { tags: { name: 'GET /products/:slug/reviews' } });
      check(reviews, { 'reviews 200': r => r.status === 200 });
      sleep(0.5);
    }
  }

  sleep(1);
}
