'use client';

import { X } from 'lucide-react';
import { useEffect, useRef, type ReactNode } from 'react';

interface DialogProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  /** Ligne d'information sous le titre. */
  description?: ReactNode;
}

/** Fenêtre modale accessible basée sur l'élément natif <dialog> (focus piégé, Échap pour fermer). */
export function Dialog({ open, title, description, onClose, children }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      if (typeof dialog.showModal === 'function') dialog.showModal();
      else dialog.setAttribute('open', '');
      // Le navigateur place le focus sur le premier élément (le bouton Fermer) : on le donne au premier champ utile
      dialog.querySelector<HTMLElement>('[data-autofocus]')?.focus();
    }
    if (!open && dialog.open) {
      if (typeof dialog.close === 'function') dialog.close();
      else dialog.removeAttribute('open');
    }
  }, [open]);

  return (
    <dialog
      ref={ref}
      aria-labelledby="dialog-title"
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      className="w-[min(480px,calc(100vw-2rem))] rounded-plaque bg-white p-0 text-ink shadow-dialog backdrop:bg-ink/40"
    >
      {open && (
        <div className="p-6">
          <div className="mb-4 flex items-start justify-between gap-4">
            <div>
              <h2 id="dialog-title">{title}</h2>
              {description && <div className="mt-1 text-sm text-ink-soft">{description}</div>}
            </div>
            <button type="button" className="btn-quiet -mr-2 -mt-1" onClick={onClose} aria-label="Fermer">
              <X className="size-4" />
            </button>
          </div>
          {children}
        </div>
      )}
    </dialog>
  );
}
