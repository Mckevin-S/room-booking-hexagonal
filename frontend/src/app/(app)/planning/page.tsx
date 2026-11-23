'use client';

import { ChevronLeft, ChevronRight, DoorClosed } from 'lucide-react';
import { useEffect, useState } from 'react';
import { BookingDialog, type BookingIntent } from '@/components/BookingDialog';
import { EmptyState } from '@/components/EmptyState';
import { Timeline } from '@/components/Timeline';
import { api } from '@/lib/api';
import { addDays, dayLabel, fromMinutes, hourLabel, isoDate, nowTime, toMinutes } from '@/lib/time';
import { useToast } from '@/lib/toast';
import type { Planning } from '@/lib/types';
import { useSites } from '@/lib/useSites';

function Legend() {
  const items = [
    { className: 'bg-signal', label: 'Vos réservations' },
    { className: 'bg-present', label: 'Présence confirmée' },
    { className: 'bg-busy', label: 'Réservée par un collègue' },
  ];
  return (
    <ul className="flex flex-wrap gap-4 text-sm text-ink-soft">
      {items.map((item) => (
        <li key={item.label} className="inline-flex items-center gap-2">
          <span className={`size-3 rounded-sm ${item.className}`} aria-hidden />
          {item.label}
        </li>
      ))}
      <li className="inline-flex items-center gap-2">
        <span className="h-3 w-0.5 bg-alert" aria-hidden />
        Maintenant
      </li>
    </ul>
  );
}

export default function PlanningPage() {
  const toast = useToast();
  const { sites, siteId, setSiteId, site } = useSites();
  const today = isoDate(new Date());
  const [date, setDate] = useState(today);
  const [planning, setPlanning] = useState<Planning | null>(null);
  const [intent, setIntent] = useState<BookingIntent | null>(null);
  const [now, setNow] = useState(nowTime());

  const [reload, setReload] = useState(0);
  const notifyError = toast.error;

  // Une réponse arrivée après un changement de date ou de site est ignorée : elle n'écrase pas la plus récente
  useEffect(() => {
    if (!siteId) return;
    let current = true;
    api
      .planning(siteId, date)
      .then((p) => current && setPlanning(p))
      .catch((e) => current && notifyError(e));
    return () => {
      current = false;
    };
  }, [siteId, date, reload, notifyError]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(nowTime()), 60_000);
    return () => window.clearInterval(timer);
  }, []);

  const isToday = date === today;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="first-letter:uppercase" data-testid="planning-day">
            {dayLabel(date)}
          </h1>
          <p className="mt-1 text-ink-soft">
            {site ? `${site.name}, ouvert de ${hourLabel(site.opensAt)} à ${hourLabel(site.closesAt)}.` : ' '} Cliquez sur une plage libre pour la réserver.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <select className="input" value={siteId} onChange={(e) => setSiteId(e.target.value)} aria-label="Site" data-testid="planning-site">
            {sites.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
          <div className="flex items-center rounded-plaque border border-line bg-white">
            <button type="button" className="btn-quiet h-10 rounded-none" onClick={() => setDate(addDays(date, -1))} aria-label="Jour précédent">
              <ChevronLeft className="size-4" />
            </button>
            <input
              type="date"
              className="h-10 border-x border-line px-2 text-sm"
              value={date}
              onChange={(e) => e.target.value && setDate(e.target.value)}
              aria-label="Date"
              data-testid="planning-date"
            />
            <button type="button" className="btn-quiet h-10 rounded-none" onClick={() => setDate(addDays(date, 1))} aria-label="Jour suivant" data-testid="planning-next">
              <ChevronRight className="size-4" />
            </button>
          </div>
          {!isToday && (
            <button type="button" className="btn-secondary" onClick={() => setDate(today)}>
              Aujourd&apos;hui
            </button>
          )}
        </div>
      </div>

      {planning && planning.date === date && planning.site.id === siteId ? (
        planning.open ? (
          <>
            <Timeline
              planning={planning}
              now={isToday ? now : undefined}
              onSlotClick={(room, start) => {
                const end = fromMinutes(Math.min(toMinutes(start) + 60, toMinutes(planning.site.closesAt)));
                setIntent({ room, date, start, end, attendees: Math.min(2, room.seats) });
              }}
            />
            <Legend />
          </>
        ) : (
          <div className="panel">
            <EmptyState icon={DoorClosed} title="Le site est fermé ce jour-là">
              Les salles se réservent du lundi au vendredi.
            </EmptyState>
          </div>
        )
      ) : (
        <div className="panel h-64 animate-pulse" aria-busy />
      )}

      <BookingDialog
        intent={intent}
        hours={planning ? { opensAt: planning.site.opensAt, closesAt: planning.site.closesAt } : undefined}
        onClose={() => setIntent(null)}
        onBooked={(booking) => {
          setIntent(null);
          toast.success(`Salle ${booking.roomName} réservée de ${hourLabel(booking.start)} à ${hourLabel(booking.end)}.`);
          setReload((n) => n + 1);
        }}
      />
    </div>
  );
}
