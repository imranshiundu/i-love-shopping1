'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { AuthenticateWithRedirectCallback } from '@clerk/nextjs';
import { config } from '@/lib/config';
import { FiAlertCircle } from 'react-icons/fi';

/**
 * Completes the Clerk social sign-in redirect leg, then forwards to
 * /clerk-callback, which exchanges the Clerk session for store JWTs.
 * Unreachable without Clerk — renders a plain notice then bounces home.
 */
export default function SsoCallbackPage() {
  const router = useRouter();
  useEffect(() => {
    if (!config.clerk.enabled) router.replace('/');
  }, [router]);
  if (!config.clerk.enabled) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 px-4 text-center">
        <FiAlertCircle className="h-8 w-8 text-stone-400" />
        <p className="text-sm text-stone-600">Social sign-in is not configured on this deployment.</p>
      </div>
    );
  }
  return (
    <AuthenticateWithRedirectCallback
      signInFallbackRedirectUrl="/clerk-callback"
      signUpFallbackRedirectUrl="/clerk-callback"
    />
  );
}
