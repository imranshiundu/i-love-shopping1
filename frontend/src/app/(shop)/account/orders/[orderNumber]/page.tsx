'use client';
import { Suspense, useEffect, useState } from 'react';
import Link from 'next/link';
import { orders as ordersApi } from '@/services/api';
import { formatKES, formatDate } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import { useAuth } from '@/contexts/AuthContext';
import {
  FiPackage, FiCheckCircle, FiClock, FiTruck, FiBox, FiXCircle,
  FiRefreshCw, FiArrowRight, FiArrowLeft, FiLoader, FiAlertCircle,
  FiCreditCard, FiSmartphone,
} from 'react-icons/fi';

const STATUS_META: Record<string, { label: string; icon: any; color: string; pill: string }> = {
  PENDING: { label: 'Awaiting payment', icon: FiClock, color: 'text-amber-600', pill: 'bg-amber-100 text-amber-700' },
  CONFIRMED: { label: 'Confirmed', icon: FiCheckCircle, color: 'text-emerald-600', pill: 'bg-emerald-100 text-emerald-700' },
  PROCESSING: { label: 'Processing', icon: FiBox, color: 'text-blue-600', pill: 'bg-blue-100 text-blue-700' },
  SHIPPED: { label: 'Shipped', icon: FiTruck, color: 'text-indigo-600', pill: 'bg-indigo-100 text-indigo-700' },
  DELIVERED: { label: 'Delivered', icon: FiCheckCircle, color: 'text-emerald-600', pill: 'bg-emerald-100 text-emerald-700' },
  CANCELLED: { label: 'Cancelled', icon: FiXCircle, color: 'text-rose-600', pill: 'bg-rose-100 text-rose-700' },
  REFUNDED: { label: 'Refunded', icon: FiRefreshCw, color: 'text-purple-600', pill: 'bg-purple-100 text-purple-700' },
};

const PROVIDER_ICON: Record<string, any> = {
  MPESA: FiSmartphone,
  STRIPE: FiCreditCard,
};

