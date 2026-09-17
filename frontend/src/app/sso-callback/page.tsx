'use client';
import { AuthenticateWithRedirectCallback } from '@clerk/nextjs';

/**
 * Completes the Clerk social sign-in redirect leg, then forwards to
 * /clerk-callback, which exchanges the Clerk session for store JWTs.
 */
export default function SsoCallbackPage() {
  return (
    <AuthenticateWithRedirectCallback
      signInFallbackRedirectUrl="/clerk-callback"
      signUpFallbackRedirectUrl="/clerk-callback"
    />
  );
}
