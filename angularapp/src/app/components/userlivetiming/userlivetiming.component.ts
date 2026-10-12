import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { RaceTrack } from '../../models/race-track.model';
import { LiveStanding } from '../../models/live-standing.model';
import { RaceSession, isLiveOrUpcoming, sessionLabel, sessionTime } from '../../models/session.model';
import { RaceService } from '../../services/race.service';
import { SessionService } from '../../services/session.service';
import { RaceControlService } from '../../services/race-control.service';
import { LIVE_REFRESH_MS, RACE_TIME_ZONE_LABEL } from '../../constants';
import { todayIso } from '../../validation';
import { formatRaceTime } from '../adminracecontrol/race-time';
import { carState, isLeaderRow, pickerDateProblem, positionText } from '../adminracecontrol/live-board';

// Live Timing (driver, read only): pick track + date + session, the board reloads every 4 s -> RaceControlService
// only sessions that are live or still to come are listed
@Component({
  selector: 'app-userlivetiming',
  templateUrl: './userlivetiming.component.html'
})
export class UserlivetimingComponent implements OnInit, OnDestroy {

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

  private pollTimer: number = 0;
  private sessionsRequest?: Subscription;
  private standingsRequest?: Subscription;

  constructor(private raceService: RaceService, private sessionService: SessionService,
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

  // load the board now, then every 4 s
  private startPolling(): void {
    this.stopPolling();
    this.loadStandings();
    this.pollTimer = window.setInterval(() => this.loadStandings(), LIVE_REFRESH_MS);
  }

  private loadStandings(): void {
    if (!this.selectedSession) {
      return;
    }
    // an older answer that is still on its way is dropped
    this.standingsRequest?.unsubscribe();
    this.standingsRequest = this.raceControlService.getLiveStandings(this.selectedSession.sessionId).subscribe({
      next: (rows) => {
        this.standings = rows;
        this.lastUpdated = new Date();
        this.boardError = '';
      },
      error: (err) => this.boardError = err.error?.message || 'Live timing is not reachable, retrying...'
    });
  }

  private stopPolling(): void {
    window.clearInterval(this.pollTimer);
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
