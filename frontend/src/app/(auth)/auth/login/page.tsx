'use client';
import { useState, FormEvent, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import toast from 'react-hot-toast';
import { FiArrowRight, FiEye, FiEyeOff, FiLock, FiMail } from 'react-icons/fi';

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const redirectTo = searchParams.get('redirect') || '/';

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await login(email, password);
      toast.success('Welcome back');
      router.push(redirectTo);
    } catch (e: any) { toast.error(e.message || 'Login failed'); }
    setLoading(false);
  };

  return (
    <div className="grid min-h-[100dvh] lg:grid-cols-[1fr_1.1fr]">
      <aside
        className="relative hidden flex-col justify-between overflow-hidden p-12 text-white lg:flex"
        style={{
          background:
            'radial-gradient(40rem 26rem at 15% -10%, oklch(0.4 0.14 250 / 0.6), transparent 60%),' +
            'linear-gradient(160deg, #101418 0%, #161d26 60%, #101418 100%)',
        }}
      >
        <Link href="/" className="relative text-xl font-extrabold tracking-tight">
          i-love-shopping<span className="text-primary-400">.</span>
        </Link>

        <div className="relative">
          <p className="text-sm font-semibold uppercase tracking-[0.22em] text-primary-300">Members first</p>
          <h2 className="mt-4 max-w-[16ch] text-4xl font-extrabold leading-[1.05] tracking-tight">
            Your orders, addresses and favourites - all in one place.
          </h2>
          <ul className="mt-8 space-y-3 text-stone-300">
            {[
              'Live order tracking from payment to doorstep',
              'One-tap checkout with saved addresses',
              'Early access to offers and new arrivals',
            ].map(point => (
              <li key={point} className="flex items-center gap-3">
                <span className="h-1.5 w-1.5 rounded-full bg-primary-400" />
                {point}
              </li>
            ))}
          </ul>
        </div>

        <p className="relative text-xs text-stone-500">
          &copy; {new Date().getFullYear()} i-love-shopping. Crafted in Nairobi.
        </p>
      </aside>

      <main className="flex items-center justify-center bg-white px-5 py-16 sm:px-10">
        <div className="w-full max-w-md">
          <Link href="/" className="mb-10 block text-xl font-extrabold tracking-tight lg:hidden">
            i-love-shopping<span className="text-primary-600">.</span>
          </Link>

          <h1 className="text-3xl font-bold tracking-tight">Sign in</h1>
          <p className="mt-2 text-sm leading-relaxed text-stone-500">
            Good to see you again. Enter your details to pick up right where you left off.
          </p>

          <form onSubmit={handleSubmit} className="mt-9 space-y-5">
            <div>
              <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-stone-700">Email address</label>
              <div className="relative">
                <FiMail className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
                <input
                  id="email" type="email" value={email} onChange={e => setEmail(e.target.value)}
                  required autoComplete="email" placeholder="you@example.com"
                  className="w-full rounded-xl border border-stone-300 bg-stone-50/50 py-3 pl-11 pr-4 text-sm transition-all placeholder:text-stone-400 focus:border-primary-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100"
                />
              </div>
            </div>

            <div>
              <div className="mb-1.5 flex items-center justify-between">
                <label htmlFor="password" className="block text-sm font-medium text-stone-700">Password</label>
                <Link href="/auth/forgot-password" className="text-xs font-semibold text-primary-600 hover:text-primary-700">
                  Forgot password?
                </Link>
              </div>
              <div className="relative">
                <FiLock className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
                <input
                  id="password" type={showPassword ? 'text' : 'password'} value={password}
                  onChange={e => setPassword(e.target.value)} required autoComplete="current-password"
                  placeholder="Your password"
                  className="w-full rounded-xl border border-stone-300 bg-stone-50/50 py-3 pl-11 pr-12 text-sm transition-all placeholder:text-stone-400 focus:border-primary-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100"
                />
                <button
                  type="button" onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <FiEyeOff /> : <FiEye />}
                </button>
              </div>
            </div>

            <button
              type="submit" disabled={loading}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-stone-900 py-3.5 font-semibold text-white shadow-lg shadow-stone-900/20 transition-all hover:-translate-y-0.5 hover:bg-stone-800 disabled:opacity-60 disabled:hover:translate-y-0"
            >
              {loading ? 'Signing in...' : <>Sign in <FiArrowRight /></>}
            </button>
          </form>

          <p className="mt-8 text-center text-sm text-stone-500">
            New to the store?{' '}
            <Link href="/auth/register" className="font-semibold text-primary-600 hover:text-primary-700">
              Create an account
            </Link>
          </p>
        </div>
      </main>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="min-h-[100dvh] bg-white" />}>
      <LoginForm />
    </Suspense>
  );
}
