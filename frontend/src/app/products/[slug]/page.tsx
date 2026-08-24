'use client';
import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { products as productsApi } from '@/services/api';
import { Product } from '@/types';
import { useAuth } from '@/contexts/AuthContext';
import { formatKES } from '@/lib/utils';
import { FiStar, FiShoppingCart, FiMinus, FiPlus, FiChevronRight } from 'react-icons/fi';
import toast from 'react-hot-toast';

export default function ProductDetailPage() {
  const { slug } = useParams();
  const { refreshCart } = useAuth();
  const [product, setProduct] = useState<Product | null>(null);
  const [similar, setSimilar] = useState<Product[]>([]);
  const [selectedImage, setSelectedImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    productsApi.getBySlug(slug as string).then(res => {
      setProduct(res.data || null);
      if (res.data) productsApi.getSimilar(res.data.slug).then(r => setSimilar(r.data || [])).catch(() => {});
    }).finally(() => setLoading(false));
  }, [slug]);

  const addToCart = async () => {
    if (!product) return;
    setAdding(true);
    try {
      await productsApi.getSuggestions(''); // just for context
      const { cart } = await import('@/services/api');
      await cart.addItem(product.id, quantity);
      await refreshCart();
      toast.success('Added to cart!');
    } catch (e: any) { toast.error(e.message || 'Failed to add to cart'); }
    setAdding(false);
  };

  if (loading) return <div className="max-w-7xl mx-auto px-4 py-8"><div className="animate-pulse h-96 bg-gray-200 rounded-lg" /></div>;
  if (!product) return <div className="max-w-7xl mx-auto px-4 py-8 text-center"><p className="text-lg text-gray-500">Product not found</p><Link href="/products" className="text-primary-600 hover:underline mt-4 block">Back to products</Link></div>;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <nav className="flex items-center gap-2 text-sm text-gray-500 mb-6">
        <Link href="/" className="hover:text-primary-600">Home</Link><FiChevronRight />
        <Link href="/products" className="hover:text-primary-600">Products</Link><FiChevronRight />
        {product.category && <><Link href={`/products?category=${product.category.slug}`} className="hover:text-primary-600">{product.category.name}</Link><FiChevronRight /></>}
        <span className="text-gray-900">{product.name}</span>
      </nav>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
        <div>
          <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden mb-4">
            {product.images?.[selectedImage] ? (
              <img src={product.images[selectedImage].url} alt={product.images[selectedImage].alt || product.name} className="w-full h-full object-cover" />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-gray-400">No Image</div>
            )}
          </div>
          {product.images && product.images.length > 1 && (
            <div className="flex gap-2 overflow-x-auto">
              {product.images.map((img, i) => (
                <button key={img.id} onClick={() => setSelectedImage(i)}
                  className={`w-20 h-20 rounded overflow-hidden border-2 flex-shrink-0 ${i === selectedImage ? 'border-primary-600' : 'border-transparent'}`}>
                  <img src={img.url} alt={img.alt} className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        <div>
          <p className="text-sm text-gray-500">{product.brand?.name}</p>
          <h1 className="text-3xl font-bold mt-2">{product.name}</h1>

          <div className="flex items-center gap-2 mt-3">
            <div className="flex">
              {[1, 2, 3, 4, 5].map(s => (
                <FiStar key={s} className={`h-5 w-5 ${s <= Math.round(product.averageRating) ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'}`} />
              ))}
            </div>
            <span className="text-sm text-gray-600">{product.averageRating?.toFixed(1) || 'No'} rating</span>
            {product.reviewCount > 0 && <span className="text-sm text-gray-400">({product.reviewCount} reviews)</span>}
          </div>

          <div className="flex items-baseline gap-3 mt-4">
            <span className="text-3xl font-bold text-primary-600">{formatKES(product.price)}</span>
            {product.compareAtPrice && (
              <>
                <span className="text-xl text-gray-400 line-through">{formatKES(product.compareAtPrice)}</span>
                <span className="bg-red-100 text-red-700 text-sm px-2 py-1 rounded">-{product.discountPercentage}%</span>
              </>
            )}
          </div>

          <p className="mt-6 text-gray-700 leading-relaxed">{product.description}</p>

          <div className="mt-6 space-y-2 text-sm">
            <div className="flex justify-between"><span className="text-gray-500">SKU</span><span>{product.sku}</span></div>
            <div className="flex justify-between"><span className="text-gray-500">Category</span><span>{product.category?.name}</span></div>
            {product.weight > 0 && <div className="flex justify-between"><span className="text-gray-500">Weight</span><span>{product.weight} {product.weightUnit || 'kg'}</span></div>}
          </div>

          <div className="mt-6 flex items-center gap-4">
            {product.inStock ? (
              <>
                <div className="flex items-center border rounded-lg">
                  <button onClick={() => setQuantity(q => Math.max(1, q - 1))} className="px-3 py-2 hover:bg-gray-50"><FiMinus /></button>
                  <span className="px-4 py-2 font-medium">{quantity}</span>
                  <button onClick={() => setQuantity(q => Math.min(product.stock, q + 1))} className="px-3 py-2 hover:bg-gray-50"><FiPlus /></button>
                </div>
                <button onClick={addToCart} disabled={adding}
                  className="flex-1 bg-primary-600 text-white py-3 rounded-lg font-semibold hover:bg-primary-700 disabled:opacity-50 flex items-center justify-center gap-2">
                  <FiShoppingCart /> {adding ? 'Adding...' : 'Add to Cart'}
                </button>
              </>
            ) : (
              <span className="text-red-600 font-medium">Out of Stock</span>
            )}
          </div>
        </div>
      </div>

      {similar.length > 0 && (
        <section className="mt-16">
          <h2 className="text-2xl font-bold mb-6">Similar Products</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {similar.map(p => (
              <Link key={p.id} href={`/products/${p.slug}`} className="bg-white rounded-lg overflow-hidden hover:shadow-lg transition border">
                <div className="aspect-square bg-gray-100">
                  {p.images?.[0] && <img src={p.images[0].url} alt={p.name} className="w-full h-full object-cover" />}
                </div>
                <div className="p-4">
                  <h3 className="font-semibold text-sm">{p.name}</h3>
                  <span className="font-bold text-primary-600 mt-1 block">{formatKES(p.price)}</span>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
