'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { admin, categories as categoriesApi } from '@/services/api';
import { Category } from '@/types';
import toast from 'react-hot-toast';
import { FiPlus, FiTrash2, FiEdit2 } from 'react-icons/fi';

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState({ name: '', description: '' });

  const load = async () => {
    setLoading(true);
    try { const res = await categoriesApi.list(); setCategories(res.data || []); }
    catch { toast.error('Failed'); }
    setLoading(false);
  };
  useEffect(() => { load(); }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingId) { await admin.updateCategory(editingId, form); toast.success('Updated'); }
      else { await admin.createCategory(form); toast.success('Created'); }
      setShowForm(false); setEditingId(null); setForm({ name: '', description: '' }); load();
    } catch (e: any) { toast.error(e.message); }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Delete?')) return;
    try { await admin.deleteCategory(id); toast.success('Deleted'); load(); }
    catch (e: any) { toast.error(e.message); }
  };

  const startEdit = (cat: Category) => { setEditingId(cat.id); setForm({ name: cat.name, description: cat.description || '' }); setShowForm(true); };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold">Manage Categories</h1>
        <div className="flex gap-3">
          <Link href="/admin" className="text-primary-600 hover:underline text-sm">&larr; Dashboard</Link>
          <button onClick={() => { setShowForm(!showForm); setEditingId(null); setForm({ name: '', description: '' }); }}
            className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700 flex items-center gap-1">
            <FiPlus /> Add Category
          </button>
        </div>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-lg p-6 border mb-6 space-y-4">
          <div><label className="block text-sm font-medium mb-1">Name *</label><input type="text" value={form.name} onChange={e => setForm({...form, name: e.target.value})} required className="w-full border rounded-lg px-3 py-2 text-sm" /></div>
          <div><label className="block text-sm font-medium mb-1">Description</label><textarea value={form.description} onChange={e => setForm({...form, description: e.target.value})} rows={2} className="w-full border rounded-lg px-3 py-2 text-sm" /></div>
          <div className="flex gap-2">
            <button type="submit" className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700">{editingId ? 'Update' : 'Create'}</button>
            <button type="button" onClick={() => { setShowForm(false); setEditingId(null); }} className="border px-4 py-2 rounded-lg text-sm">Cancel</button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="bg-white rounded-lg h-12 animate-pulse" />)}</div>
      ) : (
        <div className="bg-white rounded-lg border divide-y">
          {categories.map(cat => (
            <div key={cat.id} className="px-4 py-3 flex items-center justify-between">
              <div>
                <p className="font-medium">{cat.name}</p>
                <p className="text-xs text-gray-500">{cat.productCount || 0} products</p>
              </div>
              <div className="flex gap-2">
                <button onClick={() => startEdit(cat)} className="text-gray-400 hover:text-primary-600"><FiEdit2 /></button>
                <button onClick={() => handleDelete(cat.id)} className="text-gray-400 hover:text-red-600"><FiTrash2 /></button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
