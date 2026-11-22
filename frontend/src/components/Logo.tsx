/** Logotype : une plaque de porte stylisée, l'objet emblématique d'une salle de réunion. */
export function Logo({ inverted = false, compact = false }: { inverted?: boolean; compact?: boolean }) {
  return (
    <span className="inline-flex items-center gap-2 font-bold">
      <svg width="26" height="26" viewBox="0 0 26 26" aria-hidden>
        <rect x="1" y="1" width="24" height="24" rx="5" fill={inverted ? '#fff' : '#2346D8'} />
        <rect x="6" y="7" width="14" height="3" rx="1.5" fill={inverted ? '#2346D8' : '#fff'} />
        <rect x="6" y="13" width="9" height="2" rx="1" fill={inverted ? '#2346D8' : '#fff'} opacity="0.7" />
        <circle cx="18.5" cy="18" r="2.5" fill="#16865F" />
      </svg>
      <span className={`text-lg tracking-[-0.01em] ${inverted ? 'text-white' : 'text-ink'} ${compact ? 'hidden md:inline' : ''}`}>Quorum</span>
    </span>
  );
}
