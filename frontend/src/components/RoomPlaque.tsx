import type { ReactNode } from 'react';
import type { Room } from '@/lib/types';
import { CapacityMeter } from './CapacityMeter';
import { Equipments } from './Equipments';

interface RoomPlaqueProps {
  room: Room;
  attendees?: number;
  /** Mention à droite du nom : disponibilité, créneau suggéré… */
  status?: ReactNode;
  action?: ReactNode;
  /** Liseré de la plaque : libre (vert), indisponible (rouge). */
  tone?: 'free' | 'unavailable';
}

/** Une salle présentée comme sa plaque de porte : nom, étage, places, équipements. */
export function RoomPlaque({ room, attendees, status, action, tone = 'free' }: RoomPlaqueProps) {
  return (
    <article
      className={`panel flex flex-col gap-3 border-l-[5px] p-4 sm:flex-row sm:items-center sm:justify-between ${
        tone === 'free' ? 'border-l-present' : 'border-l-alert'
      }`}
      data-testid={`room-${room.id}`}
    >
      <div className="flex min-w-0 flex-col gap-2">
        <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
          <h3 className="text-xl font-bold leading-none">{room.name}</h3>
          <span className="text-sm text-ink-soft">
            {room.floor ? `${room.floor}, ` : ''}
            {room.seats} places
          </span>
          {status}
        </div>
        {attendees !== undefined && <CapacityMeter seats={room.seats} attendees={attendees} />}
        <Equipments items={room.equipments} />
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </article>
  );
}
