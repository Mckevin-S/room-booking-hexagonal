'use client';

import { CalendarCheck2, History } from 'lucide-react';
import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { BookingList } from '@/components/BookingList';
import { EmptyState } from '@/components/EmptyState';
import { api } from '@/lib/api';
import { useToast } from '@/lib/toast';
import type { Booking } from '@/lib/types';

export default function MyBookingsPage() {
  const toast = useToast();
  const [past, setPast] = useState(false);
  const [bookings, setBookings] = useState<Booking[] | null>(null);

  const [reload, setReload] = useState(0);
  const notifyError = toast.error;
  const load = useCallback(() => setReload((n) => n + 1), []);

  // Une réponse arrivée après un changement d'onglet est ignorée : elle n'écrase pas la plus récente
  useEffect(() => {
    let current = true;
    api
      .myBookings(past)
      .then((found) => current && setBookings(found))
      .catch((e) => current && notifyError(e));
    return () => {
      current = false;
    };
  }, [past, reload, notifyError]);

  const tabs = [
    { value: false, label: 'À venir' },
    { value: true, label: 'Passées' },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1>Mes réservations</h1>
          <p className="mt-1 text-ink-soft">Confirmez votre présence en arrivant : sans confirmation, la salle est libérée 15 minutes après le début.</p>
        </div>
        <div role="tablist" className="flex rounded-plaque border border-line bg-white p-1">
          {tabs.map((tab) => (
            <button
              key={tab.label}
              role="tab"
              type="button"
              aria-selected={past === tab.value}
              onClick={() => {
                if (tab.value !== past) setBookings(null);
                setPast(tab.value);
              }}
              className={`rounded px-4 py-1.5 text-sm font-bold ${past === tab.value ? 'bg-ink text-white' : 'text-ink-soft hover:text-ink'}`}
              data-testid={`tab-${tab.value ? 'past' : 'upcoming'}`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {bookings === null ? (
        <div className="panel h-40 animate-pulse" aria-busy />
      ) : bookings.length ? (
        <BookingList bookings={bookings} onChange={load} />
      ) : (
        <div className="panel">
          {past ? (
            <EmptyState icon={History} title="Pas encore d'historique">
              Vos réunions terminées, annulées ou libérées apparaîtront ici.
            </EmptyState>
          ) : (
            <EmptyState icon={CalendarCheck2} title="Aucune réservation à venir">
              <Link href="/reserver" className="font-bold text-signal underline-offset-2 hover:underline">
                Réserver une salle
              </Link>
            </EmptyState>
          )}
        </div>
      )}
    </div>
  );
}
