import { ApiError, SESSION_KEY, api, readSession, toApiError } from './api';

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

describe('client de l’API', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.localStorage.clear();
  });

  it('reprend le détail et le code métier des erreurs RFC 7807', async () => {
    const error = await toApiError(jsonResponse(409, { detail: 'La salle Vauban est déjà réservée sur ce créneau.', code: 'SLOT_TAKEN' }));
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(409);
    expect(error.code).toBe('SLOT_TAKEN');
    expect(error.message).toBe('La salle Vauban est déjà réservée sur ce créneau.');
  });

  it('donne un message clair quand la réponse est vide', async () => {
    expect((await toApiError(new Response(null, { status: 502 }))).message).toMatch(/ne répond pas/);
    expect((await toApiError(new Response(null, { status: 401 }))).message).toMatch(/session a expiré/);
  });

  it('ignore une session expirée', () => {
    window.localStorage.setItem(SESSION_KEY, JSON.stringify({ token: 't', expiresAt: '2000-01-01T00:00:00Z', user: {} }));
    expect(readSession()).toBeNull();
  });

  it('envoie le jeton et les critères de recherche', async () => {
    window.localStorage.setItem(SESSION_KEY, JSON.stringify({ token: 'jeton', expiresAt: '2999-01-01T00:00:00Z', user: {} }));
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(200, { rooms: [], suggestions: [] }));

    await api.search({ siteId: 'LIL', date: '2026-12-02', start: '10:00', end: '11:00', attendees: 4, equipments: ['SCREEN', 'WHITEBOARD'] });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/sites/LIL/availability?date=2026-12-02&start=10%3A00&end=11%3A00&attendees=4&equipments=SCREEN%2CWHITEBOARD');
    expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer jeton');
  });

  it('signale un service injoignable', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));
    await expect(api.sites()).rejects.toMatchObject({ status: 0, message: expect.stringMatching(/Impossible de joindre/) });
  });
});
