'use client';
import { useState, FormEvent } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { Suspense } from 'react';
import { auth } from '@/services/api';
import toast from 'react-hot-toast';
import { FiArrowLeft, FiLock, FiCheckCircle } from 'react-icons/fi';

function ResetPasswordContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!token) { toast.error('Reset link is missing its token. Request a new one.'); return; }
    if (password.length < 8) { toast.error('Password must be at least 8 characters'); return; }
    if (password !== confirm) { toast.error('Passwords do not match'); return; }
    setLoading(true);
    try {
      await auth.resetPassword(token, password);
      setDone(true);
    } catch (e: any) { toast.error(e.message || 'Reset link is invalid or expired'); }
    setLoading(false);
  };

  return (
    <div className="flex min-h-[100dvh] items-center justify-center px-5 py-14" style={{ background: 'linear-gradient(160deg, #101418 0%, #161d26 100%)' }}>
      <div className="w-full max-w-md">
        <Link href="/auth/login" className="mb-8 inline-flex items-center gap-2 text-sm font-medium text-stone-400 hover:text-white">
          <FiArrowLeft /> Back to sign in
        </Link>

        <div className="rounded-3xl border border-white/10 bg-white p-8 shadow-2xl">
          {done ? (
            <div className="text-center">
              <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-50 text-emerald-600 ring-8 ring-emerald-50/60">
                <FiCheckCircle className="h-6 w-6" />
              </span>
              <h1 className="mt-5 text-xl font-bold tracking-tight">Password updated</h1>
              <p className="mt-2 leading-relaxed text-stone-500">
                Your password has been reset. You can now sign in with your new password.
              </p>
              <Link href="/auth/login"
                className="mt-6 inline-block w-full rounded-xl bg-stone-900 py-3 text-center font-semibold text-white shadow-lg shadow-stone-900/20 transition-all hover:-translate-y-0.5 hover:bg-stone-800">
                Sign in
              </Link>
            </div>
          ) : (
            <>
              <h1 className="text-2xl font-bold tracking-tight">Set a new password</h1>
              <p className="mt-1.5 text-sm leading-relaxed text-stone-500">
                Choose a new password (minimum 8 characters).
              </p>
              {!token && (
                <p className="mt-4 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-700">
                  This page needs a reset link. Open the link from your email, or request a new one.
                </p>
              )}
              <form onSubmit={handleSubmit} className="mt-7 space-y-4">
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-stone-700">New password</label>
                  <div className="relative">
                    <FiLock className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
                    <input type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={8}
                      placeholder="Minimum 8 characters" autoComplete="new-password"
                      className="w-full rounded-xl border border-stone-300 bg-stone-50/50 py-2.5 pl-10 pr-4 text-sm transition-all placeholder:text-stone-400 focus:border-primary-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100" />
                  </div>
                </div>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-stone-700">Confirm password</label>
                  <div className="relative">
                    <FiLock className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
                    <input type="password" value={confirm} onChange={e => setConfirm(e.target.value)} required minLength={8}
                      placeholder="Repeat your new password" autoComplete="new-password"
                      className="w-full rounded-xl border border-stone-300 bg-stone-50/50 py-2.5 pl-10 pr-4 text-sm transition-all placeholder:text-stone-400 focus:border-primary-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100" />
                  </div>
                </div>
                <button type="submit" disabled={loading}
                  className="w-full rounded-xl bg-stone-900 py-3 font-semibold text-white shadow-lg shadow-stone-900/20 transition-all hover:-translate-y-0.5 hover:bg-stone-800 disabled:opacity-60 disabled:hover:translate-y-0">
                  {loading ? 'Updating...' : 'Update password'}
                </button>
              </form>
            </>
          )}
        </div>

        <p className="mt-8 text-center text-sm text-stone-500">
          Didn&apos;t get a link?{' '}
          <Link href="/auth/forgot-password" className="font-semibold text-primary-400 hover:text-primary-300">Request a new one</Link>
        </p>
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense>
      <ResetPasswordContent />
    </Suspense>
  );
}
