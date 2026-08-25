'use client';
import { useState, FormEvent } from 'react';
import Link from 'next/link';
import { auth } from '@/services/api';
import toast from 'react-hot-toast';
import { FiArrowLeft, FiMail } from 'react-icons/fi';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await auth.forgotPassword(email);
      setSent(true);
    } catch (e: any) { toast.error(e.message || 'Something went wrong'); }
    setLoading(false);
  };

  return (
    <div className="flex min-h-[100dvh] items-center justify-center px-5 py-14" style={{ background: 'linear-gradient(160deg, #101418 0%, #161d26 100%)' }}>
      <div className="w-full max-w-md">
        <Link href="/auth/login" className="mb-8 inline-flex items-center gap-2 text-sm font-medium text-stone-400 hover:text-white">
          <FiArrowLeft /> Back to sign in
        </Link>

        <div className="rounded-3xl border border-white/10 bg-white p-8 shadow-2xl">
          {sent ? (
            <div className="text-center">
              <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-50 text-emerald-600 ring-8 ring-emerald-50/60">
                <FiMail className="h-6 w-6" />
              </span>
              <h1 className="mt-5 text-xl font-bold tracking-tight">Check your inbox</h1>
              <p className="mt-2 leading-relaxed text-stone-500">
                If an account exists for <strong>{email}</strong>, a reset link is on its way.
                It expires in 30 minutes.
              </p>
              <button onClick={() => setSent(false)}
                className="mt-6 text-sm font-semibold text-primary-600 hover:text-primary-700">
                Use a different email
              </button>
            </div>
          ) : (
            <>
              <h1 className="text-2xl font-bold tracking-tight">Reset your password</h1>
              <p className="mt-1.5 text-sm leading-relaxed text-stone-500">
                Enter the email you registered with and we will send you a reset link.
              </p>
              <form onSubmit={handleSubmit} className="mt-7 space-y-4">
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-stone-700">Email address</label>
                  <div className="relative">
                    <FiMail className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
                    <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
                      placeholder="you@example.com" autoComplete="email"
                      className="w-full rounded-xl border border-stone-300 bg-stone-50/50 py-2.5 pl-10 pr-4 text-sm transition-all placeholder:text-stone-400 focus:border-primary-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100" />
                  </div>
                </div>
                <button type="submit" disabled={loading}
                  className="w-full rounded-xl bg-stone-900 py-3 font-semibold text-white shadow-lg shadow-stone-900/20 transition-all hover:-translate-y-0.5 hover:bg-stone-800 disabled:opacity-60 disabled:hover:translate-y-0">
                  {loading ? 'Sending...' : 'Send reset link'}
                </button>
              </form>
            </>
          )}
        </div>

        <p className="mt-8 text-center text-sm text-stone-500">
          Remembered it?{' '}
          <Link href="/auth/login" className="font-semibold text-primary-400 hover:text-primary-300">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
