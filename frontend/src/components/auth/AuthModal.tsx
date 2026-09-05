'use client';
import { useState, FormEvent } from 'react';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { config } from '@/lib/config';
import { getRegisterCaptchaToken } from '@/lib/captcha';
import toast from 'react-hot-toast';
import { FiX, FiUser, FiMail, FiLock, FiArrowRight, FiEye, FiEyeOff } from 'react-icons/fi';
import { FaGoogle, FaGithub } from 'react-icons/fa';

export type AuthMode = 'login' | 'register';

const fieldCls =
  'w-full rounded-xl border border-stone-300 bg-stone-50/50 py-2.5 pl-10 pr-4 text-sm transition-all placeholder:text-stone-400 focus:border-primary-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100';

function Field({ icon: Icon, label, hint, children }: { icon: any; label: string; hint?: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-stone-700">{label}</label>
      <div className="relative">
        <Icon className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
        {children}
      </div>
      {hint && <p className="mt-1 text-xs text-stone-400">{hint}</p>}
    </div>
  );
}

function OAuthButtons() {
  const showGoogle = config.oauth.google;
  const showGithub = config.oauth.github;
  if (!showGoogle && !showGithub) return null;
  const start = (provider: 'google' | 'github') => {
    window.location.href = `${config.api.baseUrl}/oauth2/authorization/${provider}`;
  };
  return (
    <div className="mt-5">
      <div className="flex items-center gap-3 text-xs text-stone-400">
        <span className="h-px flex-1 bg-stone-200" /> or continue with <span className="h-px flex-1 bg-stone-200" />
      </div>
      <div className="mt-3 grid gap-2.5 sm:grid-cols-2">
        {showGoogle && (
          <button type="button" onClick={() => start('google')}
            className="flex items-center justify-center gap-2 rounded-xl border border-stone-300 py-2.5 text-sm font-semibold text-stone-700 transition-colors hover:bg-stone-50">
            <FaGoogle className="text-[#DB4437]" /> Google
          </button>
        )}
        {showGithub && (
          <button type="button" onClick={() => start('github')}
            className="flex items-center justify-center gap-2 rounded-xl border border-stone-300 py-2.5 text-sm font-semibold text-stone-700 transition-colors hover:bg-stone-50">
            <FaGithub /> GitHub
          </button>
        )}
      </div>
    </div>
  );
}

export default function AuthModal({
  initialMode = 'login',
  onSuccess,
  onClose,
}: {
  initialMode?: AuthMode;
  onSuccess: () => void;
  onClose: () => void;
}) {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<AuthMode>(initialMode);
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const switchMode = (m: AuthMode) => {
    setMode(m);
    setPassword('');
    setConfirmPassword('');
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (mode === 'login') {
        await login(email, password);
        toast.success('Welcome back');
      } else {
        if (password !== confirmPassword) { toast.error('Passwords do not match'); return; }
        if (password.length < config.commerce.minPasswordLength) {
          toast.error(`Password must be at least ${config.commerce.minPasswordLength} characters`); return;
        }
        if (!name.trim()) { toast.error('Please enter your full name'); return; }
        const captchaToken = await getRegisterCaptchaToken();
        await register(email, password, name, captchaToken);
        toast.success('Account created. Welcome!');
      }
      onSuccess();
    } catch (err: any) {
      toast.error(err?.message || (mode === 'login' ? 'Login failed' : 'Registration failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div className="relative z-10 max-h-[90dvh] w-full max-w-md overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl sm:p-8">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 rounded-full p-1 text-stone-400 transition-colors hover:bg-stone-100 hover:text-stone-600"
          aria-label="Close"
        >
          <FiX className="h-5 w-5" />
        </button>

        <h2 className="text-xl font-bold">{mode === 'login' ? 'Sign in' : 'Create your account'}</h2>
        <p className="mt-1 text-sm text-stone-500">
          {mode === 'login'
            ? 'Good to see you again. Your cart carries over.'
            : 'Join for faster checkout, order tracking and offers.'}
        </p>

        <OAuthButtons />

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          {mode === 'register' && (
            <Field icon={FiUser} label="Full name">
              <input type="text" value={name} onChange={e => setName(e.target.value)} required
                placeholder="Amina Wanjiru" autoComplete="name" className={fieldCls} />
            </Field>
          )}
          <Field icon={FiMail} label="Email address">
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
              placeholder="you@example.com" autoComplete="email" className={fieldCls} />
          </Field>
          <Field icon={FiLock} label="Password" hint={mode === 'register' ? `At least ${config.commerce.minPasswordLength} characters` : undefined}>
            <div className="relative">
              <input type={showPassword ? 'text' : 'password'} value={password} onChange={e => setPassword(e.target.value)}
                required minLength={config.commerce.minPasswordLength} autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                className={fieldCls + ' pr-10'} />
              <button type="button" onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600" aria-label="Toggle password">
                {showPassword ? <FiEyeOff /> : <FiEye />}
              </button>
            </div>
          </Field>
          {mode === 'register' && (
            <Field icon={FiLock} label="Confirm password">
              <input type="password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)}
                required autoComplete="new-password" className={fieldCls} />
            </Field>
          )}

          <button type="submit" disabled={loading}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700 disabled:opacity-60">
            {loading ? (mode === 'login' ? 'Signing in...' : 'Creating account...') : (
              <>{mode === 'login' ? 'Sign in' : 'Create account'} <FiArrowRight /></>
            )}
          </button>
        </form>

        <div className="mt-6 rounded-xl bg-stone-50 p-3.5 text-sm text-stone-600">
          {mode === 'login' ? (
            <>New to the store?{' '}
              <button onClick={() => switchMode('register')} className="font-semibold text-primary-600 hover:text-primary-700">
                Create an account
              </button>
            </>
          ) : (
            <>Already a member?{' '}
              <button onClick={() => switchMode('login')} className="font-semibold text-primary-600 hover:text-primary-700">
                Sign in
              </button>
            </>
          )}
        </div>

        {mode === 'login' && (
          <p className="mt-3 text-center text-xs">
            <Link href="/auth/forgot-password" className="font-medium text-stone-400 hover:text-stone-600">
              Forgot password?
            </Link>
          </p>
        )}
      </div>
    </div>
  );
}
