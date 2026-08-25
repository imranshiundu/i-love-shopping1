'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { auth, orders as ordersApi } from '@/services/api';
import { config } from '@/lib/config';
import { formatKES, formatDate } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import {
  FiEdit2, FiX, FiCheck, FiLock, FiPackage, FiMapPin,
  FiShield, FiArrowUpRight, FiTrendingUp,
} from 'react-icons/fi';
import toast from 'react-hot-toast';

export default function AccountPage() {
  const { user, refreshUser } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [editing, setEditing] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [changingPw, setChangingPw] = useState(false);
  const [orderStats, setOrderStats] = useState({ count: 0, spent: 0 });

  useEffect(() => {
    if (user) { setName(user.name); setEmail(user.email); }
  }, [user]);

  useEffect(() => {
    ordersApi.list(0, 50).then(res => {
      const list: any[] = (res.data as any)?.orders || (Array.isArray(res.data) ? res.data : []);
      const paid = list.filter(o => !['CANCELLED', 'REFUNDED', 'PENDING'].includes(o.status));
      setOrderStats({
        count: list.length,
        spent: paid.reduce((sum, o) => sum + (o.total || 0), 0),
      });
    }).catch(() => {});
  }, []);

  const handleProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingProfile(true);
    try { await auth.updateProfile({ name, email }); await refreshUser(); setEditing(false); toast.success('Profile updated'); }
    catch (e: any) { toast.error(e.message); }
    setSavingProfile(false);
  };

  const handlePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < config.commerce.minPasswordLength) {
      toast.error(`Password must be at least ${config.commerce.minPasswordLength} characters`);
      return;
    }
    setChangingPw(true);
    try {
      await auth.changePassword(currentPassword, newPassword);
      setCurrentPassword(''); setNewPassword(''); toast.success('Password changed');
    } catch (e: any) { toast.error(e.message || 'Failed to change password'); }
    setChangingPw(false);
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <Reveal>
        <section className="relative overflow-hidden rounded-3xl bg-stone-950 p-8 text-white sm:p-10">
          <div
            aria-hidden
            className="absolute inset-0"
            style={{ background: 'radial-gradient(36rem 20rem at 90% -30%, oklch(0.45 0.14 250 / 0.5), transparent 60%)' }}
          />
          <div className="relative flex flex-wrap items-center gap-5">
            <span className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary-500/20 text-2xl font-extrabold text-primary-300 ring-2 ring-primary-400/40">
              {user?.name?.charAt(0).toUpperCase() || '?'}
            </span>
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary-300">My account</p>
              <h1 className="mt-1 text-2xl font-bold sm:text-3xl">{user?.name}</h1>
              <p className="text-sm text-stone-300">
                {user?.email}
                {user?.createdAt && <> - member since {formatDate(user.createdAt)}</>}
              </p>
            </div>
            <div className="ml-auto grid grid-cols-2 gap-3">
              <div className="rounded-xl border border-white/10 bg-white/5 px-4 py-3 backdrop-blur">
                <p className="text-[11px] uppercase tracking-wider text-stone-400">Orders placed</p>
                <p className="text-lg font-bold tabular-nums">{orderStats.count}</p>
              </div>
              <div className="rounded-xl border border-white/10 bg-white/5 px-4 py-3 backdrop-blur">
                <p className="text-[11px] uppercase tracking-wider text-stone-400">Total spent</p>
                <p className="text-lg font-bold tabular-nums">{formatKES(orderStats.spent)}</p>
              </div>
            </div>
          </div>
        </section>
      </Reveal>

      <div className="mt-8 grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        <div className="space-y-6">
          <Reveal delay={80}>
            <section className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-7">
              <div className="flex items-center justify-between">
                <h2 className="flex items-center gap-2 font-bold"><FiEdit2 className="text-primary-600" /> Profile details</h2>
                {editing ? (
                  <button onClick={() => { setEditing(false); if (user) { setName(user.name); setEmail(user.email); } }}
                    className="flex items-center gap-1 text-sm text-stone-500 hover:text-stone-800"><FiX /> Cancel</button>
                ) : (
                  <button onClick={() => setEditing(true)}
                    className="rounded-lg bg-stone-100 px-3.5 py-1.5 text-sm font-semibold hover:bg-stone-200">Edit</button>
                )}
              </div>
              <form onSubmit={handleProfile} className="mt-5 space-y-4">
                <LabeledInput label="Full name" value={name} onChange={setName} disabled={!editing} />
                <LabeledInput label="Email" type="email" value={email} onChange={setEmail} disabled={!editing} />
                {editing && (
                  <button type="submit" disabled={savingProfile}
                    className="flex items-center gap-1.5 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-60">
                    <FiCheck /> Save changes
                  </button>
                )}
              </form>
            </section>
          </Reveal>

          <Reveal delay={140}>
            <section className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-7">
              <h2 className="flex items-center gap-2 font-bold"><FiLock className="text-primary-600" /> Password and security</h2>
              <form onSubmit={handlePassword} className="mt-5 space-y-4">
                <LabeledInput label="Current password" type="password" value={currentPassword} onChange={setCurrentPassword} required />
                <LabeledInput label={`New password (min ${config.commerce.minPasswordLength} chars)`} type="password" value={newPassword} onChange={setNewPassword} required minLength={config.commerce.minPasswordLength} />
                <button type="submit" disabled={changingPw}
                  className="rounded-xl bg-stone-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-stone-800 disabled:opacity-60">
                  {changingPw ? 'Updating...' : 'Change password'}
                </button>
              </form>
              <div className="mt-5 flex items-center gap-3 rounded-xl bg-stone-50 p-4 text-sm">
                <FiShield className={`h-5 w-5 shrink-0 ${user?.twoFactorEnabled ? 'text-emerald-600' : 'text-stone-400'}`} />
                <p className="text-stone-600">
                  Two-factor authentication is <strong>{user?.twoFactorEnabled ? 'enabled' : 'disabled'}</strong>.
                  Email verification: <strong>{user?.emailVerified ? 'verified' : 'pending'}</strong>.
                </p>
              </div>
            </section>
          </Reveal>
        </div>

        <Reveal delay={120}>
          <aside className="space-y-4 lg:sticky lg:top-28 lg:self-start">
            <div className="rounded-2xl border border-stone-200/80 bg-white p-6">
              <h3 className="font-bold">Quick actions</h3>
              <nav className="mt-4 space-y-2">
                <ActionLink href="/account/orders" icon={FiPackage} label="My orders" note={`${orderStats.count} all-time`} />
                <ActionLink href="/account/addresses" icon={FiMapPin} label="Address book" note="Delivery details" />
                {user?.roles?.includes('ADMIN') && (
                  <ActionLink href="/admin" icon={FiTrendingUp} label="Admin dashboard" note="Manage the store" accent />
                )}
              </nav>
            </div>
          </aside>
        </Reveal>
      </div>
    </div>
  );
}

