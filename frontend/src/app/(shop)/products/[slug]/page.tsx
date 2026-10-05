import type { Metadata } from 'next';
import ProductDetailClient from './ProductDetailClient';
import { products as productsApi } from '@/services/api';

interface Props {
  params: Promise<{ slug: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const product = await productsApi.getBySlug(slug).then(r => r.data).catch(() => null);

  if (!product) {
    return {
      title: 'Product not found - i-love-shopping',
      description: 'The product you are looking for is not available.',
    };
  }

  const title = `${product.name} - i-love-shopping`.slice(0, 60);
  const description = (product.description || '').slice(0, 158);
  const image = product.images?.[0]?.url;

  return {
    title,
    description,
    alternates: { canonical: `/products/${product.slug}` },
    openGraph: {
      title,
      description,
      type: 'website',
      ...(image ? { images: [{ url: image, alt: product.name }] } : {}),
    },
  };
}

export default function ProductDetailPage({ params }: Props) {
  return <ProductDetailClient />;
}
