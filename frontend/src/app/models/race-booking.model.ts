import { RaceSession } from './session.model';
import { User } from './user.model';

// one car in one session, track / date / times are inside booking.session
export interface RaceBooking {
  raceBookingId?: number;
  user: User;
  session: RaceSession;
  status: string;           // Pending, Approved, Rejected or Waitlisted
  carNumber: number;
  carCategory: string;
  driverLicenseNumber: string;
  comments?: string;
  adminNote?: string;
  totalPrice?: number;
  termsAcceptedAt?: string;
  lapCount?: number;
  lastLapAt?: string;
  raceState?: string;
  finalPosition?: number;
  gridPosition?: number;
  paymentStatus?: string;   // PAID, REFUND_PENDING, REFUNDED ...
  waitlistPosition?: number; // 1 = next to move up, only while Waitlisted
}

// POST /api/payments/order and /confirm. no user, no license: the backend takes both itself
// the status flag of both registrations pages: "Waitlisted #2", empty = the status word itself
export function statusLabel(booking: RaceBooking): string {
  return booking.status === 'Waitlisted' && booking.waitlistPosition ? 'Waitlisted #' + booking.waitlistPosition : '';
}

// the "Race" column of both registrations pages
export function raceStatusText(booking: RaceBooking): string {
  if (booking.raceState === 'FINISHED') {
    if (booking.finalPosition) {
      return 'Finished P' + booking.finalPosition;
    }
    return 'Finished';
  }
  return (booking.raceState || 'NOT_STARTED') + ' - Laps: ' + (booking.lapCount ?? 0);
}

export interface RaceBookingRequest {
  sessionId: number;
  carNumber: number;
  carCategory: string;
  comments?: string;
  termsAccepted: boolean;
}

// PUT /api/race-booking/{id}, a reject needs an adminNote
export interface BookingStatusRequest {
  status?: string;
  adminNote?: string;
}

export interface CarCategory {
  name: string;
  imageUrl: string | null;
}
