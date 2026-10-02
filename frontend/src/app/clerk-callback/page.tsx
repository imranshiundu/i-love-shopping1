'use client';
import { Suspense, useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth as useClerkAuth } from '@clerk/nextjs';
import { useAuth } from '@/contexts/AuthContext';
import { config } from '@/lib/config';
import { FiLoader, FiXCircle } from 'react-icons/fi';

/**
 * Exchanges an active Clerk session for store JWTs (POST /auth/clerk) and
 * signs the user into the storefront, then returns them to where they
 * started (or home).
 */
function ClerkCallback() {
  const router = useRouter();
  const { isLoaded, isSignedIn, getToken } = useClerkAuth();
  const { loginWithTokens } = useAuth();
  const [error, setError] = useState('');
  const done = useRef(false);

  const takeNext = () => {
    try {
      const next = sessionStorage.getItem('ils_post_clerk_next') || '/';
      sessionStorage.removeItem('ils_post_clerk_next');
      return next.startsWith('/') ? next : '/';
    } catch {
      return '/';
    }
  };

  useEffect(() => {
    if (!isLoaded || done.current) return;
    if (!isSignedIn) {
      router.replace('/sign-in');
      return;
    }
    done.current = true;
    (async () => {
      try {
        const token = await getToken();
        if (!token) throw new Error('No Clerk session found.');
        const res = await fetch(`${config.api.baseUrl}/auth/clerk`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token }),
        });
        const body = await res.json().catch(() => null);
        if (!res.ok || !body?.success) {
          throw new Error(body?.error?.message || 'Sign-in failed. Please try again.');
        }
        await loginWithTokens(body.data.accessToken, body.data.refreshToken);
        router.replace(takeNext());
      } catch (e: unknown) {
        setError(e instanceof Error ? e.message : 'Could not complete sign-in.');
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoaded, isSignedIn]);

  return (
    <div className="flex min-h-[100dvh] items-center justify-center px-5" style={{ background: 'linear-gradient(160deg, #101418 0%, #161d26 100%)' }}>
      <div className="w-full max-w-md rounded-3xl bg-white p-8 text-center shadow-2xl">
        {error ? (
          <>
            <FiXCircle className="mx-auto h-8 w-8 text-rose-500" />
            <h1 className="mt-4 text-xl font-bold">Sign-in failed</h1>
            <p className="mt-1 text-sm text-stone-500">{error}</p>
            <Link href="/" className="mt-6 inline-block rounded-xl bg-stone-900 px-6 py-3 font-semibold text-white">
              Back to store
            </Link>
          </>
        ) : (
          <>
            <FiLoader className="mx-auto h-8 w-8 animate-spin text-primary-600" />
            <h1 className="mt-4 text-xl font-bold">Completing sign-in...</h1>
            <p className="mt-1 text-sm text-stone-500">One moment while we connect your account.</p>
          </>
        )}
      </div>
    </div>
  );
}

function ClerkNotConfigured() {
  const router = useRouter();
  useEffect(() => { router.replace('/'); }, [router]);
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 px-4 text-center">
      <FiXCircle className="h-8 w-8 text-stone-400" />
      <p className="text-sm text-stone-600">Social sign-in is not configured on this deployment.</p>
    </div>
  );
}

export default function ClerkCallbackPage() {
  // The Clerk flow only mounts when Clerk is configured — the hook above
  // would throw without a provider on a password-only deployment.
  return (
    <Suspense>
      {config.clerk.enabled ? <ClerkCallback /> : <ClerkNotConfigured />}
    </Suspense>
  );
}
