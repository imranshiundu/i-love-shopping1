import Link from 'next/link';

export default function NotFound() {
  return (
    <div className="flex min-h-[70dvh] items-center justify-center px-4 py-24">
      <div className="max-w-md text-center">
        <p className="text-7xl font-extrabold tracking-tight text-primary-600" aria-hidden="true">404</p>
        <h1 className="mt-4 text-2xl font-bold tracking-tight">Page not found</h1>
        <p className="mt-2 text-stone-500">
          The page you're looking for doesn't exist or may have moved. The link could be outdated.
        </p>
        <div className="mt-8 flex items-center justify-center gap-3">
          <Link href="/" className="rounded-xl bg-primary-600 px-6 py-3 font-semibold text-white transition-colors hover:bg-primary-700">
            Back to the store
          </Link>
          <Link href="/products" className="rounded-xl border border-stone-300 px-6 py-3 font-semibold text-stone-700 transition-colors hover:bg-stone-50">
            Browse products
          </Link>
        </div>
      </div>
    </div>
  );
}
