'use client';
import { Suspense, useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { FiLoader, FiCheckCircle, FiXCircle } from 'react-icons/fi';

function OAuthCallback() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { loginWithTokens } = useAuth();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');
    const error = searchParams.get('error');
    if (error) {
      setStatus('error');
      setMessage('Sign-in was cancelled or failed. Please try again.');
      return;
    }
    if (!accessToken || !refreshToken) {
      setStatus('error');
      setMessage('Missing credentials from the provider. Please try again.');
      return;
    }
    let cancelled = false;
    loginWithTokens(accessToken, refreshToken)
      .then(() => {
        if (cancelled) return;
        setStatus('success');
        setTimeout(() => router.push('/'), 800);
      })
      .catch((e: any) => {
        if (cancelled) return;
        setStatus('error');
        setMessage(e?.message || 'Could not complete sign-in.');
      });
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="flex min-h-[100dvh] items-center justify-center px-5" style={{ background: 'linear-gradient(160deg, #101418 0%, #161d26 100%)' }}>
      <div className="w-full max-w-md rounded-3xl bg-white p-8 text-center shadow-2xl">
        {status === 'loading' && (
          <>
            <FiLoader className="mx-auto h-8 w-8 animate-spin text-primary-600" />
            <h1 className="mt-4 text-xl font-bold">Completing sign-in...</h1>
            <p className="mt-1 text-sm text-stone-500">One moment while we connect your account.</p>
          </>
        )}
        {status === 'success' && (
          <>
            <FiCheckCircle className="mx-auto h-8 w-8 text-emerald-500" />
            <h1 className="mt-4 text-xl font-bold">Signed in</h1>
            <p className="mt-1 text-sm text-stone-500">Taking you to the store...</p>
          </>
        )}
        {status === 'error' && (
          <>
            <FiXCircle className="mx-auto h-8 w-8 text-rose-500" />
            <h1 className="mt-4 text-xl font-bold">Sign-in failed</h1>
            <p className="mt-1 text-sm text-stone-500">{message}</p>
            <Link href="/" className="mt-6 inline-block rounded-xl bg-stone-900 px-6 py-3 font-semibold text-white">
              Back to store
            </Link>
          </>
        )}
      </div>
    </div>
  );
}

export default function OAuthRedirectPage() {
  return (
    <Suspense>
      <OAuthCallback />
    </Suspense>
  );
}
