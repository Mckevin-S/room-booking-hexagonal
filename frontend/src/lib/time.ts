/** Dates et heures locales du site, sans dépendance : les réservations se font par quart d'heure. */

export const QUARTER = 15;

/** « 2026-12-01 » à partir d'une date locale. */
export function isoDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function parseDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function addDays(iso: string, days: number): string {
  const date = parseDate(iso);
  date.setDate(date.getDate() + days);
  return isoDate(date);
}

export function isWeekend(iso: string): boolean {
  const day = parseDate(iso).getDay();
  return day === 0 || day === 6;
}

/** Prochain jour ouvré à partir d'aujourd'hui inclus. */
export function nextWorkingDay(from: string): string {
  let day = from;
  while (isWeekend(day)) day = addDays(day, 1);
  return day;
}

/** « 09:30 » → 570 */
export function toMinutes(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

/** 570 → « 09:30 » */
export function fromMinutes(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

/** Arrondit au quart d'heure supérieur. */
export function ceilToQuarter(minutes: number): number {
  return Math.ceil(minutes / QUARTER) * QUARTER;
}

/** Arrondit au quart d'heure inférieur. */
export function floorToQuarter(minutes: number): number {
  return Math.floor(minutes / QUARTER) * QUARTER;
}

/** Heures proposées dans les listes : chaque quart d'heure de la plage d'ouverture. */
export function quarterOptions(opensAt: string, closesAt: string): string[] {
  const options: string[] = [];
  for (let t = toMinutes(opensAt); t <= toMinutes(closesAt); t += QUARTER) options.push(fromMinutes(t));
  return options;
}

/** « 10h00 », « 9h30 » : affichage des heures à la française. */
export function hourLabel(time: string): string {
  const [h, m] = time.split(':');
  return `${Number(h)}h${m}`;
}

/** « 1 h 30 », « 45 min » */
export function durationLabel(start: string, end: string): string {
  const minutes = toMinutes(end) - toMinutes(start);
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h} h` : `${h} h ${String(m).padStart(2, '0')}`;
}

const DAY_FORMAT = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });

/** « mercredi 2 décembre » ; « aujourd'hui » et « demain » quand c'est plus parlant. */
export function dayLabel(iso: string, today: string = isoDate(new Date())): string {
  if (iso === today) return "aujourd'hui";
  if (iso === addDays(today, 1)) return 'demain';
  return DAY_FORMAT.format(parseDate(iso));
}

/** Position horizontale (en %) d'une heure sur une frise allant de l'ouverture à la fermeture. */
export function timelineOffset(time: string, opensAt: string, closesAt: string): number {
  const span = toMinutes(closesAt) - toMinutes(opensAt);
  const value = Math.min(Math.max(toMinutes(time), toMinutes(opensAt)), toMinutes(closesAt));
  return ((value - toMinutes(opensAt)) / span) * 100;
}

/** Heure (au quart d'heure inférieur) correspondant à une position horizontale sur la frise. */
export function timeAtOffset(ratio: number, opensAt: string, closesAt: string): string {
  const span = toMinutes(closesAt) - toMinutes(opensAt);
  const clamped = Math.min(Math.max(ratio, 0), 1);
  return fromMinutes(floorToQuarter(toMinutes(opensAt) + clamped * span));
}

/** « 09:30 » pour l'instant présent. */
export function nowTime(now: Date = new Date()): string {
  return fromMinutes(now.getHours() * 60 + now.getMinutes());
}
