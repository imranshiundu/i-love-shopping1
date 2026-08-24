'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { cart as cartApi } from '@/services/api';
import { formatKES } from '@/lib/utils';
import { config } from '@/lib/config';
import { FiTrash2, FiMinus, FiPlus, FiShoppingBag, FiArrowRight } from 'react-icons/fi';
import toast from 'react-hot-toast';

export default function CartPage() {
  const { cart, refreshCart, user } = useAuth();
  const [updating, setUpdating] = useState<string | null>(null);

  const updateQuantity = async (itemId: string, quantity: number) => {
    setUpdating(itemId);
    try { await cartApi.updateItem(itemId, quantity); await refreshCart(); }
    catch (e: any) { toast.error(e.message); }
    setUpdating(null);
  };

  const removeItem = async (itemId: string) => {
    setUpdating(itemId);
    try { await cartApi.removeItem(itemId); await refreshCart(); toast.success('Item removed'); }
    catch (e: any) { toast.error(e.message); }
    setUpdating(null);
  };

  if (!cart || cart.items.length === 0) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-16 text-center">
        <FiShoppingBag className="h-16 w-16 text-gray-300 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-2">Your cart is empty</h1>
        <p className="text-gray-500 mb-6">Start shopping to add items to your cart.</p>
        <Link href="/products" className="bg-primary-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-primary-700 inline-block">Browse Products</Link>
      </div>
    );
  }

  const subtotal = cart.items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const shipping = subtotal >= config.commerce.freeShippingThreshold ? 0 : config.commerce.shippingCost;
  const tax = Math.round(subtotal * config.commerce.taxRate);
  const total = subtotal + shipping + tax;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold mb-8">Shopping Cart ({cart.totalItems} items)</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-4">
          {cart.items.map(item => (
            <div key={item.id} className="bg-white rounded-lg p-4 border flex gap-4">
              <div className="w-24 h-24 bg-gray-100 rounded overflow-hidden flex-shrink-0">
                {item.image ? <img src={item.image} alt={item.name} className="w-full h-full object-cover" /> : <div className="w-full h-full flex items-center justify-center text-gray-400 text-xs">No img</div>}
              </div>
              <div className="flex-1 min-w-0">
                <Link href={`/products/${item.slug}`} className="font-semibold hover:text-primary-600 line-clamp-1">{item.name}</Link>
                <p className="text-primary-600 font-bold mt-1">{formatKES(item.price)}</p>
                <div className="flex items-center justify-between mt-3">
                  <div className="flex items-center border rounded">
                    <button onClick={() => updateQuantity(item.id, item.quantity - 1)} disabled={updating === item.id || item.quantity <= 1} className="px-2 py-1 hover:bg-gray-50 disabled:opacity-50"><FiMinus className="h-4 w-4" /></button>
                    <span className="px-3 text-sm font-medium">{item.quantity}</span>
                    <button onClick={() => updateQuantity(item.id, item.quantity + 1)} disabled={updating === item.id || item.quantity >= item.stock} className="px-2 py-1 hover:bg-gray-50 disabled:opacity-50"><FiPlus className="h-4 w-4" /></button>
                  </div>
                  <div className="flex items-center gap-4">
                    <span className="font-semibold">{formatKES(item.price * item.quantity)}</span>
                    <button onClick={() => removeItem(item.id)} className="text-red-500 hover:text-red-700"><FiTrash2 /></button>
                  </div>
                </div>
                {item.quantity >= item.stock && <p className="text-xs text-orange-600 mt-1">Max stock reached</p>}
              </div>
            </div>
          ))}
        </div>

        <div className="bg-white rounded-lg p-6 border h-fit sticky top-24">
          <h2 className="text-xl font-bold mb-4">Order Summary</h2>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between"><span>Subtotal</span><span>{formatKES(subtotal)}</span></div>
            <div className="flex justify-between"><span>Shipping</span><span>{shipping === 0 ? <span className="text-green-600">Free</span> : formatKES(shipping)}</span></div>
            <div className="flex justify-between"><span>Tax (16%)</span><span>{formatKES(tax)}</span></div>
            <div className="border-t pt-2 mt-2 flex justify-between font-bold text-lg"><span>Total</span><span>{formatKES(total)}</span></div>
          </div>
          {shipping > 0 && <p className="text-xs text-green-600 mt-2">Free shipping on orders over {formatKES(5000)}</p>}
          <Link href="/checkout" className="block w-full bg-primary-600 text-white text-center py-3 rounded-lg font-semibold hover:bg-primary-700 mt-4">
            Proceed to Checkout <FiArrowRight className="inline ml-1" />
          </Link>
          <Link href="/products" className="block text-center text-primary-600 hover:underline mt-3 text-sm">Continue Shopping</Link>
        </div>
      </div>
    </div>
  );
}
