'use client';
import { useState, useEffect } from 'react';
import { auth } from '@/services/api';
import { config } from '@/lib/config';
import { Address } from '@/types';
import toast from 'react-hot-toast';
import { FiEdit2, FiTrash2, FiCheck } from 'react-icons/fi';

export default function AddressesPage() {
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<string | null>(null);
  const [form, setForm] = useState<Address>({ name: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: config.commerce.defaultCountry, phone: '' });

  const load = async () => {
    setLoading(true);
    try { const res = await auth.getAddresses(); setAddresses(res.data || []); } catch {}
    setLoading(false);
  };
  useEffect(() => { load(); }, []);

  const handleSave = async () => {
    try {
      if (editing) {
        await fetch(`${config.api.baseUrl}/user/addresses/${editing}`, {
          method: 'PUT', credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(form),
        });
      } else {
        await auth.addAddress(form);
      }
      toast.success('Address saved');
      setEditing(null); setForm({ name: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: config.commerce.defaultCountry, phone: '' });
      load();
    } catch (e: any) { toast.error(e.message); }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Delete this address?')) return;
    try {
      await fetch(`${config.api.baseUrl}/user/addresses/${id}`, { method: 'DELETE', credentials: 'include' });
      toast.success('Deleted'); load();
    } catch (e: any) { toast.error(e.message); }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-8">Address Book</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-4">
          {loading ? (
            [...Array(2)].map((_, i) => <div key={i} className="bg-white rounded-lg h-32 animate-pulse" />)
          ) : addresses.map(addr => (
            <div key={addr.id} className={`bg-white rounded-lg p-4 border ${addr.isDefault ? 'ring-2 ring-primary-500' : ''}`}>
              <div className="flex justify-between items-start">
                <div>
                  <p className="font-semibold">{addr.name}</p>
                  <p className="text-sm text-gray-600">{addr.line1}</p>
                  {addr.line2 && <p className="text-sm text-gray-600">{addr.line2}</p>}
                  <p className="text-sm text-gray-600">{addr.city}, {addr.state} {addr.postalCode}</p>
                  <p className="text-sm text-gray-600">{addr.country}</p>
                  {addr.phone && <p className="text-sm text-gray-600 mt-1">{addr.phone}</p>}
                  {addr.isDefault && <span className="text-xs bg-primary-100 text-primary-700 px-2 py-1 rounded mt-2 inline-block">Default</span>}
                </div>
                <div className="flex gap-2">
                  <button onClick={() => { setEditing(addr.id!); setForm(addr); }} className="text-gray-400 hover:text-primary-600"><FiEdit2 /></button>
                  <button onClick={() => handleDelete(addr.id!)} className="text-gray-400 hover:text-red-600"><FiTrash2 /></button>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="bg-white rounded-lg p-6 border h-fit">
          <h2 className="text-xl font-bold mb-4">{editing ? 'Edit Address' : 'Add Address'}</h2>
          <div className="space-y-3">
            {[
              { label: 'Name', key: 'name' }, { label: 'Address Line 1', key: 'line1' }, { label: 'Address Line 2', key: 'line2' },
              { label: 'City', key: 'city' }, { label: 'State/County', key: 'state' }, { label: 'Postal Code', key: 'postalCode' },
              { label: 'Phone', key: 'phone' },
            ].map(f => (
              <div key={f.key}>
                <label className="block text-sm font-medium mb-1">{f.label}</label>
                <input type="text" value={(form as any)[f.key] || ''} onChange={e => setForm({ ...form, [f.key]: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2 text-sm" />
              </div>
            ))}
            <div className="flex gap-2">
              <button onClick={handleSave} className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700 flex items-center gap-1">
                <FiCheck /> {editing ? 'Update' : 'Add'} Address
              </button>
              {editing && <button onClick={() => { setEditing(null); setForm({ name: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: config.commerce.defaultCountry, phone: '' }); }} className="border px-4 py-2 rounded-lg text-sm">Cancel</button>}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
