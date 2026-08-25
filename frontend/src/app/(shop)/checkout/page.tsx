'use client';
import { useState, useEffect } from 'react';
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
  FiCheck, FiArrowLeft, FiArrowRight, FiShoppingBag,
  FiSmartphone, FiCreditCard, FiLock, FiZap,
} from 'react-icons/fi';
import toast from 'react-hot-toast';

const STEPS = ['Delivery', 'Payment'];

type PayMethod = 'mpesa' | 'airtel' | 'stripe' | 'flutterwave';

export default function CheckoutPage() {
  const router = useRouter();
  const { cart, refreshCart, user } = useAuth();
  const { currency } = useCurrency();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PayMethod>('mpesa');
  const [orderId, setOrderId] = useState<string | null>(null);
  const [orderNumber, setOrderNumber] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [card, setCard] = useState({ number: '', expiry: '', cvv: '', name: '' });
  const [cardErrors, setCardErrors] = useState<Record<string, string>>({});

  const [shipping, setShipping] = useState<Address>({
    name: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: config.commerce.defaultCountry, phone: '',
  });
  const [billing, setBilling] = useState<Address>({
    name: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: config.commerce.defaultCountry,
  });
  const [sameAsShipping, setSameAsShipping] = useState(true);
  const [notes, setNotes] = useState('');

  const subtotal = cart?.items.reduce((sum, item) => sum + item.priceSnapshot * item.quantity, 0) || 0;
  const shippingCost = subtotal >= config.commerce.freeShippingThreshold ? 0 : config.commerce.shippingCost;
  const tax = Math.round(subtotal * config.commerce.taxRate);
  const total = subtotal + shippingCost + tax;

  const [prefilled, setPrefilled] = useState(false);

  useEffect(() => {
    if (prefilled || !user) return;
    auth.getAddresses().then(res => {
      const list: Address[] = res.data || [];
      const preferred = list.find(a => a.isDefault) || list[0];
      if (preferred) {
        setShipping(prev => ({
          ...prev,
          name: prev.name || preferred.name || user.name || '',
          line1: prev.line1 || preferred.line1 || '',
          line2: prev.line2 || preferred.line2 || '',
          city: prev.city || preferred.city || '',
          state: prev.state || preferred.state || '',
          postalCode: prev.postalCode || preferred.postalCode || '',
          country: preferred.country || config.commerce.defaultCountry,
          phone: prev.phone || preferred.phone || '',
        }));
        setBilling(prev => ({
          ...prev,
          name: prev.name || preferred.name || '',
          line1: prev.line1 || preferred.line1 || '',
          line2: prev.line2 || preferred.line2 || '',
          city: prev.city || preferred.city || '',
          state: prev.state || preferred.state || '',
          postalCode: prev.postalCode || preferred.postalCode || '',
          country: preferred.country || config.commerce.defaultCountry,
        }));
      }
      setPrefilled(true);
    }).catch(() => setPrefilled(true));
  }, [user, prefilled]);

  const REQUIRED_FIELDS: { key: keyof Address; label: string }[] = [
    { key: 'name', label: 'Full name' },
    { key: 'line1', label: 'Address line 1' },
    { key: 'city', label: 'City / Town' },
    { key: 'state', label: 'County / State' },
    { key: 'postalCode', label: 'Postal code' },
    { key: 'phone', label: 'Phone number' },
  ];

  const [fieldErrors, setFieldErrors] = useState<Record<string, boolean>>({});

  const validateAddress = (): boolean => {
    const errors: Record<string, boolean> = {};
    for (const { key, label } of REQUIRED_FIELDS) {
      if (!String(shipping[key as keyof Address] || '').trim()) {
        errors[key as string] = true;
      }
    }
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      const first = Object.keys(errors)[0];
      toast.error(`Please fill in ${REQUIRED_FIELDS.find(f => f.key === first)?.label}`);
      return false;
    }
    const digits = (shipping.phone || '').replace(/\D/g, '');
    if (digits.length < 9) {
      setFieldErrors({ phone: true });
      toast.error('Enter a valid phone number');
      return false;
    }
    if (!sameAsShipping) {
      for (const { key, label } of REQUIRED_FIELDS.filter(f => f.key !== 'phone')) {
        if (!String(billing[key as keyof Address] || '').trim()) {
          toast.error(`Billing address is missing ${label}`);
          return false;
        }
      }
    }
    setFieldErrors({});
    return true;
  };


  const validateCard = (): boolean => {
    if (paymentMethod !== 'stripe' && paymentMethod !== 'flutterwave') return true;
    const errors: Record<string, string> = {};
    const digits = card.number.replace(/\D/g, '');
    if (digits.length < 13 || digits.length > 19) {
      errors.number = 'Enter a valid card number';
    } else {
      let sum = 0;
      let dbl = false;
      for (let i = digits.length - 1; i >= 0; i--) {
        let d = parseInt(digits[i], 10);
        if (dbl) { d *= 2; if (d > 9) d -= 9; }
        sum += d;
        dbl = !dbl;
      }
      if (sum % 10 !== 0) errors.number = 'This card number is invalid';
    }
    const m = card.expiry.trim().match(/^(0[1-9]|1[0-2])\s*\/\s*(\d{2})$/);
    if (!m) {
      errors.expiry = 'Use MM/YY';
    } else {
      const exp = new Date(2000 + parseInt(m[2], 10), parseInt(m[1], 10), 1);
      if (exp <= new Date()) errors.expiry = 'This card has expired';
    }
    if (!/^\d{3,4}$/.test(card.cvv.trim())) errors.cvv = '3 or 4 digits';
    if (!card.name.trim()) errors.name = 'Name on card is required';
    setCardErrors(errors);
    if (Object.keys(errors).length > 0) {
      toast.error('Check your card details');
      return false;
    }
    return true;
  };

  const handlePlaceOrder = async () => {
    if (!validateAddress()) return;
    setLoading(true);
    try {
      const res = await orders.checkout({
        shippingAddress: shipping,
        billingAddress: sameAsShipping ? shipping : billing,
        notes,
      });
      if (res.data) {
        setOrderId(res.data.id);
        setOrderNumber(res.data.number || '');
        setStep(2);
        await refreshCart();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    } catch (e: any) { toast.error(e.message || 'Checkout failed'); }
    setLoading(false);
  };

  const handlePayment = async () => {
    if (!orderId) return;
    if (!validateCard()) { setLoading(false); return; }
    setLoading(true);
    try {
      if (paymentMethod === 'mpesa') {
        if (!phoneNumber) { toast.error('Enter your M-Pesa phone number'); setLoading(false); return; }
        const push = await orders.mpesaStkPush(orderId, String(total), phoneNumber);
        toast.success('PIN prompt sent - approve it on your phone');
        if (push.data?.checkoutRequestId) {
          await new Promise(r => setTimeout(r, 2500));
          const result = await payments.mpesaSimulateConfirm(push.data.checkoutRequestId);
          if (result.data?.status === 'successful') {
            toast.success('M-Pesa payment received');
            router.push(`/checkout/success?order=${orderNumber}`);
          } else {
            throw new Error(result.data?.resultDesc || 'M-Pesa payment was not completed');
          }
        } else {
          setTimeout(() => router.push(`/checkout/success?order=${orderNumber}`), 3000);
        }
      } else if (paymentMethod === 'airtel') {
        if (!phoneNumber) { toast.error('Enter your Airtel Money phone number'); setLoading(false); return; }
        const res = await payments.airtelInitiate(orderId, total, phoneNumber);
        if (res.data?.referenceId) {
          toast.success('Approve the PIN prompt on your Airtel line');
          await payments.airtelConfirm(res.data.referenceId);
          toast.success('Airtel Money payment complete');
          router.push(`/checkout/success?order=${orderNumber}`);
        }
      } else if (paymentMethod === 'stripe') {
        const res = await payments.stripeCreateIntent(orderId, total);
        if (res.data?.clientSecret) {
          await payments.stripeConfirm(res.data.paymentIntentId);
          toast.success('Card payment processed');
          router.push(`/checkout/success?order=${orderNumber}`);
        }
      } else if (paymentMethod === 'flutterwave') {
        const res = await payments.flutterwaveCreate(orderId, total, user?.email);
        if (res.data?.transactionRef) {
          toast.success('Confirming with Flutterwave...');
          await payments.flutterwaveVerify(res.data.transactionRef);
          toast.success('Flutterwave payment complete');
          router.push(`/checkout/success?order=${orderNumber}`);
        }
      }
    } catch (e: any) { toast.error(e.message || 'Payment failed'); }
    setLoading(false);
  };

  if (!cart || cart.items.length === 0) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-24 text-center">
        <span className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-stone-100">
          <FiShoppingBag className="h-9 w-9 text-stone-400" />
        </span>
        <h1 className="mt-6 text-2xl font-bold">Nothing to check out yet</h1>
        <p className="mx-auto mt-2 max-w-[42ch] text-stone-500">Add items to your cart first, then come back to complete your order.</p>
        <Link href="/products" className="mt-8 inline-flex items-center gap-2 rounded-xl bg-primary-600 px-7 py-3.5 font-semibold text-white hover:bg-primary-700">
          Browse products <FiArrowRight />
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <ol className="mb-10 flex items-center gap-3" aria-label="Checkout progress">
        {STEPS.map((label, idx) => {
          const n = idx + 1;
          const active = step >= n;
          return (
            <li key={label} className="flex flex-1 items-center gap-3 last:flex-none">
              <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-sm font-bold transition-colors ${
                active ? 'bg-primary-600 text-white shadow-md shadow-primary-600/30' : 'bg-stone-200 text-stone-500'
              }`}>
                {step > n ? <FiCheck /> : n}
              </span>
              <span className={`text-sm font-semibold ${active ? 'text-stone-900' : 'text-stone-400'}`}>{label}</span>
              {n < STEPS.length && <span className={`hidden h-px flex-1 sm:block ${step > n ? 'bg-primary-500' : 'bg-stone-200'}`} />}
            </li>
          );
        })}
      </ol>

      <div key={step} className="page-enter">
      {step === 1 && (
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-[1.5fr_1fr]">
          <div className="space-y-5">
            <Reveal>
              <section className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-7">
                <h2 className="text-lg font-bold">Delivery address</h2>
                <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {([
                    { label: 'Full name', key: 'name', required: true, span: true },
                    { label: 'Address line 1', key: 'line1', required: true, span: true },
                    { label: 'Address line 2', key: 'line2', required: false, span: true },
                    { label: 'City / Town', key: 'city', required: true, span: false },
                    { label: 'County / State', key: 'state', required: true, span: false },
                    { label: 'Postal code', key: 'postalCode', required: true, span: false },
                    { label: 'Phone', key: 'phone', required: true, span: false },
                  ]).map(field => (
                    <div key={field.key} className={field.span ? 'sm:col-span-2' : ''}>
                      <label className="mb-1 block text-sm font-medium text-stone-700">
                        {field.label}{field.required && <span className="text-rose-500"> *</span>}
                      </label>
                      <input
                        type="text"
                        value={(shipping as any)[field.key] || ''}
                        onChange={e => {
                          setShipping({ ...shipping, [field.key]: e.target.value });
                          if (fieldErrors[field.key]) setFieldErrors(prev => ({ ...prev, [field.key]: false }));
                        }}
                        required={field.required}
                        placeholder={field.key === 'phone' ? '+254 7XX XXX XXX' : ''}
                        className={`w-full rounded-xl border px-3.5 py-2.5 text-sm transition-colors focus:outline-none focus:ring-2 ${
                          fieldErrors[field.key]
                            ? 'border-rose-400 bg-rose-50/40 focus:border-rose-500 focus:ring-rose-100'
                            : 'border-stone-300 focus:border-primary-500 focus:ring-primary-100'
                        }`}
                      />
                      {fieldErrors[field.key] && (
                        <p className="mt-1 text-xs font-medium text-rose-600">This field is required</p>
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
                        <input
                          type="text" value={(billing as any)[key] || ''}
                          onChange={e => setBilling({ ...billing, [key]: e.target.value })}
                          className="w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100"
                        />
                      </div>
                    ))}
                  </div>
                )}
              </section>
            </Reveal>

            <Reveal delay={90}>
              <section className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-7">
                <h2 className="text-lg font-bold">Order notes</h2>
                <textarea
                  value={notes} onChange={e => setNotes(e.target.value)} rows={3}
                  placeholder="Gate code, preferred delivery time, gift note..."
                  className="mt-4 w-full resize-none rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100"
                />
              </section>
            </Reveal>
          </div>

          <aside className="lg:sticky lg:top-28 lg:self-start">
            <Reveal delay={140}>
              <SummaryCard cart={cart.items} subtotal={subtotal} shippingCost={shippingCost} tax={tax} total={total}>
                <button onClick={handlePlaceOrder} disabled={loading}
                  className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700 disabled:opacity-60 disabled:hover:translate-y-0">
                  {loading ? 'Placing order...' : 'Continue to payment'} {!loading && <FiArrowRight />}
                </button>
              </SummaryCard>
            </Reveal>
          </aside>
        </div>
      )}

      {step === 2 && (
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-[1.5fr_1fr]">
          <Reveal>
            <section className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-8">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-bold">How would you like to pay?</h2>
                  <p className="mt-1 text-sm text-stone-500">Order <span className="font-mono font-semibold">{orderNumber}</span> is reserved.</p>
                </div>
                <button onClick={() => setStep(1)} className="flex items-center gap-1 text-sm font-medium text-stone-500 hover:text-primary-600">
                  <FiArrowLeft /> Edit details
                </button>
              </div>

              <p className="mt-3 flex items-center justify-center gap-2 rounded-full bg-amber-50 px-4 py-1.5 text-xs font-medium text-amber-800 ring-1 ring-amber-200">
                <FiLock className="h-3 w-3" /> Sandbox mode - payments are simulated, no real money moves
              </p>

              {currency.code !== 'KES' && (
                <p className="mt-2 text-center text-xs text-stone-500">
                  Prices shown in {currency.code} are estimates - your payment is processed in Kenyan shillings (KES).
                </p>
              )}

              <div className="mt-6 grid gap-3">
                {[
                  { id: 'mpesa', label: 'M-Pesa', desc: 'STK push to your Safaricom line', icon: FiSmartphone, accent: 'text-emerald-600 bg-emerald-100' },
                  { id: 'airtel', label: 'Airtel Money', desc: 'PIN prompt on your Airtel line', icon: FiZap, accent: 'text-rose-600 bg-rose-100' },
                  { id: 'stripe', label: 'Card - Stripe', desc: 'Visa, Mastercard, Amex', icon: FiCreditCard, accent: 'text-indigo-600 bg-indigo-100' },
                  { id: 'flutterwave', label: 'Card - Flutterwave', desc: 'Cards and mobile wallets across Africa', icon: FiLock, accent: 'text-orange-600 bg-orange-100' },
                ].map(m => (
                  <label key={m.id}>
                    <input type="radio" name="payment" value={m.id} checked={paymentMethod === m.id}
                      onChange={() => setPaymentMethod(m.id as PayMethod)} className="peer sr-only" />
                    <div className="flex cursor-pointer items-center gap-4 rounded-xl border-2 border-stone-200 p-4 transition-all peer-checked:border-primary-600 peer-checked:bg-primary-50/50 peer-checked:shadow-sm hover:border-stone-300">
                      <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${m.accent}`}>
                        <m.icon className="h-5 w-5" />
                      </span>
                      <span>
                        <span className="block font-semibold">{m.label}</span>
                        <span className="block text-sm text-stone-500">{m.desc}</span>
                      </span>
                      <span className={`ml-auto flex h-5 w-5 items-center justify-center rounded-full border-2 ${paymentMethod === m.id ? 'border-primary-600 bg-primary-600' : 'border-stone-300'}`}>
                        {paymentMethod === m.id && <FiCheck className="h-3 w-3 text-white" />}
                      </span>
                    </div>
                  </label>
                ))}
              </div>


              {(paymentMethod === 'stripe' || paymentMethod === 'flutterwave') && (
                <div className="mt-5 rounded-xl bg-stone-50 p-4 ring-1 ring-stone-200">
                  <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-stone-400">
                    Card details - validated in your browser, never stored
                  </p>
                  <div className="grid gap-3 sm:grid-cols-2">
                    <div className="sm:col-span-2">
                      <input
                        inputMode="numeric" placeholder="4242 4242 4242 4242"
                        value={card.number}
                        onChange={e => {
                          const digits = e.target.value.replace(/\D/g, '').slice(0, 19);
                          setCard({ ...card, number: digits.replace(/(.{4})/g, '$1 ').trim() });
                          if (cardErrors.number) setCardErrors(prev => ({ ...prev, number: '' }));
                        }}
                        className={`${cardInputCls} ${cardErrors.number ? 'border-rose-400' : ''}`}
                      />
                      {cardErrors.number && <p className="mt-1 text-xs font-medium text-rose-600">{cardErrors.number}</p>}
                    </div>
                    <div>
                      <input
                        inputMode="numeric" placeholder="MM / YY"
                        value={card.expiry}
                        onChange={e => {
                          let v = e.target.value.replace(/\D/g, '').slice(0, 4);
                          if (v.length > 2) v = v.slice(0, 2) + ' / ' + v.slice(2);
                          setCard({ ...card, expiry: v });
                          if (cardErrors.expiry) setCardErrors(prev => ({ ...prev, expiry: '' }));
                        }}
                        className={`${cardInputCls} ${cardErrors.expiry ? 'border-rose-400' : ''}`}
                      />
                      {cardErrors.expiry && <p className="mt-1 text-xs font-medium text-rose-600">{cardErrors.expiry}</p>}
                    </div>
                    <div>
                      <input
                        inputMode="numeric" placeholder="CVV"
                        value={card.cvv}
                        onChange={e => setCard({ ...card, cvv: e.target.value.replace(/\D/g, '').slice(0, 4) })}
                        className={`${cardInputCls} ${cardErrors.cvv ? 'border-rose-400' : ''}`}
                      />
                      {cardErrors.cvv && <p className="mt-1 text-xs font-medium text-rose-600">{cardErrors.cvv}</p>}
                    </div>
                    <div className="sm:col-span-2">
                      <input
                        placeholder="Name on card"
                        value={card.name}
                        onChange={e => setCard({ ...card, name: e.target.value })}
                        className={`${cardInputCls} ${cardErrors.name ? 'border-rose-400' : ''}`}
                      />
                      {cardErrors.name && <p className="mt-1 text-xs font-medium text-rose-600">{cardErrors.name}</p>}
                    </div>
                  </div>
                  <p className="mt-3 text-xs text-stone-400">
                    Sandbox tip: 4242 4242 4242 4242 with any future expiry succeeds.
                  </p>
                </div>
              )}

              {(paymentMethod === 'mpesa' || paymentMethod === 'airtel') && (
                <div className={`mt-5 rounded-xl p-4 ring-1 ${paymentMethod === 'mpesa' ? 'bg-emerald-50 ring-emerald-200' : 'bg-rose-50 ring-rose-200'}`}>
                  <label className={`mb-1 block text-sm font-medium ${paymentMethod === 'mpesa' ? 'text-emerald-900' : 'text-rose-900'}`}>
                    {paymentMethod === 'mpesa' ? 'M-Pesa phone number' : 'Airtel Money phone number'}
                  </label>
                  <input
                    type="tel" value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)}
                    placeholder={paymentMethod === 'mpesa' ? '254712345678' : '254701234567'}
                    className={`w-full rounded-xl border px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 ${
                      paymentMethod === 'mpesa'
                        ? 'border-emerald-300 focus:border-emerald-500 focus:ring-emerald-100'
                        : 'border-rose-300 focus:border-rose-500 focus:ring-rose-100'
                    }`}
                  />
                  <p className={`mt-2 text-xs ${paymentMethod === 'mpesa' ? 'text-emerald-700' : 'text-rose-700'}`}>
                    You will receive a prompt on this number to approve the payment.
                  </p>
                </div>
              )}

              <button onClick={handlePayment} disabled={loading}
                className="mt-6 w-full rounded-xl bg-primary-600 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700 disabled:opacity-60 disabled:hover:translate-y-0">
                {loading ? 'Processing...' : `Pay ${formatKES(total)}`}
              </button>
              <p className="mt-3 flex items-center justify-center gap-1.5 text-xs text-stone-400">
                <FiLock className="h-3 w-3" /> Card details are handled by the gateway - never stored on our servers.
              </p>
            </section>
          </Reveal>

          <aside className="lg:sticky lg:top-28 lg:self-start">
            <Reveal delay={100}>
              <SummaryCard cart={cart.items} subtotal={subtotal} shippingCost={shippingCost} tax={tax} total={total} />
            </Reveal>
          </aside>
        </div>
      )}
      </div>
    </div>
  );
}

const cardInputCls = 'w-full rounded-xl border border-stone-300 bg-white px-3.5 py-2.5 text-sm transition-colors focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100';

function SummaryCard({ cart, subtotal, shippingCost, tax, total, children }: {
  cart: any[]; subtotal: number; shippingCost: number; tax: number; total: number; children?: React.ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-stone-200/80 bg-white p-6">
      <h2 className="text-lg font-bold">Your order</h2>
      <ul className="mt-4 space-y-3">
        {cart.map(item => (
          <li key={item.id} className="flex items-center gap-3">
            <span className="relative shrink-0 overflow-hidden rounded-lg bg-stone-100">
              {item.productImage && <img src={item.productImage} alt="" className="h-12 w-12 object-cover" />}
              <span className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-stone-900 text-[10px] font-bold text-white">
                {item.quantity}
              </span>
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
      {children}
    </div>
  );
}
