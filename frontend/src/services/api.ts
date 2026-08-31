import { ApiResponse, AuthResponse, Product, ProductSearchResponse, Cart, Order, Category, Brand, Address, Review, User } from '@/types';

import { config } from '@/lib/config';
const API_URL = config.api.baseUrl;

let accessToken: string | null = null;
let refreshToken: string | null = null;

const ACCESS_TOKEN_KEY = 'iloveshopping_access_token';
const REFRESH_TOKEN_KEY = 'iloveshopping_refresh_token';

export function setAccessToken(token: string | null) {
  accessToken = token;
  if (typeof window !== 'undefined') {
    if (token) localStorage.setItem(ACCESS_TOKEN_KEY, token);
    else localStorage.removeItem(ACCESS_TOKEN_KEY);
  }
}
export function getAccessToken() {
  if (accessToken) return accessToken;
  if (typeof window !== 'undefined') return localStorage.getItem(ACCESS_TOKEN_KEY);
  return null;
}
export function setRefreshToken(token: string | null) {
  refreshToken = token;
  if (typeof window !== 'undefined') {
    if (token) localStorage.setItem(REFRESH_TOKEN_KEY, token);
    else localStorage.removeItem(REFRESH_TOKEN_KEY);
  }
}
export function getRefreshToken() {
  if (refreshToken) return refreshToken;
  if (typeof window !== 'undefined') return localStorage.getItem(REFRESH_TOKEN_KEY);
  return null;
}

async function request<T>(path: string, options: RequestInit & { _retried?: boolean } = {}): Promise<ApiResponse<T>> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...((options.headers as Record<string, string>) || {}) };
  // Tokens persist in localStorage and are sent via Authorization header for
  // cross-origin reliability (cloudflared rewrites SameSite=None cookies to
  // SameSite=Lax, so cookie-based auth breaks across origins on refresh).
  const token = getAccessToken();
  if (token && !headers['Authorization']) headers['Authorization'] = `Bearer ${token}`;
  // Cart session ID is sent via X-Cart-Session header for cross-origin reliability
  // (some proxies like cloudflared strip/replace cookies with SameSite=None).
  if (typeof window !== 'undefined' && !headers['X-Cart-Session']) {
    const sessionId = localStorage.getItem('cartSessionId');
    if (sessionId) headers['X-Cart-Session'] = sessionId;
  }
  const res = await fetch(`${API_URL}${path}`, { ...options, headers, credentials: 'include' });

  // Capture cart session ID from response header
  const cartSessionId = res.headers.get('X-Cart-Session');
  if (cartSessionId && typeof window !== 'undefined') {
    localStorage.setItem('cartSessionId', cartSessionId);
  }

  let data: any = null;
  try { data = await res.json(); } catch { /* empty body */ }

  // If access token expired, attempt one silent refresh then retry
  if (res.status === 401 && !options._retried) {
    try {
      const refreshed = await authRefreshInternal();
      if (refreshed) {
        return await request<T>(path, { ...options, _retried: true });
      }
    } catch { /* fall through */ }
  }

  if (!res.ok) {
    const message =
      data?.error?.message ||
      data?.detail ||
      data?.message ||
      `Request failed (${res.status})`;
    throw new Error(message);
  }
  if (data?.error) throw new Error(data.error.message || 'Request failed');
  return data;
}

