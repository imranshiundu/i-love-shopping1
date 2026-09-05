'use client';
import { useState, useEffect, useRef, useCallback, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { loadStripe } from '@stripe/stripe-js';
import {
  Elements,
  CardNumberElement,
  CardExpiryElement,
  CardCvcElement,
  useElements,
  useStripe,
} from '@stripe/react-stripe-js';
import { useAuth } from '@/contexts/AuthContext';
import { orders, payments as paymentsApi, auth } from '@/services/api';
import { config } from '@/lib/config';
import { formatKES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import AuthModal from '@/components/auth/AuthModal';
import PhoneInput from '@/components/ui/PhoneInput';
import { Address } from '@/types';
import {
  FiCheck, FiArrowRight, FiShoppingBag, FiX,
  FiSmartphone, FiCreditCard, FiLock, FiLoader, FiAlertCircle, FiCheckCircle,
} from 'react-icons/fi';
import toast from 'react-hot-toast';

type PayMethod = 'mpesa' | 'stripe';
type StkStatus = 'idle' | 'sending' | 'waiting_pin' | 'polling' | 'success' | 'failed' | 'cancelled';

const STK_POLL_MS = 3000;
const STK_POLL_MAX = 40;

const stripePromise = config.stripe.publishableKey
  ? loadStripe(config.stripe.publishableKey)
  : null;

const FIELD_STYLE = {
  base: { fontSize: '15px', color: '#1c1917', '::placeholder': { color: '#a8a29e' }, fontFamily: 'system-ui, sans-serif' },
  invalid: { color: '#e11d48' },
};

const PAY_METHODS: { id: PayMethod; label: string; desc: string; icon: typeof FiSmartphone; accent: string }[] = [
  { id: 'mpesa', label: 'M-Pesa', desc: 'STK push to your Safaricom line', icon: FiSmartphone, accent: 'text-emerald-600 bg-emerald-100' },
  { id: 'stripe', label: 'Card — Visa & Mastercard', desc: 'Secure card payment via Stripe', icon: FiCreditCard, accent: 'text-indigo-600 bg-indigo-100' },
];

function CardBrands() {
  return (
    <div className="flex items-center gap-2">
      <span className="rounded-md bg-blue-700 px-1.5 py-0.5 text-[11px] font-extrabold italic tracking-tight text-white">VISA</span>
      <span className="rounded-md bg-stone-900 px-1.5 py-0.5 text-[11px] font-bold text-white">Mastercard</span>
      <span className="rounded-md bg-amber-400 px-1.5 py-0.5 text-[11px] font-extrabold text-white">AMEX</span>
    </div>
  );
}

function StripePaymentForm({ orderId, total, onResult }: { orderId: string; total: number; onResult: (ok: boolean, msg: string) => void }) {
  const stripe = useStripe();
  const elements = useElements();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [complete, setComplete] = useState({ number: false, expiry: false, cvc: false });
  const [fieldError, setFieldError] = useState<{ number?: string; expiry?: string; cvc?: string }>({});

  const handlePay = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (!stripe || !elements) return;
    const numberEl = elements.getElement(CardNumberElement);
    const expiryEl = elements.getElement(CardExpiryElement);
    const cvcEl = elements.getElement(CardCvcElement);
    if (!numberEl || !expiryEl || !cvcEl) return;
    // Surface the specific invalid field, not a generic message
    if (!complete.number || fieldError.number) {
      setError(fieldError.number || 'Please enter a valid card number');
      return;
    }
    if (!complete.expiry || fieldError.expiry) {
      setError(fieldError.expiry || 'Please enter a valid expiry date');
      return;
    }
    if (!complete.cvc || fieldError.cvc) {
      setError(fieldError.cvc || 'Please enter a valid CVC');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      if (total < config.stripe.minAmount) {
        throw new Error(
          `Card payments require a minimum of ${formatKES(config.stripe.minAmount)}. Your total is ${formatKES(total)} — please add more items or use M-Pesa.`
        );
      }
      const intentRes = await paymentsApi.stripeCreateIntent(orderId, total);
      const clientSecret = intentRes.data?.clientSecret;
      if (!clientSecret) throw new Error('Failed to initialize card payment');
      const result = await stripe.confirmCardPayment(clientSecret, {
        payment_method: { card: numberEl },
      });
      if (result.error) {
        const msg = mapStripeError(result.error);
        setError(msg);
        onResult(false, msg);
      } else if (result.paymentIntent?.status === 'succeeded') {
        // Mark the order as paid in the backend (webhook may be delayed/unconfigured)
        try {
          await paymentsApi.stripeConfirm(result.paymentIntent.id);
        } catch { /* order already confirmed via webhook */ }
        onResult(true, 'Card payment successful');
      } else {
        setError('Payment requires further action');
        onResult(false, 'Payment requires further action');
      }
    } catch (e: any) {
      setError(e?.message || 'Payment failed');
      onResult(false, e?.message || 'Payment failed');
    } finally {
      setSubmitting(false);
    }
  };

  const mapStripeError = (err: any): string => {
    const code = err?.code;
    const msg = err?.message || 'Card declined';
    switch (code) {
      case 'card_declined': return 'Your card was declined. Please try another card.';
      case 'expired_card': return 'Your card has expired. Please use a valid card.';
      case 'incorrect_cvc': return 'The CVC you entered is incorrect.';
      case 'incorrect_number': return 'The card number is incorrect.';
      case 'processing_error': return 'There was an error processing your card. Please try again.';
      case 'insufficient_funds': return 'Your card has insufficient funds.';
      case 'invalid_expiry_month': return 'The expiry month is invalid.';
      case 'invalid_expiry_year': return 'The expiry year is invalid.';
      default: return msg;
    }
  };

  const inputWrap = 'rounded-xl border border-stone-300 bg-white px-3.5 py-3 transition-colors focus-within:border-primary-500 focus-within:ring-2 focus-within:ring-primary-100';

  return (
    <form onSubmit={handlePay}>
      <div className="mb-3 flex items-center justify-between">
        <p className="text-xs font-semibold uppercase tracking-widest text-stone-400">Card details</p>
        <CardBrands />
      </div>
      <div className="space-y-3">
        <div>
          <label className="mb-1 block text-xs font-semibold uppercase tracking-widest text-stone-400">Card number</label>
          <div className={inputWrap}>
            <CardNumberElement
              options={{ style: FIELD_STYLE, placeholder: 'Card number' }}
              onChange={e => {
                setComplete(prev => ({ ...prev, number: e.complete }));
                setFieldError(prev => ({ ...prev, number: e.error?.message }));
                if (e.error?.message) setError(e.error.message);
              }}
            />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-widest text-stone-400">Expiry</label>
            <div className={inputWrap}>
              <CardExpiryElement
                options={{ style: FIELD_STYLE, placeholder: 'MM / YY' }}
                onChange={e => {
                  setComplete(prev => ({ ...prev, expiry: e.complete }));
                  setFieldError(prev => ({ ...prev, expiry: e.error?.message }));
                  if (e.error?.message) setError(e.error.message);
                }}
              />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-widest text-stone-400">CVC</label>
            <div className={inputWrap}>
              <CardCvcElement
                options={{ style: FIELD_STYLE, placeholder: '123' }}
                onChange={e => {
                  setComplete(prev => ({ ...prev, cvc: e.complete }));
                  setFieldError(prev => ({ ...prev, cvc: e.error?.message }));
                  if (e.error?.message) setError(e.error.message);
                }}
              />
            </div>
          </div>
        </div>
      </div>
      {error && <p className="mt-2 text-sm text-rose-600">{error}</p>}
      <button type="submit" disabled={!stripe || submitting}
        className="mt-4 w-full rounded-xl bg-primary-600 py-3 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700 disabled:opacity-50">
        {submitting ? 'Processing…' : `Pay ${formatKES(total)}`}
      </button>
    </form>
  );
}

function CheckoutContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const retryOrderNumber = searchParams.get('retry');
  const { cart, cartLoading, refreshCart, user } = useAuth();

  const [paymentMethod, setPaymentMethod] = useState<PayMethod>('mpesa');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [stkStatus, setStkStatus] = useState<StkStatus>('idle');
  const [stkMessage, setStkMessage] = useState('');
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const pollCountRef = useRef(0);
  const checkoutRequestIdRef = useRef<string | null>(null);
  const orderNumberRef = useRef<string | null>(null);
  const capturedTotalRef = useRef<number>(0);
  const [payTotal, setPayTotal] = useState(0);
  const [orderForPayment, setOrderForPayment] = useState<{ id: string; number: string } | null>(null);

  // Auth gate: user must sign in/register before paying
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [proceedAfterAuth, setProceedAfterAuth] = useState(false);

  const [savedAddresses, setSavedAddresses] = useState<Address[]>([]);
  const [selectedShippingId, setSelectedShippingId] = useState<string>('');

  const [shipping, setShipping] = useState<Address & { email?: string }>({
    name: '', line1: '', line2: '', city: '', state: '', postalCode: '',
    country: config.commerce.defaultCountry, phone: '', type: 'SHIPPING', email: '',
  } as any);
  const [billing, setBilling] = useState<Address>({
    name: '', line1: '', line2: '', city: '', state: '', postalCode: '',
    country: config.commerce.defaultCountry, type: 'BILLING',
  });
  const [sameAsShipping, setSameAsShipping] = useState(true);
  const [notes, setNotes] = useState('');

  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, boolean>>({});

  const subtotal = cart?.items.reduce((sum, item) => sum + item.priceSnapshot * item.quantity, 0) || 0;
  const shippingCost = subtotal >= config.commerce.freeShippingThreshold ? 0 : config.commerce.shippingCost;
  const tax = Math.round(subtotal * config.commerce.taxRate * 100) / 100;
  const total = subtotal + shippingCost + tax;

  useEffect(() => {
    refreshCart();
  }, [refreshCart]);

  // Check for pending orders (incomplete payments) for logged-in users
  const [pendingOrder, setPendingOrder] = useState<any | null>(null);
  useEffect(() => {
    if (!user) return;
    (async () => {
      try {
        const res = await orders.list(0, 10, 'PENDING');
        const list: any[] = Array.isArray(res.data) ? res.data : ((res.data as any)?.orders || []);
        if (list.length > 0) {
          setPendingOrder(list[0]);
          orderNumberRef.current = list[0].number;
          capturedTotalRef.current = list[0].total;
          setPayTotal(list[0].total);
        }
      } catch { /* ignore */ }
    })();
  }, [user]);

  // Handle retry from success page
  useEffect(() => {
    if (!retryOrderNumber) return;
    (async () => {
      try {
        const res = await orders.getByNumber(retryOrderNumber);
        const order = res.data as any;
        if (order) {
          orderNumberRef.current = order.number;
          capturedTotalRef.current = order.total;
          setPayTotal(order.total);
        }
      } catch { /* ignore */ }
    })();
  }, [retryOrderNumber]);

  useEffect(() => {
    if (!user) return;
    auth.getAddresses().then(res => {
      const list: Address[] = (res.data as any) || [];
      setSavedAddresses(list);
      if (list.length > 0) {
        const preferred = list.find(a => a.isDefault) || list[0];
        applyAddress(preferred, 'shipping');
        setSelectedShippingId(preferred.id || '');
      }
    }).catch(() => {});
  }, [user]);

  useEffect(() => {
    return () => { if (pollRef.current) clearInterval(pollRef.current); };
  }, []);

  const applyAddress = (addr: Address, target: 'shipping' | 'billing') => {
    const mapped: Address = {
      name: addr.name || '', line1: addr.line1 || '', line2: addr.line2 || '',
      city: addr.city || '', state: addr.state || '', postalCode: addr.postalCode || '',
      country: addr.country || config.commerce.defaultCountry, phone: addr.phone || '',
      type: target === 'shipping' ? 'SHIPPING' : 'BILLING',
    };
    if (target === 'shipping') setShipping({ ...mapped, email: (shipping as any).email } as any);
    else setBilling(mapped);
  };

  const REQUIRED: { key: keyof Address; label: string }[] = [
    { key: 'name', label: 'Full name' }, { key: 'line1', label: 'Address line 1' },
    { key: 'city', label: 'City / Town' }, { key: 'state', label: 'County / State' },
    { key: 'postalCode', label: 'Postal code' }, { key: 'phone', label: 'Phone number' },
  ];

  const validateAddress = (): boolean => {
    const errors: Record<string, boolean> = {};
    for (const { key } of REQUIRED) {
      if (!String((shipping as any)[key as keyof Address] || '').trim()) errors[key as string] = true;
    }
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      const first = Object.keys(errors)[0];
      toast.error(`Please fill in ${REQUIRED.find(f => f.key === first)?.label}`);
      return false;
    }
    if ((shipping.phone || '').replace(/\D/g, '').length < 9) {
      toast.error('Enter a valid phone number'); return false;
    }
    if (!sameAsShipping) {
      for (const { key, label } of REQUIRED.filter(f => f.key !== 'phone')) {
        if (!String(billing[key as keyof Address] || '').trim()) {
          toast.error(`Billing address missing ${label}`); return false;
        }
      }
    }
    setFieldErrors({});
    return true;
  };

  const validatePayment = (): boolean => {
    if (paymentMethod === 'mpesa') {
      if (!phoneNumber) { toast.error('Enter your M-Pesa phone number'); return false; }
      if (phoneNumber.replace(/\D/g, '').length < 9) {
        toast.error('Enter a valid phone number'); return false;
      }
    }
    return true;
  };

  const handlePaidSuccess = useCallback(() => {
    setStkStatus('success');
    setStkMessage('Payment confirmed!');
    toast.success('Payment confirmed');
    const orderNum = orderNumberRef.current;
    setTimeout(() => router.push(`/checkout/success?order=${orderNum || ''}`), 600);
  }, [router]);

  const startMpesaPolling = useCallback((checkoutRequestId: string) => {
    pollCountRef.current = 0;
    checkoutRequestIdRef.current = checkoutRequestId;
    setStkStatus('polling');
    setStkMessage('Waiting for M-Pesa confirmation...');
    if (pollRef.current) clearInterval(pollRef.current);

    pollRef.current = setInterval(async () => {
      pollCountRef.current += 1;
      if (pollCountRef.current > STK_POLL_MAX) {
        if (pollRef.current) clearInterval(pollRef.current);
        setStkStatus('failed');
        setStkMessage('Payment timed out.');
        toast.error('Payment timed out');
        return;
      }
      try {
        // Always check order status first — the callback/query updates it to CONFIRMED
        if (orderNumberRef.current) {
          try {
            const orderRes = await orders.getByNumber(orderNumberRef.current);
            const orderStatus = (orderRes.data as any)?.status;
            if (['CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'].includes(orderStatus)) {
              if (pollRef.current) clearInterval(pollRef.current);
              handlePaidSuccess();
              return;
            }
          } catch { /* fall through to STK query */ }
        }
        const result = await orders.mpesaStkQuery(checkoutRequestId);
        const code = (result.data?.responseCode || '') as string;
        const desc = ((result.data?.responseDescription || result.data?.customerMessage || '') as string).toLowerCase();
        if (code === '0' || desc.includes('successfully') || desc.includes('completed')) {
          if (pollRef.current) clearInterval(pollRef.current);
          handlePaidSuccess();
          return;
        }
        // 1037 = still in-flight (user may be entering PIN) — keep polling
        if (code === '1037' || desc.includes('waiting for payment')) {
          setStkMessage(`Waiting for M-Pesa confirmation... (${pollCountRef.current}/${STK_POLL_MAX})`);
          return;
        }
        if (desc.includes('cancelled')) {
          if (pollRef.current) clearInterval(pollRef.current);
          setStkStatus('cancelled'); setStkMessage('Payment was cancelled.');
          toast.error('Payment cancelled');
          return;
        }
        if (code !== '0' && code !== '' && (desc.includes('failed') || desc.includes('declined') || desc.includes('insufficient') || desc.includes('expired') || desc.includes('timeout'))) {
          if (pollRef.current) clearInterval(pollRef.current);
          setStkStatus('failed'); setStkMessage('Payment was not completed.');
          toast.error('Payment not completed');
          return;
        }
        setStkMessage(`Checking status... (${pollCountRef.current}/${STK_POLL_MAX})`);
      } catch (e: any) {
        setStkMessage(`Waiting for M-Pesa... (${pollCountRef.current}/${STK_POLL_MAX})`);
        if (pollCountRef.current >= 3) console.warn('STK query error:', e?.message);
      }
    }, STK_POLL_MS);
  }, [handlePaidSuccess, router]);

  const handlePlaceOrder = async () => {
    if (!validateAddress()) return;
    // Require authentication before payment — user must sign in / register
    if (!user) {
      setShowAuthModal(true);
      setProceedAfterAuth(true);
      return;
    }
    setPayTotal(total);
    setShowPaymentModal(true);
    setStkStatus('idle');
    setStkMessage('');
  };

  const handleAuthSuccess = async () => {
    setShowAuthModal(false);
    if (!proceedAfterAuth) return;
    setProceedAfterAuth(false);
    await refreshCart();
    setShowPaymentModal(true);
    setStkStatus('idle');
    setStkMessage('');
  };

  const handleConfirmPayment = async () => {
    if (!validatePayment()) return;

    setStkStatus('sending');
    setStkMessage('Creating your order...');

    try {
      let orderId = '';
      let orderNum = '';
      let orderTotal = payTotal || total;

      // If an order was already created (e.g. a failed Stripe attempt or a resumed
      // pending order), reuse it rather than re-checking-out an empty cart.
      if (orderNumberRef.current) {
        orderNum = orderNumberRef.current;
        orderTotal = capturedTotalRef.current || orderTotal;
        try {
          const existing = await orders.getByNumber(orderNum);
          const st = (existing.data as any)?.status;
          if (existing.data && ['PENDING', 'EXPIRED', 'CANCELLED'].includes(st)) {
            orderId = (existing.data as any).id;
            orderTotal = Number((existing.data as any).total) || orderTotal;
            setPayTotal(orderTotal);
          }
        } catch { /* fall through to fresh checkout */ }
      }

      if (!orderId) {
        const res = await orders.checkout({
          shippingAddress: { ...shipping, type: 'SHIPPING' },
          billingAddress: sameAsShipping ? { ...shipping, type: 'BILLING' } : { ...billing, type: 'BILLING' },
          notes,
          guestEmail: user?.email || undefined,
        });
        const order = res.data as any;
        if (!order) throw new Error('Checkout failed');
        orderId = order.id;
        orderNum = order.number;
        orderTotal = Number(order.total) || total;
        orderNumberRef.current = orderNum;
        capturedTotalRef.current = orderTotal;
        setPayTotal(orderTotal);
        await refreshCart();
      }

      if (paymentMethod === 'mpesa') {
        setStkStatus('waiting_pin');
        setStkMessage('Enter your M-Pesa PIN on your phone...');
        const pushRes = await orders.mpesaStkPush(orderId, String(orderTotal), phoneNumber);
        const data = pushRes.data as any;
        const checkoutRequestId = data?.checkoutRequestId;
        if (!checkoutRequestId) {
          // Cancel order, restore cart, surface error
          try { await orders.cancel(orderNum); } catch {}
          await refreshCart();
          throw new Error(data?.customerMessage || data?.responseDescription || 'Failed to send M-Pesa prompt');
        }
        toast.success('PIN prompt sent — check your phone');
        startMpesaPolling(checkoutRequestId);
      } else if (paymentMethod === 'stripe') {
        setStkStatus('sending');
        setStkMessage('Enter your card details and click Pay…');
        setOrderForPayment({ id: orderId, number: orderNum });
      }
    } catch (e: any) {
      const msg = e?.message || 'Checkout failed';
      if (msg.toLowerCase().includes('insufficient stock')) {
        toast.error('Some items are no longer available. Cart restored.');
        await refreshCart();
      } else if (msg.includes('401')) {
        toast.error('Please sign in to complete your order');
        setShowAuthModal(true);
        setProceedAfterAuth(true);
      } else {
        toast.error(msg);
      }
      setStkStatus('failed');
      setStkMessage(msg);
    }
  };

  const onStripeResult = (ok: boolean, msg: string) => {
    if (ok) {
      handlePaidSuccess();
    } else {
      setStkStatus('failed');
      setStkMessage(msg);
      toast.error(msg);
    }
  };

  const resetPayment = () => {
    if (pollRef.current) clearInterval(pollRef.current);
    setStkStatus('idle'); setStkMessage('');
    setOrderForPayment(null);
    checkoutRequestIdRef.current = null; orderNumberRef.current = null; pollCountRef.current = 0;
  };

  const closeModal = () => {
    if (stkStatus === 'sending' || stkStatus === 'waiting_pin' || stkStatus === 'polling') return;
    setShowPaymentModal(false);
    resetPayment();
  };

  if (cartLoading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-24 text-center">
        <FiLoader className="mx-auto h-8 w-8 animate-spin text-primary-600" />
        <p className="mt-4 text-stone-500">Loading your cart...</p>
      </div>
    );
  }

  // While payment is in progress, the cart is empty by design — keep the form mounted.
  const isInPaymentFlow = stkStatus !== 'idle' || orderForPayment !== null;
  if (!cart || (cart.items.length === 0 && !isInPaymentFlow && !showPaymentModal && !showAuthModal)) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-24 text-center">
        <span className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-stone-100">
          <FiShoppingBag className="h-9 w-9 text-stone-400" />
        </span>
        <h1 className="mt-6 text-2xl font-bold">Nothing to check out yet</h1>
        <p className="mx-auto mt-2 max-w-[42ch] text-stone-500">Add items to your cart first.</p>
        <Link href="/products" className="mt-8 inline-flex items-center gap-2 rounded-xl bg-primary-600 px-7 py-3.5 font-semibold text-white hover:bg-primary-700">
          Browse products <FiArrowRight />
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Checkout</h1>
        <p className="mt-1 text-sm text-stone-500">Complete your delivery details to proceed to payment.</p>
      </div>

      {!user && (
        <div className="mb-6 flex items-center justify-between gap-4 rounded-2xl border border-primary-200 bg-primary-50 p-4">
          <div>
            <p className="font-semibold text-primary-900">Sign in for a smoother checkout</p>
            <p className="text-sm text-primary-800">You&apos;ll need an account to complete payment — your order history is kept here.</p>
          </div>
          <button onClick={() => setShowAuthModal(true)}
            className="shrink-0 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700">
            Sign in / Register
          </button>
        </div>
      )}

      {pendingOrder && (
        <div className="mb-6 flex items-center justify-between gap-4 rounded-2xl border border-amber-200 bg-amber-50 p-4">
          <div>
            <p className="font-semibold text-amber-900">You have an incomplete order</p>
            <p className="text-sm text-amber-800">Order <strong>{pendingOrder.number}</strong> for {formatKES(pendingOrder.total)} is awaiting payment.</p>
          </div>
          <button onClick={() => { setShowPaymentModal(true); setStkStatus('idle'); }}
            className="rounded-xl bg-amber-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-amber-700">
            Resume payment
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-[1.5fr_1fr]">
        <div className="space-y-5">
          <Reveal>
            <section className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-7">
              <h2 className="text-lg font-bold">Delivery address</h2>
              {savedAddresses.length > 0 && (
                <div className="mt-4 space-y-2">
                  <p className="text-sm font-medium text-stone-600">Use a saved address:</p>
                  <div className="grid gap-2">
                    {savedAddresses.map(addr => (
                      <label key={addr.id} className="flex cursor-pointer items-center gap-3 rounded-xl border border-stone-200 p-3 transition-all hover:border-primary-300 has-[:checked]:border-primary-500 has-[:checked]:bg-primary-50/50">
                        <input type="radio" name="savedShipping" checked={selectedShippingId === addr.id}
                          onChange={() => { setSelectedShippingId(addr.id || ''); applyAddress(addr, 'shipping'); }}
                          className="accent-primary-600" />
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-semibold">{addr.name}</p>
                          <p className="truncate text-xs text-stone-500">{addr.line1}, {addr.city}, {addr.country}</p>
                        </div>
                        {addr.isDefault && <span className="shrink-0 rounded-full bg-primary-100 px-2 py-0.5 text-xs font-medium text-primary-700">Default</span>}
                      </label>
                    ))}
                  </div>
                  <div className="flex items-center gap-2 text-xs text-stone-400">
                    <span className="h-px flex-1 bg-stone-200" /><span>or enter a new address below</span><span className="h-px flex-1 bg-stone-200" />
                  </div>
                </div>
              )}
              <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2">
                {([
                  { label: 'Full name', key: 'name', span: true }, { label: 'Address line 1', key: 'line1', span: true },
                  { label: 'Address line 2', key: 'line2', span: true },
                  { label: 'City / Town', key: 'city' }, { label: 'County / State', key: 'state' },
                  { label: 'Postal code', key: 'postalCode' }, { label: 'Phone', key: 'phone' },
                ]).map(field => (
                  <div key={field.key} className={field.span ? 'sm:col-span-2' : ''}>
                    <label className="mb-1 block text-sm font-medium text-stone-700">{field.label}<span className="text-rose-500"> *</span></label>
                    {field.key === 'phone' ? (
                      <PhoneInput
                        value={(shipping as any).phone || ''}
                        onChange={v => { setShipping({ ...shipping, phone: v }); setSelectedShippingId(''); if (fieldErrors.phone) setFieldErrors(prev => ({ ...prev, phone: false })); }}
                        placeholder="0712345678 or 254712345678"
                        className={fieldErrors.phone ? 'border-rose-400 bg-rose-50/40' : 'border-stone-300'}
                      />
                    ) : (
                      <input type="text" value={(shipping as any)[field.key] || ''}
                        onChange={e => { setShipping({ ...shipping, [field.key]: e.target.value }); setSelectedShippingId(''); if (fieldErrors[field.key]) setFieldErrors(prev => ({ ...prev, [field.key]: false })); }}
                        className={`w-full rounded-xl border px-3.5 py-2.5 text-sm transition-colors focus:outline-none focus:ring-2 ${fieldErrors[field.key] ? 'border-rose-400 bg-rose-50/40 focus:border-rose-500 focus:ring-rose-100' : 'border-stone-300 focus:border-primary-500 focus:ring-primary-100'}`} />
                    )}
                  </div>
                ))}
              </div>
              <label className="mt-5 flex cursor-pointer items-center gap-2.5 text-sm font-medium">
                <input type="checkbox" checked={sameAsShipping} onChange={e => setSameAsShipping(e.target.checked)} className="h-4 w-4 accent-primary-600" />
                Billing address is the same as delivery
              </label>
              {!sameAsShipping && (
                <div className="mt-4 grid grid-cols-1 gap-4 rounded-xl bg-stone-50 p-4 sm:grid-cols-2">
                  {(['name', 'line1', 'line2', 'city', 'state', 'postalCode'] as const).map(key => (
                    <div key={key} className={key === 'line1' || key === 'name' ? 'sm:col-span-2' : ''}>
                      <label className="mb-1 block text-sm font-medium capitalize text-stone-700">{key.replace(/([A-Z])/g, ' $1')}</label>
                      <input type="text" value={(billing as any)[key] || ''}
                        onChange={e => setBilling({ ...billing, [key]: e.target.value })}
                        className="w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100" />
                    </div>
                  ))}
                </div>
              )}
            </section>
          </Reveal>

          <Reveal delay={80}>
            <section className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-7">
              <h2 className="text-lg font-bold">Order notes</h2>
              <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={3}
                placeholder="Gate code, preferred delivery time, gift note..."
                className="mt-4 w-full resize-none rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100" />
            </section>
          </Reveal>
        </div>

        <aside className="lg:sticky lg:top-28 lg:self-start">
          <Reveal delay={140}>
            <div className="rounded-2xl border border-stone-200/80 bg-white p-6">
              <h2 className="text-lg font-bold">Your order</h2>
              {cart.items.length > 0 && (
                <ul className="mt-4 space-y-3">
                  {cart.items.map(item => (
                    <li key={item.id} className="flex items-center gap-3">
                      <span className="relative shrink-0 overflow-hidden rounded-lg bg-stone-100">
                        {item.productImage && <img src={item.productImage} alt="" className="h-12 w-12 object-cover" />}
                        <span className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-stone-900 text-[10px] font-bold text-white">{item.quantity}</span>
                      </span>
                      <span className="min-w-0 flex-1 truncate text-sm">{item.productName}</span>
                      <span className="text-sm font-semibold">{formatKES(item.lineTotal)}</span>
                    </li>
                  ))}
                </ul>
              )}
              <dl className="mt-5 space-y-2.5 border-t border-stone-200 pt-4 text-sm">
                <div className="flex justify-between"><dt className="text-stone-500">Subtotal</dt><dd className="font-semibold">{formatKES(subtotal)}</dd></div>
                <div className="flex justify-between"><dt className="text-stone-500">Delivery</dt><dd className="font-semibold">{shippingCost === 0 ? 'Free' : formatKES(shippingCost)}</dd></div>
                <div className="flex justify-between"><dt className="text-stone-500">VAT ({Math.round(config.commerce.taxRate * 100)}%)</dt><dd className="font-semibold">{formatKES(tax)}</dd></div>
                <div className="flex justify-between border-t border-stone-200 pt-3">
                  <dt className="text-base font-bold">Total</dt><dd className="text-xl font-extrabold">{formatKES(total)}</dd>
                </div>
              </dl>
              <button onClick={handlePlaceOrder}
                className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700">
                {user ? <>Pay {formatKES(total)} <FiArrowRight /></> : <>Continue to payment <FiArrowRight /></>}
              </button>
              <p className="mt-3 flex items-center justify-center gap-1.5 text-xs text-stone-400">
                <FiLock className="h-3 w-3" /> Secure — encrypted end to end
              </p>
            </div>
          </Reveal>
        </aside>
      </div>

      {showPaymentModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={closeModal} />
          <div className="relative z-10 w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl sm:p-8 max-h-[90vh] overflow-y-auto">
            <button onClick={closeModal} className="absolute right-4 top-4 rounded-full p-1 text-stone-400 transition-colors hover:bg-stone-100 hover:text-stone-600">
              <FiX className="h-5 w-5" />
            </button>

            <h2 className="text-xl font-bold">Choose payment method</h2>
            <p className="mt-1 text-sm text-stone-500">Total to pay: <span className="font-semibold text-stone-800">{formatKES(payTotal || total)}</span></p>

            {paymentMethod === 'stripe' && (payTotal || total) < config.stripe.minAmount && (
              <div className="mt-4 flex items-start gap-3 rounded-xl bg-amber-50 p-4 ring-1 ring-amber-200">
                <FiAlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
                <div className="text-sm text-amber-900">
                  <p className="font-semibold">Card payments need a minimum of {formatKES(config.stripe.minAmount)}</p>
                  <p className="mt-0.5 text-amber-800">
                    Your order is {formatKES(payTotal || total)}. Please add more items to your cart, or use <strong>M-Pesa</strong> for this amount.
                  </p>
                </div>
              </div>
            )}

            <div className="mt-5 grid gap-3">
              {PAY_METHODS.map(m => (
                <label key={m.id}>
                  <input type="radio" name="payment" value={m.id} checked={paymentMethod === m.id}
                    onChange={() => {
                      setPaymentMethod(m.id);
                      setOrderForPayment(null);
                      // Reset any failure/pending state so the Confirm button returns
                      if (pollRef.current) clearInterval(pollRef.current);
                      setStkStatus('idle');
                      setStkMessage('');
                    }}
                    className="peer sr-only" />
                  <div className={`flex cursor-pointer items-center gap-4 rounded-xl border-2 p-4 transition-all ${paymentMethod === m.id ? 'border-primary-600 bg-primary-50/50 shadow-sm' : 'border-stone-200 hover:border-stone-300'}`}>
                    <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${m.accent}`}>
                      <m.icon className="h-5 w-5" />
                    </span>
                    <span className="flex-1">
                      <span className="block font-semibold">{m.label}</span>
                      <span className="block text-sm text-stone-500">{m.desc}</span>
                    </span>
                    <span className={`flex h-5 w-5 items-center justify-center rounded-full border-2 ${paymentMethod === m.id ? 'border-primary-600 bg-primary-600' : 'border-stone-300'}`}>
                      {paymentMethod === m.id && <FiCheck className="h-3 w-3 text-white" />}
                    </span>
                  </div>
                </label>
              ))}
            </div>

            {paymentMethod === 'mpesa' && (
              <div className="mt-5 rounded-xl bg-emerald-50 p-4 ring-1 ring-emerald-200">
                <label className="mb-1 block text-sm font-medium text-emerald-900">M-Pesa phone number</label>
                <PhoneInput
                  value={phoneNumber}
                  onChange={setPhoneNumber}
                  placeholder="0712345678 or 254712345678"
                  className="border-emerald-300"
                />
                <p className="mt-2 text-xs text-emerald-700">Accepts 07xx, 01xx, or 254xx formats. You will receive a PIN prompt to approve the payment.</p>
              </div>
            )}

            {paymentMethod === 'stripe' && stripePromise && orderForPayment && (
              <div className="mt-5">
                <Elements stripe={stripePromise} options={{ appearance: { theme: 'stripe' } }}>
                  <StripePaymentForm
                    orderId={orderForPayment.id}
                    total={payTotal || capturedTotalRef.current || total}
                    onResult={onStripeResult}
                  />
                </Elements>
              </div>
            )}
            {paymentMethod === 'stripe' && (!orderForPayment || !stripePromise) && (
              <div className="mt-5 rounded-xl bg-stone-50 p-4 ring-1 ring-stone-200">
                <div className="mb-3"><CardBrands /></div>
                <p className="text-sm text-stone-600">
                  Secure card payments via Stripe. Click <strong>Confirm &amp; Pay</strong> below, then enter your card details to complete the purchase.
                </p>
              </div>
            )}

            {stkStatus !== 'idle' && paymentMethod !== 'stripe' && (
              <div className={`mt-5 rounded-xl p-4 ring-1 ${
                stkStatus === 'success' ? 'bg-emerald-50 ring-emerald-200' :
                stkStatus === 'cancelled' || stkStatus === 'failed' ? 'bg-rose-50 ring-rose-200' :
                'bg-amber-50 ring-amber-200'
              }`}>
                <div className="flex items-start gap-3">
                  {stkStatus === 'sending' || stkStatus === 'waiting_pin' || stkStatus === 'polling' ? (
                    <FiLoader className="mt-0.5 h-5 w-5 shrink-0 animate-spin text-amber-600" />
                  ) : stkStatus === 'success' ? (
                    <FiCheckCircle className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
                  ) : (
                    <FiAlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-rose-600" />
                  )}
                  <div className="flex-1">
                    <p className={`text-sm font-semibold ${stkStatus === 'success' ? 'text-emerald-900' : stkStatus === 'cancelled' || stkStatus === 'failed' ? 'text-rose-900' : 'text-amber-900'}`}>
                      {stkStatus === 'sending' && 'Initiating payment...'}
                      {stkStatus === 'waiting_pin' && 'Enter your M-Pesa PIN on your phone'}
                      {stkStatus === 'polling' && 'Waiting for confirmation...'}
                      {stkStatus === 'success' && 'Payment confirmed!'}
                      {stkStatus === 'failed' && 'Payment failed'}
                      {stkStatus === 'cancelled' && 'Payment cancelled'}
                    </p>
                    <p className={`mt-1 text-xs ${stkStatus === 'success' ? 'text-emerald-700' : stkStatus === 'cancelled' || stkStatus === 'failed' ? 'text-rose-700' : 'text-amber-700'}`}>
                      {stkMessage}
                    </p>
                    {(stkStatus === 'failed' || stkStatus === 'cancelled') && (
                      <button onClick={async () => {
                        const orderNum = orderNumberRef.current;
                        if (!orderNum) { resetPayment(); return; }
                        if (!phoneNumber) { toast.error('Enter your M-Pesa phone number'); return; }
                        setStkStatus('sending');
                        setStkMessage('Retrying payment...');
                        try {
                          const res = await orders.retryPayment(orderNum, phoneNumber);
                          const data = res.data as any;
                          const checkoutRequestId = data?.checkoutRequestId;
                          if (checkoutRequestId) {
                            setStkStatus('waiting_pin');
                            setStkMessage('Enter your M-Pesa PIN on your phone...');
                            startMpesaPolling(checkoutRequestId);
                          } else {
                            throw new Error(data?.customerMessage || 'Retry failed');
                          }
                        } catch (e: any) {
                          setStkStatus('failed');
                          setStkMessage(e?.message || 'Retry failed');
                          toast.error(e?.message || 'Retry failed');
                        }
                      }} className="mt-3 text-sm font-medium text-rose-700 underline hover:text-rose-900">Try again</button>
                    )}
                  </div>
                </div>
              </div>
            )}

            {stkStatus === 'idle' && !(paymentMethod === 'stripe' && orderForPayment) && (
              <button onClick={handleConfirmPayment}
                className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700 disabled:opacity-60">
                <FiLock className="h-4 w-4" /> Confirm &amp; Pay {formatKES(payTotal || total)}
              </button>
            )}

            <p className="mt-3 flex items-center justify-center gap-1.5 text-xs text-stone-400">
              <FiLock className="h-3 w-3" /> Secure — encrypted end to end
            </p>
          </div>
        </div>
      )}

      {showAuthModal && (
        <AuthModal onSuccess={handleAuthSuccess} onClose={() => { setShowAuthModal(false); setProceedAfterAuth(false); }} />
      )}
    </div>
  );
}

export default function CheckoutPage() {
  return (
    <Suspense fallback={<div className="max-w-lg mx-auto px-4 py-16 text-center">Loading...</div>}>
      <CheckoutContent />
    </Suspense>
  );
}
