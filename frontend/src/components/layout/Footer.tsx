import Link from 'next/link';
import { config } from '@/lib/config';

export default function Footer() {
  return (
    <footer className="bg-gray-900 text-gray-300 mt-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div>
            <h3 className="text-white text-lg font-bold mb-4">{config.app.name}</h3>
            <p className="text-sm">Your one-stop e-commerce platform for quality products at great prices. Shop with confidence.</p>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Shop</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="/products" className="hover:text-white">All Products</Link></li>
              <li><Link href="/products?onSaleOnly=true" className="hover:text-white">On Sale</Link></li>
              <li><Link href="/products?sortBy=newest" className="hover:text-white">New Arrivals</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Account</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="/account" className="hover:text-white">My Profile</Link></li>
              <li><Link href="/account/orders" className="hover:text-white">Order History</Link></li>
              <li><Link href="/account/addresses" className="hover:text-white">Address Book</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Help</h4>
            <ul className="space-y-2 text-sm">
              <li><span>Contact: {config.app.supportEmail}</span></li>
              <li><span>{config.app.companyLocation}</span></li>
            </ul>
          </div>
        </div>
        <div className="border-t border-gray-800 mt-8 pt-8 text-center text-sm">
          <p>&copy; {new Date().getFullYear()} {config.app.name}. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}