async function authRefreshInternal(): Promise<boolean> {
  // Single-flight: concurrent 401s share ONE refresh call. The backend rotates
  // the refresh token on each refresh (revoking the previous one), so parallel
  // refreshes would race and the loser would be logged out.
  if (refreshPromise) return refreshPromise;
  refreshPromise = (async () => {
    try {
      const rt = getRefreshToken();
      if (!rt) return false;
      const res = await fetch(`${API_URL}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Refresh-Token': rt },
        credentials: 'include',
      });
      const body: any = await res.json().catch(() => null);
      if (!res.ok || !body?.data?.accessToken) return false;
      setAccessToken(body.data.accessToken);
      if (body.data.refreshToken) setRefreshToken(body.data.refreshToken);
      return true;
    } catch {
      return false;
    } finally {
      refreshPromise = null;
    }
  })();
  return refreshPromise;
}

let refreshPromise: Promise<boolean> | null = null;

export const auth = {
  register: (email: string, password: string, name: string, captchaToken: string) =>
    request<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify({ email, password, name, captchaToken }) }),
  login: (email: string, password: string, rememberMe = false, twoFactorCode?: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password, rememberMe, twoFactorCode }) }),
  refresh: () => {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    // Refresh token persisted in localStorage and sent via header (cloudflared
    // rewrites SameSite=None cookies, so cross-origin cookie refresh is unreliable).
    const rt = getRefreshToken();
    if (rt) headers['X-Refresh-Token'] = rt;
    return request<AuthResponse>('/auth/refresh', { method: 'POST', headers, credentials: 'include' });
  },
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
  forgotPassword: (email: string) => request<void>('/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
  resetPassword: (token: string, newPassword: string) => request<void>('/auth/reset-password', { method: 'POST', body: JSON.stringify({ token, newPassword }) }),
  verifyEmail: (token: string) => request<void>(`/auth/verify-email?token=${token}`),
  getProfile: () => request<User>('/user/profile'),
  updateProfile: (data: { name?: string; email?: string; avatar?: string }) =>
    request<User>('/user/profile', { method: 'PUT', body: JSON.stringify(data) }),
  getAddresses: () => request<Address[]>('/user/addresses'),
  addAddress: (addr: Address) => request<Address>('/user/addresses', { method: 'POST', body: JSON.stringify(addr) }),
  updateAddress: (id: string, addr: Address) => request<Address>(`/user/addresses/${id}`, { method: 'PUT', body: JSON.stringify(addr) }),
  deleteAddress: (id: string) => request<void>(`/user/addresses/${id}`, { method: 'DELETE' }),
  changePassword: (currentPassword: string, newPassword: string) =>
    request<void>('/user/password', { method: 'POST', body: JSON.stringify({ currentPassword, newPassword }) }),
};

export const products = {
  search: (params: Record<string, string>) => {
    const qs = new URLSearchParams(params).toString();
    return request<ProductSearchResponse>(`/products?${qs}`);
  },
  getBySlug: (slug: string) => request<Product>(`/products/${slug}`),
  getSuggestions: (q: string) => request<string[]>(`/products/search/suggestions?query=${encodeURIComponent(q)}`),
  getSimilar: (slug: string) => request<Product[]>(`/products/similar/${slug}`),
  getReviews: (slug: string, page = 0) => request<{ reviews: Review[]; pagination: any }>(`/products/${slug}/reviews?page=${page}`),
  addReview: (slug: string, rating: number, title: string, content: string) =>
    request<Review>(`/products/${slug}/reviews`, { method: 'POST', body: JSON.stringify({ rating, title, content }) }),
};

export const categories = {
  list: () => request<Category[]>('/categories'),
  getBySlug: (slug: string) => request<Category>(`/categories/${slug}`),
  getProducts: (slug: string, params: Record<string, string> = {}) => {
    const qs = new URLSearchParams(params).toString();
    return request<ProductSearchResponse>(`/categories/${slug}/products?${qs}`);
  },
};

export const brands = {
  list: () => request<Brand[]>('/brands'),
  getBySlug: (slug: string) => request<Brand>(`/brands/${slug}`),
  getProducts: (slug: string, params: Record<string, string> = {}) => {
    const qs = new URLSearchParams(params).toString();
    return request<ProductSearchResponse>(`/brands/${slug}/products?${qs}`);
  },
};

export const cart = {
  get: () => request<Cart>('/cart'),
  addItem: (productId: string, quantity: number, variantId?: string) =>
    request<Cart>('/cart/items', { method: 'POST', body: JSON.stringify({ productId, quantity, variantId }) }),
  updateItem: (itemId: string, quantity: number) =>
    request<Cart>(`/cart/items/${itemId}`, { method: 'PATCH', body: JSON.stringify({ quantity }) }),
  removeItem: (itemId: string) => request<Cart>(`/cart/items/${itemId}`, { method: 'DELETE' }),
  clear: () => request<void>('/cart', { method: 'DELETE' }),
  merge: () => request<Cart>('/cart/merge', { method: 'POST' }),
};

export const orders = {
  checkout: (data: { shippingAddress: Address; billingAddress?: Address; notes?: string; guestEmail?: string }) =>
    request<Order>('/orders/checkout', { method: 'POST', body: JSON.stringify(data) }),
  list: (page = 0, size = 10, status?: string, from?: string, to?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) params.set('status', status);
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    return request<{ orders: Order[]; pagination: any }>(`/orders?${params.toString()}`);
  },
  getByNumber: (number: string) => request<Order>(`/orders/${number}`),
  cancel: (number: string) => request<Order>(`/orders/${number}/cancel`, { method: 'POST' }),
  mpesaStkPush: (orderId: string, amount: string, phoneNumber: string) =>
    request<any>('/orders/payments/mpesa/stk-push', { method: 'POST', body: JSON.stringify({ orderId, amount, phoneNumber }) }),
  mpesaStkQuery: (checkoutRequestId: string) =>
    request<any>('/orders/payments/mpesa/stk-query', { method: 'POST', body: JSON.stringify({ checkoutRequestId }) }),
  retryPayment: (orderNumber: string, phoneNumber: string) =>
    request<any>(`/orders/${orderNumber}/retry-payment`, { method: 'POST', body: JSON.stringify({ phoneNumber }) }),
};

export const payments = {
  getPaymentHistory: (page = 0, size = 20) =>
    request<any[]>(`/payments?page=${page}&size=${size}`),
  stripeCreateIntent: (orderId: string, amount: number, currency = 'KES') =>
    request<any>('/payments/stripe/create-intent', { method: 'POST', body: JSON.stringify({ orderId, amount, currency }) }),
  stripeConfirm: (paymentIntentId: string) =>
    request<any>('/payments/stripe/confirm', { method: 'POST', body: JSON.stringify({ paymentIntentId }) }),
};

export const admin = {
  getStats: () => request<any>('/admin/stats'),
  listOrders: (page = 0, size = 20, status?: string) =>
    request<{ content: Order[]; totalPages: number; totalElements: number; numberOfElements: number }>(
      `/admin/orders?page=${page}&size=${size}${status ? `&status=${status}` : ''}`
    ),
  updateOrderStatus: (orderNumber: string, status: string) =>
    request<Order>(`/admin/orders/${orderNumber}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),
  listUsers: (page = 0, size = 20) => request<{ users: User[]; pagination: any }>(`/admin/users?page=${page}&size=${size}`),
  createCategory: (data: Partial<Category>) => request<Category>('/categories', { method: 'POST', body: JSON.stringify(data) }),
  updateCategory: (id: string, data: Partial<Category>) => request<Category>(`/categories/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteCategory: (id: string) => request<void>(`/categories/${id}`, { method: 'DELETE' }),
  createBrand: (data: Partial<Brand>) => request<Brand>('/brands', { method: 'POST', body: JSON.stringify(data) }),
  updateBrand: (id: string, data: Partial<Brand>) => request<Brand>(`/brands/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteBrand: (id: string) => request<void>(`/brands/${id}`, { method: 'DELETE' }),
  createProduct: (data: any) => request<Product>('/products', { method: 'POST', body: JSON.stringify(data) }),
  updateProduct: (id: string, data: any) => request<Product>(`/products/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteProduct: (id: string) => request<void>(`/products/${id}`, { method: 'DELETE' }),
};
