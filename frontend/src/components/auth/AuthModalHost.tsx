'use client';
import { Suspense, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import AuthModal from './AuthModal';

/**
 * Global sign-in/register modal. Mount once in the shop layout.
 * Opens imperatively via openAuthModal(), or declaratively via
 * ?auth=login|register&next=/some/path (used by auth-guarded pages).
 */
function AuthModalHostInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { authModal, openAuthModal, closeAuthModal } = useAuth();

  useEffect(() => {
    const mode = searchParams.get('auth');
    if (mode === 'login' || mode === 'register') {
      openAuthModal(mode, searchParams.get('next') || '/');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!authModal) return null;

  const handleSuccess = () => {
    const next = authModal.next;
    closeAuthModal();
    router.push(next);
  };

  const handleClose = () => {
    closeAuthModal();
    // Drop ?auth= from the URL so a refresh doesn't reopen the modal.
    const url = new URL(window.location.href);
    if (url.searchParams.has('auth')) {
      url.searchParams.delete('auth');
      url.searchParams.delete('next');
      router.replace(url.pathname + (url.searchParams.toString() ? `?${url.searchParams}` : ''));
    }
  };

  return (
    <AuthModal
      key={authModal.mode}
      initialMode={authModal.mode}
      onSuccess={handleSuccess}
      onClose={handleClose}
    />
  );
}

export default function AuthModalHost() {
  return (
    <Suspense>
      <AuthModalHostInner />
    </Suspense>
  );
}
