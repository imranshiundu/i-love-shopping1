'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { payments } from '@/services/api';
import { formatKES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import {
  FiCreditCard, FiSmartphone, FiLoader, FiCheckCircle,
  FiXCircle, FiClock, FiExternalLink, FiArrowRight,
} from 'react-icons/fi';
import toast from 'react-hot-toast';

interface PaymentItem {
  id: string;
  orderId: string;
  provider: string;
  providerId: string;
  amount: number;
  currency: string;
  status: string;
  metadata: string | null;
  createdAt: string;
}

const STATUS_CONFIG: Record<string, { label: string; color: string; icon: any }> = {
  SUCCEEDED: { label: 'Paid', color: 'bg-emerald-50 text-emerald-700 ring-emerald-200', icon: FiCheckCircle },
  PENDING: { label: 'Pending', color: 'bg-amber-50 text-amber-700 ring-amber-200', icon: FiClock },
  PROCESSING: { label: 'Processing', color: 'bg-blue-50 text-blue-700 ring-blue-200', icon: FiLoader },
  FAILED: { label: 'Failed', color: 'bg-rose-50 text-rose-700 ring-rose-200', icon: FiXCircle },
  CANCELLED: { label: 'Cancelled', color: 'bg-stone-50 text-stone-500 ring-stone-200', icon: FiXCircle },
  REFUNDED: { label: 'Refunded', color: 'bg-purple-50 text-purple-700 ring-purple-200', icon: FiCreditCard },
};

const PROVIDER_CONFIG: Record<string, { label: string; icon: any; color: string }> = {
  MPESA: { label: 'M-Pesa', icon: FiSmartphone, color: 'text-emerald-600 bg-emerald-100' },
  STRIPE: { label: 'Stripe', icon: FiCreditCard, color: 'text-indigo-600 bg-indigo-100' },
  PAYPAL: { label: 'PayPal', icon: FiCreditCard, color: 'text-blue-600 bg-blue-100' },
  FLUTTERWAVE: { label: 'Flutterwave', icon: FiCreditCard, color: 'text-orange-600 bg-orange-100' },
  AIRTEL_MONEY: { label: 'Airtel Money', icon: FiSmartphone, color: 'text-rose-600 bg-rose-100' },
};

function formatDate(dateStr: string) {
  try {
    return new Date(dateStr).toLocaleDateString('en-KE', {
      day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return dateStr;
  }
}

export default function PaymentsPage() {
  const [paymentList, setPaymentList] = useState<PaymentItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPayments();
  }, []);

  const loadPayments = async () => {
    try {
      const res = await payments.getPaymentHistory(0, 50);
      setPaymentList(res.data || []);
    } catch {
      toast.error('Failed to load payment history');
    }
    setLoading(false);
  };

  const getStatusConfig = (status: string) => STATUS_CONFIG[status] || STATUS_CONFIG.FAILED;
  const getProviderConfig = (provider: string) => PROVIDER_CONFIG[provider] || { label: provider, icon: FiCreditCard, color: 'text-stone-600 bg-stone-100' };

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <FiLoader className="h-8 w-8 animate-spin text-primary-600" />
      </div>
    );
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Payments</h1>
        <p className="mt-1 text-sm text-stone-500">Your transaction history and payment receipts.</p>
      </div>

      {paymentList.length === 0 ? (
        <Reveal>
          <div className="rounded-2xl border border-stone-200/80 bg-white p-12 text-center">
            <span className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-stone-100">
              <FiCreditCard className="h-7 w-7 text-stone-400" />
            </span>
            <h2 className="mt-4 text-lg font-bold">No payments yet</h2>
            <p className="mx-auto mt-2 max-w-[40ch] text-sm text-stone-500">
              Your payment history will appear here after your first purchase.
            </p>
            <Link href="/products"
              className="mt-6 inline-flex items-center gap-2 rounded-xl bg-primary-600 px-6 py-3 font-semibold text-white hover:bg-primary-700">
              Start shopping <FiArrowRight />
            </Link>
          </div>
        </Reveal>
      ) : (
        <div className="space-y-3">
          {paymentList.map((payment, i) => {
            const status = getStatusConfig(payment.status);
            const provider = getProviderConfig(payment.provider);
            const ProviderIcon = provider.icon;
            const StatusIcon = status.icon;

            return (
              <Reveal key={payment.id} delay={i * 40}>
                <div className="rounded-2xl border border-stone-200/80 bg-white p-5 transition-all hover:shadow-sm sm:p-6">
                  <div className="flex items-start gap-4">
                    <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${provider.color}`}>
                      <ProviderIcon className="h-5 w-5" />
                    </span>

                    <div className="min-w-0 flex-1">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="font-semibold">{provider.label} Payment</p>
                          <p className="mt-0.5 text-xs text-stone-500">
                            {formatDate(payment.createdAt)}
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-lg font-bold">{formatKES(payment.amount)}</p>
                          <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ${status.color}`}>
                            <StatusIcon className={`h-3 w-3 ${payment.status === 'PROCESSING' ? 'animate-spin' : ''}`} />
                            {status.label}
                          </span>
                        </div>
                      </div>

                      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-stone-500">
                        {payment.orderId && (
                          <Link href={`/account/orders`} className="hover:text-primary-600 hover:underline">
                            Order #{payment.orderId.substring(0, 8)}...
                          </Link>
                        )}
                        {payment.providerId && payment.providerId !== 'PENDING_STK' && (
                          <span className="font-mono">{payment.providerId.substring(0, 20)}{payment.providerId.length > 20 ? '...' : ''}</span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              </Reveal>
            );
          })}
        </div>
      )}
    </div>
  );
}
