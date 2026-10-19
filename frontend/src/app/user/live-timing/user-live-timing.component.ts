import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { RaceTrack } from '../../shared/models/race-track.model';
import { LiveStanding } from '../../shared/models/live-standing.model';
import { RaceSession, isLiveOrUpcoming, sessionLabel, sessionTime } from '../../shared/models/session.model';
import { RaceService } from '../../core/services/race.service';
import { SessionService } from '../../core/services/session.service';
import { RaceControlService } from '../../core/services/race-control.service';
import { LiveBoardService } from '../../core/services/live-board.service';
import { RACE_TIME_ZONE_LABEL } from '../../core/constants';
import { todayIso } from '../../shared/utils/validation';
import { formatRaceTime } from '../../shared/live/race-time';
import { carState, isLeaderRow, pickerDateProblem, positionText } from '../../shared/live/live-board';
import { byKey } from '../../shared/utils/track-by';

// Live Timing (driver, read only): pick track + date + session, the board reloads every 4 s -> RaceControlService
// only sessions that are live or still to come are listed
@Component({
  selector: 'app-userlivetiming',
  templateUrl: './user-live-timing.component.html'
})
export class UserLiveTimingComponent implements OnInit, OnDestroy {

  tracks: RaceTrack[] = [];
  selectedTrackId: number | null = null;
  raceDate: string = todayIso();
  sessions: RaceSession[] = [];
  loadingSessions: boolean = false;
  selectedSession: RaceSession | null = null;
  standings: LiveStanding[] = [];
  lastUpdated: Date | null = null;
  dateError: string = '';               // under the Date box
  pickerError: string = '';             // tracks / sessions could not be loaded
  boardError: string = '';
  timeZoneLabel: string = RACE_TIME_ZONE_LABEL;

  private boardWatch?: Subscription;
  private watchedSessionId: number | null = null;
  private sessionsRequest?: Subscription;
  private standingsRequest?: Subscription;

  readonly byTrack = byKey<RaceTrack>('trackId');
  readonly bySession = byKey<RaceSession>('sessionId');
  readonly byCar = byKey<LiveStanding>('raceBookingId');

  constructor(private liveBoard: LiveBoardService, private raceService: RaceService, private sessionService: SessionService,
              private raceControlService: RaceControlService) { }

  ngOnInit(): void {
    this.raceService.getAllTracks().subscribe({
      next: (tracks) => this.tracks = tracks,
      error: () => this.pickerError = 'Unable to load tracks'
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  // the Date box changed: check it first, a cleared or impossible date asks the server nothing
  onDateChange(box: HTMLInputElement): void {
    this.dateError = pickerDateProblem(box);
    this.onPickerChange();
  }

  // track or date changed: forget the open session and list the sessions of that day
  onPickerChange(): void {
    this.changeSession();
    this.sessions = [];
    // an older answer that is still on its way is dropped
    this.sessionsRequest?.unsubscribe();
    this.loadingSessions = false;
    // "Unable to load tracks" stays: without tracks there is nothing to pick
    if (this.tracks.length > 0) {
      this.pickerError = '';
    }
    if (!this.selectedTrackId || !this.raceDate || this.dateError) {
      return;
    }
    this.loadingSessions = true;
    this.sessionsRequest = this.sessionService.getSessionsForTrackOnDate(this.selectedTrackId, this.raceDate).subscribe({
      next: (sessions) => {
        this.sessions = sessions.filter(isLiveOrUpcoming);
        this.loadingSessions = false;
      },
      error: () => {
        this.loadingSessions = false;
        this.pickerError = 'Unable to load sessions';
      }
    });
  }

  selectSession(session: RaceSession): void {
    this.selectedSession = session;
    this.standings = [];
    this.startPolling();
  }

  changeSession(): void {
    this.stopPolling();
    this.selectedSession = null;
    this.standings = [];
    this.boardError = '';
    this.lastUpdated = null;
  }

  // the board comes from the server as it changes (LiveBoardService: server-sent events, polling only as a fallback)
  // called again after an action: the same board is then read once more at once
  private startPolling(): void {
    if (!this.selectedSession) {
      this.stopPolling();
      return;
    }
    if (this.watchedSessionId === this.selectedSession.sessionId) {
      this.loadStandings();
      return;
    }
    this.stopPolling();
    this.watchedSessionId = this.selectedSession.sessionId;
    this.boardWatch = this.liveBoard.watch(this.watchedSessionId).subscribe({
      next: (rows) => this.showBoard(rows),
      error: (err) => this.boardError = err.error?.message || 'Live timing is not reachable, retrying...'
    });
  }

  private loadStandings(): void {
    if (!this.selectedSession) {
      return;
    }
    // an older answer that is still on its way is dropped
    this.standingsRequest?.unsubscribe();
    this.standingsRequest = this.raceControlService.getLiveStandings(this.selectedSession.sessionId).subscribe({
      next: (rows) => this.showBoard(rows),
      error: (err) => this.boardError = err.error?.message || 'Live timing is not reachable, retrying...'
    });
  }

  private showBoard(rows: LiveStanding[]): void {
    this.standings = rows;
    this.lastUpdated = new Date();
    this.boardError = '';
  }

  private stopPolling(): void {
    this.boardWatch?.unsubscribe();
    this.watchedSessionId = null;
    this.standingsRequest?.unsubscribe();
  }

  // ---- what the table shows ----
  label(session: RaceSession): string {
    return sessionLabel(session);
  }

  time(session: RaceSession): string {
    return sessionTime(session);
  }

  stateOf(row: LiveStanding): string {
    return carState(row);
  }

  positionLabel(row: LiveStanding, index: number): string {
    return positionText(row, index);
  }

  isLeader(row: LiveStanding, index: number): boolean {
    return isLeaderRow(row, index);
  }

  lastLapLabel(row: LiveStanding): string {
    return formatRaceTime(row.lastLapAt);
  }

  get lastUpdatedLabel(): string {
    return formatRaceTime(this.lastUpdated);
  }
}
