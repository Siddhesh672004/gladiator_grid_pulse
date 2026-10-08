import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { RaceSession, sessionLabel, sessionTime } from '../../models/session.model';
import { SessionService } from '../../services/session.service';
import { COMPETITION_PHASE, SESSION_STATUS, SESSION_TYPE } from '../../constants';
import { pageOf } from '../../shared/paging';
import { FieldOption, toOptions } from '../field/field.component';

// View Sessions (/admin/view/sessions): every session, newest date first -> SessionService
// practice session not started yet -> Edit / Cancel, competition session -> Control. ?type=Competition = the navbar's Competitions
@Component({
  selector: 'app-adminviewsessions',
  templateUrl: './adminviewsessions.component.html'
})
export class AdminviewsessionsComponent implements OnInit, OnDestroy {

  sessions: RaceSession[] = [];
  statusOptions: FieldOption[] = toOptions([SESSION_STATUS.SCHEDULED, SESSION_STATUS.LIVE, SESSION_STATUS.COMPLETED, SESSION_STATUS.CANCELLED]);
  typeOptions: FieldOption[] = toOptions(['Practice', 'Competition']);
  searchText: string = '';
  statusFilter: string = 'All';
  typeFilter: string = 'All';
  page: number = 1;
  pageSize: number = 10;
  isLoading: boolean = true;
  errorMessage: string = '';
  successMessage: string = '';
  sessionToCancel: RaceSession | null = null;
  private qualifyingDone: number[] = [];          // competitions whose race grid exists
  private typeParam?: Subscription;

  constructor(private sessionService: SessionService, private route: ActivatedRoute, private router: Router) { }

  ngOnInit(): void {
    // the Type box follows ?type= in the url (the navbar's Competitions link sets it)
    this.typeParam = this.route.queryParamMap.subscribe(params => {
      this.typeFilter = params.get('type') || 'All';
      this.page = 1;
    });
    this.loadSessions();
  }

  // the Type box only changes the url, the lines above do the rest
  setType(type: string): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: { type: type === 'All' ? null : type } });
  }

  ngOnDestroy(): void {
    this.typeParam?.unsubscribe();
  }

  loadSessions(): void {
    this.sessionService.getAllSessions().subscribe({
      next: (sessions) => {
        // newest date first, a later start first on the same day
        const key = (session: RaceSession) => session.sessionDate + 'T' + session.startTime;
        this.sessions = sessions.sort((a, b) => key(b).localeCompare(key(a)));
        this.qualifyingDone = sessions
          .filter(session => session.phase === COMPETITION_PHASE.QUALIFYING && session.status === SESSION_STATUS.COMPLETED)
          .map(session => session.competitionId ?? 0);
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Unable to load sessions';
      }
    });
  }

  get filteredSessions(): RaceSession[] {
    const text = this.searchText.trim().toLowerCase();
    return this.sessions.filter(session =>
      (session.trackName.toLowerCase().includes(text) || (session.competitionName || '').toLowerCase().includes(text)) &&
      (this.statusFilter === 'All' || session.status === this.statusFilter) &&
      (this.typeFilter === 'All' || session.sessionType === this.typeFilter.toUpperCase()));
  }

  get pagedSessions(): RaceSession[] {
    return pageOf(this.filteredSessions, this.page, this.pageSize);
  }

  label(session: RaceSession): string {
    return sessionLabel(session);
  }

  time(session: RaceSession): string {
    return sessionTime(session);
  }

  isPractice(session: RaceSession): boolean {
    return session.sessionType === SESSION_TYPE.PRACTICE;
  }

  // the race of a competition gets its cars when qualifying is finished, the drivers paid on the qualifying
  waitsForGrid(session: RaceSession): boolean {
    return this.isRaceStage(session) && !session.approvedCars
      && !this.qualifyingDone.includes(session.competitionId ?? 0);
  }

  // a race place costs nothing, the entry fee is on the qualifying row
  isRaceStage(session: RaceSession): boolean {
    return session.phase === COMPETITION_PHASE.RACING;
  }

  // edit / cancel only before the session starts
  canChange(session: RaceSession): boolean {
    return this.isPractice(session) && session.status === SESSION_STATUS.SCHEDULED;
  }

  editSession(session: RaceSession): void {
    this.router.navigate(['/admin/edit/session', session.sessionId]);
  }

  openCompetition(session: RaceSession): void {
    this.router.navigate(['/admin/competition', session.competitionId]);
  }

  askCancel(session: RaceSession): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.sessionToCancel = session;
  }

  cancelSession(): void {
    const session = this.sessionToCancel;
    this.sessionToCancel = null;
    if (!session) {
      return;
    }
    this.sessionService.cancelSession(session.sessionId).subscribe({
      next: () => {
        this.successMessage = 'Session cancelled. Its bookings were rejected, the payments are refunded and the drivers were told by email.';
        this.loadSessions();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Unable to cancel the session'
    });
  }
}
