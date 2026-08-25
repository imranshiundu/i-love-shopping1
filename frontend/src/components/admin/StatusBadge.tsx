import { statusColor } from '@/lib/utils';

export default function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`inline-block rounded-full px-2.5 py-1 text-[10px] font-bold ${statusColor(status)}`}>
      {status}
    </span>
  );
}
