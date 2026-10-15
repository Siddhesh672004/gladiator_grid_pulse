// the gate's answer for an e-ticket code (GET /api/tickets/verify/{code})
export interface TicketCheck {
  valid: boolean;
  bookingId: number | null;
  driverName: string | null;
  carNumber: number | null;
  trackName: string | null;
  sessionDate: string | null;
  startTime: string | null;
  bookingStatus: string | null;
  message: string;
}
