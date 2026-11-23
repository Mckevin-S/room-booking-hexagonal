/** Places occupées par la réunion sur la capacité de la salle : une case par place. */
export function CapacityMeter({ seats, attendees }: { seats: number; attendees: number }) {
  const used = Math.min(attendees, seats);
  return (
    <div className="flex items-center gap-2" aria-label={`${attendees} participants pour ${seats} places`}>
      <div className="flex flex-wrap gap-[3px]" aria-hidden>
        {Array.from({ length: seats }, (_, i) => (
          <span key={i} className={`block size-2 rounded-[2px] ${i < used ? 'bg-signal' : 'bg-line'}`} />
        ))}
      </div>
      <span className="text-xs text-ink-soft">
        {attendees}/{seats} places
      </span>
    </div>
  );
}
