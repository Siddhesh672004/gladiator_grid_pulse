import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { Competition } from '../../models/competition.model';
import { LiveStanding } from '../../models/live-standing.model';
import { RaceSession, sessionTime } from '../../models/session.model';
import { RaceControlService } from '../../services/race-control.service';
import { LiveBoardService } from '../../services/live-board.service';
import { CompetitionService } from '../../services/competition.service';
import { COMPETITION_PHASE, COMPETITION_STATUS, RACE_STATES, RACE_TIME_ZONE_LABEL, SESSION_STATUS } from '../../constants';
import { formatRaceTime } from '../adminracecontrol/race-time';
import { carState, isLeaderRow, positionText } from '../adminracecontrol/live-board';

// Competition Control (/admin/competition/:id): one next-step button per stage, the board shows qualifying first, then the race
@Component({
  selector: 'app-admincompetitioncontrol',
  templateUrl: './admincompetitioncontrol.component.html'
})
export class AdmincompetitioncontrolComponent implements OnInit, OnDestroy {

  competition: Competition | null = null;
  isLoading: boolean = true;
  selectedSession: RaceSession | null = null;     // the stage on the board: qualifying or race
  standings: LiveStanding[] = [];
  lastUpdated: Date | null = null;
  boardError: string = '';
  timeZoneLabel: string = RACE_TIME_ZONE_LABEL;

  busyId: number | null = null;                   // the car whose button was just pressed
  stepBusy: boolean = false;
  actionError: string = '';
  showCancelPopup: boolean = false;

  // the five phases in their order, one start light each
  steps = [
    { status: COMPETITION_STATUS.UPCOMING, label: 'Upcoming' },
    { status: COMPETITION_STATUS.QUALIFYING, label: 'Qualifying' },
    { status: COMPETITION_STATUS.QUALIFYING_COMPLETED, label: 'Grid Ready' },
    { status: COMPETITION_STATUS.RACING, label: 'Racing' },
    { status: COMPETITION_STATUS.COMPLETED, label: 'Completed' }
  ];

  private boardWatch?: Subscription;
  private watchedSessionId: number | null = null;
  private standingsRequest?: Subscription;
  private pageClosed: boolean = false;

  constructor(private liveBoard: LiveBoardService, private raceControlService: RaceControlService, private competitionService: CompetitionService,
              private route: ActivatedRoute, private router: Router) { }

  ngOnInit(): void {
    const competitionId = Number(this.route.snapshot.paramMap.get('id'));
    this.competitionService.getCompetitionById(competitionId).subscribe({
      next: (competition) => {
        this.isLoading = false;
        this.showCompetition(competition);
      },
      error: () => {
        this.isLoading = false;
        this.actionError = 'This competition does not exist.';
      }
    });
  }

  ngOnDestroy(): void {
    this.pageClosed = true;
    this.stopPolling();
  }

  // after loading and after every step: put the stage that matters now on the board
  private showCompetition(competition: Competition): void {
    this.competition = competition;
    if (this.gridExists) {
      this.selectSession(competition.raceSession);
    } else {
      this.selectSession(competition.qualifyingSession);
    }
  }

  selectSession(session: RaceSession): void {
    this.selectedSession = session;
    this.standings = [];
    this.startPolling();
  }

  showQualifying(): void {
    if (this.competition) {
      this.selectSession(this.competition.qualifyingSession);
    }
  }

