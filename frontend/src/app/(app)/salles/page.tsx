'use client';

import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { ReasonDialog } from '@/components/ReasonDialog';
import { RoomPlaque } from '@/components/RoomPlaque';
import { api } from '@/lib/api';
import { useSession } from '@/lib/session';
import { useToast } from '@/lib/toast';
import type { Room } from '@/lib/types';
import { useSites } from '@/lib/useSites';

export default function RoomsPage() {
  const { user } = useSession();
  const router = useRouter();
  const toast = useToast();
  const { sites, siteId, setSiteId } = useSites();
  const [rooms, setRooms] = useState<Room[] | null>(null);
  const [closing, setClosing] = useState<Room | null>(null);

  useEffect(() => {
    if (user && !user.officeManager) router.replace('/');
  }, [user, router]);

  const load = useCallback(() => {
    if (siteId) api.rooms(siteId).then(setRooms).catch(toast.error);
  }, [siteId, toast.error]);

  useEffect(load, [load]);

  async function reopen(room: Room) {
    try {
      await api.putBackInService(room.id);
      toast.success(`La salle ${room.name} est de nouveau réservable.`);
      load();
    } catch (e) {
      toast.error(e);
    }
  }

  if (!user?.officeManager) return null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1>Salles</h1>
          <p className="mt-1 text-ink-soft">
            Une salle mise hors service n&apos;est plus réservable ; ses réservations à venir sont annulées et leurs organisateurs prévenus.
          </p>
        </div>
        <select className="input" value={siteId} onChange={(e) => setSiteId(e.target.value)} aria-label="Site">
          {sites.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-col gap-3">
        {rooms?.map((room) => (
          <RoomPlaque
            key={room.id}
            room={room}
            tone={room.inService ? 'free' : 'unavailable'}
            status={
              room.inService ? (
                <span className="rounded bg-present-tint px-2 py-0.5 text-sm font-bold text-present">En service</span>
              ) : (
                <span className="rounded bg-alert-tint px-2 py-0.5 text-sm font-bold text-alert">Hors service : {room.outOfServiceReason}</span>
              )
            }
            action={
              room.inService ? (
                <button type="button" className="btn-secondary" onClick={() => setClosing(room)} data-testid={`close-${room.id}`}>
                  Mettre hors service
                </button>
              ) : (
                <button type="button" className="btn-primary" onClick={() => reopen(room)} data-testid={`reopen-${room.id}`}>
                  Remettre en service
                </button>
              )
            }
          />
        ))}
      </div>

      <ReasonDialog
        open={closing !== null}
        title={`Mettre ${closing?.name ?? ''} hors service`}
        description="Les réservations à venir de cette salle seront annulées avec ce motif."
        label="Motif communiqué aux organisateurs"
        confirmLabel="Mettre hors service"
        required
        danger
        onClose={() => setClosing(null)}
        onConfirm={async (reason) => {
          if (!closing) return;
          const result = await api.takeOutOfService(closing.id, reason);
          toast.success(
            result.cancelledBookings
              ? `${closing.name} est hors service : ${result.cancelledBookings} réservation${result.cancelledBookings > 1 ? 's annulées' : ' annulée'}, organisateurs prévenus.`
              : `${closing.name} est hors service. Aucune réservation à venir n'était concernée.`,
          );
          setClosing(null);
          load();
        }}
      />
    </div>
  );
}
