'use client';

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { SESSION_KEY, api, onUnauthorized, readSession } from './api';
import type { Session, User } from './types';

interface SessionContextValue {
  /** null : pas connecté ; undefined : lecture du stockage en cours. */
  user: User | null | undefined;
  login: (email: string, password: string) => Promise<User>;
  logout: () => void;
}

const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null | undefined>(undefined);

  useEffect(() => {
    setUser(readSession()?.user ?? null);
  }, []);

  const logout = useCallback(() => {
    window.localStorage.removeItem(SESSION_KEY);
    setUser(null);
  }, []);

  useEffect(() => onUnauthorized(logout), [logout]);

  const login = useCallback(async (email: string, password: string) => {
    const session: Session = await api.login(email, password);
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    setUser(session.user);
    return session.user;
  }, []);

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout]);
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext);
  if (!context) throw new Error('useSession doit être utilisé dans <SessionProvider>');
  return context;
}