function ActionLink({ href, icon: Icon, label, note, accent }: { href: string; icon: any; label: string; note: string; accent?: boolean }) {
  return (
    <Link href={href}
      className={`group flex items-center justify-between rounded-xl border p-4 transition-colors ${
        accent ? 'border-primary-200 bg-primary-50/50 hover:bg-primary-50' : 'border-stone-100 hover:border-primary-200 hover:bg-primary-50/40'
      }`}>
      <span className="flex items-center gap-3">
        <span className={`rounded-lg p-2 ${accent ? 'bg-primary-100 text-primary-700' : 'bg-stone-100 text-stone-700'}`}>
          <Icon className="h-4 w-4" />
        </span>
        <span>
          <span className={`block text-sm font-semibold ${accent ? 'text-primary-700' : ''}`}>{label}</span>
          <span className="block text-xs text-stone-500">{note}</span>
        </span>
      </span>
      <FiArrowUpRight className="h-4 w-4 text-stone-400 transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" />
    </Link>
  );
}

function LabeledInput({ label, value, onChange, type = 'text', disabled, required, minLength }: {
  label: string; value: string; onChange: (v: string) => void;
  type?: string; disabled?: boolean; required?: boolean; minLength?: number;
}) {
  return (
    <div>
      <label className="mb-1 block text-sm font-medium text-stone-700">{label}</label>
      <input
        type={type} value={value} onChange={e => onChange(e.target.value)}
        disabled={disabled} required={required} minLength={minLength}
        className="w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm transition-colors focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100 disabled:bg-stone-50 disabled:text-stone-500"
      />
    </div>
  );
}
