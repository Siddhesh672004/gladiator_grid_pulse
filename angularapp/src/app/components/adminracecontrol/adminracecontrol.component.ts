import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { RaceTrack } from '../../models/race-track.model';
import { LiveStanding } from '../../models/live-standing.model';
import { RaceSession, sessionLabel, sessionTime } from '../../models/session.model';
import { RaceService } from '../../services/race.service';
import { SessionService } from '../../services/session.service';
import { RaceControlService } from '../../services/race-control.service';
import { LiveBoardService } from '../../services/live-board.service';
import { RACE_STATES, RACE_TIME_ZONE_LABEL, SESSION_STATUS, SESSION_TYPE } from '../../constants';
import { todayIso } from '../../validation';
import { formatRaceTime } from './race-time';
import { carState, isLeaderRow, pickerDateProblem, positionText } from './live-board';

// Practice Control (admin): pick track + date + practice session, then Start, +1 Lap, Retire per car and
// Finish / Reset for the session -> RaceControlService. a competition is run from its own Competition Control page
@Component({
  selector: 'app-adminracecontrol',
  templateUrl: './adminracecontrol.component.html'
})
export class AdminracecontrolComponent implements OnInit, OnDestroy {

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

  busyId: number | null = null;         // the car whose button was just pressed
  sessionBusy: boolean = false;
  showResetPopup: boolean = false;
  actionError: string = '';

  private boardWatch?: Subscription;
  private watchedSessionId: number | null = null;
  private sessionsRequest?: Subscription;
  private standingsRequest?: Subscription;
  private pageClosed: boolean = false;

  constructor(private liveBoard: LiveBoardService, private raceService: RaceService, private sessionService: SessionService,
              private raceControlService: RaceControlService) { }

  ngOnInit(): void {
    this.raceService.getAllTracks().subscribe({
      next: (tracks) => this.tracks = tracks,
      error: () => this.pickerError = 'Unable to load tracks'
    });
  }

  ngOnDestroy(): void {
    this.pageClosed = true;
    this.stopPolling();
  }

  // the Date box changed: check it first, a cleared or impossible date asks the server nothing
  onDateChange(box: HTMLInputElement): void {
    this.dateError = pickerDateProblem(box);
    this.onPickerChange();
  }

  // track or date changed: forget the open session and list the practice sessions of that day
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
        this.sessions = sessions.filter(session => session.sessionType === SESSION_TYPE.PRACTICE);
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
    if (!this.selectedSession || this.pageClosed) {
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

  // race control changes the status (SCHEDULED -> LIVE -> COMPLETED), so read the session again
  private reloadSelectedSession(): void {
    if (!this.selectedSession) {
      return;
    }
    this.sessionService.getSessionById(this.selectedSession.sessionId).subscribe({
      next: (session) => this.selectedSession = session
    });
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

  // ---- which buttons a car gets ----
  showsStart(row: LiveStanding): boolean {
    return this.stateOf(row) === RACE_STATES.NOT_STARTED;
  }

  showsRaceActions(row: LiveStanding): boolean {
    return this.stateOf(row) === RACE_STATES.RACING;
  }

  canStart(row: LiveStanding): boolean {
    return this.showsStart(row) && this.isIdle();
  }

  canRace(row: LiveStanding): boolean {
    return this.showsRaceActions(row) && this.isIdle();
  }

  // also when every car has retired: the session must still be closed, or it would stay LIVE
  get canFinish(): boolean {
    return this.isIdle() && this.standings.length > 0 && this.selectedSession?.status !== SESSION_STATUS.COMPLETED;
  }

  // a finished session is locked: its results (and the feedback about it) stay as they are
  get canReset(): boolean {
    return this.canFinish;
  }

  private isIdle(): boolean {
    return this.busyId === null && !this.sessionBusy;
  }

  // ---- the car buttons ----
  start(row: LiveStanding): void {
    if (!row.raceBookingId || !this.canStart(row)) {
      return;
    }
    this.markCarBusy(row);
    this.raceControlService.startRace(row.raceBookingId).subscribe({
      next: () => this.carActionDone(),
      error: (err) => this.carActionFailed(err)
    });
  }

  addLap(row: LiveStanding): void {
    if (!row.raceBookingId || !this.canRace(row)) {
      return;
    }
    this.markCarBusy(row);
    this.raceControlService.incrementLap(row.raceBookingId).subscribe({
      next: () => this.carActionDone(),
      error: (err) => this.carActionFailed(err)
    });
  }

  retire(row: LiveStanding): void {
    if (!row.raceBookingId || !this.canRace(row)) {
      return;
    }
    this.markCarBusy(row);
    this.raceControlService.retireCar(row.raceBookingId).subscribe({
      next: () => this.carActionDone(),
      error: (err) => this.carActionFailed(err)
    });
  }

  private markCarBusy(row: LiveStanding): void {
    this.actionError = '';
    this.busyId = row.raceBookingId;
  }

  private carActionDone(): void {
    this.busyId = null;
    this.reloadSelectedSession();   // the first Start makes the session LIVE
    this.startPolling();
  }

  private carActionFailed(err: any): void {
    this.busyId = null;
    this.actionError = err.error?.message || 'The action could not be completed';
    this.startPolling();
  }

  // ---- the session buttons ----
  finishSession(): void {
    if (!this.selectedSession || !this.isIdle()) {
      return;
    }
    this.markSessionBusy();
    this.raceControlService.finishSession(this.selectedSession.sessionId).subscribe({
      next: (rows) => this.sessionActionDone(rows),
      error: (err) => this.sessionActionFailed(err)
    });
  }

  resetSession(): void {
    this.showResetPopup = false;
    if (!this.selectedSession || !this.isIdle()) {
      return;
    }
    this.markSessionBusy();
    this.raceControlService.resetSession(this.selectedSession.sessionId).subscribe({
      next: (rows) => this.sessionActionDone(rows),
      error: (err) => this.sessionActionFailed(err)
    });
  }

  private markSessionBusy(): void {
    this.actionError = '';
    this.sessionBusy = true;
  }

  private sessionActionDone(rows: LiveStanding[]): void {
    this.sessionBusy = false;
    this.standings = rows;
    this.reloadSelectedSession();   // Finish -> COMPLETED, Reset -> SCHEDULED
    this.startPolling();
  }

  private sessionActionFailed(err: any): void {
    this.sessionBusy = false;
    this.actionError = err.error?.message || 'The session could not be updated';
  }
}
