import type {
  Booking,
  BookingDraft,
  Notifications,
  OutOfServiceResult,
  Planning,
  Room,
  SearchCriteria,
  SearchResult,
  Session,
  Site,
  User,
} from './types';

export const SESSION_KEY = 'quorum.session';

/** Erreur renvoyée par l'API (Problem Details RFC 7807), avec le code métier quand il existe. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly code?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/** Message affichable à partir de n'importe quelle réponse en erreur. */
export async function toApiError(response: Response): Promise<ApiError> {
  let detail: string | undefined;
  let code: string | undefined;
  try {
    const body = await response.json();
    detail = typeof body?.detail === 'string' ? body.detail : undefined;
    code = typeof body?.code === 'string' ? body.code : undefined;
  } catch {
    // corps vide ou non JSON
  }
  if (!detail) {
    detail =
      response.status === 401
        ? 'Votre session a expiré. Reconnectez-vous.'
        : response.status === 403
          ? "Cette action ne vous est pas autorisée."
          : response.status >= 500
            ? "Le service de réservation ne répond pas. Réessayez dans un instant."
            : 'La demande n’a pas pu aboutir.';
  }
  return new ApiError(response.status, detail, code);
}

export function readSession(): Session | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as Session;
    return new Date(session.expiresAt).getTime() > Date.now() ? session : null;
  } catch {
    return null;
  }
}

type Listener = () => void;
const unauthorizedListeners = new Set<Listener>();

/** Prévient l'application quand le jeton n'est plus accepté. */
export function onUnauthorized(listener: Listener): () => void {
  unauthorizedListeners.add(listener);
  return () => unauthorizedListeners.delete(listener);
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  const session = readSession();
  if (session) headers.Authorization = `Bearer ${session.token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  let response: Response;
  try {
    response = await fetch(`/api${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError(0, 'Impossible de joindre le service de réservation. Vérifiez votre connexion.');
  }
  if (!response.ok) {
    const error = await toApiError(response);
    if (response.status === 401 && session) unauthorizedListeners.forEach((l) => l());
    throw error;
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

function query(params: Record<string, string | number | string[]>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      if (value.length) search.set(key, value.join(','));
    } else {
      search.set(key, String(value));
    }
  });
  return search.toString();
}

export const api = {
  login: (email: string, password: string) => request<Session>('POST', '/auth/login', { email, password }),
  me: () => request<User>('GET', '/auth/me'),
  sites: () => request<Site[]>('GET', '/sites'),
  rooms: (siteId: string) => request<Room[]>('GET', `/sites/${siteId}/rooms`),
  search: ({ siteId, ...criteria }: SearchCriteria) =>
    request<SearchResult>('GET', `/sites/${siteId}/availability?${query({ ...criteria })}`),
  planning: (siteId: string, date: string) => request<Planning>('GET', `/sites/${siteId}/planning?${query({ date })}`),
  book: (draft: BookingDraft) => request<Booking>('POST', '/bookings', draft),
  myBookings: (past = false) => request<Booking[]>('GET', `/bookings/mine?past=${past}`),
  checkIn: (id: string) => request<Booking>('POST', `/bookings/${id}/check-in`),
  cancel: (id: string, reason?: string) => request<Booking>('POST', `/bookings/${id}/cancel`, { reason: reason ?? null }),
  takeOutOfService: (roomId: string, reason: string) =>
    request<OutOfServiceResult>('PUT', `/rooms/${roomId}/out-of-service`, { reason }),
  putBackInService: (roomId: string) => request<Room>('DELETE', `/rooms/${roomId}/out-of-service`),
  notifications: () => request<Notifications>('GET', '/notifications'),
  markNotificationsRead: () => request<void>('POST', '/notifications/read'),
};
