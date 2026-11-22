'use client';

import { useEffect, useState, type FormEvent } from 'react';
import { messageOf } from '@/lib/toast';
import { Dialog } from './Dialog';

interface ReasonDialogProps {
  open: boolean;
  title: string;
  description?: string;
  label: string;
  confirmLabel: string;
  /** Motif obligatoire (office manager) ou facultatif (organisateur). */
  required: boolean;
  danger?: boolean;
  onClose: () => void;
  onConfirm: (reason: string) => Promise<void>;
}

/** Confirmation d'une action qui touche d'autres personnes, avec son motif. */
export function ReasonDialog({ open, title, description, label, confirmLabel, required, danger, onClose, onConfirm }: ReasonDialogProps) {
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setReason('');
      setError(null);
    }
  }, [open]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onConfirm(reason.trim());
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog open={open} title={title} description={description} onClose={onClose}>
      <form onSubmit={submit} className="flex flex-col gap-4">
        <label className="field">
          <span>
            {label}
            {!required && <span className="font-normal text-ink-faint"> (facultatif)</span>}
          </span>
          <textarea
            className="input h-24 py-2"
            value={reason}
            maxLength={200}
            required={required}
            data-autofocus
            onChange={(e) => setReason(e.target.value)}
            data-testid="reason"
          />
        </label>
        {error && (
          <p role="alert" className="rounded-plaque bg-alert-tint px-3 py-2 text-sm text-alert">
            {error}
          </p>
        )}
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-secondary" onClick={onClose}>
            Revenir
          </button>
          <button
            type="submit"
            className={danger ? 'btn-danger' : 'btn-primary'}
            disabled={saving || (required && !reason.trim())}
            data-testid="reason-submit"
          >
            {confirmLabel}
          </button>
        </div>
      </form>
    </Dialog>
  );
}
