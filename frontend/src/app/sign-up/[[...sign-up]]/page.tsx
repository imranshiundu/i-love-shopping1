'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { SignUp } from '@clerk/nextjs';
import { config } from '@/lib/config';
import { FiAlertCircle } from 'react-icons/fi';

/**
 * Clerk sign-up page. Optional like sign-in — without a publishable key the
 * store's own registration (header modal) is the way in.
 */
export default function SignUpPage() {
  const router = useRouter();
  useEffect(() => {
    if (!config.clerk.enabled) router.replace('/');
  }, [router]);
  if (!config.clerk.enabled) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 px-4 text-center">
        <FiAlertCircle className="h-8 w-8 text-stone-400" />
        <p className="text-sm text-stone-600">Social sign-up is not configured. Use the create-account button in the header.</p>
      </div>
    );
  }
  return (
    <div className="flex min-h-screen items-center justify-center">
      <SignUp fallbackRedirectUrl="/clerk-callback" />
    </div>
  );
}