function OrderDetailContent({ orderNumber }: { orderNumber: string }) {
  const { user, loading } = useAuth();
  const [order, setOrder] = useState<any | null>(null);
  const [loadingOrder, setLoadingOrder] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!orderNumber) return;
    (async () => {
      setLoadingOrder(true);
      try {
        const res = await ordersApi.getByNumber(orderNumber);
        setOrder(res.data as any);
        setError(null);
      } catch (e: any) {
        setError(e?.message || 'Could not load this order');
      }
      setLoadingOrder(false);
    })();
  }, [orderNumber]);

  if (loadingOrder) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-24 text-center">
        <FiLoader className="mx-auto h-8 w-8 animate-spin text-primary-600" />
        <p className="mt-4 text-stone-500">Loading order details...</p>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-24 text-center">
        <span className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-rose-50">
          <FiAlertCircle className="h-7 w-7 text-rose-500" />
        </span>
        <h1 className="mt-5 text-2xl font-bold">Order not found</h1>
        <p className="mx-auto mt-2 max-w-[42ch] text-stone-500">{error || 'We could not find this order.'}</p>
        <Link href="/account/orders"
          className="mt-8 inline-flex items-center gap-2 rounded-xl bg-primary-600 px-6 py-3 font-semibold text-white hover:bg-primary-700">
          <FiArrowLeft /> Back to orders
        </Link>
      </div>
    );
  }

  const meta = STATUS_META[order.status] || { label: order.status, icon: FiBox, color: 'text-stone-600', pill: 'bg-stone-100 text-stone-600' };
  const StatusIcon = meta.icon;
  const items: any[] = order.items || [];
  const payments: any[] = order.payments || [];

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <Link href="/account/orders" className="inline-flex items-center gap-2 text-sm font-semibold text-stone-500 hover:text-stone-700">
        <FiArrowLeft /> Back to orders
      </Link>

      <Reveal>
        <div className="mt-5 flex flex-wrap items-center gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Order {order.number}</h1>
            <p className="mt-1 text-sm text-stone-500">
              Placed {formatDate(order.createdAt)}
              {order.updatedAt && order.updatedAt !== order.createdAt && <> · updated {formatDate(order.updatedAt)}</>}
            </p>
          </div>
          <span className={`inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-sm font-bold ${meta.pill}`}>
            <StatusIcon className="h-4 w-4" />
            {meta.label}
          </span>
        </div>
      </Reveal>

      {order.status === 'CANCELLED' && (
        <Reveal delay={60}>
          <div className="mt-6 flex items-start gap-3 rounded-2xl bg-rose-50 p-4 ring-1 ring-rose-200">
            <FiXCircle className="mt-0.5 h-5 w-5 shrink-0 text-rose-600" />
            <div className="text-sm text-rose-900">
              <p className="font-semibold">This order was cancelled</p>
              <p className="mt-0.5 text-rose-800">No payment was taken and any reserved stock was returned to the catalogue.</p>
            </div>
          </div>
        </Reveal>
      )}

      {order.status === 'PENDING' && (
        <Reveal delay={60}>
          <div className="mt-6 flex items-center justify-between gap-4 rounded-2xl bg-amber-50 p-4 ring-1 ring-amber-200">
            <div className="flex items-start gap-3">
              <FiClock className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
              <div className="text-sm text-amber-900">
                <p className="font-semibold">This order is awaiting payment</p>
                <p className="mt-0.5 text-amber-800">Complete payment to confirm your order.</p>
              </div>
            </div>
            <Link href={`/checkout?retry=${order.number}`}
              className="shrink-0 rounded-xl bg-amber-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-amber-700">
              Pay now
            </Link>
          </div>
        </Reveal>
      )}

      <Reveal delay={100}>
        <div className="mt-6 rounded-2xl border border-stone-200/80 bg-white p-6">
          <h2 className="text-lg font-bold">Items</h2>
          <ul className="mt-4 space-y-4">
            {items.map(item => (
              <li key={item.id} className="flex items-center gap-4">
                <span className="h-14 w-14 shrink-0 overflow-hidden rounded-xl bg-stone-100">
                  {item.productImage && <img src={item.productImage} alt="" className="h-full w-full object-cover" />}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="font-semibold">{item.productName}</p>
                  <p className="text-sm text-stone-500">Qty {item.quantity} × {formatKES(item.price)}</p>
                </div>
                <span className="font-semibold tabular-nums">{formatKES(item.total)}</span>
              </li>
            ))}
          </ul>

          <dl className="mt-6 space-y-2.5 border-t border-stone-200 pt-5 text-sm">
            <div className="flex justify-between"><dt className="text-stone-500">Subtotal</dt><dd className="font-semibold">{formatKES(order.subtotal)}</dd></div>
            <div className="flex justify-between"><dt className="text-stone-500">Delivery</dt><dd className="font-semibold">{formatKES(order.shipping)}</dd></div>
            <div className="flex justify-between"><dt className="text-stone-500">VAT</dt><dd className="font-semibold">{formatKES(order.tax)}</dd></div>
            <div className="flex justify-between border-t border-stone-200 pt-3">
              <dt className="text-base font-bold">Total</dt><dd className="text-lg font-extrabold">{formatKES(order.total)}</dd>
            </div>
          </dl>
        </div>
      </Reveal>

      {order.shippingAddress && (
        <Reveal delay={140}>
          <div className="mt-5 rounded-2xl border border-stone-200/80 bg-white p-6">
            <h2 className="text-lg font-bold">Delivery address</h2>
            <p className="mt-3 text-sm leading-relaxed text-stone-700">
              {order.shippingAddress.name}<br />
              {order.shippingAddress.line1}{order.shippingAddress.line2 ? `, ${order.shippingAddress.line2}` : ''}<br />
              {order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.postalCode}<br />
              {order.shippingAddress.country}
              {order.shippingAddress.phone && <> · {order.shippingAddress.phone}</>}
            </p>
            {order.notes && <p className="mt-3 rounded-xl bg-stone-50 p-3 text-sm text-stone-600">Note: {order.notes}</p>}
          </div>
        </Reveal>
      )}

      {payments.length > 0 && (
        <Reveal delay={180}>
          <div className="mt-5 rounded-2xl border border-stone-200/80 bg-white p-6">
            <h2 className="text-lg font-bold">Payments</h2>
            <ul className="mt-4 space-y-3">
              {payments.map(p => {
                const PIcon = PROVIDER_ICON[p.provider] || FiCreditCard;
                return (
                  <li key={p.id} className="flex items-center gap-3 text-sm">
                    <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-stone-100">
                      <PIcon className="h-4 w-4 text-stone-500" />
                    </span>
                    <div className="flex-1">
                      <p className="font-semibold">{p.provider} · {formatKES(p.amount)}</p>
                      <p className="text-xs text-stone-500">{formatDate(p.createdAt)}</p>
                    </div>
                    <span className="text-xs font-bold uppercase tracking-wide text-stone-400">{p.status}</span>
                  </li>
                );
              })}
            </ul>
          </div>
        </Reveal>
      )}

      <Reveal delay={220}>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          <Link href="/products"
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary-600 px-6 py-3 font-semibold text-white shadow-lg shadow-primary-600/25 hover:bg-primary-700">
            Continue shopping <FiArrowRight />
          </Link>
          {order.status === 'PENDING' && user && (
            <Link href={`/checkout?retry=${order.number}`}
              className="inline-flex items-center justify-center gap-2 rounded-xl border border-stone-300 px-6 py-3 font-semibold text-stone-700 hover:bg-stone-50">
              Retry payment
            </Link>
          )}
        </div>
      </Reveal>
    </div>
  );
}

export default function OrderDetailPage({ params }: { params: { orderNumber: string } }) {
  return (
    <Suspense fallback={<div className="mx-auto max-w-lg px-4 py-16 text-center">Loading...</div>}>
      <OrderDetailContent orderNumber={params.orderNumber} />
    </Suspense>
  );
}
