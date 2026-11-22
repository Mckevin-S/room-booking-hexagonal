import type { Metadata, Viewport } from 'next';
import type { ReactNode } from 'react';
import { Providers } from '@/components/Providers';
import './globals.css';

export const metadata: Metadata = {
  title: { default: 'Quorum', template: '%s · Quorum' },
  description: 'Réservation des salles de réunion de l’entreprise',
};

export const viewport: Viewport = { themeColor: '#2346D8' };

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="fr">
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
