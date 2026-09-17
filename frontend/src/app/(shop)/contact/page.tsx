'use client';
import { useState } from 'react';
import { contact } from '@/services/api';
import { useAuth } from '@/contexts/AuthContext';
import Reveal from '@/components/ui/Reveal';
import { FiMail, FiMapPin, FiSend, FiClock } from 'react-icons/fi';
import toast from 'react-hot-toast';

export default function ContactPage() {
  const { user } = useAuth();
  const [form, setForm] = useState({ name: '', email: '', subject: '', message: '' });
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  const fieldCls =
    'w-full rounded-xl border border-stone-300 bg-stone-50/50 px-4 py-2.5 text-sm transition-all placeholder:text-stone-400 focus:border-primary-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100';

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSending(true);
    try {
      await contact.send(form.name.trim(), form.email.trim(), form.subject.trim(), form.message.trim());
      setSent(true);
    } catch (err: any) {
      toast.error(err?.message || 'Could not send your message. Please try again.');
    }
    setSending(false);
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      <Reveal>
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Client care</p>
        <h1 className="mt-1.5 text-3xl font-bold tracking-tight sm:text-4xl">Get in touch</h1>
        <p className="mt-2 max-w-2xl text-stone-500">
          Questions about an order, a product, or anything else? Send us a message — the team replies within one business day.
        </p>
      </Reveal>

      <div className="mt-10 grid gap-8 lg:grid-cols-[1fr_1.4fr]">
        <Reveal delay={80}>
          <div className="space-y-4">
            <div className="rounded-2xl border border-stone-200/80 bg-white p-5">
              <h2 className="flex items-center gap-2.5 font-bold"><FiMail className="text-primary-600" /> Email</h2>
              <p className="mt-2 text-sm text-stone-500">{`support@iloveshopping.com`}</p>
            </div>
            <div className="rounded-2xl border border-stone-200/80 bg-white p-5">
              <h2 className="flex items-center gap-2.5 font-bold"><FiMapPin className="text-primary-600" /> Location</h2>
              <p className="mt-2 text-sm text-stone-500">Nairobi, Kenya</p>
            </div>
            <div className="rounded-2xl border border-stone-200/80 bg-white p-5">
              <h2 className="flex items-center gap-2.5 font-bold"><FiClock className="text-primary-600" /> Hours</h2>
              <p className="mt-2 text-sm text-stone-500">Mon&ndash;Fri, 9am&ndash;5pm EAT</p>
            </div>
          </div>
        </Reveal>

        <Reveal delay={140}>
          {sent ? (
            <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-8 text-center">
              <h2 className="text-xl font-bold text-emerald-800">Message sent</h2>
              <p className="mt-2 text-sm text-emerald-700">Thanks for reaching out — we'll reply to {form.email} within one business day.</p>
              <button onClick={() => { setSent(false); setForm({ name: '', email: '', subject: '', message: '' }); }}
                className="mt-6 rounded-xl bg-emerald-600 px-6 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700">
                Send another message
              </button>
            </div>
          ) : (
            <form onSubmit={submit} className="rounded-2xl border border-stone-200/80 bg-white p-6 sm:p-7">
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="contact-name" className="mb-1.5 block text-sm font-medium text-stone-700">Full name</label>
                  <input id="contact-name" type="text" required value={form.name}
                    onChange={e => setForm({ ...form, name: e.target.value })} placeholder="Amina Wanjiru"
                    autoComplete="name" className={fieldCls} />
                </div>
                <div>
                  <label htmlFor="contact-email" className="mb-1.5 block text-sm font-medium text-stone-700">Email address</label>
                  <input id="contact-email" type="email" required value={form.email}
                    onChange={e => setForm({ ...form, email: e.target.value })}
                    placeholder="you@example.com" autoComplete="email"
                    className={fieldCls} />
                </div>
              </div>
              <div className="mt-4">
                <label htmlFor="contact-subject" className="mb-1.5 block text-sm font-medium text-stone-700">Subject <span className="text-stone-400">(optional)</span></label>
                <input id="contact-subject" type="text" value={form.subject}
                  onChange={e => setForm({ ...form, subject: e.target.value })}
                  placeholder="Order #ILS-... question" className={fieldCls} />
              </div>
              <div className="mt-4">
                <label htmlFor="contact-message" className="mb-1.5 block text-sm font-medium text-stone-700">Message</label>
                <textarea id="contact-message" required rows={6} value={form.message}
                  onChange={e => setForm({ ...form, message: e.target.value })}
                  placeholder="How can we help?" className={fieldCls + ' resize-none'} />
              </div>
              <button type="submit" disabled={sending}
                className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 py-3.5 font-semibold text-white shadow-lg shadow-primary-600/25 transition-all hover:-translate-y-0.5 hover:bg-primary-700 disabled:opacity-60 disabled:hover:translate-y-0">
                {sending ? 'Sending...' : <>Send message <FiSend /></>}
              </button>
            </form>
          )}
        </Reveal>
      </div>
    </div>
  );
}
