'use client';

import { Wrench } from 'lucide-react';
import type { MouseEvent } from 'react';
import { fromMinutes, hourLabel, timeAtOffset, timelineOffset, toMinutes } from '@/lib/time';
import type { PlannedBooking, Planning, Room } from '@/lib/types';

interface TimelineProps {
  planning: Planning;
  /** Heure courante si la journée affichée est aujourd'hui. */
  now?: string;
  onSlotClick: (room: Room, start: string) => void;
}

function blockStyle(booking: PlannedBooking): string {
  if (booking.status === 'CHECKED_IN') return 'bg-present text-white';
  if (booking.mine) return 'bg-signal text-white';
  return 'bg-busy text-busy-ink';
}

/**
 * Tableau d'occupation du site : une ligne par salle, une colonne par heure d'ouverture.
 * Un clic sur une plage libre propose de la réserver à partir du quart d'heure visé.
 */
export function Timeline({ planning, now, onSlotClick }: TimelineProps) {
  const { opensAt, closesAt } = planning.site;
  const hours: string[] = [];
  for (let t = toMinutes(opensAt); t <= toMinutes(closesAt); t += 60) hours.push(fromMinutes(t));
  const nowVisible = now !== undefined && toMinutes(now) >= toMinutes(opensAt) && toMinutes(now) <= toMinutes(closesAt);

  function handleClick(event: MouseEvent<HTMLDivElement>, room: Room) {
    if (!room.inService) return;
    const rect = event.currentTarget.getBoundingClientRect();
    const start = timeAtOffset((event.clientX - rect.left) / rect.width, opensAt, closesAt);
    if (now !== undefined && toMinutes(start) < toMinutes(now)) return;
    onSlotClick(room, start);
  }

  return (
    <div className="panel overflow-x-auto" data-testid="timeline">
      <div className="min-w-[860px]">
        <div className="grid grid-cols-[180px_1fr] border-b border-line">
          <div className="px-4 py-2 text-sm font-bold text-ink-soft">Salle</div>
          <div className="relative h-9">
            {hours.map((h) => (
              <span
                key={h}
                className="absolute top-2 -translate-x-1/2 text-xs text-ink-faint first:translate-x-0 last:-translate-x-full"
                style={{ left: `${timelineOffset(h, opensAt, closesAt)}%` }}
              >
                {hourLabel(h)}
              </span>
            ))}
          </div>
        </div>

        {planning.rooms.map(({ room, bookings }) => (
          <div key={room.id} className="grid grid-cols-[180px_1fr] border-b border-line last:border-b-0" data-testid={`row-${room.id}`}>
            <div className="flex flex-col justify-center px-4 py-3">
              <span className="font-bold leading-tight">{room.name}</span>
              <span className="text-xs text-ink-soft">{room.seats} places</span>
            </div>
            <div
              className={`relative h-16 ${room.inService ? 'cursor-copy' : 'cursor-not-allowed'}`}
              onClick={(event) => handleClick(event, room)}
              role="presentation"
            >
              {hours.map((h) => (
                <span
                  key={h}
                  aria-hidden
                  className="absolute inset-y-0 border-l border-dashed border-line"
                  style={{ left: `${timelineOffset(h, opensAt, closesAt)}%` }}
                />
              ))}
              {!room.inService && (
                <div className="absolute inset-1 flex items-center gap-2 rounded bg-[repeating-linear-gradient(135deg,#F5F6F8_0_8px,#E6EAF0_8px_16px)] px-3 text-sm text-ink-soft">
                  <Wrench className="size-4" aria-hidden />
                  Hors service : {room.outOfServiceReason}
                </div>
              )}
              {bookings.map((b) => {
                const left = timelineOffset(b.start, opensAt, closesAt);
                const width = timelineOffset(b.end, opensAt, closesAt) - left;
                // Une réunion courte n'a la place que pour une initiale : le détail reste dans l'infobulle
                const compact = toMinutes(b.end) - toMinutes(b.start) <= 30;
                return (
                  <div
                    key={b.id}
                    onClick={(event) => event.stopPropagation()}
                    className={`absolute inset-y-2 cursor-default overflow-hidden rounded text-xs leading-tight ${
                      compact ? 'grid place-items-center px-0.5' : 'px-2 py-1'
                    } ${blockStyle(b)}`}
                    style={{ left: `${left}%`, width: `calc(${width}% - 2px)` }}
                    title={`${b.title} – ${b.organizerName}, ${hourLabel(b.start)}–${hourLabel(b.end)}`}
                    data-testid="planned-booking"
                  >
                    {compact ? (
                      <span className="font-bold" aria-label={b.title}>
                        {b.title.charAt(0)}
                      </span>
                    ) : (
                      <>
                        <span className="block truncate font-bold">{b.title}</span>
                        <span className="block truncate opacity-90">
                          {hourLabel(b.start)}–{hourLabel(b.end)} · {b.mine ? 'Vous' : b.organizerName}
                        </span>
                      </>
                    )}
                  </div>
                );
              })}
              {nowVisible && (
                <span
                  aria-hidden
                  className="pointer-events-none absolute inset-y-0 w-0.5 bg-alert"
                  style={{ left: `${timelineOffset(now!, opensAt, closesAt)}%` }}
                />
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
