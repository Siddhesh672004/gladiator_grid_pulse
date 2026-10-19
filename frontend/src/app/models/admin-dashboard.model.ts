import { Revenue } from './payment.model';

// GET /api/admin/dashboard, same names as the java records

export interface DashboardMetrics {
  totalUsers: number;
  registrations: number;
  totalTracks: number;
  totalBookings: number;
  activeRaces: number;
  totalRevenue: number;
  totalFeedback: number;
  feedbackNeedsAttention: number;
}

export interface ChartPoint {
  label: string;
  value: number;
}

export interface BookingRaceStatistic {
  label: string;
  bookings: number;
  races: number;
}

export interface RecentRegistration {
  raceBookingId: number;
  username: string;
  email: string;
  registeredAt: string | null;    // UTC
  status: string;
  userRole: string;
  trackName: string;
  session: string;
  raceStatus: string;
}

export interface LiveRace {
  sessionId: number;
  trackName: string;
  session: string;
  startTime: string;
  raceStatus: string;
  activeCars: number;
  currentLap: number;
  totalLaps: number | null;
  progressPercentage: number | null;
}

export interface AdminDashboard {
  metrics: DashboardMetrics;
  registrationStatus: ChartPoint[];
  trackCategories: ChartPoint[];
  bookingRaceStatistics: BookingRaceStatistic[];
  recentRegistrations: RecentRegistration[];
  liveRaces: LiveRace[];
  statement: Revenue;
}
