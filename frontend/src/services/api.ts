import { ApiResponse, AuthResponse, Product, ProductSearchResponse, Cart, Order, Category, Brand, Address, Review, User } from '@/types';

import { config } from '@/lib/config';
const API_URL = config.api.baseUrl;

let accessToken: string | null = null;

export function setAccessToken(token: string | null) { accessToken = token; }
export function getAccessToken() { return accessToken; }

async function request<T>(path: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...((options.headers as Record<string, string>) || {}) };
  if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`;
  const res = await fetch(`${API_URL}${path}`, { ...options, headers, credentials: 'include' });
  const data = await res.json();
  if (data.error) throw new Error(data.error.message || 'Request failed');
  return data;
}

export const auth = {
  register: (email: string, password: string, name: string, captchaToken: string) =>
    request<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify({ email, password, name, captchaToken }) }),
  login: (email: string, password: string, rememberMe = false, twoFactorCode?: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password, rememberMe, twoFactorCode }) }),
  refresh: () => request<AuthResponse>('/auth/refresh', { method: 'POST' }),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
  forgotPassword: (email: string) => request<void>('/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
  resetPassword: (token: string, newPassword: string) => request<void>('/auth/reset-password', { method: 'POST', body: JSON.stringify({ token, newPassword }) }),
  verifyEmail: (token: string) => request<void>(`/auth/verify-email?token=${token}`),
  getProfile: () => request<User>('/user/profile'),
  updateProfile: (data: { name?: string; email?: string; avatar?: string }) =>
    request<User>('/user/profile', { method: 'PUT', body: JSON.stringify(data) }),
  getAddresses: () => request<Address[]>('/user/addresses'),
  addAddress: (addr: Address) => request<Address>('/user/addresses', { method: 'POST', body: JSON.stringify(addr) }),
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
  create: () => request<Cart>('/cart', { method: 'POST' }),
  addItem: (productId: string, quantity: number, variantId?: string) =>
    request<Cart>('/cart/items', { method: 'POST', body: JSON.stringify({ productId, quantity, variantId }) }),
  updateItem: (itemId: string, quantity: number) =>
    request<Cart>(`/cart/items/${itemId}`, { method: 'PATCH', body: JSON.stringify({ quantity }) }),
  removeItem: (itemId: string) => request<Cart>(`/cart/items/${itemId}`, { method: 'DELETE' }),
  clear: () => request<void>('/cart', { method: 'DELETE' }),
};

export const orders = {
  checkout: (data: { shippingAddress: Address; billingAddress?: Address; notes?: string }) =>
    request<Order>('/orders/checkout', { method: 'POST', body: JSON.stringify(data) }),
  list: (page = 0, size = 10) => request<{ orders: Order[]; pagination: any }>(`/orders?page=${page}&size=${size}`),
  getByNumber: (number: string) => request<Order>(`/orders/${number}`),
  cancel: (number: string) => request<Order>(`/orders/${number}/cancel`, { method: 'POST' }),
  mpesaStkPush: (orderId: string, amount: string, phoneNumber: string) =>
    request<any>('/orders/payments/mpesa/stk-push', { method: 'POST', body: JSON.stringify({ orderId, amount, phoneNumber }) }),
};

export const payments = {
  mpesaSimulateConfirm: (checkoutRequestId: string) =>
    request<any>('/payments/mpesa/simulate-confirm', { method: 'POST', body: JSON.stringify({ checkoutRequestId }) }),
  stripeCreateIntent: (orderId: string, amount: number, currency = 'kes') =>
    request<any>('/payments/stripe/create-intent', { method: 'POST', body: JSON.stringify({ orderId, amount, currency }) }),
  stripeConfirm: (paymentIntentId: string) =>
    request<any>('/payments/stripe/confirm', { method: 'POST', body: JSON.stringify({ paymentIntentId }) }),
  paypalCreateOrder: (orderId: string, amount: number, currency = 'KES') =>
    request<any>('/payments/paypal/create-order', { method: 'POST', body: JSON.stringify({ orderId, amount, currency }) }),
  paypalCapture: (paypalOrderId: string) =>
    request<any>('/payments/paypal/capture', { method: 'POST', body: JSON.stringify({ paypalOrderId }) }),
  flutterwaveCreate: (orderId: string, amount: number, customerEmail?: string, currency = 'KES') =>
    request<any>('/payments/flutterwave/create-payment', { method: 'POST', body: JSON.stringify({ orderId, amount, currency, customerEmail }) }),
  flutterwaveVerify: (transactionRef: string) =>
    request<any>('/payments/flutterwave/verify', { method: 'POST', body: JSON.stringify({ transactionRef }) }),
  airtelInitiate: (orderId: string, amount: number, phoneNumber: string) =>
    request<any>('/payments/airtel/initiate', { method: 'POST', body: JSON.stringify({ orderId, amount, phoneNumber }) }),
  airtelConfirm: (referenceId: string) =>
    request<any>('/payments/airtel/confirm', { method: 'POST', body: JSON.stringify({ referenceId }) }),
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
