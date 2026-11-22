'use client';

import { useEffect, useState, type FormEvent } from 'react';
import { api } from '@/lib/api';
import { durationLabel, dayLabel, hourLabel, quarterOptions, toMinutes } from '@/lib/time';
import { messageOf } from '@/lib/toast';
import type { Booking, Room } from '@/lib/types';
import { Dialog } from './Dialog';

export interface BookingIntent {
  room: Room;
  date: string;
  start: string;
  end: string;
  attendees: number;
}

interface BookingDialogProps {
  intent: BookingIntent | null;
  /** Heures d'ouverture du site : si fournies, le créneau est modifiable dans la fenêtre. */
  hours?: { opensAt: string; closesAt: string };
  onClose: () => void;
  onBooked: (booking: Booking) => void;
}

/** Dernière étape d'une réservation : l'objet de la réunion, et au besoin l'horaire et le nombre de participants. */
export function BookingDialog({ intent, hours, onClose, onBooked }: BookingDialogProps) {
  const [title, setTitle] = useState('');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [attendees, setAttendees] = useState(1);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (intent) {
      setTitle('');
      setStart(intent.start);
      setEnd(intent.end);
      setAttendees(intent.attendees);
      setError(null);
    }
  }, [intent]);

  if (!intent) return <Dialog open={false} title="" onClose={onClose}>{null}</Dialog>;

  const times = hours ? quarterOptions(hours.opensAt, hours.closesAt) : [];
  const validSlot = toMinutes(end) > toMinutes(start);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!intent) return;
    setSaving(true);
    setError(null);
    try {
      const booking = await api.book({ roomId: intent.room.id, date: intent.date, start, end, title: title.trim(), attendees });
      onBooked(booking);
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog
      open
      title={`Réserver ${intent.room.name}`}
      onClose={onClose}
      description={
        <>
          {dayLabel(intent.date)}, {hourLabel(start)} – {hourLabel(end)}
          {validSlot && ` (${durationLabel(start, end)})`} · {intent.room.seats} places
        </>
      }
    >
      <form onSubmit={submit} className="flex flex-col gap-4">
        <label className="field">
          <span>Objet de la réunion</span>
          <input
            className="input"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={80}
            required
            data-autofocus
            placeholder="Revue de sprint, point client…"
            data-testid="booking-title"
          />
        </label>
        <div className="grid grid-cols-3 gap-3">
          {hours && (
            <>
              <label className="field">
                <span>Début</span>
                <select className="input" value={start} onChange={(e) => setStart(e.target.value)} data-testid="booking-start">
                  {times.slice(0, -1).map((t) => (
                    <option key={t} value={t}>
                      {hourLabel(t)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span>Fin</span>
                <select className="input" value={end} onChange={(e) => setEnd(e.target.value)} data-testid="booking-end">
                  {times.slice(1).map((t) => (
                    <option key={t} value={t}>
                      {hourLabel(t)}
                    </option>
                  ))}
                </select>
              </label>
            </>
          )}
          <label className="field">
            <span>Participants</span>
            <input
              className="input"
              type="number"
              min={1}
              max={intent.room.seats}
              value={attendees}
              onChange={(e) => setAttendees(Number(e.target.value))}
              data-testid="booking-attendees"
            />
          </label>
        </div>
        {error && (
          <p role="alert" className="rounded-plaque bg-alert-tint px-3 py-2 text-sm text-alert">
            {error}
          </p>
        )}
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-secondary" onClick={onClose}>
            Annuler
          </button>
          <button type="submit" className="btn-primary" disabled={saving || !title.trim() || !validSlot} data-testid="booking-submit">
            {saving ? 'Réservation…' : 'Réserver'}
          </button>
        </div>
      </form>
    </Dialog>
  );
}
