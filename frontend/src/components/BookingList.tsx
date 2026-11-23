'use client';

import { useState } from 'react';
import { api } from '@/lib/api';
import { dayLabel, hourLabel } from '@/lib/time';
import { useToast } from '@/lib/toast';
import type { Booking } from '@/lib/types';
import { ReasonDialog } from './ReasonDialog';
import { StatusBadge } from './StatusBadge';

interface BookingListProps {
  bookings: Booking[];
  onChange: () => void;
}

/** Liste de réservations avec leurs actions : confirmer sa présence, annuler. */
export function BookingList({ bookings, onChange }: BookingListProps) {
  const toast = useToast();
  const [cancelling, setCancelling] = useState<Booking | null>(null);

  async function checkIn(booking: Booking) {
    try {
      await api.checkIn(booking.id);
      toast.success(`Présence confirmée en salle ${booking.roomName}.`);
      onChange();
    } catch (e) {
      toast.error(e);
    }
  }

  return (
    <>
      <ul className="panel divide-y divide-line">
        {bookings.map((b) => (
          <li key={b.id} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center" data-testid="booking-row">
            <div className="w-40 shrink-0">
              <p className="font-bold first-letter:uppercase">{dayLabel(b.date)}</p>
              <p className="text-sm text-ink-soft">
                {hourLabel(b.start)} – {hourLabel(b.end)}
              </p>
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate font-bold">{b.title}</p>
              <p className="text-sm text-ink-soft">
                Salle {b.roomName}, {b.siteName} · {b.attendees} participant{b.attendees > 1 ? 's' : ''}
              </p>
              {b.cancellationReason && <p className="mt-1 text-sm text-ink-soft">Motif : {b.cancellationReason}</p>}
            </div>
            <div className="flex items-center gap-2">
              <StatusBadge status={b.status} label={b.statusLabel} />
              {b.canCheckIn && (
                <button type="button" className="btn-primary bg-present hover:bg-present hover:brightness-95" onClick={() => checkIn(b)} data-testid="check-in">
                  Confirmer ma présence
                </button>
              )}
              {b.canCancel && (
                <button type="button" className="btn-secondary" onClick={() => setCancelling(b)} data-testid="cancel">
                  Annuler
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
      <ReasonDialog
        open={cancelling !== null}
        title="Annuler la réservation"
        description={cancelling ? `${cancelling.title}, salle ${cancelling.roomName}, ${dayLabel(cancelling.date)} à ${hourLabel(cancelling.start)}.` : undefined}
        label="Motif"
        confirmLabel="Annuler la réservation"
        required={false}
        danger
        onClose={() => setCancelling(null)}
        onConfirm={async (reason) => {
          if (!cancelling) return;
          await api.cancel(cancelling.id, reason || undefined);
          toast.success(`Réservation annulée : la salle ${cancelling.roomName} est de nouveau libre.`);
          setCancelling(null);
          onChange();
        }}
      />
    </>
  );
}
