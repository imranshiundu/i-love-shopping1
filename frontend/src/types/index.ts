export interface ApiResponse<T> {
  statusCode: number;
  data?: T;
  error?: { statusCode: number; error: string; message: string; path?: string };
  message?: string;
  timestamp?: string;
}

export interface User {
  id: string;
  email: string;
  name: string;
  avatar?: string;
  emailVerified: boolean;
  twoFactorEnabled: boolean;
  roles: string[];
  createdAt?: string;
}

export interface AuthResponse {
  accessToken?: string;
  refreshToken?: string;
  expiresIn?: number;
  user: User;
  twoFactorRequired: boolean;
  sessionId?: string;
  message?: string;
}

export interface Product {
  id: string;
  name: string;
  slug: string;
  description: string;
  price: number;
  compareAtPrice?: number;
  sku: string;
  stock: number;
  weight: number;
  weightUnit?: string;
  dimensions?: string;
  isActive: boolean;
  category: CategorySummary;
  brand: BrandSummary;
  images: ProductImage[];
  inStock: boolean;
  onSale: boolean;
  discountPercentage: number;
  averageRating: number;
  reviewCount: number;
}

export interface CategorySummary { id: string; name: string; slug: string; }
export interface BrandSummary { id: string; name: string; slug: string; logo?: string; }
export interface ProductImage { id: string; url: string; alt: string; sortOrder: number; }

export interface Category {
  id: string; name: string; slug: string; description?: string; image?: string;
  sortOrder: number; parentId?: string; children?: Category[]; productCount?: number;
}

export interface Brand { id: string; name: string; slug: string; logo?: string; description?: string; }

export interface ProductSearchResponse {
  products: Product[];
  pagination: { page: number; size: number; totalElements: number; totalPages: number };
  facets: { categories: CategoryFacet[]; brands: BrandFacet[]; priceRange: PriceRangeFacet };
}

export interface CategoryFacet { id: string; name: string; slug: string; count: number; }
export interface BrandFacet { id: string; name: string; slug: string; count: number; }
export interface PriceRangeFacet { min: number; max: number; }

export interface CartItem {
  id: string; productId: string; name: string; slug: string; price: number;
  quantity: number; image?: string; stock: number; total: number;
}

export interface Cart {
  id: string; items: CartItem[]; totalItems: number; subtotal: number;
}

export interface Order {
  id: string; number: string; status: string; subtotal: number; tax: number;
  shipping: number; total: number; currency: string;
  shippingAddress: Address; billingAddress?: Address; notes?: string;
  items: OrderItem[]; payments: Payment[];
  createdAt: string; updatedAt: string;
  totalPaid: number; fullyPaid: boolean; canBeCancelled: boolean;
}

export interface OrderItem {
  id: string; productId: string; name: string; price: number;
  quantity: number; total: number; image?: string;
}

export interface Payment {
  id: string; provider: string; providerId?: string; amount: number;
  currency: string; status: string; createdAt: string;
}

export interface Address {
  id?: string; type?: string; name: string; line1: string; line2?: string;
  city: string; state: string; postalCode: string; country: string;
  phone?: string; isDefault?: boolean;
}

export interface Review {
  id: string; productId: string; userId: string; userName?: string;
  rating: number; title: string; content: string;
  isVerifiedPurchase: boolean; createdAt: string;
}

export interface PageInfo { page: number; size: number; totalElements: number; totalPages: number; }
