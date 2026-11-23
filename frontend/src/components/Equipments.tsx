import { Accessibility, Monitor, PenLine, Video, type LucideIcon } from 'lucide-react';
import type { Equipment, EquipmentCode } from '@/lib/types';

export const EQUIPMENT: Record<EquipmentCode, { label: string; icon: LucideIcon }> = {
  SCREEN: { label: 'Écran', icon: Monitor },
  VIDEO_CONFERENCE: { label: 'Visioconférence', icon: Video },
  WHITEBOARD: { label: 'Tableau blanc', icon: PenLine },
  ACCESSIBLE: { label: 'Accessible PMR', icon: Accessibility },
};

export const EQUIPMENT_CODES = Object.keys(EQUIPMENT) as EquipmentCode[];

/** Équipements d'une salle, en pictogrammes accompagnés de leur nom. */
export function Equipments({ items }: { items: Equipment[] }) {
  if (!items.length) return <span className="text-sm text-ink-faint">Sans équipement</span>;
  return (
    <ul className="flex flex-wrap gap-x-3 gap-y-1 text-sm text-ink-soft">
      {items.map(({ code, label }) => {
        const Icon = EQUIPMENT[code].icon;
        return (
          <li key={code} className="inline-flex items-center gap-1">
            <Icon className="size-4" aria-hidden />
            {label}
          </li>
        );
      })}
    </ul>
  );
}
