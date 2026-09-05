'use client';
import { useEffect, useState, Suspense, FormEvent } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { auth } from '@/services/api';
import toast from 'react-hot-toast';
import { FiArrowLeft, FiCheckCircle, FiXCircle, FiLoader, FiMail } from 'react-icons/fi';

function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>(token ? 'loading' : 'error');
  const [message, setMessage] = useState('');
  const [resendEmail, setResendEmail] = useState('');
  const [resending, setResending] = useState(false);

  const handleResend = async (e: FormEvent) => {
    e.preventDefault();
    setResending(true);
    try {
      await auth.resendVerification(resendEmail);
      toast.success('If the address is registered and unverified, a new link is on its way');
    } catch (err: any) { toast.error(err.message || 'Something went wrong'); }
    setResending(false);
  };

  useEffect(() => {
    if (!token) { setMessage('This page needs a verification link. Open the link from your email.'); return; }
    let cancelled = false;
    auth.verifyEmail(token)
      .then(() => { if (!cancelled) setStatus('success'); })
      .catch((e: any) => { if (!cancelled) { setStatus('error'); setMessage(e.message || 'Verification link is invalid or expired'); } });
    return () => { cancelled = true; };
  }, [token]);

  return (
    <div className="flex min-h-[100dvh] items-center justify-center px-5 py-14" style={{ background: 'linear-gradient(160deg, #101418 0%, #161d26 100%)' }}>
      <div className="w-full max-w-md">
        <Link href="/auth/login" className="mb-8 inline-flex items-center gap-2 text-sm font-medium text-stone-400 hover:text-white">
          <FiArrowLeft /> Back to sign in
        </Link>

        <div className="rounded-3xl border border-white/10 bg-white p-8 shadow-2xl">
          <div className="text-center">
            {status === 'loading' && (
              <>
                <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-stone-100 text-stone-500 ring-8 ring-stone-100/60">
                  <FiLoader className="h-6 w-6 animate-spin" />
                </span>
                <h1 className="mt-5 text-xl font-bold tracking-tight">Verifying your email...</h1>
                <p className="mt-2 leading-relaxed text-stone-500">One moment while we confirm your address.</p>
              </>
            )}
            {status === 'success' && (
              <>
                <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-50 text-emerald-600 ring-8 ring-emerald-50/60">
                  <FiCheckCircle className="h-6 w-6" />
                </span>
                <h1 className="mt-5 text-xl font-bold tracking-tight">Email verified</h1>
                <p className="mt-2 leading-relaxed text-stone-500">
                  Your email address is confirmed. You can now sign in and shop.
                </p>
                <Link href="/auth/login"
                  className="mt-6 inline-block w-full rounded-xl bg-stone-900 py-3 text-center font-semibold text-white shadow-lg shadow-stone-900/20 transition-all hover:-translate-y-0.5 hover:bg-stone-800">
                  Sign in
                </Link>
              </>
            )}
            {status === 'error' && (
              <>
                <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-red-50 text-red-500 ring-8 ring-red-50/60">
                  <FiXCircle className="h-6 w-6" />
                </span>
                <h1 className="mt-5 text-xl font-bold tracking-tight">Verification failed</h1>
                <p className="mt-2 leading-relaxed text-stone-500">{message}</p>
                <p className="mt-4 text-sm text-stone-500">
                  Links expire after 24 hours and work only once. Enter your email below and we will send a fresh link.
                </p>
                <form onSubmit={handleResend} className="mt-5 space-y-3">
                  <div className="relative">
                    <FiMail className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
                    <input type="email" value={resendEmail} onChange={e => setResendEmail(e.target.value)} required
                      placeholder="you@example.com" autoComplete="email"
                      className="w-full rounded-xl border border-stone-300 bg-stone-50/50 py-2.5 pl-10 pr-4 text-sm transition-all placeholder:text-stone-400 focus:border-primary-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100" />
                  </div>
                  <button type="submit" disabled={resending}
                    className="w-full rounded-xl bg-stone-900 py-3 font-semibold text-white shadow-lg shadow-stone-900/20 transition-all hover:-translate-y-0.5 hover:bg-stone-800 disabled:opacity-60 disabled:hover:translate-y-0">
                    {resending ? 'Sending...' : 'Resend verification link'}
                  </button>
                </form>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense>
      <VerifyEmailContent />
    </Suspense>
  );
}
