'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { SignIn } from '@clerk/nextjs';
import { config } from '@/lib/config';
import { FiAlertCircle } from 'react-icons/fi';

/**
 * Clerk sign-in page. Clerk is an optional layer — without a publishable key
 * the store's own password auth (header sign-in modal) is the only auth, and
 * this route simply returns to the storefront instead of mounting Clerk.
 */
export default function SignInPage() {
  const router = useRouter();
  useEffect(() => {
    if (!config.clerk.enabled) router.replace('/');
  }, [router]);
  if (!config.clerk.enabled) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 px-4 text-center">
        <FiAlertCircle className="h-8 w-8 text-stone-400" />
        <p className="text-sm text-stone-600">Social sign-in is not configured. Use the sign-in button in the header.</p>
      </div>
    );
  }
  return (
    <div className="flex min-h-screen items-center justify-center">
      <SignIn fallbackRedirectUrl="/clerk-callback" />
    </div>
  );
}
