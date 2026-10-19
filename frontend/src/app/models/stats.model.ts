import { ChartPoint } from './admin-dashboard.model';

// the driver's own numbers (GET /api/stats/me), completed sessions only
export interface DriverStats {
  sessionsDriven: number;
  totalLaps: number;
  competitionsEntered: number;
  wins: number;
  podiums: number;
  dnfs: number;
  bestRaceFinish: number | null;
  bestQualifying: number | null;
  lapsPerMonth: ChartPoint[];          // Jan to Dec of this year
}

// one line of the public leaderboard (GET /api/stats/leaderboard)
export interface LeaderboardEntry {
  position: number;
  username: string;
  wins: number;
  podiums: number;
  laps: number;
}
