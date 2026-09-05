'use client';
import { Suspense, useEffect, useState } from 'react';
import Link from 'next/link';
import { orders as ordersApi } from '@/services/api';
import { config } from '@/lib/config';
import { formatKES, formatDate } from '@/lib/utils';
import { FiArrowLeft, FiPrinter, FiLoader, FiAlertCircle } from 'react-icons/fi';

function InvoiceContent({ orderNumber }: { orderNumber: string }) {
  const [order, setOrder] = useState<any | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const res = await ordersApi.getByNumber(orderNumber);
        setOrder(res.data as any);
      } catch (e: any) {
        setError(e?.message || 'Could not load this order');
      }
    })();
  }, [orderNumber]);

  if (error || (order === null && error !== null)) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-24 text-center print:hidden">
        <FiAlertCircle className="mx-auto h-8 w-8 text-rose-500" />
        <p className="mt-4 text-stone-500">{error || 'Order not found.'}</p>
        <Link href="/account/orders" className="mt-6 inline-block rounded-xl bg-stone-900 px-6 py-3 font-semibold text-white">Back to orders</Link>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-24 text-center print:hidden">
        <FiLoader className="mx-auto h-8 w-8 animate-spin text-primary-600" />
        <p className="mt-4 text-stone-500">Loading invoice...</p>
      </div>
    );
  }

  const items: any[] = order.items || [];
  const addr = order.shippingAddress || {};
  const payUrl = typeof window !== 'undefined' ? `${window.location.origin}/checkout?retry=${order.number}` : '';
  const isUnpaid = ['PENDING', 'EXPIRED', 'CANCELLED'].includes(order.status);

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 print:max-w-none print:px-0 print:py-0">
      <div className="mb-6 flex items-center justify-between print:hidden">
        <Link href={`/account/orders/${order.number}`} className="inline-flex items-center gap-2 text-sm font-semibold text-stone-500 hover:text-stone-700">
          <FiArrowLeft /> Back to order
        </Link>
        <button onClick={() => window.print()}
          className="inline-flex items-center gap-2 rounded-xl bg-stone-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-stone-700">
          <FiPrinter /> Print / Save PDF
        </button>
      </div>

      <div className="rounded-2xl border border-stone-200 bg-white p-8 print:rounded-none print:border-0 print:p-0 sm:p-10">
        <div className="flex flex-wrap items-start justify-between gap-4 border-b-2 border-stone-900 pb-6">
          <div>
            <p className="text-2xl font-extrabold tracking-tight">{config.app.name}<span className="text-primary-600">.</span></p>
            <p className="mt-1 text-sm text-stone-500">{config.app.companyLocation}</p>
            <p className="text-sm text-stone-500">{config.app.supportEmail}</p>
          </div>
          <div className="text-right">
            <p className="text-3xl font-extrabold tracking-tight text-stone-900">INVOICE</p>
            <p className="mt-1 font-mono text-sm font-bold text-stone-600">{order.number}</p>
            <p className="text-sm text-stone-500">Issued {formatDate(order.createdAt)}</p>
          </div>
        </div>

        <div className="mt-6 grid gap-6 sm:grid-cols-2">
          <div>
            <p className="text-xs font-bold uppercase tracking-wide text-stone-400">Bill to</p>
            <p className="mt-1 text-sm font-semibold text-stone-800">{addr.name || order.guestEmail || ''}</p>
            <p className="text-sm text-stone-600">
              {addr.line1}{addr.line2 ? `, ${addr.line2}` : ''}<br />
              {[addr.city, addr.state, addr.postalCode].filter(Boolean).join(', ')}<br />
              {addr.country}{addr.phone ? ` · ${addr.phone}` : ''}
            </p>
          </div>
          <div className="sm:text-right">
            <p className="text-xs font-bold uppercase tracking-wide text-stone-400">Status</p>
            <p className="mt-1 inline-block rounded-full bg-amber-100 px-3 py-1 text-sm font-bold text-amber-700">{order.status}</p>
            <p className="mt-2 text-xs font-bold uppercase tracking-wide text-stone-400">Amount due</p>
            <p className="text-2xl font-extrabold tabular-nums">{formatKES(order.total)}</p>
          </div>
        </div>

        <table className="mt-8 w-full text-sm">
          <thead>
            <tr className="border-b border-stone-200 text-left text-xs uppercase tracking-wide text-stone-400">
              <th className="py-2 pr-2 font-bold">Item</th>
              <th className="py-2 pr-2 text-center font-bold">Qty</th>
              <th className="py-2 text-right font-bold">Line total</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item: any) => (
              <tr key={item.id} className="border-b border-stone-100">
                <td className="py-3 pr-2">
                  <p className="font-semibold text-stone-800">{item.productName}</p>
                  <p className="text-xs text-stone-500">{formatKES(item.price)} each</p>
                </td>
                <td className="py-3 pr-2 text-center tabular-nums">{item.quantity}</td>
                <td className="py-3 text-right font-semibold tabular-nums">{formatKES(item.total)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <dl className="ml-auto mt-6 w-full max-w-xs space-y-1.5 text-sm">
          <div className="flex justify-between"><dt className="text-stone-500">Subtotal</dt><dd className="font-semibold tabular-nums">{formatKES(order.subtotal)}</dd></div>
          <div className="flex justify-between"><dt className="text-stone-500">VAT</dt><dd className="font-semibold tabular-nums">{formatKES(order.tax)}</dd></div>
          <div className="flex justify-between"><dt className="text-stone-500">Delivery</dt><dd className="font-semibold tabular-nums">{formatKES(order.shipping)}</dd></div>
          <div className="flex justify-between border-t border-stone-900 pt-2 text-base font-extrabold">
            <dt>Total due</dt><dd className="tabular-nums">{formatKES(order.total)}</dd>
          </div>
        </dl>

        {isUnpaid && (
          <div className="mt-8 rounded-xl bg-amber-50 p-4 text-sm text-amber-900 ring-1 ring-amber-200 print:border print:border-stone-300 print:bg-white">
            <p className="font-bold">How to pay</p>
            <p className="mt-1">Open this link and choose M-Pesa or card — your items stay reserved:</p>
            <p className="mt-1 break-all font-mono text-xs">{payUrl}</p>
          </div>
        )}

        <p className="mt-8 border-t border-stone-200 pt-4 text-center text-xs text-stone-400">
          {config.app.name} · {config.app.companyLocation} · {config.app.supportEmail}<br />
          Thank you for shopping with us.
        </p>
      </div>
    </div>
  );
}

export default function InvoicePage({ params }: { params: { orderNumber: string } }) {
  return (
    <Suspense>
      <InvoiceContent orderNumber={params.orderNumber} />
    </Suspense>
  );
}
