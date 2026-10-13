import { Component, OnInit } from '@angular/core';
import { User } from '../../models/user.model';
import { RaceSession, sessionSummary } from '../../models/session.model';
import { FeedbackService } from '../../services/feedback.service';
import { FeedbackRow, sentimentLabel, starText } from '../../models/feedback.model';
import { SENTIMENTS } from '../../constants';
import { pageOf } from '../../shared/paging';
import { FieldOption, toOptions } from '../field/field.component';

// every driver's feedback: stars, AI label (Positive / Negative / Neutral + Needs attention), counters, filters, Summarise with AI -> FeedbackService
// when the AI is down a feedback is "Not analysed" and its row gets an Analyse button
const NOT_ANALYSED = 'NOT_ANALYSED';
const AI_DOWN = 'The AI is not available right now. Please try again later.';

@Component({
  selector: 'app-adminviewfeedback',
  templateUrl: './adminviewfeedback.component.html'
})
export class AdminviewfeedbackComponent implements OnInit {

  feedbacks: FeedbackRow[] = [];
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

  constructor(private feedbackService: FeedbackService) { }

  ngOnInit(): void {
    this.feedbackService.getFeedbacks().subscribe({
      next: (feedbacks) => {
        // newest first
        this.feedbacks = feedbacks.sort((a, b) => (b.feedbackId ?? 0) - (a.feedbackId ?? 0));
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Unable to load feedback';
      }
    });
  }

  // the rows left after the search box, the two filters and the "needs attention" tick
  get filteredFeedbacks(): FeedbackRow[] {
    const text = this.searchText.trim().toLowerCase();
    return this.feedbacks.filter(feedback => this.hasText(feedback, text) && this.passesFilters(feedback));
  }

  // search looks at the driver's name, the track and the feedback text
  private hasText(feedback: FeedbackRow, text: string): boolean {
    return (feedback.user?.username || '').toLowerCase().includes(text)
      || (feedback.session?.trackName || '').toLowerCase().includes(text)
      || (feedback.feedbackText || '').toLowerCase().includes(text);
  }

  private passesFilters(feedback: FeedbackRow): boolean {
    if (this.sentimentFilter !== 'All' && this.sentimentOf(feedback) !== this.sentimentFilter) {
      return false;
    }
    // the select gives text ('4'), the rating is a number (4) -> compare both as text
    if (this.ratingFilter !== 'All' && String(feedback.rating) !== this.ratingFilter) {
      return false;
    }
    if (this.urgentOnly && feedback.urgent !== true) {
      return false;
    }
    return true;
  }

  get pagedFeedbacks(): FeedbackRow[] {
    return pageOf(this.filteredFeedbacks, this.page, this.pageSize);
  }

  // old feedback (from before the AI) has no label -> "not analysed"
  sentimentOf(feedback: FeedbackRow): string {
    return feedback.sentiment || NOT_ANALYSED;
  }

  // the counters look at ALL feedback, not only the filtered rows
  countOf(sentiment: string): number {
    return this.feedbacks.filter(feedback => this.sentimentOf(feedback) === sentiment).length;
  }

  get urgentCount(): number {
    return this.feedbacks.filter(feedback => feedback.urgent === true).length;
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
    if (this.feedbacks.length === 0) {
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
