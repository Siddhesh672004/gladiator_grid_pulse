import { COMPETITION_PHASE, RACE_TIME_ZONE, SESSION_STATUS, SESSION_TYPE } from '../constants';

// same fields as SessionResponse in spring, times come as "08:00:00"
// phase / competition fields only for a competition session, approvedCars only in session lists
export interface RaceSession {
  sessionId: number;
  trackId: number;
  trackName: string;
  trackLocation: string;
  sessionType: string;      // PRACTICE or COMPETITION
  phase?: string;           // QUALIFYING or RACING
  competitionId?: number;
  competitionName?: string;
  sessionDate: string;
  startTime: string;
  endTime: string;
  price: number;
  maxCars: number;
  status: string;           // SCHEDULED, LIVE, COMPLETED or CANCELLED
  approvedCars?: number;
}

export interface SessionRequest {
  trackId: number;
  sessionDate: string;
  startTime: string;
  endTime: string;
  price: number;
  maxCars: number;
}

export function sessionLabel(session: RaceSession): string {
  if (session.sessionType === SESSION_TYPE.PRACTICE) {
    return 'Practice';
  }
  const stage = session.phase === COMPETITION_PHASE.QUALIFYING ? 'Qualifying' : 'Race';
  return `${session.competitionName} - ${stage}`;
}

// session times are India time, same as RaceSession.hasStarted() / hasEnded() in java
// the texts compare like dates: "2026-11-20T09:30" < "2026-11-20T10:00"
export function hasStarted(session: RaceSession): boolean {
  return session.sessionDate + 'T' + session.startTime.substring(0, 5) <= nowInRaceZone();
}

export function hasEnded(session: RaceSession): boolean {
  return session.sessionDate + 'T' + session.endTime.substring(0, 5) <= nowInRaceZone();
}

// same rule as RaceSession.isBookable() + a free seat in java: booking closes at the start time or when the seats are full
export function isBookable(session: RaceSession): boolean {
  return session.status === SESSION_STATUS.SCHEDULED && session.phase !== COMPETITION_PHASE.RACING
    && !hasStarted(session) && (session.approvedCars ?? 0) < session.maxCars;
}

export function isLiveOrUpcoming(session: RaceSession): boolean {
  return session.status === SESSION_STATUS.LIVE || (session.status === SESSION_STATUS.SCHEDULED && !hasEnded(session));
}

// "2026-10-09" in India time
export function raceToday(): string {
  return nowInRaceZone().substring(0, 10);
}

// "2026-10-09T16:05" in India time, whatever the clock of the browser says
function nowInRaceZone(): string {
  const formatter = new Intl.DateTimeFormat('en-CA', { timeZone: RACE_TIME_ZONE, year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false });
  let year = '', month = '', day = '', hour = '', minute = '';
  for (const part of formatter.formatToParts(new Date())) {
    if (part.type === 'year') { year = part.value; }
    if (part.type === 'month') { month = part.value; }
    if (part.type === 'day') { day = part.value; }
    if (part.type === 'hour') { hour = part.value; }
    if (part.type === 'minute') { minute = part.value; }
  }
  // some browsers write midnight as 24
  if (hour === '24') {
    hour = '00';
  }
  return `${year}-${month}-${day}T${hour}:${minute}`;
}

// "Practice · 2026-10-09 · 16:00 - 17:00", on the feedback pages
export function sessionSummary(session: RaceSession | undefined): string {
  if (!session) {
    return '—';
  }
  return `${sessionLabel(session)} · ${session.sessionDate} · ${sessionTime(session)}`;
}

export function sessionTime(session: RaceSession): string {
  return `${session.startTime.substring(0, 5)} - ${session.endTime.substring(0, 5)}`;
}
