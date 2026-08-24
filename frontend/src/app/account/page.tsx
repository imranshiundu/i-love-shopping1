'use client';
import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { auth } from '@/services/api';
import { formatDate } from '@/lib/utils';
import toast from 'react-hot-toast';
import Link from 'next/link';

export default function AccountPage() {
  const { user, refreshUser } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [changingPw, setChangingPw] = useState(false);

  useEffect(() => { if (user) { setName(user.name); setEmail(user.email); } }, [user]);

  const handleProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try { await auth.updateProfile({ name, email }); await refreshUser(); setEditing(false); toast.success('Profile updated'); }
    catch (e: any) { toast.error(e.message); }
    setLoading(false);
  };

  const handlePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < 8) { toast.error('Password must be at least 8 characters'); return; }
    setChangingPw(true);
    try {
      await fetch(`${process.env.NEXT_PUBLIC_API_URL}/user/password`, {
        method: 'POST', credentials: 'include',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${localStorage.getItem('token') || ''}` },
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      setCurrentPassword(''); setNewPassword(''); toast.success('Password changed');
    } catch (e: any) { toast.error(e.message || 'Failed'); }
    setChangingPw(false);
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-8">My Account</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="md:col-span-2 space-y-6">
          <div className="bg-white rounded-lg p-6 border">
            <h2 className="text-xl font-bold mb-4">Profile</h2>
            <form onSubmit={handleProfile} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Name</label>
                <input type="text" value={name} onChange={e => setName(e.target.value)} disabled={!editing}
                  className="w-full border rounded-lg px-3 py-2 disabled:bg-gray-50" />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Email</label>
                <input type="email" value={email} onChange={e => setEmail(e.target.value)} disabled={!editing}
                  className="w-full border rounded-lg px-3 py-2 disabled:bg-gray-50" />
              </div>
              <div className="flex gap-2">
                {editing ? (
                  <>
                    <button type="submit" disabled={loading} className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700 disabled:opacity-50">Save</button>
                    <button type="button" onClick={() => { setEditing(false); if (user) { setName(user.name); setEmail(user.email); } }} className="border px-4 py-2 rounded-lg text-sm">Cancel</button>
                  </>
                ) : (
                  <button type="button" onClick={() => setEditing(true)} className="border px-4 py-2 rounded-lg text-sm">Edit Profile</button>
                )}
              </div>
            </form>
          </div>

          <div className="bg-white rounded-lg p-6 border">
            <h2 className="text-xl font-bold mb-4">Change Password</h2>
            <form onSubmit={handlePassword} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Current Password</label>
                <input type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required className="w-full border rounded-lg px-3 py-2" />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">New Password</label>
                <input type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} required minLength={8} className="w-full border rounded-lg px-3 py-2" />
              </div>
              <button type="submit" disabled={changingPw} className="bg-primary-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-primary-700 disabled:opacity-50">
                {changingPw ? 'Changing...' : 'Change Password'}
              </button>
            </form>
          </div>
        </div>

        <div className="space-y-4">
          <div className="bg-white rounded-lg p-6 border">
            <h3 className="font-bold mb-2">Quick Links</h3>
            <nav className="space-y-2 text-sm">
              <Link href="/account/orders" className="block hover:text-primary-600">Order History</Link>
              <Link href="/account/addresses" className="block hover:text-primary-600">Address Book</Link>
              {user?.roles?.includes('ADMIN') && <Link href="/admin" className="block text-primary-600 font-medium">Admin Dashboard</Link>}
            </nav>
          </div>
          <div className="bg-white rounded-lg p-6 border text-sm text-gray-500">
            <p>Member since {user?.createdAt ? formatDate(user.createdAt) : 'N/A'}</p>
            <p className="mt-1">Email verified: {user?.emailVerified ? 'Yes' : 'No'}</p>
            <p className="mt-1">2FA: {user?.twoFactorEnabled ? 'Enabled' : 'Disabled'}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
