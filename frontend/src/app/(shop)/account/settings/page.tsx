'use client';
import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { auth } from '@/services/api';
import { config } from '@/lib/config';
import Reveal from '@/components/ui/Reveal';
import { FiCheck, FiLock, FiShield, FiUser } from 'react-icons/fi';
import toast from 'react-hot-toast';

export default function AccountSettingsPage() {
  const { user, refreshUser } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [changingPw, setChangingPw] = useState(false);

  useEffect(() => {
    if (user) { setName(user.name); setEmail(user.email); }
  }, [user]);

  const handleProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !email.trim()) { toast.error('Name and email are required'); return; }
    setSavingProfile(true);
    try { await auth.updateProfile({ name, email }); await refreshUser(); toast.success('Profile updated'); }
    catch (e: any) { toast.error(e.message); }
    setSavingProfile(false);
  };

  const handlePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < config.commerce.minPasswordLength) {
      toast.error(`New password must be at least ${config.commerce.minPasswordLength} characters`); return;
    }
    if (newPassword !== confirmPassword) { toast.error('New passwords do not match'); return; }
    setChangingPw(true);
    try {
      await auth.changePassword(currentPassword, newPassword);
      setCurrentPassword(''); setNewPassword(''); setConfirmPassword('');
      toast.success('Password changed - other sessions stay signed in');
    } catch (e: any) { toast.error(e.message || 'Failed to change password'); }
    setChangingPw(false);
  };

  return (
    <div className="max-w-3xl space-y-6">
      <Reveal>
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Account</p>
          <h1 className="mt-1.5 text-2xl font-bold tracking-tight sm:text-3xl">Settings</h1>
          <p className="mt-1 text-sm text-stone-500">Manage your profile details and sign-in security.</p>
        </div>
      </Reveal>

      <Reveal delay={80}>
        <section className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-7">
          <h2 className="flex items-center gap-2 font-bold"><FiUser className="text-primary-600" /> Profile details</h2>
          <form onSubmit={handleProfile} className="mt-5 space-y-4" id="profile-form">
            <Field label="Full name">
              <input value={name} onChange={e => setName(e.target.value)} required
                className={inputCls} />
            </Field>
            <Field label="Email address">
              <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
                className={inputCls} />
            </Field>
            <button type="submit" disabled={savingProfile}
              className="flex items-center gap-1.5 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-60">
              {savingProfile ? 'Saving...' : <>Save changes <FiCheck /></>}
            </button>
          </form>
        </section>
      </Reveal>

      <Reveal delay={140}>
        <section className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-7">
          <h2 className="flex items-center gap-2 font-bold"><FiLock className="text-primary-600" /> Password and security</h2>
          <form onSubmit={handlePassword} className="mt-5 space-y-4">
            <Field label="Current password">
              <input type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)}
                required autoComplete="current-password" className={inputCls} />
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label={`New password (min ${config.commerce.minPasswordLength})`}>
                <input type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)}
                  required minLength={config.commerce.minPasswordLength} autoComplete="new-password" className={inputCls} />
              </Field>
              <Field label="Confirm new password">
                <input type="password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)}
                  required autoComplete="new-password" className={inputCls} />
              </Field>
            </div>
            <button type="submit" disabled={changingPw}
              className="rounded-xl bg-stone-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-stone-800 disabled:opacity-60">
              {changingPw ? 'Updating...' : 'Change password'}
            </button>
          </form>

          <div className="mt-6 flex flex-wrap gap-3 border-t border-stone-100 pt-5">
            <SecurityChip
              ok={!!user?.emailVerified}
              onLabel="Email verified"
              offLabel="Email not verified - check your inbox"
            />
            <SecurityChip
              ok={!!user?.twoFactorEnabled}
              onLabel="Two-factor authentication enabled"
              offLabel="Two-factor authentication available"
            />
          </div>
        </section>
      </Reveal>
    </div>
  );
}

function SecurityChip({ ok, onLabel, offLabel }: { ok: boolean; onLabel: string; offLabel: string }) {
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-semibold ring-1 ${
      ok ? 'bg-emerald-50 text-emerald-700 ring-emerald-200' : 'bg-stone-50 text-stone-500 ring-stone-200'
    }`}>
      <FiShield className="h-3.5 w-3.5" /> {ok ? onLabel : offLabel}
    </span>
  );
}

const inputCls = 'w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm transition-colors focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100';

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="mb-1 block text-sm font-medium text-stone-700">{label}</label>
      {children}
    </div>
  );
}
