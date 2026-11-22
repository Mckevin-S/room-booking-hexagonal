'use client';

import { CheckCircle2, CircleAlert, X } from 'lucide-react';
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';

type Tone = 'success' | 'error';
interface Toast {
  id: number;
  tone: Tone;
  message: string;
}

interface ToastContextValue {
  success: (message: string) => void;
  error: (error: unknown) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);
let nextId = 1;

export function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Une erreur inattendue est survenue.';
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: number) => setToasts((all) => all.filter((t) => t.id !== id)), []);

  const push = useCallback(
    (tone: Tone, message: string) => {
      const id = nextId++;
      setToasts((all) => [...all.slice(-2), { id, tone, message }]);
      window.setTimeout(() => dismiss(id), tone === 'error' ? 7000 : 4500);
    },
    [dismiss],
  );

  const value = useMemo<ToastContextValue>(
    () => ({ success: (m) => push('success', m), error: (e) => push('error', messageOf(e)) }),
    [push],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed bottom-4 right-4 z-50 flex w-[min(420px,calc(100vw-2rem))] flex-col gap-2">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role={toast.tone === 'error' ? 'alert' : 'status'}
            className={`pointer-events-auto flex items-start gap-3 rounded-plaque border-l-4 bg-white px-4 py-3 text-sm shadow-dialog ${
              toast.tone === 'error' ? 'border-alert' : 'border-present'
            }`}
          >
            {toast.tone === 'error' ? (
              <CircleAlert className="mt-0.5 size-4 shrink-0 text-alert" aria-hidden />
            ) : (
              <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-present" aria-hidden />
            )}
            <p className="flex-1 text-ink">{toast.message}</p>
            <button type="button" onClick={() => dismiss(toast.id)} className="text-ink-faint hover:text-ink" aria-label="Fermer">
              <X className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast doit être utilisé dans <ToastProvider>');
  return context;
}
