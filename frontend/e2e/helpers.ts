import { expect, type Page } from '@playwright/test';

export type DemoAccount = 'mckevin' | 'julie' | 'nicolas';

/** Connexion avec un compte de démonstration depuis l'écran de connexion. */
export async function loginAs(page: Page, who: DemoAccount): Promise<void> {
  await page.goto('/connexion');
  // Repartir d'une session vide : un collaborateur déjà connecté serait redirigé vers l'accueil
  await page.evaluate(() => window.localStorage.clear());
  await page.goto('/connexion');
  await page.getByTestId(`demo-${who}@demo.fr`).click();
  await expect(page).toHaveURL(/\/$/);
}

/**
 * Jour ouvré situé au moins une semaine plus tard : les données de démonstration n'y placent aucune réservation,
 * et l'horaire reste dans le futur quelle que soit l'heure d'exécution.
 */
export function quietWorkingDay(): string {
  const day = new Date();
  day.setDate(day.getDate() + 7);
  while (day.getDay() === 0 || day.getDay() === 6) day.setDate(day.getDate() + 1);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${day.getFullYear()}-${pad(day.getMonth() + 1)}-${pad(day.getDate())}`;
}

/** Recherche de salle depuis la page « Réserver ». */
export async function search(page: Page, date: string, start: string, end: string, attendees: number, equipments: string[] = []) {
  await page.getByTestId('nav-reserver').click();
  await page.getByTestId('search-date').fill(date);
  await page.getByTestId('search-start').selectOption(start);
  await page.getByTestId('search-end').selectOption(end);
  await page.getByTestId('search-attendees').fill(String(attendees));
  for (const code of equipments) await page.getByTestId(`equipment-${code}`).click();
  await page.getByTestId('search-submit').click();
}
