import type { MetadataRoute } from 'next';
import { products as productsApi, categories as categoriesApi } from '@/services/api';

const BASE_URL = process.env.NEXT_PUBLIC_APP_URL || 'http://localhost:3000';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const staticRoutes = ['', '/products', '/cart', '/checkout', '/about', '/contact'].map(path => ({
    url: `${BASE_URL}${path}`,
    lastModified: new Date(),
    changeFrequency: 'daily' as const,
    priority: path === '' ? 1 : 0.7,
  }));

  const productRoutes = await productsApi
    .search({ page: '0', size: '200' })
    .then(r => (r.data?.products || []).map(p => ({
      url: `${BASE_URL}/products/${p.slug}`,
      lastModified: new Date(),
      changeFrequency: 'weekly' as const,
      priority: 0.6,
    })))
    .catch(() => []);

  const categoryRoutes = await categoriesApi
    .list()
    .then(r => (r.data || []).map(c => ({
      url: `${BASE_URL}/products?category=${c.slug}`,
      lastModified: new Date(),
      changeFrequency: 'weekly' as const,
      priority: 0.5,
    })))
    .catch(() => []);

  return [...staticRoutes, ...productRoutes, ...categoryRoutes];
}