  showRace(): void {
    if (this.competition && this.gridExists) {
      this.selectSession(this.competition.raceSession);
    }
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

  // ---- where the competition stands ----
  // the race has cars only after "Finish Qualifying" created the starting grid
  get gridExists(): boolean {
    const status = this.competition?.status;
    return status === COMPETITION_STATUS.QUALIFYING_COMPLETED || status === COMPETITION_STATUS.RACING
      || status === COMPETITION_STATUS.COMPLETED;
  }

  get isCancelled(): boolean {
    return this.competition?.status === COMPETITION_STATUS.CANCELLED;
  }

  get viewingRace(): boolean {
    return this.selectedSession?.phase === COMPETITION_PHASE.RACING;
  }

  get boardTitle(): string {
    if (!this.viewingRace) {
      return 'Qualifying';
    }
    if (this.competition?.status === COMPETITION_STATUS.QUALIFYING_COMPLETED) {
      return 'Starting Grid';
    }
    if (this.competition?.status === COMPETITION_STATUS.COMPLETED) {
      return 'Final Results';
    }
    return 'Race';
  }

  get lapLimit(): number {
    if (!this.competition) {
      return 0;
    }
    if (this.viewingRace) {
      return this.competition.raceLaps;
    }
    return this.competition.qualifyingLaps;
  }

  // position of the current phase in "steps": 0 = Upcoming ... 4 = Completed, -1 = cancelled
  private get currentStepIndex(): number {
    return this.steps.findIndex(step => step.status === this.competition?.status);
  }

  isDone(stepIndex: number): boolean {
    return stepIndex < this.currentStepIndex;
  }

  isCurrent(stepIndex: number): boolean {
    return stepIndex === this.currentStepIndex;
  }

  // one start light per phase reached: Upcoming = 1 ... Completed = 5
  get lightsOn(): number {
    return this.currentStepIndex + 1;
  }

  // ---- the next-step button ----
  get nextStepLabel(): string {
    switch (this.competition?.status) {
      case COMPETITION_STATUS.UPCOMING: return 'Start Qualifying';
      case COMPETITION_STATUS.QUALIFYING: return 'Finish Qualifying';
      case COMPETITION_STATUS.QUALIFYING_COMPLETED: return 'Start Race';
      case COMPETITION_STATUS.RACING: return 'Finish Race';
      default: return '';
    }
  }

  runNextStep(): void {
    const competition = this.competition;
    if (!competition || !this.isIdle()) {
      return;
    }
    this.actionError = '';
    this.stepBusy = true;
    const id = competition.competitionId;
    // which call the button stands for depends on the phase the competition is in
    let step: Observable<Competition>;
    if (competition.status === COMPETITION_STATUS.UPCOMING) {
      step = this.competitionService.startQualifying(id);
    } else if (competition.status === COMPETITION_STATUS.QUALIFYING) {
      step = this.competitionService.finishQualifying(id);
    } else if (competition.status === COMPETITION_STATUS.QUALIFYING_COMPLETED) {
      step = this.competitionService.startRace(id);
    } else {
      step = this.competitionService.finishRace(id);
    }
    step.subscribe({
      next: (updated) => {
        this.stepBusy = false;
        this.showCompetition(updated);
      },
      error: (err) => {
        this.stepBusy = false;
        this.actionError = err.error?.message || 'The step could not be completed';
      }
    });
  }

  get canCancel(): boolean {
    return this.competition?.status === COMPETITION_STATUS.UPCOMING && this.isIdle();
  }

  cancelCompetition(): void {
    this.showCancelPopup = false;
    if (!this.competition) {
      return;
    }
    this.stepBusy = true;
    this.competitionService.cancelCompetition(this.competition.competitionId).subscribe({
      next: (updated) => {
        this.stepBusy = false;
        this.showCompetition(updated);
      },
      error: (err) => {
        this.stepBusy = false;
        this.actionError = err.error?.message || 'The competition could not be cancelled';
      }
    });
  }

  // ---- the car buttons ----
  // +1 Lap / Retire: only for a racing car, in the session that is LIVE
  showsRaceActions(row: LiveStanding): boolean {
    return this.stateOf(row) === RACE_STATES.RACING && this.selectedSession?.status === SESSION_STATUS.LIVE;
  }

  canRace(row: LiveStanding): boolean {
    return this.showsRaceActions(row) && this.isIdle();
  }

  addLap(row: LiveStanding): void {
    if (!this.canRace(row)) {
      return;
    }
    this.markCarBusy(row);
    this.competitionService.addLap(row.raceBookingId).subscribe({
      next: () => this.carActionDone(),
      error: (err) => this.carActionFailed(err)
    });
  }

  retire(row: LiveStanding): void {
    if (!this.canRace(row)) {
      return;
    }
    this.markCarBusy(row);
    this.competitionService.retireCar(row.raceBookingId).subscribe({
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
    this.startPolling();
  }

  private carActionFailed(err: any): void {
    this.busyId = null;
    this.actionError = err.error?.message || 'The action could not be completed';
    this.startPolling();
  }

  private isIdle(): boolean {
    return this.busyId === null && !this.stepBusy;
  }

  // ---- what the table shows ----
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

  goBack(): void {
    this.router.navigate(['/admin/view/sessions']);
  }
}
