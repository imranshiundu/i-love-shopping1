'use client';
import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { admin } from '@/services/api';
import { Review } from '@/types';
import { formatDate } from '@/lib/utils';
import Reveal from '@/components/ui/Reveal';
import { FiStar, FiThumbsUp, FiCheckCircle, FiXCircle, FiClock, FiShield } from 'react-icons/fi';
import toast from 'react-hot-toast';

const STATUS_FILTERS = [
  { id: 'PENDING', label: 'Pending', icon: FiClock },
  { id: 'APPROVED', label: 'Approved', icon: FiCheckCircle },
  { id: 'REJECTED', label: 'Rejected', icon: FiXCircle },
  { id: 'all', label: 'All', icon: FiShield },
] as const;

const statusBadge: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-700',
  APPROVED: 'bg-emerald-100 text-emerald-700',
  REJECTED: 'bg-rose-100 text-rose-700',
};

export default function AdminReviewsPage() {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [status, setStatus] = useState<string>('PENDING');
  const [page, setPage] = useState(0);
  const [pagination, setPagination] = useState<{ totalPages: number; totalElements: number }>({ totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await admin.listReviews(status, page);
      setReviews(res.data?.reviews || []);
      setPagination(res.data?.pagination || { totalPages: 0, totalElements: 0 });
    } catch (e: any) {
      toast.error(e.message || 'Could not load reviews');
    }
    setLoading(false);
  }, [status, page]);

  useEffect(() => { load(); }, [load]);

  const moderate = async (reviewId: string, next: 'APPROVED' | 'REJECTED') => {
    setBusyId(reviewId);
    try {
      await admin.moderateReview(reviewId, next);
      toast.success(next === 'APPROVED' ? 'Review approved' : 'Review rejected');
      await load();
    } catch (e: any) {
      toast.error(e.message || 'Moderation failed');
    }
    setBusyId(null);
  };

  return (
    <div className="space-y-6">
      <Reveal>
        <div>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Review moderation</h1>
          <p className="mt-1 text-sm text-stone-500">Approve or reject customer reviews before they go public.</p>
        </div>
      </Reveal>

      <Reveal delay={60}>
        <div className="flex flex-wrap items-center gap-2" role="tablist" aria-label="Filter reviews by status">
          {STATUS_FILTERS.map(f => (
            <button
              key={f.id}
              role="tab"
              aria-selected={status === f.id}
              onClick={() => { setStatus(f.id); setPage(0); }}
              className={`flex items-center gap-2 rounded-xl border-2 px-4 py-2 text-sm font-semibold transition-colors ${
                status === f.id ? 'border-primary-600 bg-primary-50 text-primary-700' : 'border-stone-200 hover:border-stone-300'
              }`}
            >
              <f.icon className="h-4 w-4" /> {f.label}
            </button>
          ))}
          <span className="ml-auto text-sm text-stone-400">{pagination.totalElements} review{pagination.totalElements === 1 ? '' : 's'}</span>
        </div>
      </Reveal>

      <Reveal delay={120}>
        {loading ? (
          <div className="space-y-3">{[...Array(4)].map((_, i) => <div key={i} className="h-24 animate-pulse rounded-2xl bg-stone-200/50" />)}</div>
        ) : reviews.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-stone-300 p-12 text-center">
            <p className="text-sm font-medium text-stone-600">Nothing in this queue.</p>
            <p className="mt-1 text-sm text-stone-400">Reviews appear here when customers submit them.</p>
          </div>
        ) : (
          <ul className="space-y-3">
            {reviews.map(review => (
              <li key={review.id} className="rounded-2xl border border-stone-200/80 bg-white p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <div className="flex items-center gap-0.5" aria-label={`Rated ${review.rating} of 5`}>
                        {[1, 2, 3, 4, 5].map(s => (
                          <FiStar key={s} className={`h-3.5 w-3.5 ${s <= review.rating ? 'fill-amber-400 text-amber-400' : 'text-stone-300'}`} />
                        ))}
                      </div>
                      <span className={`rounded-full px-2.5 py-0.5 text-[11px] font-bold ${statusBadge[review.status || 'PENDING']}`}>
                        {review.status || 'PENDING'}
                      </span>
                      {review.isVerifiedPurchase && (
                        <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">Verified purchase</span>
                      )}
                    </div>
                    {review.title && <h2 className="mt-2 font-semibold">{review.title}</h2>}
                    <p className="mt-1 text-sm leading-relaxed text-stone-600">{review.content}</p>
                    <p className="mt-2 text-xs text-stone-400">
                      {review.userName || 'Shopper'} · {formatDate(review.createdAt)} ·{' '}
                      <span className="inline-flex items-center gap-1"><FiThumbsUp className="h-3 w-3" /> {review.helpfulCount ?? 0} helpful</span>
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <button
                      onClick={() => moderate(review.id, 'APPROVED')}
                      disabled={busyId === review.id || review.status === 'APPROVED'}
                      className="flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-emerald-700 disabled:opacity-40"
                    >
                      <FiCheckCircle className="h-4 w-4" /> Approve
                    </button>
                    <button
                      onClick={() => moderate(review.id, 'REJECTED')}
                      disabled={busyId === review.id || review.status === 'REJECTED'}
                      className="flex items-center gap-1.5 rounded-xl border-2 border-rose-200 px-4 py-2 text-sm font-semibold text-rose-700 transition-colors hover:bg-rose-50 disabled:opacity-40"
                    >
                      <FiXCircle className="h-4 w-4" /> Reject
                    </button>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}

        {pagination.totalPages > 1 && (
          <div className="mt-6 flex items-center justify-between" aria-label="Pagination">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
              className="rounded-xl border border-stone-300 px-4 py-2 text-sm font-semibold disabled:opacity-40">
              Previous
            </button>
            <span className="text-sm text-stone-500">Page {page + 1} of {pagination.totalPages}</span>
            <button onClick={() => setPage(p => Math.min(pagination.totalPages - 1, p + 1))} disabled={page >= pagination.totalPages - 1}
              className="rounded-xl border border-stone-300 px-4 py-2 text-sm font-semibold disabled:opacity-40">
              Next
            </button>
          </div>
        )}
      </Reveal>

      <p className="text-xs text-stone-400">
        Tip: approved reviews appear on product pages sorted by helpfulness by default.{' '}
        <Link href="/admin" className="underline hover:text-stone-600">Back to dashboard</Link>
      </p>
    </div>
  );
}
