'use client';
import { Suspense, useEffect, useState, useRef } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { FiCheckCircle, FiArrowRight, FiPackage, FiMail, FiPhone, FiLoader, FiAlertCircle } from 'react-icons/fi';
import { config } from '@/lib/config';
import { useAuth } from '@/contexts/AuthContext';
import { orders } from '@/services/api';

type PollState = 'pending' | 'confirmed' | 'failed' | 'expired' | 'timeout';

function SuccessContent() {
  const searchParams = useSearchParams();
  const orderNumber = searchParams.get('order');
  const { user, loading } = useAuth();
  const [pollState, setPollState] = useState<PollState>('pending');
  const [orderStatus, setOrderStatus] = useState<string>('PENDING');
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const attemptsRef = useRef(0);

  useEffect(() => {
    if (!orderNumber) return;
    if (pollRef.current) clearInterval(pollRef.current);
    attemptsRef.current = 0;
    pollRef.current = setInterval(async () => {
      attemptsRef.current += 1;
      if (attemptsRef.current > 60) { // 60 * 3s = 3 min
        if (pollRef.current) clearInterval(pollRef.current);
        setPollState('timeout');
        return;
      }
      try {
        const res = await orders.getByNumber(orderNumber);
        const status = res.data?.status;
        if (status) setOrderStatus(status);
        if (status === 'CONFIRMED' || status === 'PROCESSING' || status === 'SHIPPED' || status === 'DELIVERED') {
          if (pollRef.current) clearInterval(pollRef.current);
          setPollState('confirmed');
        } else if (status === 'CANCELLED' || status === 'EXPIRED') {
          if (pollRef.current) clearInterval(pollRef.current);
          setPollState(status === 'EXPIRED' ? 'expired' : 'failed');
        }
      } catch { /* ignore transient */ }
    }, 3000);
    return () => { if (pollRef.current) clearInterval(pollRef.current); };
  }, [orderNumber]);

  const isPending = pollState === 'pending' || pollState === 'timeout';
  const isError = pollState === 'failed' || pollState === 'expired';

  return (
    <div className="mx-auto max-w-2xl px-4 py-20 sm:px-6">
      <div className="text-center">
        <span
          className={`mx-auto flex h-24 w-24 items-center justify-center rounded-full ring-8 ${
            isError ? 'bg-rose-50 ring-rose-50/50' : 'bg-emerald-50 ring-emerald-50/50'
          }`}
          style={{ animation: 'hero-rise 0.7s cubic-bezier(0.16, 1, 0.3, 1) both' }}
        >
          {isPending ? (
            <FiLoader className="h-12 w-12 text-amber-500 animate-spin" />
          ) : isError ? (
            <FiAlertCircle className="h-12 w-12 text-rose-500" />
          ) : (
            <FiCheckCircle className="h-12 w-12 text-emerald-500" />
          )}
        </span>
        <h1 className="mt-8 text-3xl font-bold tracking-tight sm:text-4xl">
          {isError
            ? (pollState === 'expired' ? 'Payment window expired' : 'Payment failed')
            : isPending
              ? 'Waiting for payment confirmation…'
              : 'Thank you - order confirmed'}
        </h1>
        <p className="mx-auto mt-3 max-w-[46ch] leading-relaxed text-stone-600">
          {isError
            ? 'No money was taken. Please try again or use a different payment method.'
            : isPending
              ? 'We are confirming your payment with the provider. This usually takes a few seconds.'
              : 'We have received your payment and your order is being prepared. A confirmation with full details is on its way to your inbox.'}
        </p>
        {orderNumber && (
          <p className="mt-5 text-sm text-stone-500">
            Order reference{' '}
            <code className="rounded-lg bg-stone-100 px-2.5 py-1 font-mono text-sm font-bold text-stone-900">{orderNumber}</code>
            <span className="ml-2 text-xs uppercase tracking-wider text-stone-400">Status: {orderStatus}</span>
          </p>
        )}
      </div>

      {!isError && (
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
      )}

      <div className="mt-10 flex flex-col gap-3 sm:flex-row sm:justify-center">
        {isError ? (
          <Link href={`/checkout?retry=${orderNumber}`}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary-600 px-7 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700">
            Try payment again <FiArrowRight />
          </Link>
        ) : !loading && !user ? (
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
