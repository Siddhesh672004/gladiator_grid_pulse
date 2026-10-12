import { RaceSession } from './session.model';

// same fields as CompetitionResponse in spring
export interface Competition {
  competitionId: number;
  competitionName: string;
  trackId: number;
  trackName: string;
  trackCategory: string;
  competitionDate: string;
  status: string;           // UPCOMING, QUALIFYING, QUALIFYING_COMPLETED, RACING, COMPLETED or CANCELLED
  qualifyingLaps: number;
  raceLaps: number;
  maxCars: number;
  qualifyingSession: RaceSession;
  raceSession: RaceSession;
}

// POST /api/competitions, laps and max cars come from the rule
export interface CompetitionRequest {
  trackId: number;
  competitionName: string;
  competitionDate: string;
  qualifyingStartTime: string;
  qualifyingEndTime: string;
  raceStartTime: string;
  raceEndTime: string;
  entryFee: number;
}

// GET /api/competitions/rule/{trackId}
export interface CompetitionRule {
  trackCategory: string;
  qualifyingLaps: number;
  raceLaps: number;
  maxCars: number;
}
