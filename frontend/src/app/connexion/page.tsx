'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState, type FormEvent } from 'react';
import { Logo } from '@/components/Logo';
import { useSession } from '@/lib/session';
import { messageOf } from '@/lib/toast';

const DEMO_ACCOUNTS = [
  { email: 'mckevin@demo.fr', name: 'Mckevin Kamguem', role: 'Collaborateur, Lille' },
  { email: 'julie@demo.fr', name: 'Julie Dumont', role: 'Collaboratrice, Lille' },
  { email: 'nicolas@demo.fr', name: 'Nicolas Lefebvre', role: 'Office manager, Lille' },
];

export default function LoginPage() {
  const { user, login } = useSession();
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (user) router.replace('/');
  }, [user, router]);

  async function signIn(address: string, secret: string) {
    setPending(true);
    setError(null);
    try {
      await login(address, secret);
      router.replace('/');
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setPending(false);
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    signIn(email, password);
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">
      <section className="relative hidden flex-col justify-between overflow-hidden bg-signal p-12 text-white lg:flex">
        <Logo inverted />
        <div className="max-w-md">
          <p className="text-4xl font-bold leading-[1.1] tracking-[-0.02em]">
            La bonne salle, au bon moment, sans réunion fantôme.
          </p>
          <p className="mt-4 text-white/80">
            Cherchez par taille et par équipement, réservez au quart d&apos;heure, confirmez votre présence en arrivant :
            une salle laissée vide est rendue aux autres au bout de 15 minutes.
          </p>
        </div>
        {/* Plaque de porte : l'objet que l'on croise devant chaque salle */}
        <div className="w-80 rounded-plaque bg-white p-5 text-ink shadow-dialog" aria-hidden>
          <div className="flex items-baseline justify-between">
            <span className="text-2xl font-bold">Vauban</span>
            <span className="text-sm text-ink-soft">3e étage</span>
          </div>
          <div className="mt-3 flex items-center gap-2 text-sm">
            <span className="size-2.5 rounded-full bg-present" />
            <span className="font-bold text-present">Libre jusqu&apos;à 14h00</span>
          </div>
          <div className="mt-3 border-t border-line pt-3 text-sm text-ink-soft">6 places · Écran · Tableau blanc</div>
        </div>
      </section>

      <section className="flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <Logo />
          </div>
          <h1>Connexion</h1>
          <p className="mt-1 text-sm text-ink-soft">Utilisez votre adresse professionnelle.</p>
          <form onSubmit={submit} className="mt-6 flex flex-col gap-4">
            <label className="field">
              <span>E-mail</span>
              <input className="input" type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </label>
            <label className="field">
              <span>Mot de passe</span>
              <input
                className="input"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </label>
            {error && (
              <p role="alert" className="rounded-plaque bg-alert-tint px-3 py-2 text-sm text-alert">
                {error}
              </p>
            )}
            <button type="submit" className="btn-primary h-10" disabled={pending}>
              Se connecter
            </button>
          </form>

          <div className="mt-10 border-t border-line pt-6">
            <p className="text-sm font-bold">Explorer la démonstration</p>
            <p className="mt-1 text-sm text-ink-soft">Comptes fictifs, mot de passe demo1234.</p>
            <ul className="mt-3 flex flex-col gap-2">
              {DEMO_ACCOUNTS.map((account) => (
                <li key={account.email}>
                  <button
                    type="button"
                    className="flex w-full items-center justify-between rounded-plaque border border-line bg-white px-3 py-2 text-left text-sm hover:border-signal"
                    onClick={() => signIn(account.email, 'demo1234')}
                    disabled={pending}
                    data-testid={`demo-${account.email}`}
                  >
                    <span className="font-bold">{account.name}</span>
                    <span className="text-ink-soft">{account.role}</span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>
    </div>
  );
}
