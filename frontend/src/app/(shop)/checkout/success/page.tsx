'use client';
import { Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { FiCheckCircle, FiArrowRight } from 'react-icons/fi';

function SuccessContent() {
  const searchParams = useSearchParams();
  const orderNumber = searchParams.get('order');

  return (
    <div className="max-w-lg mx-auto px-4 py-16 text-center">
      <FiCheckCircle className="h-20 w-20 text-green-500 mx-auto mb-6" />
      <h1 className="text-3xl font-bold mb-4">Order Placed Successfully!</h1>
      <p className="text-gray-600 mb-2">Thank you for your purchase.</p>
      {orderNumber && <p className="text-sm text-gray-500 mb-8">Order ID: <code className="bg-gray-100 px-2 py-1 rounded">{orderNumber}</code></p>}
      <div className="flex flex-col sm:flex-row gap-4 justify-center">
        <Link href="/account/orders" className="bg-primary-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-primary-700 flex items-center justify-center gap-2">
          View Orders <FiArrowRight />
        </Link>
        <Link href="/products" className="border px-6 py-3 rounded-lg font-semibold hover:bg-gray-50">Continue Shopping</Link>
      </div>
    </div>
  );
}

export default function CheckoutSuccessPage() {
  return (
    <Suspense fallback={<div className="max-w-lg mx-auto px-4 py-16 text-center">Loading...</div>}>
      <SuccessContent />
    </Suspense>
  );
}
