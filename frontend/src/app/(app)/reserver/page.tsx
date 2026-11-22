'use client';

import { CalendarX2, Search } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState, type FormEvent } from 'react';
import { BookingDialog, type BookingIntent } from '@/components/BookingDialog';
import { EmptyState } from '@/components/EmptyState';
import { EQUIPMENT, EQUIPMENT_CODES } from '@/components/Equipments';
import { RoomPlaque } from '@/components/RoomPlaque';
import { api } from '@/lib/api';
import {
  ceilToQuarter,
  dayLabel,
  fromMinutes,
  hourLabel,
  isWeekend,
  isoDate,
  nextWorkingDay,
  quarterOptions,
  toMinutes,
} from '@/lib/time';
import { useToast } from '@/lib/toast';
import type { EquipmentCode, SearchCriteria, SearchResult } from '@/lib/types';
import { useSites } from '@/lib/useSites';

/** Créneau proposé par défaut : le prochain quart d'heure utile aujourd'hui, sinon 10 h le prochain jour ouvré. */
function defaultSlot(now = new Date()): { date: string; start: string; end: string } {
  const today = isoDate(now);
  const next = ceilToQuarter(now.getHours() * 60 + now.getMinutes() + 15);
  if (!isWeekend(today) && next >= toMinutes('08:00') && next + 60 <= toMinutes('20:00')) {
    return { date: today, start: fromMinutes(next), end: fromMinutes(next + 60) };
  }
  const day = next + 60 > toMinutes('20:00') || isWeekend(today) ? nextWorkingDay(isoDate(new Date(now.getTime() + 86_400_000))) : today;
  return { date: day, start: '10:00', end: '11:00' };
}

