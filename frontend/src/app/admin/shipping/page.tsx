'use client';
import { useState, useEffect, useCallback } from 'react';
import { admin } from '@/services/api';
import { formatKES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import { FiTruck, FiPlus, FiEdit2, FiTrash2, FiX } from 'react-icons/fi';
import toast from 'react-hot-toast';

interface ShippingMethod {
  id: string; name: string; description?: string; cost: number;
  estimatedDays?: string; active: boolean; displayOrder: number;
}

const EMPTY = { name: '', description: '', cost: '10', estimatedDays: '', active: true, displayOrder: '0' };

export default function AdminShippingPage() {
  const [methods, setMethods] = useState<ShippingMethod[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<typeof EMPTY>(EMPTY);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await admin.listShippingMethods();
      setMethods(res.data || []);
    } catch (e: any) { toast.error(e.message); }
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setEditingId(null); setForm(EMPTY); setModalOpen(true); };

  const openEdit = (m: ShippingMethod) => {
    setEditingId(m.id);
    setForm({
      name: m.name, description: m.description || '', cost: String(m.cost),
      estimatedDays: m.estimatedDays || '', active: m.active,
      displayOrder: String(m.displayOrder ?? 0),
    });
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.name.trim()) { toast.error('Name is required'); return; }
    const cost = Number(form.cost);
    if (isNaN(cost) || cost < 0) { toast.error('Cost must be zero or more'); return; }
    setSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        cost,
        estimatedDays: form.estimatedDays.trim() || undefined,
        active: form.active,
        displayOrder: Number(form.displayOrder) || 0,
      };
      if (editingId) {
        await admin.saveShippingMethod({ ...payload, id: editingId });
      } else {
        await admin.createShippingMethod(payload);
      }
      toast.success(editingId ? 'Delivery option updated' : 'Delivery option created');
      setModalOpen(false);
      await load();
    } catch (e: any) {
      toast.error(e.message || 'Could not save');
    }
    setSaving(false);
  };

  const remove = async (m: ShippingMethod) => {
    if (!confirm(`Delete "${m.name}"?`)) return;
    try {
      await admin.deleteShippingMethod(m.id);
      toast.success('Deleted');
      await load();
    } catch (e: any) { toast.error(e.message || 'Could not delete'); }
  };

  return (
    <div className="space-y-6">
      <Reveal>
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Delivery options</h1>
            <p className="mt-1 text-sm text-stone-500">What shoppers choose at checkout. Orders over the free-shipping threshold are always free.</p>
          </div>
          <button onClick={openCreate}
            className="flex items-center gap-2 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700">
            <FiPlus /> New option
          </button>
        </div>
      </Reveal>

      <Reveal delay={80}>
        <div className="overflow-hidden rounded-2xl border border-stone-200/80 bg-white">
          {loading ? (
            <div className="space-y-3 p-6">{[...Array(3)].map((_, i) => <div key={i} className="h-14 animate-pulse rounded-xl bg-stone-200/50" />)}</div>
          ) : methods.length === 0 ? (
            <p className="px-6 py-12 text-center text-stone-500">No delivery options yet. Create your first one.</p>
          ) : (
            <table className="w-full min-w-[680px] text-sm">
              <thead>
                <tr className="border-b border-stone-200 bg-stone-50/60 text-left text-xs font-bold uppercase tracking-wider text-stone-500">
                  <th className="px-5 py-3">Option</th>
                  <th className="px-5 py-3">Cost</th>
                  <th className="px-5 py-3">Estimate</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {methods.map(m => (
                  <tr key={m.id} className="border-b border-stone-100 last:border-0">
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2.5">
                        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-50 text-primary-600"><FiTruck /></span>
                        <div>
                          <p className="font-semibold">{m.name}</p>
                          {m.description && <p className="text-xs text-stone-400">{m.description}</p>}
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 font-semibold tabular-nums">{m.cost === 0 ? 'Free' : formatKES(m.cost)}</td>
                    <td className="px-5 py-3.5 text-stone-500">{m.estimatedDays || '-'}</td>
                    <td className="px-5 py-3.5">
                      <span className={`rounded-full px-2.5 py-0.5 text-[11px] font-bold ${m.active ? 'bg-emerald-100 text-emerald-700' : 'bg-stone-200 text-stone-500'}`}>
                        {m.active ? 'Active' : 'Hidden'}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex justify-end gap-1.5">
                        <button onClick={() => openEdit(m)} aria-label={`Edit ${m.name}`}
                          className="rounded-lg p-2 text-stone-400 hover:bg-primary-50 hover:text-primary-700"><FiEdit2 /></button>
                        <button onClick={() => remove(m)} aria-label={`Delete ${m.name}`}
                          className="rounded-lg p-2 text-stone-400 hover:bg-rose-50 hover:text-rose-600"><FiTrash2 /></button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </Reveal>

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setModalOpen(false)} />
          <div className="relative z-10 w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl sm:p-7">
            <button onClick={() => setModalOpen(false)} aria-label="Close"
              className="absolute right-4 top-4 rounded-full p-1 text-stone-400 hover:bg-stone-100 hover:text-stone-600"><FiX className="h-5 w-5" /></button>
            <h2 className="text-xl font-bold">{editingId ? 'Edit delivery option' : 'New delivery option'}</h2>
            <div className="mt-5 space-y-4">
              <div>
                <label htmlFor="sm-name" className="mb-1.5 block text-sm font-medium text-stone-700">Name</label>
                <input id="sm-name" type="text" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}
                  placeholder="Standard delivery" className="w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-4 focus:ring-primary-100" />
              </div>
              <div>
                <label htmlFor="sm-desc" className="mb-1.5 block text-sm font-medium text-stone-700">Description</label>
                <input id="sm-desc" type="text" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })}
                  placeholder="Nationwide courier, tracked" className="w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-4 focus:ring-primary-100" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label htmlFor="sm-cost" className="mb-1.5 block text-sm font-medium text-stone-700">Cost (KES)</label>
                  <input id="sm-cost" type="number" min="0" step="0.01" value={form.cost} onChange={e => setForm({ ...form, cost: e.target.value })}
                    className="w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-4 focus:ring-primary-100" />
                </div>
                <div>
                  <label htmlFor="sm-days" className="mb-1.5 block text-sm font-medium text-stone-700">Estimate</label>
                  <input id="sm-days" type="text" value={form.estimatedDays} onChange={e => setForm({ ...form, estimatedDays: e.target.value })}
                    placeholder="2-3 business days" className="w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-4 focus:ring-primary-100" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label htmlFor="sm-order" className="mb-1.5 block text-sm font-medium text-stone-700">Display order</label>
                  <input id="sm-order" type="number" min="0" value={form.displayOrder} onChange={e => setForm({ ...form, displayOrder: e.target.value })}
                    className="w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-4 focus:ring-primary-100" />
                </div>
                <label className="flex cursor-pointer items-end gap-2 pb-2.5 text-sm font-medium text-stone-700">
                  <input type="checkbox" checked={form.active} onChange={e => setForm({ ...form, active: e.target.checked })}
                    className="h-4 w-4 rounded border-stone-300 text-primary-600 focus:ring-primary-300" />
                  Active
                </label>
              </div>
            </div>
            <div className="mt-6 flex items-center gap-3">
              <button onClick={save} disabled={saving}
                className="flex-1 rounded-xl bg-primary-600 py-3 font-semibold text-white shadow-lg shadow-primary-600/25 transition-colors hover:bg-primary-700 disabled:opacity-60">
                {saving ? 'Saving...' : editingId ? 'Save changes' : 'Create option'}
              </button>
              <button onClick={() => setModalOpen(false)} className="rounded-xl px-4 py-3 text-sm font-semibold text-stone-500 hover:text-stone-700">
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
