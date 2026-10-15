import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { RaceSession, sessionLabel, sessionTime } from '../../models/session.model';
import { SessionService } from '../../services/session.service';
import { COMPETITION_PHASE, SEARCH_DEBOUNCE_MS, SESSION_STATUS, SESSION_TYPE } from '../../constants';
import { pageCount } from '../../shared/paging';
import { FieldOption, toOptions } from '../field/field.component';

// View Sessions (/admin/view/sessions): every session, newest date first, one page from the backend -> SessionService
// practice session not started yet -> Edit / Cancel, competition session -> Control. ?type=Competition = the navbar's Competitions
@Component({
  selector: 'app-adminviewsessions',
  templateUrl: './adminviewsessions.component.html'
})
export class AdminviewsessionsComponent implements OnInit, OnDestroy {

  sessions: RaceSession[] = [];        // the rows of this page only
  total: number = 0;                   // every session that matches the search + filters
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
  private typeParam?: Subscription;
  private pageRequest?: Subscription;
  private searchChanges = new Subject<void>();
  private searchWatch?: Subscription;

  constructor(private sessionService: SessionService, private route: ActivatedRoute, private router: Router) { }

  ngOnInit(): void {
    // the Type box follows ?type= in the url (the navbar's Competitions link sets it)
    this.typeParam = this.route.queryParamMap.subscribe(params => {
      this.typeFilter = params.get('type') || 'All';
      this.filterChanged();
    });
    // typing: one request once the typing stops, back to page 1
    this.searchWatch = this.searchChanges.pipe(debounceTime(SEARCH_DEBOUNCE_MS)).subscribe(() => this.filterChanged());
  }

  // the Type box only changes the url, the lines above do the rest
  setType(type: string): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: { type: type === 'All' ? null : type } });
  }

  ngOnDestroy(): void {
    this.typeParam?.unsubscribe();
    this.searchWatch?.unsubscribe();
    this.pageRequest?.unsubscribe();
  }

  // newest date first, a later start first on the same day (the backend sorts)
  loadSessions(): void {
    // an older answer that is still on its way is dropped
    this.pageRequest?.unsubscribe();
    this.pageRequest = this.sessionService.getSessionPage(this.searchText.trim(), this.statusFilter, this.typeFilter,
      this.page - 1, this.pageSize).subscribe({
      next: (result) => {
        // the page got empty (the list got shorter): show the new last page
        const lastPage = pageCount(result.totalElements, this.pageSize);
        if (result.content.length === 0 && this.page > lastPage) {
          this.page = lastPage;
          this.loadSessions();
          return;
        }
        this.sessions = result.content;
        this.total = result.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Unable to load sessions';
      }
    });
  }

  searchTyped(): void {
    this.searchChanges.next();
  }

  filterChanged(): void {
    this.page = 1;
    this.loadSessions();
  }

  goToPage(page: number): void {
    this.page = page;
    this.loadSessions();
  }

  get hasFilters(): boolean {
    return this.searchText.trim() !== '' || this.statusFilter !== 'All' || this.typeFilter !== 'All';
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

  // the race of a competition gets its cars when qualifying is finished (at least one), the drivers paid on the qualifying
  waitsForGrid(session: RaceSession): boolean {
    return this.isRaceStage(session) && !session.approvedCars;
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
