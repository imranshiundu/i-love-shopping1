'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { orders, payments } from '@/services/api';
import { formatKES } from '@/lib/utils';
import { config } from '@/lib/config';
import { Address } from '@/types';
import toast from 'react-hot-toast';

export default function CheckoutPage() {
  const router = useRouter();
  const { cart, refreshCart } = useAuth();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<'mpesa' | 'stripe' | 'paypal'>('mpesa');
  const [orderId, setOrderId] = useState<string | null>(null);
  const [phoneNumber, setPhoneNumber] = useState('');

  const [shipping, setShipping] = useState<Address>({
    name: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: config.commerce.defaultCountry, phone: '',
  });
  const [billing, setBilling] = useState<Address>({
    name: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: config.commerce.defaultCountry,
  });
  const [sameAsShipping, setSameAsShipping] = useState(true);
  const [notes, setNotes] = useState('');

  const subtotal = cart?.items.reduce((sum, item) => sum + item.price * item.quantity, 0) || 0;
  const shippingCost = subtotal >= config.commerce.freeShippingThreshold ? 0 : config.commerce.shippingCost;
  const tax = Math.round(subtotal * config.commerce.taxRate);
  const total = subtotal + shippingCost + tax;

  const handlePlaceOrder = async () => {
    setLoading(true);
    try {
      const res = await orders.checkout({
        shippingAddress: shipping,
        billingAddress: sameAsShipping ? shipping : billing,
        notes,
      });
      if (res.data) {
        setOrderId(res.data.id);
        setStep(2);
        await refreshCart();
      }
    } catch (e: any) { toast.error(e.message || 'Checkout failed'); }
    setLoading(false);
  };

  const handlePayment = async () => {
    if (!orderId) return;
    setLoading(true);
    try {
      if (paymentMethod === 'mpesa') {
        if (!phoneNumber) { toast.error('Enter phone number'); setLoading(false); return; }
        await orders.mpesaStkPush(orderId, String(total), phoneNumber);
        toast.success('Check your phone for M-Pesa prompt');
        setTimeout(() => router.push(`/checkout/success?order=${orderId}`), 3000);
      } else if (paymentMethod === 'stripe') {
        const res = await payments.stripeCreateIntent(orderId, total);
        if (res.data?.clientSecret) {
          await payments.stripeConfirm(res.data.paymentIntentId);
          toast.success('Payment processed!');
          router.push(`/checkout/success?order=${orderId}`);
        }
      } else if (paymentMethod === 'paypal') {
        const res = await payments.paypalCreateOrder(orderId, total);
        if (res.data?.paypalOrderId) {
          await payments.paypalCapture(res.data.paypalOrderId);
          toast.success('Payment processed!');
          router.push(`/checkout/success?order=${orderId}`);
        }
      }
    } catch (e: any) { toast.error(e.message || 'Payment failed'); }
    setLoading(false);
  };

  if (!cart || cart.items.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <p className="text-gray-500 mb-4">Your cart is empty. Add items before checkout.</p>
        <button onClick={() => router.push('/products')} className="bg-primary-600 text-white px-6 py-3 rounded-lg">Browse Products</button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="flex items-center gap-4 mb-8">
        <div className={`flex items-center gap-2 ${step >= 1 ? 'text-primary-600' : 'text-gray-400'}`}>
          <span className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${step >= 1 ? 'bg-primary-600 text-white' : 'bg-gray-200'}`}>1</span>
          <span className="hidden sm:inline">Shipping</span>
        </div>
        <div className="flex-1 h-px bg-gray-200" />
        <div className={`flex items-center gap-2 ${step >= 2 ? 'text-primary-600' : 'text-gray-400'}`}>
          <span className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${step >= 2 ? 'bg-primary-600 text-white' : 'bg-gray-200'}`}>2</span>
          <span className="hidden sm:inline">Payment</span>
        </div>
      </div>

      {step === 1 && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white rounded-lg p-6 border">
              <h2 className="text-xl font-bold mb-4">Shipping Address</h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[
                  { label: 'Full Name', key: 'name', required: true },
                  { label: 'Address Line 1', key: 'line1', required: true },
                  { label: 'Address Line 2', key: 'line2' },
                  { label: 'City', key: 'city', required: true },
                  { label: 'State/County', key: 'state', required: true },
                  { label: 'Postal Code', key: 'postalCode', required: true },
                  { label: 'Phone', key: 'phone', required: true },
                ].map(field => (
                  <div key={field.key} className={field.key === 'line1' || field.key === 'name' ? 'sm:col-span-2' : ''}>
                    <label className="block text-sm font-medium mb-1">{field.label} {field.required && '*'}</label>
                    <input type="text" value={(shipping as any)[field.key]} onChange={e => setShipping({ ...shipping, [field.key]: e.target.value })}
                      className="w-full border rounded-lg px-3 py-2 text-sm" required={field.required} />
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-white rounded-lg p-6 border">
              <label className="flex items-center gap-2 mb-4">
                <input type="checkbox" checked={sameAsShipping} onChange={e => setSameAsShipping(e.target.checked)} />
                <span className="text-sm font-medium">Billing address same as shipping</span>
              </label>
              {!sameAsShipping && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {['name', 'line1', 'line2', 'city', 'state', 'postalCode'].map(key => (
                    <div key={key} className={key === 'line1' || key === 'name' ? 'sm:col-span-2' : ''}>
                      <label className="block text-sm font-medium mb-1 capitalize">{key}</label>
                      <input type="text" value={(billing as any)[key]} onChange={e => setBilling({ ...billing, [key]: e.target.value })} className="w-full border rounded-lg px-3 py-2 text-sm" />
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="bg-white rounded-lg p-6 border">
              <label className="block text-sm font-medium mb-1">Order Notes (optional)</label>
              <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={3} className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="Any special instructions..." />
            </div>
          </div>

          <div className="bg-white rounded-lg p-6 border h-fit">
            <h2 className="text-xl font-bold mb-4">Order Summary</h2>
            <div className="space-y-3 mb-4">
              {cart.items.map(item => (
                <div key={item.id} className="flex justify-between text-sm">
                  <span className="truncate flex-1">{item.name} x{item.quantity}</span>
                  <span className="ml-2 font-medium">{formatKES(item.price * item.quantity)}</span>
                </div>
              ))}
            </div>
            <div className="border-t pt-3 space-y-2 text-sm">
              <div className="flex justify-between"><span>Subtotal</span><span>{formatKES(subtotal)}</span></div>
              <div className="flex justify-between"><span>Shipping</span><span>{shippingCost === 0 ? 'Free' : formatKES(shippingCost)}</span></div>
              <div className="flex justify-between"><span>Tax (16%)</span><span>{formatKES(tax)}</span></div>
              <div className="border-t pt-2 flex justify-between font-bold text-lg"><span>Total</span><span>{formatKES(total)}</span></div>
            </div>
            <button onClick={handlePlaceOrder} disabled={loading}
              className="w-full bg-primary-600 text-white py-3 rounded-lg font-semibold hover:bg-primary-700 disabled:opacity-50 mt-4">
              {loading ? 'Placing Order...' : 'Place Order'}
            </button>
          </div>
        </div>
      )}

      {step === 2 && (
        <div className="max-w-lg mx-auto">
          <div className="bg-white rounded-lg p-8 border text-center">
            <h2 className="text-2xl font-bold mb-2">Select Payment Method</h2>
            <p className="text-gray-500 mb-6">Order total: <strong>{formatKES(total)}</strong></p>

            <div className="space-y-3 mb-6">
              {[
                { id: 'mpesa', label: 'M-Pesa', desc: 'Pay via STK Push' },
                { id: 'stripe', label: 'Credit/Debit Card', desc: 'Stripe (simulated)' },
                { id: 'paypal', label: 'PayPal', desc: 'PayPal (simulated)' },
              ].map(m => (
                <label key={m.id} className={`block border-2 rounded-lg p-4 cursor-pointer text-left transition ${paymentMethod === m.id ? 'border-primary-600 bg-primary-50' : 'border-gray-200 hover:border-gray-300'}`}>
                  <input type="radio" name="payment" value={m.id} checked={paymentMethod === m.id} onChange={() => setPaymentMethod(m.id as any)} className="hidden" />
                  <span className="font-semibold">{m.label}</span>
                  <span className="block text-sm text-gray-500">{m.desc}</span>
                </label>
              ))}
            </div>

            {paymentMethod === 'mpesa' && (
              <div className="mb-6">
                <label className="block text-sm font-medium mb-1">M-Pesa Phone Number</label>
                <input type="tel" value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)} placeholder="254712345678"
                  className="w-full border rounded-lg px-3 py-2" />
              </div>
            )}

            <button onClick={handlePayment} disabled={loading}
              className="w-full bg-primary-600 text-white py-3 rounded-lg font-semibold hover:bg-primary-700 disabled:opacity-50">
              {loading ? 'Processing...' : `Pay ${formatKES(total)}`}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
