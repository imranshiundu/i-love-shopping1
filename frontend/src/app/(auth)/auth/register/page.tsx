'use client';
import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import toast from 'react-hot-toast';
import { FiArrowRight, FiUser, FiMail, FiLock } from 'react-icons/fi';
import { config } from '@/lib/config';

export default function RegisterPage() {
  const router = useRouter();
  const { register } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (password !== confirmPassword) { toast.error('Passwords do not match'); return; }
    if (password.length < config.commerce.minPasswordLength) {
      toast.error(`Password must be at least ${config.commerce.minPasswordLength} characters`); return;
    }
    setLoading(true);
    try {
      await register(email, password, name);
      toast.success('Account created. Check your inbox to verify your email.');
      router.push('/auth/login');
    } catch (e: any) { toast.error(e.message || 'Registration failed'); }
    setLoading(false);
  };

  return (
    <div className="flex min-h-[100dvh] items-center justify-center px-5 py-14" style={{ background: 'linear-gradient(160deg, #101418 0%, #161d26 100%)' }}>
      <div className="w-full max-w-md">
        <Link href="/" className="mb-8 block text-center text-xl font-extrabold tracking-tight text-white">
          i-love-shopping<span className="text-primary-400">.</span>
        </Link>

        <div className="rounded-3xl border border-white/10 bg-white p-8 shadow-2xl">
          <h1 className="text-2xl font-bold tracking-tight">Create your account</h1>
          <p className="mt-1.5 text-sm text-stone-500">Join for faster checkout, order tracking and offers.</p>

          <form onSubmit={handleSubmit} className="mt-7 space-y-4">
            <AuthField icon={FiUser} label="Full name">
              <input type="text" value={name} onChange={e => setName(e.target.value)} required
                placeholder="Amina Wanjiru" autoComplete="name" className={fieldCls} />
            </AuthField>
            <AuthField icon={FiMail} label="Email address">
              <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
                placeholder="you@example.com" autoComplete="email" className={fieldCls} />
            </AuthField>
            <AuthField icon={FiLock} label="Password" hint={`At least ${config.commerce.minPasswordLength} characters`}>
              <input type="password" value={password} onChange={e => setPassword(e.target.value)}
                required minLength={config.commerce.minPasswordLength} autoComplete="new-password" className={fieldCls} />
            </AuthField>
            <AuthField icon={FiLock} label="Confirm password">
              <input type="password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)}
                required autoComplete="new-password" className={fieldCls} />
            </AuthField>

            <button type="submit" disabled={loading}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-stone-900 py-3.5 font-semibold text-white shadow-lg shadow-stone-900/20 transition-all hover:-translate-y-0.5 hover:bg-stone-800 disabled:opacity-60 disabled:hover:translate-y-0">
              {loading ? 'Creating account...' : <>Create account <FiArrowRight /></>}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-stone-500">
            Already a member?{' '}
            <Link href="/auth/login" className="font-semibold text-primary-600 hover:text-primary-700">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

const fieldCls = 'w-full rounded-xl border border-stone-300 bg-stone-50/50 py-2.5 pl-10 pr-4 text-sm transition-all placeholder:text-stone-400 focus:border-primary-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100';

function AuthField({ icon: Icon, label, hint, children }: {
  icon: any; label: string; hint?: string; children: React.ReactNode;
}) {
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
