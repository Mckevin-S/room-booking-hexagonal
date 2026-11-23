import { expect, test } from '@playwright/test';
import { loginAs, quietWorkingDay, search } from './helpers';

const DAY = quietWorkingDay();

test.describe.serial('Quorum, de la recherche à la gestion des imprévus', () => {
  test('un collaborateur trouve la salle la mieux ajustée et la réserve', async ({ page }) => {
    await loginAs(page, 'mckevin');
    await search(page, DAY, '10:00', '11:00', 5, ['SCREEN']);

    const plaques = page.locator('[data-testid^="room-"]');
    await expect(plaques.first()).toContainText('Vauban');
    await page.getByTestId('book-LIL-VAU').click();
    await page.getByTestId('booking-title').fill('Revue de sprint E2E');
    await page.getByTestId('booking-submit').click();

    await expect(page).toHaveURL(/mes-reservations/);
    const row = page.getByTestId('booking-row').filter({ hasText: 'Revue de sprint E2E' });
    await expect(row).toContainText('Salle Vauban');
    await expect(row.getByTestId('status')).toHaveText('Confirmée');
  });

  test('il annule sa réservation et la salle redevient libre', async ({ page }) => {
    await loginAs(page, 'mckevin');
    await page.getByTestId('nav-mes-reservations').click();
    const row = page.getByTestId('booking-row').filter({ hasText: 'Revue de sprint E2E' });
    await row.getByTestId('cancel').click();
    await page.getByTestId('reason-submit').click();
    await expect(row.getByTestId('status')).toHaveText('Annulée');

    await search(page, DAY, '10:00', '11:00', 5, ['SCREEN']);
    await expect(page.locator('[data-testid^="room-"]').first()).toContainText('Vauban');
  });

  test('il réserve une plage libre directement sur le planning', async ({ page }) => {
    await loginAs(page, 'mckevin');
    await page.getByTestId('nav-planning').click();
    await page.getByTestId('planning-date').fill(DAY);

    const track = page.getByTestId('row-LIL-WAZ').locator(':scope > div').nth(1);
    const box = (await track.boundingBox())!;
    // 14 h sur une journée de 8 h à 20 h : la moitié de la frise
    await page.mouse.click(box.x + box.width * 0.5 + 2, box.y + box.height / 2);
    await expect(page.getByTestId('booking-start')).toHaveValue('14:00');
    await page.getByTestId('booking-title').fill('Point équipe E2E');
    await page.getByTestId('booking-submit').click();

    await expect(page.getByTestId('row-LIL-WAZ').getByTestId('planned-booking')).toContainText('Point équipe E2E');
  });

  test('quand aucune salle n’est libre, les créneaux les plus proches sont proposés', async ({ page }) => {
    await loginAs(page, 'julie');
    await search(page, DAY, '14:00', '15:00', 15);
    await page.getByTestId('book-LIL-CIT').click();
    await page.getByTestId('booking-title').fill('Réunion plénière E2E');
    await page.getByTestId('booking-submit').click();
    await expect(page).toHaveURL(/mes-reservations/);

    await loginAs(page, 'nicolas');
    await search(page, DAY, '14:00', '15:00', 15);
    await expect(page.getByText("Aucune salle adaptée n'est libre sur ce créneau")).toBeVisible();
    await expect(page.getByTestId('suggestion-LIL-CIT-13:00')).toBeVisible();
    await expect(page.getByTestId('suggestion-LIL-CIT-15:00')).toBeVisible();
  });

  test('l’office manager met une salle hors service et l’organisateur est prévenu', async ({ page }) => {
    await loginAs(page, 'nicolas');
    await page.getByTestId('nav-salles').click();
    await page.getByTestId('close-LIL-WAZ').click();
    await page.getByTestId('reason').fill('Dégât des eaux');
    await page.getByTestId('reason-submit').click();
    await expect(page.getByTestId('room-LIL-WAZ')).toContainText('Hors service : Dégât des eaux');

    await loginAs(page, 'mckevin');
    await expect(page.getByTestId('nav-salles')).toHaveCount(0);
    await page.getByTestId('notifications').click();
    await expect(page.getByText(/Point équipe E2E.*Salle Wazemmes hors service : Dégât des eaux/)).toBeVisible();

    await loginAs(page, 'nicolas');
    await page.getByTestId('nav-salles').click();
    await page.getByTestId('reopen-LIL-WAZ').click();
    await expect(page.getByTestId('room-LIL-WAZ')).toContainText('En service');
  });
});
