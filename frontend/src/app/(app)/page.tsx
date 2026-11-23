'use client';

import { CalendarDays, CalendarPlus, Clock3, DoorOpen } from 'lucide-react';
import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { BookingList } from '@/components/BookingList';
import { EmptyState } from '@/components/EmptyState';
import { api } from '@/lib/api';
import { useSession } from '@/lib/session';
import { dayLabel, durationLabel, hourLabel } from '@/lib/time';
import { useToast } from '@/lib/toast';
import type { Booking } from '@/lib/types';

function NextMeeting({ booking, onChange }: { booking: Booking; onChange: () => void }) {
  const toast = useToast();
  const checkedIn = booking.status === 'CHECKED_IN';

  async function checkIn() {
    try {
      await api.checkIn(booking.id);
      toast.success(`Présence confirmée en salle ${booking.roomName}.`);
      onChange();
    } catch (e) {
      toast.error(e);
    }
  }

  return (
    <section className="panel overflow-hidden" data-testid="next-meeting">
      <div className="grid gap-0 md:grid-cols-[1fr_280px]">
        <div className="p-6">
          <p className="text-sm text-ink-soft first-letter:uppercase">
            {dayLabel(booking.date)}, {hourLabel(booking.start)} – {hourLabel(booking.end)} ({durationLabel(booking.start, booking.end)})
          </p>
          <h2 className="mt-1 text-2xl">{booking.title}</h2>
          <p className="mt-3 flex items-center gap-2 text-ink-soft">
            <DoorOpen className="size-4" aria-hidden />
            <span>
              Salle <strong className="text-ink">{booking.roomName}</strong>, {booking.siteName}
            </span>
          </p>
        </div>
        <div
          className={`flex flex-col justify-center gap-3 p-6 ${
            checkedIn ? 'bg-present-tint' : booking.canCheckIn ? 'bg-pending-tint' : 'bg-paper'
          }`}
        >
          {checkedIn ? (
            <p className="font-bold text-present">Présence confirmée. Bonne réunion.</p>
          ) : booking.canCheckIn ? (
            <>
              <p className="text-sm text-pending-ink">
                Confirmez votre présence avant {hourLabel(booking.releasedIfAbsentAt.slice(11, 16))}, sinon la salle sera libérée.
              </p>
              <button type="button" className="btn-primary bg-present hover:bg-present hover:brightness-95" onClick={checkIn} data-testid="next-check-in">
                Confirmer ma présence
              </button>
            </>
          ) : (
            <p className="flex gap-2 text-sm text-ink-soft">
              <Clock3 className="mt-0.5 size-4 shrink-0" aria-hidden />
              Vous pourrez confirmer votre présence {dayLabel(booking.checkInOpensAt.slice(0, 10))} à partir de{' '}
              {hourLabel(booking.checkInOpensAt.slice(11, 16))}.
            </p>
          )}
        </div>
      </div>
    </section>
  );
}

export default function HomePage() {
  const { user } = useSession();
  const toast = useToast();
  const [bookings, setBookings] = useState<Booking[] | null>(null);

  const load = useCallback(() => {
    api
      .myBookings()
      .then((all) => setBookings(all.filter((b) => b.status === 'CONFIRMED' || b.status === 'CHECKED_IN')))
      .catch(toast.error);
  }, [toast.error]);

  useEffect(load, [load]);

  const [next, ...others] = bookings ?? [];

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1>Bonjour {user?.fullName.split(' ')[0]}</h1>
          <p className="mt-1 text-ink-soft">
            {bookings === null
              ? 'Chargement de vos réservations…'
              : bookings.length
                ? `Vous avez ${bookings.length} réunion${bookings.length > 1 ? 's' : ''} à venir.`
                : 'Aucune réunion à venir.'}
          </p>
        </div>
        <div className="flex gap-2">
          <Link href="/planning" className="btn-secondary">
            <CalendarDays className="size-4" aria-hidden />
            Voir le planning
          </Link>
          <Link href="/reserver" className="btn-primary" data-testid="home-book">
            <CalendarPlus className="size-4" aria-hidden />
            Réserver une salle
          </Link>
        </div>
      </div>

      {next && <NextMeeting booking={next} onChange={load} />}

      {bookings && bookings.length === 0 && (
        <div className="panel">
          <EmptyState icon={CalendarPlus} title="Votre agenda de salles est vide">
            Cherchez une salle par taille et par équipement, ou repérez un créneau libre sur le planning du site.
          </EmptyState>
        </div>
      )}

      {others.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2>Ensuite</h2>
          <BookingList bookings={others} onChange={load} />
        </section>
      )}
    </div>
  );
}