export default function BookPage() {
  const router = useRouter();
  const toast = useToast();
  const { sites, siteId, setSiteId, site } = useSites();
  const initial = useMemo(() => defaultSlot(), []);
  const [date, setDate] = useState(initial.date);
  const [start, setStart] = useState(initial.start);
  const [end, setEnd] = useState(initial.end);
  const [attendees, setAttendees] = useState(4);
  const [equipments, setEquipments] = useState<EquipmentCode[]>([]);
  const [result, setResult] = useState<SearchResult | null>(null);
  const [searched, setSearched] = useState<SearchCriteria | null>(null);
  const [loading, setLoading] = useState(false);
  const [intent, setIntent] = useState<BookingIntent | null>(null);

  const times = quarterOptions(site?.opensAt ?? '08:00', site?.closesAt ?? '20:00');

  async function search(event: FormEvent) {
    event.preventDefault();
    const criteria: SearchCriteria = { siteId, date, start, end, attendees, equipments };
    setLoading(true);
    try {
      setResult(await api.search(criteria));
      setSearched(criteria);
    } catch (e) {
      setResult(null);
      toast.error(e);
    } finally {
      setLoading(false);
    }
  }

  function toggle(code: EquipmentCode) {
    setEquipments((all) => (all.includes(code) ? all.filter((c) => c !== code) : [...all, code]));
  }

  return (
    <div className="flex flex-col gap-8">
      <div>
        <h1>Réserver une salle</h1>
        <p className="mt-1 text-ink-soft">Indiquez le créneau et le nombre de participants : les salles libres s&apos;affichent de la mieux ajustée à la plus grande.</p>
      </div>

      <form onSubmit={search} className="panel grid gap-4 p-5 md:grid-cols-6" aria-label="Recherche de salle">
        <label className="field md:col-span-2">
          <span>Site</span>
          <select className="input" value={siteId} onChange={(e) => setSiteId(e.target.value)} data-testid="search-site">
            {sites.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <label className="field md:col-span-2">
          <span>Date</span>
          <input className="input" type="date" value={date} min={isoDate(new Date())} onChange={(e) => setDate(e.target.value)} required data-testid="search-date" />
        </label>
        <label className="field">
          <span>De</span>
          <select className="input" value={start} onChange={(e) => setStart(e.target.value)} data-testid="search-start">
            {times.slice(0, -1).map((t) => (
              <option key={t} value={t}>
                {hourLabel(t)}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>À</span>
          <select className="input" value={end} onChange={(e) => setEnd(e.target.value)} data-testid="search-end">
            {times.slice(1).map((t) => (
              <option key={t} value={t}>
                {hourLabel(t)}
              </option>
            ))}
          </select>
        </label>
        <label className="field md:col-span-1">
          <span>Participants</span>
          <input className="input" type="number" min={1} max={50} value={attendees} onChange={(e) => setAttendees(Number(e.target.value))} data-testid="search-attendees" />
        </label>
        <fieldset className="flex flex-col gap-1 text-sm md:col-span-4">
          <legend className="mb-1 font-bold text-ink-soft">Équipements nécessaires</legend>
          <div className="flex flex-wrap gap-2">
            {EQUIPMENT_CODES.map((code) => {
              const { label, icon: Icon } = EQUIPMENT[code];
              const active = equipments.includes(code);
              return (
                <button
                  key={code}
                  type="button"
                  aria-pressed={active}
                  onClick={() => toggle(code)}
                  className={`inline-flex h-10 items-center gap-2 rounded-plaque border px-3 font-bold ${
                    active ? 'border-signal bg-signal-tint text-signal-dark' : 'border-line bg-white text-ink-soft hover:border-ink-faint'
                  }`}
                  data-testid={`equipment-${code}`}
                >
                  <Icon className="size-4" aria-hidden />
                  {label}
                </button>
              );
            })}
          </div>
        </fieldset>
        <div className="flex items-end md:col-span-1">
          <button type="submit" className="btn-primary h-10 w-full" disabled={loading || !siteId} data-testid="search-submit">
            <Search className="size-4" aria-hidden />
            Chercher
          </button>
        </div>
      </form>

      {result && searched && (
        <section className="flex flex-col gap-3" aria-live="polite">
          {result.rooms.length > 0 ? (
            <>
              <h2>
                {result.rooms.length} salle{result.rooms.length > 1 ? 's' : ''} libre{result.rooms.length > 1 ? 's' : ''}{' '}
                <span className="font-normal text-ink-soft">
                  {dayLabel(searched.date)}, de {hourLabel(searched.start)} à {hourLabel(searched.end)}
                </span>
              </h2>
              {result.rooms.map((room) => (
                <RoomPlaque
                  key={room.id}
                  room={room}
                  attendees={searched.attendees}
                  action={
                    <button
                      type="button"
                      className="btn-primary"
                      onClick={() => setIntent({ room, date: searched.date, start: searched.start, end: searched.end, attendees: searched.attendees })}
                      data-testid={`book-${room.id}`}
                    >
                      Réserver {room.name}
                    </button>
                  }
                />
              ))}
            </>
          ) : result.suggestions.length > 0 ? (
            <>
              <h2>Aucune salle adaptée n&apos;est libre sur ce créneau</h2>
              <p className="-mt-1 text-sm text-ink-soft">Créneaux libres les plus proches, pour la même durée :</p>
              {result.suggestions.map((s) => (
                <RoomPlaque
                  key={`${s.room.id}-${s.start}`}
                  room={s.room}
                  attendees={searched.attendees}
                  status={
                    <span className="rounded bg-pending-tint px-2 py-0.5 text-sm font-bold text-pending-ink">
                      Libre de {hourLabel(s.start)} à {hourLabel(s.end)}
                    </span>
                  }
                  action={
                    <button
                      type="button"
                      className="btn-secondary"
                      onClick={() => setIntent({ room: s.room, date: searched.date, start: s.start, end: s.end, attendees: searched.attendees })}
                      data-testid={`suggestion-${s.room.id}-${s.start}`}
                    >
                      Réserver ce créneau
                    </button>
                  }
                />
              ))}
            </>
          ) : (
            <div className="panel">
              <EmptyState icon={CalendarX2} title="Aucune salle ne correspond">
                Aucune salle de ce site n&apos;accueille {searched.attendees} personnes avec ces équipements. Réduisez les critères ou choisissez un autre site.
              </EmptyState>
            </div>
          )}
        </section>
      )}

      <BookingDialog
        intent={intent}
        onClose={() => setIntent(null)}
        onBooked={(booking) => {
          setIntent(null);
          toast.success(`Salle ${booking.roomName} réservée ${dayLabel(booking.date)} de ${hourLabel(booking.start)} à ${hourLabel(booking.end)}.`);
          router.push('/mes-reservations');
        }}
      />
    </div>
  );
}
