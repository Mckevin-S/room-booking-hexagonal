'use client';

import { Bell, LogOut } from 'lucide-react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { api } from '@/lib/api';
import { useSession } from '@/lib/session';
import type { Notifications } from '@/lib/types';
import { Logo } from './Logo';

const NAV = [
  { href: '/', label: 'Accueil' },
  { href: '/reserver', label: 'Réserver' },
  { href: '/planning', label: 'Planning' },
  { href: '/mes-reservations', label: 'Mes réservations' },
];

const RELATIVE = new Intl.RelativeTimeFormat('fr', { numeric: 'auto' });

function ago(iso: string): string {
  const minutes = Math.round((new Date(iso).getTime() - Date.now()) / 60000);
  if (Math.abs(minutes) < 60) return RELATIVE.format(minutes, 'minute');
  const hours = Math.round(minutes / 60);
  if (Math.abs(hours) < 24) return RELATIVE.format(hours, 'hour');
  return RELATIVE.format(Math.round(hours / 24), 'day');
}

function NotificationsMenu() {
  const [data, setData] = useState<Notifications | null>(null);
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const load = useCallback(() => {
    api.notifications().then(setData).catch(() => undefined);
  }, []);

  useEffect(() => {
    load();
    const timer = window.setInterval(load, 60_000);
    return () => window.clearInterval(timer);
  }, [load]);

  useEffect(() => {
    if (!open) return;
    const close = (event: globalThis.MouseEvent) => {
      if (!ref.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);

  async function toggle() {
    const next = !open;
    setOpen(next);
    if (next && data?.unread) {
      await api.markNotificationsRead().catch(() => undefined);
      setData((d) => (d ? { unread: 0, items: d.items } : d));
    }
  }

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        className="btn-quiet relative"
        onClick={toggle}
        aria-expanded={open}
        aria-label={data?.unread ? `Notifications, ${data.unread} non lues` : 'Notifications'}
        data-testid="notifications"
      >
        <Bell className="size-5" />
        {!!data?.unread && (
          <span className="absolute right-0.5 top-0.5 min-w-4 rounded-full bg-alert px-1 text-[10px] font-bold leading-4 text-white">
            {data.unread}
          </span>
        )}
      </button>
      {open && (
        <div className="absolute right-0 top-11 z-40 w-[min(380px,calc(100vw-2rem))] rounded-plaque bg-white shadow-dialog">
          <p className="border-b border-line px-4 py-3 font-bold">Notifications</p>
          {data?.items.length ? (
            <ul className="max-h-96 overflow-y-auto">
              {data.items.map((n) => (
                <li key={n.id} className="border-b border-line px-4 py-3 text-sm last:border-b-0">
                  <p className="text-ink">{n.message}</p>
                  <p className="mt-1 text-xs text-ink-faint">{ago(n.createdAt)}</p>
                </li>
              ))}
            </ul>
          ) : (
            <p className="px-4 py-6 text-sm text-ink-soft">
              Vous serez prévenu ici si l&apos;une de vos réservations est annulée ou libérée sans votre intervention.
            </p>
          )}
        </div>
      )}
    </div>
  );
}

/** Coquille des pages connectées : barre de navigation en haut, contenu aligné à gauche. */
export function AppShell({ children }: { children: ReactNode }) {
  const { user, logout } = useSession();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (user === null) router.replace('/connexion');
  }, [user, router]);

  if (!user) return null;

  const nav = user.officeManager ? [...NAV, { href: '/salles', label: 'Salles' }] : NAV;

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-30 border-b border-line bg-white">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-3 px-4 sm:gap-6 sm:px-6">
          <Link href="/" aria-label="Quorum, accueil">
            <Logo compact />
          </Link>
          <nav className="flex h-full flex-1 gap-1 overflow-x-auto" aria-label="Navigation principale">
            {nav.map((item) => {
              const active = item.href === '/' ? pathname === '/' : pathname.startsWith(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? 'page' : undefined}
                  className={`flex items-center whitespace-nowrap border-b-[3px] px-3 text-sm font-bold ${
                    active ? 'border-signal text-ink' : 'border-transparent text-ink-soft hover:text-ink'
                  }`}
                  data-testid={`nav-${item.href.replace('/', '') || 'accueil'}`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
          <NotificationsMenu />
          <div className="hidden items-center gap-2 sm:flex">
            <span className="grid size-8 place-items-center rounded-full bg-ink text-xs font-bold text-white" aria-hidden>
              {user.initials}
            </span>
            <span className="text-sm">
              <span className="block font-bold leading-tight">{user.fullName}</span>
              <span className="block text-xs text-ink-soft">{user.officeManager ? 'Office manager' : 'Collaborateur'}</span>
            </span>
          </div>
          <button
            type="button"
            className="btn-quiet"
            onClick={() => {
              logout();
              router.replace('/connexion');
            }}
            aria-label="Se déconnecter"
            data-testid="logout"
          >
            <LogOut className="size-5" />
          </button>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6">{children}</main>
    </div>
  );
}
