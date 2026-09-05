import Link from 'next/link';
import { config } from '@/lib/config';
import { FiInstagram, FiTwitter, FiFacebook, FiMail, FiMapPin } from 'react-icons/fi';

export default function Footer() {
  const year = new Date().getFullYear();

  return (
    <footer className="bg-stone-950 text-stone-400">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-10 border-b border-white/5 py-14 md:grid-cols-4 lg:gap-8">
          <div className="col-span-2 md:col-span-1">
            <p className="text-xl font-extrabold tracking-tight text-white">
              {config.app.name}<span className="text-primary-500">.</span>
            </p>
            <p className="mt-4 max-w-[34ch] text-sm leading-relaxed">
              Considered goods from independent makers - delivered across Kenya with care.
            </p>
            <div className="mt-6 flex gap-3">
              {[FiInstagram, FiTwitter, FiFacebook].map((Icon, i) => (
                <span key={i} className="flex h-9 w-9 cursor-pointer items-center justify-center rounded-full border border-white/10 transition-colors hover:border-primary-500 hover:text-primary-400">
                  <Icon className="h-4 w-4" />
                </span>
              ))}
            </div>
          </div>

          <div>
            <h3 className="text-xs font-bold uppercase tracking-[0.18em] text-stone-200">Shop</h3>
            <ul className="mt-5 space-y-3 text-sm">
              <li><Link href="/products" className="transition-colors hover:text-white">All products</Link></li>
              <li><Link href="/products?onSaleOnly=true" className="transition-colors hover:text-white">Offers</Link></li>
              <li><Link href="/products?sortBy=newest" className="transition-colors hover:text-white">New arrivals</Link></li>
              <li><Link href="/products?sortBy=rating" className="transition-colors hover:text-white">Top rated</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="text-xs font-bold uppercase tracking-[0.18em] text-stone-200">Account</h3>
            <ul className="mt-5 space-y-3 text-sm">
              <li><Link href="/account" className="transition-colors hover:text-white">My profile</Link></li>
              <li><Link href="/account/orders" className="transition-colors hover:text-white">Order history</Link></li>
              <li><Link href="/account/addresses" className="transition-colors hover:text-white">Address book</Link></li>
              <li><Link href="/cart" className="transition-colors hover:text-white">Cart</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="text-xs font-bold uppercase tracking-[0.18em] text-stone-200">Client care</h3>
            <ul className="mt-5 space-y-3 text-sm">
              <li className="flex items-start gap-2.5">
                <FiMail className="mt-0.5 h-4 w-4 shrink-0 text-stone-600" />
                {config.app.supportEmail}
              </li>
              <li className="flex items-start gap-2.5">
                <FiMapPin className="mt-0.5 h-4 w-4 shrink-0 text-stone-600" />
                {config.app.companyLocation}
              </li>
            </ul>
          </div>
        </div>

        <div className="flex flex-col items-center justify-between gap-5 border-b border-white/5 py-7 sm:flex-row">
          <p className="text-xs uppercase tracking-[0.18em] text-stone-500">Secure checkout with</p>
          <div className="flex flex-wrap items-center justify-center gap-2">
            {['M-PESA', 'VISA', 'Mastercard', 'Stripe'].map(method => (
              <span key={method} className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-[11px] font-semibold tracking-wide text-stone-300">
                {method}
              </span>
            ))}
          </div>
        </div>

        <div className="flex flex-col items-center justify-between gap-3 py-6 text-xs text-stone-600 sm:flex-row">
          <p>&copy; {year} {config.app.name}. All rights reserved.</p>
          <p>Crafted in Nairobi. Prices include VAT where applicable.</p>
        </div>
      </div>
    </footer>
  );
}
