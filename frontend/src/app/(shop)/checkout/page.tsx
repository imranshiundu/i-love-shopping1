'use client';
import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { orders, payments, auth } from '@/services/api';
import { config } from '@/lib/config';
import { useCurrency } from '@/lib/currency';
import { formatKES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import { Address } from '@/types';
import {
  FiCheck, FiArrowRight, FiShoppingBag,
  FiSmartphone, FiCreditCard, FiLock, FiLoader, FiAlertCircle, FiCheckCircle,
} from 'react-icons/fi';
import toast from 'react-hot-toast';

type PayMethod = 'mpesa' | 'stripe' | 'flutterwave';
type StkStatus = 'idle' | 'sending' | 'waiting_pin' | 'polling' | 'success' | 'failed' | 'cancelled';

const STK_POLL_INTERVAL_MS = 3000;
const STK_POLL_MAX_ATTEMPTS = 40;

export default function CheckoutPage() {
  const router = useRouter();
  const { cart, cartLoading, refreshCart, user } = useAuth();
  const { currency } = useCurrency();
  const [loading, setLoading] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PayMethod>('mpesa');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [card, setCard] = useState({ number: '', expiry: '', cvv: '', name: '' });
  const [cardErrors, setCardErrors] = useState<Record<string, string>>({});
  const [stkStatus, setStkStatus] = useState<StkStatus>('idle');
  const [stkMessage, setStkMessage] = useState('');
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const pollCountRef = useRef(0);
  const checkoutRequestIdRef = useRef<string | null>(null);
  const orderNumberRef = useRef<string | null>(null);

  const [savedAddresses, setSavedAddresses] = useState<Address[]>([]);
  const [selectedShippingId, setSelectedShippingId] = useState<string>('');

  const [shipping, setShipping] = useState<Address>({
    name: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: config.commerce.defaultCountry, phone: '', type: 'SHIPPING',
  });
  const [billing, setBilling] = useState<Address>({
    name: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: config.commerce.defaultCountry, type: 'BILLING',
  });
  const [sameAsShipping, setSameAsShipping] = useState(true);
  const [notes, setNotes] = useState('');

  const subtotal = cart?.items.reduce((sum, item) => sum + item.priceSnapshot * item.quantity, 0) || 0;
  const shippingCost = subtotal >= config.commerce.freeShippingThreshold ? 0 : config.commerce.shippingCost;
  const tax = Math.round(subtotal * config.commerce.taxRate);
  const total = subtotal + shippingCost + tax;

  useEffect(() => {
    if (!user) return;
    auth.getAddresses().then(res => {
      const list: Address[] = res.data || [];
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
    if (target === 'shipping') setShipping(mapped); else setBilling(mapped);
  };

  const REQUIRED_FIELDS: { key: keyof Address; label: string }[] = [
    { key: 'name', label: 'Full name' }, { key: 'line1', label: 'Address line 1' },
    { key: 'city', label: 'City / Town' }, { key: 'state', label: 'County / State' },
    { key: 'postalCode', label: 'Postal code' }, { key: 'phone', label: 'Phone number' },
  ];

  const [fieldErrors, setFieldErrors] = useState<Record<string, boolean>>({});

  const validateAddress = (): boolean => {
    const errors: Record<string, boolean> = {};
    for (const { key } of REQUIRED_FIELDS) {
      if (!String(shipping[key as keyof Address] || '').trim()) errors[key as string] = true;
    }
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      toast.error(`Please fill in ${REQUIRED_FIELDS.find(f => f.key === Object.keys(errors)[0])?.label}`);
      return false;
    }
    if ((shipping.phone || '').replace(/\D/g, '').length < 9) { toast.error('Enter a valid phone number'); return false; }
    if (!sameAsShipping) {
      for (const { key, label } of REQUIRED_FIELDS.filter(f => f.key !== 'phone')) {
        if (!String(billing[key as keyof Address] || '').trim()) { toast.error(`Billing address is missing ${label}`); return false; }
      }
    }
    setFieldErrors({});
    return true;
  };

  const validatePayment = (): boolean => {
    if (paymentMethod === 'mpesa') {
      if (!phoneNumber) { toast.error('Enter your M-Pesa phone number'); return false; }
      if (phoneNumber.replace(/\D/g, '').length < 9) { toast.error('Enter a valid phone number'); return false; }
      return true;
    }
    if (paymentMethod === 'stripe' || paymentMethod === 'flutterwave') {
      const errs: Record<string, string> = {};
      const digits = card.number.replace(/\D/g, '');
      if (digits.length < 13 || digits.length > 19) errs.number = 'Enter a valid card number';
      else {
        let sum = 0, dbl = false;
        for (let i = digits.length - 1; i >= 0; i--) {
          let d = parseInt(digits[i], 10);
          if (dbl) { d *= 2; if (d > 9) d -= 9; }
          sum += d; dbl = !dbl;
        }
        if (sum % 10 !== 0) errs.number = 'This card number is invalid';
      }
      const m = card.expiry.trim().match(/^(0[1-9]|1[0-2])\s*\/\s*(\d{2})$/);
      if (!m) errs.expiry = 'Use MM/YY';
      else { const exp = new Date(2000 + parseInt(m[2], 10), parseInt(m[1], 10), 1); if (exp <= new Date()) errs.expiry = 'Expired'; }
      if (!/^\d{3,4}$/.test(card.cvv.trim())) errs.cvv = '3 or 4 digits';
      if (!card.name.trim()) errs.name = 'Name on card is required';
      setCardErrors(errs);
      if (Object.keys(errs).length > 0) { toast.error('Check your card details'); return false; }
      return true;
    }
    return true;
  };

  const startMpesaPolling = useCallback((checkoutRequestId: string) => {
    pollCountRef.current = 0;
    checkoutRequestIdRef.current = checkoutRequestId;
    setStkStatus('polling');
    setStkMessage('Waiting for M-Pesa confirmation...');
    if (pollRef.current) clearInterval(pollRef.current);

    pollRef.current = setInterval(async () => {
      pollCountRef.current += 1;
      if (pollCountRef.current > STK_POLL_MAX_ATTEMPTS) {
        if (pollRef.current) clearInterval(pollRef.current);
        setStkStatus('failed'); setStkMessage('Payment timed out.'); toast.error('Payment timed out'); return;
      }
      try {
        const result = await payments.mpesaStkQuery(checkoutRequestId);
        const desc = (result.data?.responseDescription || result.data?.customerMessage || '').toLowerCase();
        if (desc.includes('successfully') || desc.includes('completed')) {
          if (pollRef.current) clearInterval(pollRef.current);
          setStkStatus('success'); setStkMessage('Payment confirmed!');
          toast.success('M-Pesa payment confirmed');
          router.push(`/checkout/success?order=${orderNumberRef.current}`);
          return;
        }
        if (desc.includes('cancelled')) {
          if (pollRef.current) clearInterval(pollRef.current);
          setStkStatus('cancelled'); setStkMessage('Payment was cancelled.'); toast.error('Payment cancelled'); return;
        }
        if (desc.includes('timeout') || desc.includes('expired')) {
          if (pollRef.current) clearInterval(pollRef.current);
          setStkStatus('failed'); setStkMessage('Payment expired.'); toast.error('Payment expired'); return;
        }
        setStkMessage(`Checking status... (${pollCountRef.current}/${STK_POLL_MAX_ATTEMPTS})`);
      } catch { /* keep polling */ }
    }, STK_POLL_INTERVAL_MS);
  }, [router]);

  const handlePlaceOrder = async () => {
    if (!validateAddress()) return;
    if (!validatePayment()) return;
    setLoading(true);
    setStkStatus('sending');
    setStkMessage('Creating your order...');

    try {
      const res = await orders.checkout({
        shippingAddress: { ...shipping, type: 'SHIPPING' },
        billingAddress: sameAsShipping ? { ...shipping, type: 'BILLING' } : { ...billing, type: 'BILLING' },
        notes,
      });
      if (!res.data) { toast.error('Checkout failed'); setLoading(false); setStkStatus('idle'); return; }
      const orderId = res.data.id;
      orderNumberRef.current = res.data.number || '';
      await refreshCart();

      if (paymentMethod === 'mpesa') {
        setStkStatus('waiting_pin'); setStkMessage('Enter your M-Pesa PIN on your phone...');
        const pushRes = await orders.mpesaStkPush(orderId, String(total), phoneNumber);
        const checkoutRequestId = pushRes.data?.checkoutRequestId;
        if (!checkoutRequestId) { toast.error('Failed to send M-Pesa prompt'); setLoading(false); setStkStatus('failed'); return; }
        toast.success('PIN prompt sent — check your phone');
        startMpesaPolling(checkoutRequestId);

      } else if (paymentMethod === 'stripe') {
        setStkMessage('Processing card payment...');
        const intentRes = await payments.stripeCreateIntent(orderId, total);
        if (!intentRes.data?.clientSecret) { throw new Error('Failed to initialize card payment'); }
        await payments.stripeConfirm(intentRes.data.paymentIntentId);
        toast.success('Card payment processed');
        router.push(`/checkout/success?order=${orderNumberRef.current}`);

      } else if (paymentMethod === 'flutterwave') {
        setStkMessage('Redirecting to card payment...');
        const fwRes = await payments.flutterwaveInit(orderId, total, 'KES', user?.email, user?.name);
        if (fwRes.data?.checkoutUrl) {
          window.location.href = fwRes.data.checkoutUrl;
        } else {
          throw new Error('Failed to initialize card payment');
        }
      }
    } catch (e: any) {
      const msg = e.message || 'Checkout failed';
      if (msg.includes('insufficient stock')) toast.error('Some items are no longer available.');
      else if (msg.includes('401')) toast.error('Please sign in to complete your order');
      else toast.error(msg);
      setLoading(false); setStkStatus('failed'); setStkMessage(msg);
    }
  };

  const resetPayment = () => {
    if (pollRef.current) clearInterval(pollRef.current);
    setStkStatus('idle'); setStkMessage(''); setLoading(false);
    checkoutRequestIdRef.current = null; orderNumberRef.current = null; pollCountRef.current = 0;
  };

  if (cartLoading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-24 text-center">
        <FiLoader className="mx-auto h-8 w-8 animate-spin text-primary-600" />
        <p className="mt-4 text-stone-500">Loading your cart...</p>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
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

  const isProcessing = stkStatus !== 'idle' && stkStatus !== 'success' && stkStatus !== 'failed' && stkStatus !== 'cancelled';

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Checkout</h1>
        <p className="mt-1 text-sm text-stone-500">Complete your delivery details and choose how to pay.</p>
      </div>

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
                    <input type="text" value={(shipping as any)[field.key] || ''} disabled={isProcessing}
                      onChange={e => { setShipping({ ...shipping, [field.key]: e.target.value }); setSelectedShippingId(''); if (fieldErrors[field.key]) setFieldErrors(prev => ({ ...prev, [field.key]: false })); }}
                      placeholder={field.key === 'phone' ? '254712345678' : ''}
                      className={`w-full rounded-xl border px-3.5 py-2.5 text-sm transition-colors focus:outline-none focus:ring-2 ${fieldErrors[field.key] ? 'border-rose-400 bg-rose-50/40 focus:border-rose-500 focus:ring-rose-100' : 'border-stone-300 focus:border-primary-500 focus:ring-primary-100'}`} />
                  </div>
                ))}
              </div>
              <label className="mt-5 flex cursor-pointer items-center gap-2.5 text-sm font-medium">
                <input type="checkbox" checked={sameAsShipping} onChange={e => setSameAsShipping(e.target.checked)} disabled={isProcessing} className="h-4 w-4 accent-primary-600" />
                Billing address is the same as delivery
              </label>
              {!sameAsShipping && (
                <div className="mt-4 grid grid-cols-1 gap-4 rounded-xl bg-stone-50 p-4 sm:grid-cols-2">
                  {(['name', 'line1', 'line2', 'city', 'state', 'postalCode'] as const).map(key => (
                    <div key={key} className={key === 'line1' || key === 'name' ? 'sm:col-span-2' : ''}>
                      <label className="mb-1 block text-sm font-medium capitalize text-stone-700">{key.replace(/([A-Z])/g, ' $1')}</label>
                      <input type="text" value={(billing as any)[key] || ''} disabled={isProcessing}
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
              <h2 className="text-lg font-bold">How would you like to pay?</h2>
              <p className="mt-1 text-sm text-stone-500">Choose your preferred payment method.</p>

              {currency.code !== 'KES' && (
                <p className="mt-2 text-xs text-stone-500">Prices in {currency.code} are estimates — payment is processed in KES.</p>
              )}

              <div className="mt-5 grid gap-3">
                {[
                  { id: 'mpesa', label: 'M-Pesa', desc: 'STK push to your Safaricom line', icon: FiSmartphone, accent: 'text-emerald-600 bg-emerald-100' },
                  { id: 'stripe', label: 'Card — Stripe', desc: 'Visa, Mastercard, Amex (test: 4242 4242 4242 4242)', icon: FiCreditCard, accent: 'text-indigo-600 bg-indigo-100' },
                  { id: 'flutterwave', label: 'Card — Flutterwave', desc: 'Cards and mobile wallets across Africa', icon: FiLock, accent: 'text-orange-600 bg-orange-100' },
                ].map(m => (
                  <label key={m.id}>
                    <input type="radio" name="payment" value={m.id} checked={paymentMethod === m.id}
                      onChange={() => setPaymentMethod(m.id as PayMethod)} className="peer sr-only" disabled={isProcessing} />
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

              {(paymentMethod === 'stripe' || paymentMethod === 'flutterwave') && (
                <div className="mt-5 rounded-xl bg-stone-50 p-4 ring-1 ring-stone-200">
                  <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-stone-400">Card details — validated in your browser, never stored</p>
                  <div className="grid gap-3 sm:grid-cols-2">
                    <div className="sm:col-span-2">
                      <input inputMode="numeric" placeholder="4242 4242 4242 4242" disabled={isProcessing}
                        value={card.number}
                        onChange={e => { const d = e.target.value.replace(/\D/g, '').slice(0, 19); setCard({ ...card, number: d.replace(/(.{4})/g, '$1 ').trim() }); if (cardErrors.number) setCardErrors(prev => ({ ...prev, number: '' })); }}
                        className={`${cardInputCls} ${cardErrors.number ? 'border-rose-400' : ''}`} />
                      {cardErrors.number && <p className="mt-1 text-xs font-medium text-rose-600">{cardErrors.number}</p>}
                    </div>
                    <div>
                      <input inputMode="numeric" placeholder="MM / YY" disabled={isProcessing}
                        value={card.expiry}
                        onChange={e => { let v = e.target.value.replace(/\D/g, '').slice(0, 4); if (v.length > 2) v = v.slice(0, 2) + ' / ' + v.slice(2); setCard({ ...card, expiry: v }); if (cardErrors.expiry) setCardErrors(prev => ({ ...prev, expiry: '' })); }}
                        className={`${cardInputCls} ${cardErrors.expiry ? 'border-rose-400' : ''}`} />
                      {cardErrors.expiry && <p className="mt-1 text-xs font-medium text-rose-600">{cardErrors.expiry}</p>}
                    </div>
                    <div>
                      <input inputMode="numeric" placeholder="CVV" disabled={isProcessing}
                        value={card.cvv}
                        onChange={e => setCard({ ...card, cvv: e.target.value.replace(/\D/g, '').slice(0, 4) })}
                        className={`${cardInputCls} ${cardErrors.cvv ? 'border-rose-400' : ''}`} />
                      {cardErrors.cvv && <p className="mt-1 text-xs font-medium text-rose-600">{cardErrors.cvv}</p>}
                    </div>
                    <div className="sm:col-span-2">
                      <input placeholder="Name on card" disabled={isProcessing}
                        value={card.name}
                        onChange={e => setCard({ ...card, name: e.target.value })}
                        className={`${cardInputCls} ${cardErrors.name ? 'border-rose-400' : ''}`} />
                      {cardErrors.name && <p className="mt-1 text-xs font-medium text-rose-600">{cardErrors.name}</p>}
                    </div>
                  </div>
                  <p className="mt-3 text-xs text-stone-400">
                    {paymentMethod === 'stripe'
                      ? 'Stripe test card: 4242 4242 4242 4242, any future expiry, any CVV.'
                      : 'Flutterwave test cards: 4187427415564246 (Visa) or 4000000000000002 (Mastercard).'}
                  </p>
                </div>
              )}

              {paymentMethod === 'mpesa' && (
                <div className="mt-5 rounded-xl bg-emerald-50 p-4 ring-1 ring-emerald-200">
                  <label className="mb-1 block text-sm font-medium text-emerald-900">M-Pesa phone number</label>
                  <input type="tel" value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)}
                    placeholder="254712345678" disabled={isProcessing}
                    className="w-full rounded-xl border border-emerald-300 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100" />
                  <p className="mt-2 text-xs text-emerald-700">You will receive a PIN prompt to approve the payment.</p>
                </div>
              )}

              {stkStatus !== 'idle' && (
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
                        <button onClick={resetPayment} className="mt-3 text-sm font-medium text-rose-700 underline hover:text-rose-900">Try again</button>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </section>
          </Reveal>

          <Reveal delay={120}>
            <section className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-7">
              <h2 className="text-lg font-bold">Order notes</h2>
              <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={3} disabled={isProcessing}
                placeholder="Gate code, preferred delivery time, gift note..."
                className="mt-4 w-full resize-none rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100" />
            </section>
          </Reveal>
        </div>

        <aside className="lg:sticky lg:top-28 lg:self-start">
          <Reveal delay={140}>
            <div className="rounded-2xl border border-stone-200/80 bg-white p-6">
              <h2 className="text-lg font-bold">Your order</h2>
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
              <dl className="mt-5 space-y-2.5 border-t border-stone-200 pt-4 text-sm">
                <div className="flex justify-between"><dt className="text-stone-500">Subtotal</dt><dd className="font-semibold">{formatKES(subtotal)}</dd></div>
                <div className="flex justify-between"><dt className="text-stone-500">Delivery</dt><dd className="font-semibold">{shippingCost === 0 ? 'Free' : formatKES(shippingCost)}</dd></div>
                <div className="flex justify-between"><dt className="text-stone-500">VAT ({Math.round(config.commerce.taxRate * 100)}%)</dt><dd className="font-semibold">{formatKES(tax)}</dd></div>
                <div className="flex justify-between border-t border-stone-200 pt-3">
                  <dt className="text-base font-bold">Total</dt><dd className="text-xl font-extrabold">{formatKES(total)}</dd>
                </div>
              </dl>
              <button onClick={handlePlaceOrder} disabled={loading || isProcessing}
                className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700 disabled:opacity-60 disabled:hover:translate-y-0">
                {loading ? <><FiLoader className="h-4 w-4 animate-spin" /> Processing...</> :
                  <>Pay {formatKES(total)} <FiArrowRight /></>}
              </button>
              <p className="mt-3 flex items-center justify-center gap-1.5 text-xs text-stone-400">
                <FiLock className="h-3 w-3" /> Secure — encrypted end to end
              </p>
            </div>
          </Reveal>
        </aside>
      </div>
    </div>
  );
}

const cardInputCls = 'w-full rounded-xl border border-stone-300 bg-white px-3.5 py-2.5 text-sm transition-colors focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100';
