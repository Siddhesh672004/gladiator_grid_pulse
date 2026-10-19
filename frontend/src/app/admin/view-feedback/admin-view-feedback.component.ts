import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { User } from '../../shared/models/user.model';
import { RaceSession, sessionSummary } from '../../shared/models/session.model';
import { FeedbackService } from '../../core/services/feedback.service';
import { FeedbackCounts, FeedbackRow, sentimentLabel, starText } from '../../shared/models/feedback.model';
import { SEARCH_DEBOUNCE_MS, SENTIMENTS } from '../../core/constants';
import { pageCount } from '../../shared/utils/paging';
import { FieldOption, toOptions } from '../../shared/utils/field-option';
import { byIndex, byKey } from '../../shared/utils/track-by';

// every driver's feedback: stars, AI label (Positive / Negative / Neutral + Needs attention), counters, filters, Summarise with AI -> FeedbackService
// when the AI is down a feedback is "Not analysed" and its row gets an Analyse button. the table is one page from the backend
const NOT_ANALYSED = 'NOT_ANALYSED';
const AI_DOWN = 'The AI is not available right now. Please try again later.';

@Component({
  selector: 'app-adminviewfeedback',
  templateUrl: './admin-view-feedback.component.html'
})
export class AdminViewFeedbackComponent implements OnInit, OnDestroy {

  feedbacks: FeedbackRow[] = [];       // the rows of this page only
  total: number = 0;                   // every feedback that matches the search + filters
  counts: FeedbackCounts | null = null;
  sentiments: string[] = SENTIMENTS;
  sentimentOptions: FieldOption[] = SENTIMENTS.map(sentiment => ({ value: sentiment, text: sentimentLabel(sentiment) }));
  ratingOptions: FieldOption[] = [
    { value: '5', text: '5 stars' }, { value: '4', text: '4 stars' }, { value: '3', text: '3 stars' },
    { value: '2', text: '2 stars' }, { value: '1', text: '1 star' }
  ];
  searchText: string = '';
  sentimentFilter: string = 'All';
  ratingFilter: string = 'All';
  urgentOnly: boolean = false;
  page: number = 1;
  pageSize: number = 10;
  isLoading: boolean = true;
  errorMessage: string = '';
  infoMessage: string = '';
  busyId: number | null = null;
  isSummarising: boolean = false;
  summaryLines: string[] = [];
  profile: User | null = null;

  private pageRequest?: Subscription;
  private searchChanges = new Subject<void>();
  private searchWatch?: Subscription;

  readonly byIndex = byIndex;
  readonly byFeedback = byKey<FeedbackRow>('feedbackId');

  constructor(private feedbackService: FeedbackService) { }

  ngOnInit(): void {
    // typing: one request once the typing stops, back to page 1
    this.searchWatch = this.searchChanges.pipe(debounceTime(SEARCH_DEBOUNCE_MS)).subscribe(() => this.filterChanged());
    this.loadPage();
    this.loadCounts();
  }

  ngOnDestroy(): void {
    this.searchWatch?.unsubscribe();
    this.pageRequest?.unsubscribe();
  }

  // search looks at the driver's name, the track and the feedback text (the backend does it)
  loadPage(): void {
    // an older answer that is still on its way is dropped
    this.pageRequest?.unsubscribe();
    this.pageRequest = this.feedbackService.getFeedbackPage(this.searchText.trim(), this.sentimentFilter, this.ratingFilter,
      this.urgentOnly, this.page - 1, this.pageSize).subscribe({
      next: (result) => {
        // the page got empty (the list got shorter): show the new last page
        const lastPage = pageCount(result.totalElements, this.pageSize);
        if (result.content.length === 0 && this.page > lastPage) {
          this.page = lastPage;
          this.loadPage();
          return;
        }
        this.feedbacks = result.content;
        this.total = result.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Unable to load feedback';
      }
    });
  }

  private loadCounts(): void {
    this.feedbackService.getCounts().subscribe({
      next: (counts) => this.counts = counts,
      error: () => this.counts = null
    });
  }

  searchTyped(): void {
    this.searchChanges.next();
  }

  filterChanged(): void {
    this.page = 1;
    this.loadPage();
  }

  goToPage(page: number): void {
    this.page = page;
    this.loadPage();
  }

  get hasFilters(): boolean {
    return this.searchText.trim() !== '' || this.sentimentFilter !== 'All' || this.ratingFilter !== 'All' || this.urgentOnly;
  }

  // old feedback (from before the AI) has no label -> "not analysed"
  sentimentOf(feedback: FeedbackRow): string {
    return feedback.sentiment || NOT_ANALYSED;
  }

  // the counters look at ALL feedback, not only this page (GET /api/feedback/counts)
  countOf(sentiment: string): number {
    return this.counts?.sentiments[sentiment] ?? 0;
  }

  get allCount(): number {
    return this.counts?.total ?? 0;
  }

  get urgentCount(): number {
    return this.counts?.urgent ?? 0;
  }

  label(sentiment: string): string {
    return sentimentLabel(sentiment);
  }

  stars(rating: number | undefined): string {
    return starText(rating);
  }

  sessionText(session: RaceSession | undefined): string {
    return sessionSummary(session);
  }

  canAnalyse(feedback: FeedbackRow): boolean {
    return this.sentimentOf(feedback) === NOT_ANALYSED;
  }

  analyse(feedback: FeedbackRow): void {
    if (!feedback.feedbackId || this.busyId !== null) {
      return;
    }
    this.errorMessage = '';
    this.infoMessage = '';
    this.busyId = feedback.feedbackId;
    this.feedbackService.analyseFeedback(feedback.feedbackId).subscribe({
      next: (updated) => {
        this.busyId = null;
        // put the answer of the backend in place of the old row
        const index = this.feedbacks.findIndex(row => row.feedbackId === updated.feedbackId);
        if (index >= 0) {
          this.feedbacks[index] = updated;
        }
        if (this.sentimentOf(updated) === NOT_ANALYSED) {
          this.infoMessage = AI_DOWN;
        }
        // a new label moves the counters, and with a filter on the row may leave this list
        this.loadCounts();
        this.loadPage();
      },
      error: (err) => {
        this.busyId = null;
        this.errorMessage = err.error?.message || 'Unable to analyse the feedback';
      }
    });
  }

  summarise(): void {
    this.errorMessage = '';
    this.infoMessage = '';
    this.summaryLines = [];
    if (this.counts?.total === 0) {
      this.infoMessage = 'There is no feedback to summarise yet.';
      return;
    }
    this.isSummarising = true;
    this.feedbackService.getSummary().subscribe({
      next: (answer) => {
        this.isSummarising = false;
        this.summaryLines = answer.lines || [];
        // an empty list is the backend's way of saying "no AI right now"
        if (this.summaryLines.length === 0) {
          this.infoMessage = AI_DOWN;
        }
      },
      error: (err) => {
        this.isSummarising = false;
        this.errorMessage = err.error?.message || 'Unable to summarise the feedback';
      }
    });
  }

  showProfile(feedback: FeedbackRow): void {
    this.profile = feedback.user ?? null;
  }
}
