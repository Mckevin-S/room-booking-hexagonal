import type { BookingStatus } from '@/lib/types';

const STYLES: Record<BookingStatus, string> = {
  CONFIRMED: 'bg-signal-tint text-signal-dark',
  CHECKED_IN: 'bg-present-tint text-present',
  CANCELLED: 'bg-line text-ink-soft line-through decoration-1',
  RELEASED: 'bg-alert-tint text-alert',
};

export function StatusBadge({ status, label }: { status: BookingStatus; label: string }) {
  return (
    <span className={`inline-flex items-center rounded px-2 py-0.5 text-xs font-bold ${STYLES[status]}`} data-testid="status">
      {label}
    </span>
  );
}
