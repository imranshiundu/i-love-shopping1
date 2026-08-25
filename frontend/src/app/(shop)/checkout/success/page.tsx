'use client';
import { Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { FiCheckCircle, FiArrowRight, FiPackage, FiMail, FiPhone } from 'react-icons/fi';
import { config } from '@/lib/config';
import { useAuth } from '@/contexts/AuthContext';

function SuccessContent() {
  const searchParams = useSearchParams();
  const orderNumber = searchParams.get('order');
  const { user, loading } = useAuth();

  return (
    <div className="mx-auto max-w-2xl px-4 py-20 sm:px-6">
      <div className="text-center">
        <span
          className="mx-auto flex h-24 w-24 items-center justify-center rounded-full bg-emerald-50 ring-8 ring-emerald-50/50"
          style={{ animation: 'hero-rise 0.7s cubic-bezier(0.16, 1, 0.3, 1) both' }}
        >
          <FiCheckCircle className="h-12 w-12 text-emerald-500" />
        </span>
        <h1 className="mt-8 text-3xl font-bold tracking-tight sm:text-4xl">Thank you - order confirmed</h1>
        <p className="mx-auto mt-3 max-w-[46ch] leading-relaxed text-stone-600">
          We have received your payment and your order is being prepared.
          A confirmation with full details is on its way to your inbox.
        </p>
        {orderNumber && (
          <p className="mt-5 text-sm text-stone-500">
            Order reference{' '}
            <code className="rounded-lg bg-stone-100 px-2.5 py-1 font-mono text-sm font-bold text-stone-900">{orderNumber}</code>
          </p>
        )}
      </div>

      <div className="mt-10 grid gap-3 sm:grid-cols-3">
        {[
          { icon: FiPackage, title: 'Packed with care', note: 'Within one business day' },
          { icon: FiMail, title: 'Email updates', note: 'At every status change' },
          { icon: FiPhone, title: 'Questions?', note: config.app.supportEmail },
        ].map((item, i) => (
          <div key={i} className="rounded-2xl border border-stone-200/80 bg-white p-5 text-center">
            <item.icon className="mx-auto h-5 w-5 text-primary-600" />
            <p className="mt-2.5 text-sm font-semibold">{item.title}</p>
            <p className="mt-0.5 text-xs text-stone-500">{item.note}</p>
          </div>
        ))}
      </div>

      <div className="mt-10 flex flex-col gap-3 sm:flex-row sm:justify-center">
        {!loading && !user ? (
          <Link href="/products"
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary-600 px-7 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700">
            Continue shopping <FiArrowRight />
          </Link>
        ) : (
          <>
            <Link href="/account/orders"
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary-600 px-7 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700">
              Track this order <FiArrowRight />
            </Link>
            <Link href="/products"
              className="inline-flex items-center justify-center rounded-xl border border-stone-300 px-7 py-3.5 font-semibold text-stone-700 transition-colors hover:bg-stone-50">
              Continue shopping
            </Link>
          </>
        )}
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
