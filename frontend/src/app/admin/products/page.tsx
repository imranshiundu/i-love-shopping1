'use client';
import { useState, useEffect, useCallback } from 'react';
import { admin, products as productsApi, categories as categoriesApi, brands as brandsApi } from '@/services/api';
import { Product, Category, Brand } from '@/types';
import { formatKES } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import { FiPlus, FiEdit2, FiTrash2, FiX, FiTag } from 'react-icons/fi';
import toast from 'react-hot-toast';

interface FormState {
  name: string; description: string; price: string; compareAtPrice: string;
  sku: string; stock: string; categoryId: string; brandId: string; isActive: boolean;
}

const EMPTY_FORM: FormState = {
  name: '', description: '', price: '', compareAtPrice: '',
  sku: '', stock: '', categoryId: '', brandId: '', isActive: true,
};

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [p, c, b] = await Promise.all([
        productsApi.search({ page: '0', size: '100' }),
        categoriesApi.list(),
        brandsApi.list(),
      ]);
      setProducts(p.data?.products || []);
      setCategories(c.data || []);
      setBrands(b.data || []);
    } catch (e: any) { toast.error(e.message); }
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => { setEditingId(null); setForm(EMPTY_FORM); setModalOpen(true); };

  const openEdit = (p: Product) => {
    setEditingId(p.id);
    setForm({
      name: p.name, description: p.description || '', price: String(p.price ?? ''),
      compareAtPrice: p.compareAtPrice ? String(p.compareAtPrice) : '',
      sku: p.sku || '', stock: String(p.stock ?? ''),
      categoryId: p.category?.id || '', brandId: p.brand?.id || '',
      isActive: p.isActive !== false,
    });
    setModalOpen(true);
  };

  const handleSave = async () => {
    if (!form.name || !form.description || !form.price || !form.sku || !form.stock || !form.categoryId || !form.brandId) {
      toast.error('Fill every required field'); return;
    }
    if (form.compareAtPrice && Number(form.compareAtPrice) <= Number(form.price)) {
      toast.error('Compare-at price must be higher than the sale price'); return;
    }
    setSaving(true);
    const payload = {
      name: form.name,
      description: form.description,
      price: Number(form.price),
      compareAtPrice: form.compareAtPrice ? Number(form.compareAtPrice) : null,
      sku: form.sku,
      stock: Number(form.stock),
      categoryId: form.categoryId,
      brandId: form.brandId,
      isActive: form.isActive,
    };
    try {
      if (editingId) { await admin.updateProduct(editingId, payload); toast.success('Product updated'); }
      else { await admin.createProduct(payload); toast.success('Product created'); }
      setModalOpen(false);
      await load();
    } catch (e: any) { toast.error(e.message); }
    setSaving(false);
  };

  const handleDelete = async (p: Product) => {
    if (!confirm(`Delete "${p.name}"? This cannot be undone.`)) return;
    try { await admin.deleteProduct(p.id); toast.success('Product deleted'); await load(); }
    catch (e: any) { toast.error(e.message); }
  };

  const discountedCount = products.filter(p => p.compareAtPrice).length;

  return (
    <div className="space-y-6">
      <Reveal>
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Products</h1>
            <p className="mt-1 text-sm text-stone-500">
              {products.length} in catalogue - {discountedCount} currently discounted
            </p>
          </div>
          <button onClick={openCreate}
            className="flex items-center gap-2 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700">
            <FiPlus /> New product
          </button>
        </div>
      </Reveal>

      <Reveal delay={80}>
        <div className="overflow-hidden rounded-2xl border border-stone-200/80 bg-white">
          {loading ? (
            <div className="space-y-3 p-6">{[...Array(6)].map((_, i) => <div key={i} className="h-14 animate-pulse rounded-xl bg-stone-200/50" />)}</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] text-sm">
                <thead>
                  <tr className="border-b border-stone-100 text-left text-xs uppercase tracking-wider text-stone-400">
                    <th className="px-5 py-3.5 font-semibold">Product</th>
                    <th className="px-5 py-3.5 font-semibold">Category</th>
                    <th className="px-5 py-3.5 font-semibold">Price</th>
                    <th className="px-5 py-3.5 font-semibold">Offer</th>
                    <th className="px-5 py-3.5 font-semibold">Stock</th>
                    <th className="px-5 py-3.5 text-right font-semibold">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map(p => (
                    <tr key={p.id} className="border-b border-stone-50 last:border-0 hover:bg-stone-50/60">
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <span className="h-10 w-10 shrink-0 overflow-hidden rounded-lg bg-stone-100">
                            {p.images?.[0] && <img src={p.images[0].url} alt="" className="h-full w-full object-cover" />}
                          </span>
                          <div className="min-w-0">
                            <p className="max-w-[240px] truncate font-medium">{p.name}</p>
                            <p className="text-xs text-stone-400">{p.sku}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-3.5 text-stone-600">{p.category?.name}</td>
                      <td className="px-5 py-3.5">
                        <span className="font-semibold">{formatKES(p.price)}</span>
                        {p.compareAtPrice && <span className="ml-1.5 text-xs text-stone-400 line-through">{formatKES(p.compareAtPrice)}</span>}
                      </td>
                      <td className="px-5 py-3.5">
                        {p.onSale ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2 py-0.5 text-[11px] font-bold text-rose-700">
                            <FiTag /> -{p.discountPercentage}%
                          </span>
                        ) : <span className="text-xs text-stone-300">-</span>}
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={`font-semibold tabular-nums ${p.stock === 0 ? 'text-rose-600' : p.stock <= 5 ? 'text-amber-600' : ''}`}>
                          {p.stock}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex justify-end gap-1.5">
                          <button onClick={() => openEdit(p)} aria-label={`Edit ${p.name}`}
                            className="rounded-lg p-2 text-stone-400 hover:bg-primary-50 hover:text-primary-700"><FiEdit2 /></button>
                          <button onClick={() => handleDelete(p)} aria-label={`Delete ${p.name}`}
                            className="rounded-lg p-2 text-stone-400 hover:bg-rose-50 hover:text-rose-600"><FiTrash2 /></button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {products.length === 0 && (
                    <tr><td colSpan={6} className="px-6 py-12 text-center text-stone-500">No products yet. Create your first one.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </Reveal>

      {modalOpen && (
        <div className="fixed inset-0 z-[70] flex items-center justify-center overflow-y-auto bg-stone-950/60 p-4 backdrop-blur-sm" onClick={() => setModalOpen(false)}>
          <div className="my-8 w-full max-w-2xl rounded-2xl bg-white shadow-2xl" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between border-b border-stone-100 px-6 py-4">
              <h2 className="text-lg font-bold">{editingId ? 'Edit product' : 'New product'}</h2>
              <button onClick={() => setModalOpen(false)} className="rounded-lg p-2 hover:bg-stone-100" aria-label="Close"><FiX /></button>
            </div>

            <div className="grid grid-cols-1 gap-4 px-6 py-5 sm:grid-cols-2">
              <Field label="Name *" span><input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} className={inputCls} /></Field>
              <Field label="Description *" span>
                <textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} rows={3} className={`${inputCls} resize-none`} />
              </Field>
              <Field label="Selling price (KES) *">
                <input type="number" step="0.01" value={form.price} onChange={e => setForm({ ...form, price: e.target.value })} className={inputCls} />
              </Field>
              <Field label="Compare-at price" hint="Higher than selling price creates an offer">
                <input type="number" step="0.01" value={form.compareAtPrice} onChange={e => setForm({ ...form, compareAtPrice: e.target.value })} className={inputCls} placeholder="e.g. 1499" />
              </Field>
              <Field label="SKU *"><input value={form.sku} onChange={e => setForm({ ...form, sku: e.target.value })} className={inputCls} /></Field>
              <Field label="Stock *"><input type="number" value={form.stock} onChange={e => setForm({ ...form, stock: e.target.value })} className={inputCls} /></Field>
              <Field label="Category *">
                <select value={form.categoryId} onChange={e => setForm({ ...form, categoryId: e.target.value })} className={inputCls}>
                  <option value="">Select...</option>
                  {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </Field>
              <Field label="Brand *">
                <select value={form.brandId} onChange={e => setForm({ ...form, brandId: e.target.value })} className={inputCls}>
                  <option value="">Select...</option>
                  {brands.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
                </select>
              </Field>
              <label className="flex cursor-pointer items-center gap-2.5 sm:col-span-2">
                <input type="checkbox" checked={form.isActive} onChange={e => setForm({ ...form, isActive: e.target.checked })} className="h-4 w-4 accent-primary-600" />
                <span className="text-sm font-medium">Visible in the storefront</span>
              </label>
            </div>

            <div className="flex justify-end gap-3 border-t border-stone-100 px-6 py-4">
              <button onClick={() => setModalOpen(false)} className="rounded-xl border px-5 py-2.5 text-sm font-semibold hover:bg-stone-50">Cancel</button>
              <button onClick={handleSave} disabled={saving}
                className="rounded-xl bg-primary-600 px-6 py-2.5 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-60">
                {saving ? 'Saving...' : editingId ? 'Save changes' : 'Create product'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const inputCls = 'w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-100';

function Field({ label, hint, span, children }: { label: string; hint?: string; span?: boolean; children: React.ReactNode }) {
  return (
    <div className={span ? 'sm:col-span-2' : ''}>
      <label className="mb-1 block text-sm font-medium text-stone-700">{label}</label>
      {children}
      {hint && <p className="mt-1 text-xs text-stone-400">{hint}</p>}
    </div>
  );
}
