'use client';
import { useState, useEffect, useCallback } from 'react';
import { admin } from '@/services/api';
import { formatDate } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import { FiUsers, FiShield, FiCheckCircle, FiXCircle } from 'react-icons/fi';
import toast from 'react-hot-toast';

interface Customer {
  id: string;
  email: string;
  name: string;
  avatar?: string;
  emailVerified: boolean;
  twoFactorEnabled: boolean;
  roles: string[];
  createdAt: string;
}

export default function AdminCustomersPage() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await admin.listUsers();
      setCustomers(res.data as unknown as Customer[] || []);
    } catch (e: any) { toast.error(e.message); }
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = customers.filter(c =>
    !query ||
    c.name?.toLowerCase().includes(query.toLowerCase()) ||
    c.email?.toLowerCase().includes(query.toLowerCase())
  );

  const adminsCount = customers.filter(c => c.roles?.includes('ADMIN')).length;

  return (
    <div className="space-y-6">
      <Reveal>
        <div>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Customers</h1>
          <p className="mt-1 text-sm text-stone-500">
            {customers.length} registered - {adminsCount} admin{adminsCount === 1 ? '' : 's'}
          </p>
        </div>
      </Reveal>

      <div className="grid grid-cols-3 gap-4">
        <Kpi icon={FiUsers} tone="primary" label="Total customers" value={String(customers.length)} delay={0} />
        <Kpi icon={FiCheckCircle} tone="emerald" label="Verified emails" value={String(customers.filter(c => c.emailVerified).length)} delay={70} />
        <Kpi icon={FiShield} tone="amber" label="2FA enabled" value={String(customers.filter(c => c.twoFactorEnabled).length)} delay={140} />
      </div>

      <Reveal delay={100}>
        <input
          value={query} onChange={e => setQuery(e.target.value)}
          placeholder="Search by name or email..."
          className="w-full max-w-md rounded-xl border border-stone-300 bg-white px-4 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100"
        />
      </Reveal>

      <Reveal delay={140}>
        <div className="overflow-hidden rounded-2xl border border-stone-200/80 bg-white">
          {loading ? (
            <div className="space-y-3 p-6">{[...Array(5)].map((_, i) => <div key={i} className="h-12 animate-pulse rounded-xl bg-stone-200/50" />)}</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[680px] text-sm">
                <thead>
                  <tr className="border-b border-stone-100 text-left text-xs uppercase tracking-wider text-stone-400">
                    <th className="px-5 py-3.5 font-semibold">Customer</th>
                    <th className="px-5 py-3.5 font-semibold">Joined</th>
                    <th className="px-5 py-3.5 font-semibold">Role</th>
                    <th className="px-5 py-3.5 font-semibold">Email</th>
                    <th className="px-5 py-3.5 font-semibold">2FA</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(c => (
                    <tr key={c.id} className="border-b border-stone-50 last:border-0 hover:bg-stone-50/60">
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary-100 text-sm font-bold text-primary-700">
                            {c.name?.charAt(0).toUpperCase() || '?'}
                          </span>
                          <div className="min-w-0">
                            <p className="font-medium">{c.name}</p>
                            <p className="text-xs text-stone-400">{c.email}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-3.5 text-stone-500">{c.createdAt ? formatDate(c.createdAt) : '-'}</td>
                      <td className="px-5 py-3.5">
                        {c.roles?.includes('ADMIN') ? (
                          <span className="rounded-full bg-violet-100 px-2.5 py-1 text-[10px] font-bold text-violet-700">ADMIN</span>
                        ) : (
                          <span className="rounded-full bg-stone-100 px-2.5 py-1 text-[10px] font-bold text-stone-600">CUSTOMER</span>
                        )}
                      </td>
                      <td className="px-5 py-3.5">
                        {c.emailVerified
                          ? <FiCheckCircle className="h-4 w-4 text-emerald-600" aria-label="verified" />
                          : <FiXCircle className="h-4 w-4 text-stone-300" aria-label="unverified" />}
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-block rounded-full px-2.5 py-0.5 text-[10px] font-bold ${c.twoFactorEnabled ? 'bg-emerald-100 text-emerald-700' : 'bg-stone-100 text-stone-500'}`}>
                          {c.twoFactorEnabled ? 'On' : 'Off'}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {filtered.length === 0 && (
                    <tr><td colSpan={5} className="px-6 py-12 text-center text-stone-500">No customers match that search.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </Reveal>
    </div>
  );
}

function Kpi({ icon: Icon, label, value, tone, delay }: { icon: any; label: string; value: string; tone: string; delay: number }) {
  const tones: Record<string, string> = {
    primary: 'bg-primary-50 text-primary-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
  };
  return (
    <Reveal delay={delay}>
      <div className="card-lift rounded-2xl border border-stone-200/80 bg-white p-5">
        <div className="flex items-center justify-between">
          <p className="text-xs font-semibold uppercase tracking-wider text-stone-400">{label}</p>
          <span className={`rounded-xl p-2 ${tones[tone]}`}><Icon className="h-4 w-4" /></span>
        </div>
        <p className="mt-2 text-xl font-extrabold tabular-nums">{value}</p>
      </div>
    </Reveal>
  );
}
