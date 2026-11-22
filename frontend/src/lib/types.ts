/** Types échangés avec l'API (voir backend : Views et ApiDtos). Heures au format « HH:mm ». */

export type EquipmentCode = 'SCREEN' | 'VIDEO_CONFERENCE' | 'WHITEBOARD' | 'ACCESSIBLE';

export interface User {
  id: number;
  email: string;
  fullName: string;
  initials: string;
  officeManager: boolean;
  homeSiteId: string;
}

export interface Session {
  token: string;
  expiresAt: string;
  user: User;
}

export interface Site {
  id: string;
  name: string;
  city: string;
  opensAt: string;
  closesAt: string;
}

export interface Equipment {
  code: EquipmentCode;
  label: string;
}

export interface Room {
  id: string;
  siteId: string;
  name: string;
  floor?: string;
  seats: number;
  equipments: Equipment[];
  inService: boolean;
  outOfServiceReason?: string;
}

export type BookingStatus = 'CONFIRMED' | 'CHECKED_IN' | 'CANCELLED' | 'RELEASED';

export interface Booking {
  id: string;
  roomId: string;
  roomName: string;
  siteName: string;
  title: string;
  organizerName: string;
  mine: boolean;
  date: string;
  start: string;
  end: string;
  attendees: number;
  status: BookingStatus;
  statusLabel: string;
  canCheckIn: boolean;
  canCancel: boolean;
  checkInOpensAt: string;
  releasedIfAbsentAt: string;
  cancellationReason?: string;
}

export interface Suggestion {
  room: Room;
  start: string;
  end: string;
}

export interface SearchResult {
  rooms: Room[];
  suggestions: Suggestion[];
}

export interface PlannedBooking {
  id: string;
  title: string;
  organizerName: string;
  mine: boolean;
  start: string;
  end: string;
  status: BookingStatus;
}

export interface Planning {
  site: Site;
  date: string;
  open: boolean;
  rooms: { room: Room; bookings: PlannedBooking[] }[];
}

export interface AppNotification {
  id: number;
  message: string;
  createdAt: string;
  read: boolean;
}

export interface Notifications {
  unread: number;
  items: AppNotification[];
}

export interface OutOfServiceResult {
  room: Room;
  cancelledBookings: number;
}

export interface SearchCriteria {
  siteId: string;
  date: string;
  start: string;
  end: string;
  attendees: number;
  equipments: EquipmentCode[];
}

export interface BookingDraft {
  roomId: string;
  date: string;
  start: string;
  end: string;
  title: string;
  attendees: number;
}
