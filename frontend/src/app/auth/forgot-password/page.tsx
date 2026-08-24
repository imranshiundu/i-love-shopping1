'use client';
import { useState } from 'react';
import { auth } from '@/services/api';
import toast from 'react-hot-toast';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try { await auth.forgotPassword(email); setSent(true); toast.success('Reset link sent!'); }
    catch (e: any) { toast.error(e.message); }
    setLoading(false);
  };

  return (
    <div className="min-h-[60vh] flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <h1 className="text-3xl font-bold text-center mb-8">Reset Password</h1>
        {sent ? (
          <div className="bg-white rounded-lg p-8 border text-center">
            <p className="text-gray-600">If an account exists with <strong>{email}</strong>, you&apos;ll receive a password reset link shortly.</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="bg-white rounded-lg p-8 border space-y-4">
            <p className="text-gray-600 text-sm">Enter your email and we&apos;ll send you a link to reset your password.</p>
            <div>
              <label className="block text-sm font-medium mb-1">Email</label>
              <input type="email" value={email} onChange={e => setEmail(e.target.value)} required className="w-full border rounded-lg px-3 py-2" />
            </div>
            <button type="submit" disabled={loading} className="w-full bg-primary-600 text-white py-3 rounded-lg font-semibold hover:bg-primary-700 disabled:opacity-50">
              {loading ? 'Sending...' : 'Send Reset Link'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
