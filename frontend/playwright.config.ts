import { defineConfig, devices } from '@playwright/test';

/** Tests de bout en bout sur l'application complète (docker compose up). */
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  // Les scénarios modifient les données de démonstration : pas de rejeu automatique
  retries: 0,
  forbidOnly: !!process.env.CI,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    locale: 'fr-FR',
    timezoneId: 'Europe/Paris',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
