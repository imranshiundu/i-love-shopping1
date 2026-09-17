import type { Metadata } from 'next';
import Link from 'next/link';
import Reveal from '@/components/ui/Reveal';
import { FiInstagram, FiTwitter, FiFacebook, FiTruck, FiShield, FiHeart, FiGlobe } from 'react-icons/fi';

export const metadata: Metadata = {
  title: 'About us - i-love-shopping',
  description: 'Considered goods from independent makers, delivered across Kenya. Our mission, team and story.',
};

const VALUES = [
  { icon: FiHeart, title: 'Made with care', text: 'Every product comes from an independent maker we know by name.' },
  { icon: FiTruck, title: 'Delivered fast', text: 'Nationwide courier coverage with tracked, reliable delivery.' },
  { icon: FiShield, title: 'Shop safely', text: 'Encrypted end to end, with card and mobile money payments you trust.' },
  { icon: FiGlobe, title: 'Kenyan first', text: 'Built in Nairobi for the Kenyan market — prices in KES, M-Pesa included.' },
];

const TEAM = [
  { initials: 'IS', name: 'Imran Shiundu', role: 'Founder & Engineering' },
  { initials: 'AT', name: 'The Atelier', role: 'Maker partnerships' },
  { initials: 'CC', name: 'Client Care', role: 'Support & delivery' },
];

export default function AboutPage() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      <Reveal>
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-600">Our story</p>
        <h1 className="mt-1.5 text-3xl font-bold tracking-tight sm:text-4xl">Considered goods, delivered with care</h1>
        <p className="mt-4 max-w-3xl text-lg leading-relaxed text-stone-600">
          i-love-shopping started with a simple idea: shopping for well-made things should be as enjoyable as using them.
          We connect independent makers with customers across Kenya, handling everything from discovery to doorstep —
          securely, quickly, and in KES.
        </p>
      </Reveal>

      <Reveal delay={80}>
        <section className="mt-12">
          <h2 className="text-2xl font-bold tracking-tight">Mission</h2>
          <p className="mt-3 max-w-3xl leading-relaxed text-stone-600">
            To make independent commerce effortless in Kenya: a platform where makers reach the right customers,
            customers discover products they love, and every transaction is safe from checkout to delivery.
          </p>
        </section>
      </Reveal>

      <Reveal delay={120}>
        <section className="mt-12">
          <h2 className="text-2xl font-bold tracking-tight">What we stand for</h2>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {VALUES.map(v => (
              <article key={v.title} className="rounded-2xl border border-stone-200/80 bg-white p-6">
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-50 text-primary-600">
                  <v.icon className="h-5 w-5" />
                </span>
                <h3 className="mt-4 font-semibold">{v.title}</h3>
                <p className="mt-1.5 text-sm leading-relaxed text-stone-500">{v.text}</p>
              </article>
            ))}
          </div>
        </section>
      </Reveal>

      <Reveal delay={160}>
        <section className="mt-12">
          <h2 className="text-2xl font-bold tracking-tight">The team</h2>
          <div className="mt-6 grid gap-4 sm:grid-cols-3">
            {TEAM.map(member => (
              <article key={member.name} className="rounded-2xl border border-stone-200/80 bg-white p-6 text-center">
                <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary-100 text-lg font-bold text-primary-700">
                  {member.initials}
                </span>
                <h3 className="mt-4 font-semibold">{member.name}</h3>
                <p className="mt-1 text-sm text-stone-500">{member.role}</p>
              </article>
            ))}
          </div>
        </section>
      </Reveal>

      <Reveal delay={200}>
        <section className="mt-12 rounded-2xl bg-stone-950 p-8 text-center sm:p-12">
          <h2 className="text-2xl font-bold tracking-tight text-white">Ready to shop?</h2>
          <p className="mx-auto mt-2 max-w-xl text-stone-400">
            Browse the catalogue, fill your cart, and pay with M-Pesa or card — with free delivery on larger orders.
          </p>
          <Link href="/products" className="mt-6 inline-block rounded-xl bg-primary-600 px-8 py-3.5 font-semibold text-white transition-all hover:-translate-y-0.5 hover:bg-primary-700">
            Browse all products
          </Link>
          <div className="mt-8 flex items-center justify-center gap-4 text-stone-400">
            <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" aria-label="Instagram"
              className="flex h-10 w-10 items-center justify-center rounded-full border border-white/10 transition-colors hover:border-primary-500 hover:text-primary-400">
              <FiInstagram className="h-4 w-4" />
            </a>
            <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" aria-label="Twitter"
              className="flex h-10 w-10 items-center justify-center rounded-full border border-white/10 transition-colors hover:border-primary-500 hover:text-primary-400">
              <FiTwitter className="h-4 w-4" />
            </a>
            <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" aria-label="Facebook"
              className="flex h-10 w-10 items-center justify-center rounded-full border border-white/10 transition-colors hover:border-primary-500 hover:text-primary-400">
              <FiFacebook className="h-4 w-4" />
            </a>
          </div>
        </section>
      </Reveal>
    </div>
  );
}
