// one row of the public live board (GET /api/race-control/session/{id})
// the driver is only { userId, username }: no email / mobile number / license
export interface LiveStanding {
  raceBookingId: number;
  user: { userId: number; username: string };
  carNumber: number;
  carCategory: string;
  lapCount: number;
  lastLapAt?: string;
  raceState: string;
  finalPosition?: number;
  gridPosition?: number;
}
