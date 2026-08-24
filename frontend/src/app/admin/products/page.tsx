'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { admin } from '@/services/api';
import { Product } from '@/types';
import { formatKES } from '@/lib/utils';
import toast from 'react-hot-toast';
import { FiPlus, FiTrash2 } from 'react-icons/fi';

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: '', description: '', price: '', compareAtPrice: '', sku: '', stock: '', weight: '', categoryId: '', brandId: '' });

  const load = async () => {
    setLoading(true);
    try { const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/products?page=0&size=100`, { credentials: 'include' }); const data = await res.json(); setProducts(data.data?.products || []); }
    catch { toast.error('Failed to load products'); }
    setLoading(false);
  };
  useEffect(() => { load(); }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await admin.createProduct({
        name: form.name, description: form.description, price: parseFloat(form.price),
        compareAtPrice: form.compareAtPrice ? parseFloat(form.compareAtPrice) : null,
        sku: form.sku, stock: parseInt(form.stock), weight: parseFloat(form.weight) || 0,
        categoryId: form.categoryId || undefined, brandId: form.brandId || undefined,
      });
      toast.success('Product created'); setShowForm(false);
      setForm({ name: '', description: '', price: '', compareAtPrice: '', sku: '', stock: '', weight: '', categoryId: '', brandId: '' });
      load();
    } catch (e: any) { toast.error(e.message); }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Delete this product?')) return;
    try { await admin.deleteProduct(id); toast.success('Deleted'); load(); }
    catch (e: any) { toast.error(e.message); }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold">Manage Products</h1>
        <div className="flex gap-3">
          <Link href="/admin" className="text-primary-600 hover:underline text-sm">&larr; Dashboard</Link>
          <button onClick={() => setShowForm(!showForm)} className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700 flex items-center gap-1">
            <FiPlus /> Add Product
          </button>
        </div>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="bg-white rounded-lg p-6 border mb-6 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2"><label className="block text-sm font-medium mb-1">Name *</label><input type="text" value={form.name} onChange={e => setForm({...form, name: e.target.value})} required className="w-full border rounded-lg px-3 py-2 text-sm" /></div>
            <div className="sm:col-span-2"><label className="block text-sm font-medium mb-1">Description</label><textarea value={form.description} onChange={e => setForm({...form, description: e.target.value})} rows={3} className="w-full border rounded-lg px-3 py-2 text-sm" /></div>
            <div><label className="block text-sm font-medium mb-1">Price (KES) *</label><input type="number" value={form.price} onChange={e => setForm({...form, price: e.target.value})} required step="0.01" className="w-full border rounded-lg px-3 py-2 text-sm" /></div>
            <div><label className="block text-sm font-medium mb-1">Compare Price</label><input type="number" value={form.compareAtPrice} onChange={e => setForm({...form, compareAtPrice: e.target.value})} step="0.01" className="w-full border rounded-lg px-3 py-2 text-sm" /></div>
            <div><label className="block text-sm font-medium mb-1">SKU *</label><input type="text" value={form.sku} onChange={e => setForm({...form, sku: e.target.value})} required className="w-full border rounded-lg px-3 py-2 text-sm" /></div>
            <div><label className="block text-sm font-medium mb-1">Stock *</label><input type="number" value={form.stock} onChange={e => setForm({...form, stock: e.target.value})} required className="w-full border rounded-lg px-3 py-2 text-sm" /></div>
          </div>
          <div className="flex gap-2">
            <button type="submit" className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700">Create Product</button>
            <button type="button" onClick={() => setShowForm(false)} className="border px-4 py-2 rounded-lg text-sm">Cancel</button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="space-y-4">{[...Array(5)].map((_, i) => <div key={i} className="bg-white rounded-lg h-16 animate-pulse" />)}</div>
      ) : (
        <div className="bg-white rounded-lg border overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b"><tr>
              <th className="px-4 py-3 text-left">Product</th><th className="px-4 py-3 text-left">SKU</th>
              <th className="px-4 py-3 text-left">Price</th><th className="px-4 py-3 text-left">Stock</th>
              <th className="px-4 py-3 text-left">Actions</th>
            </tr></thead>
            <tbody className="divide-y">
              {products.map(p => (
                <tr key={p.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3"><p className="font-medium">{p.name}</p><p className="text-xs text-gray-500">{p.brand?.name}</p></td>
                  <td className="px-4 py-3 text-gray-500">{p.sku}</td>
                  <td className="px-4 py-3 font-medium">{formatKES(p.price)}</td>
                  <td className="px-4 py-3"><span className={p.stock > 0 ? 'text-green-600' : 'text-red-600'}>{p.stock}</span></td>
                  <td className="px-4 py-3"><button onClick={() => handleDelete(p.id)} className="text-red-500 hover:text-red-700"><FiTrash2 /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
