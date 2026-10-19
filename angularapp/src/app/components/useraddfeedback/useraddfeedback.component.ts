import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Subscription } from 'rxjs';
import { Feedback, FeedbackRow } from '../../models/feedback.model';
import { RaceBooking } from '../../models/race-booking.model';
import { RaceSession, sessionLabel, sessionTime } from '../../models/session.model';
import { FeedbackService } from '../../services/feedback.service';
import { RaceService } from '../../services/race.service';
import { UserStorageService } from '../../services/user-storage.service';
import { BOOKING_STATUS, SESSION_STATUS } from '../../constants';
import { maxChars, minChars, required } from '../../validation';
import { FieldOption } from '../field/field.component';
import { byIndex } from '../../shared/track-by';

// add feedback -> FeedbackService. only sessions the driver took part in, that are over and not reviewed yet
const AI_DOWN = 'AI suggestions are not available right now. You can still send your own text.';
// same numbers as FeedbackRequest / SuggestionRequest in spring
const TEXT_MIN = 10;
const TEXT_MAX = 1000;

@Component({
  selector: 'app-useraddfeedback',
  templateUrl: './useraddfeedback.component.html'
})
export class UseraddfeedbackComponent implements OnInit, OnDestroy {

  feedbackForm: FormGroup;
  sessions: RaceSession[] = [];
  sessionOptions: FieldOption[] = [];       // the same sessions as lines of the drop-down
  stars: number[] = [1, 2, 3, 4, 5];
  textMax: number = TEXT_MAX;
  isLoading: boolean = true;
  isSaving: boolean = false;
  loadError: string = '';
  errorMessage: string = '';                // the server's answer to Submit
  showModal: boolean = false;
  suggestions: string[] = [];
  isImproving: boolean = false;
  aiMessage: string = '';

  private editSubscription?: Subscription;

  readonly byIndex = byIndex;

  constructor(private fb: FormBuilder, private feedbackService: FeedbackService, private raceService: RaceService,
              private userStorage: UserStorageService) {
    this.feedbackForm = this.fb.group({
      sessionId: [null, [required]],
      rating: [null, [required]],
      feedbackText: ['', [required, minChars(TEXT_MIN), maxChars(TEXT_MAX)]]
    });
  }

  ngOnInit(): void {
    // the server's answer goes away when the driver changes something
    this.editSubscription = this.feedbackForm.valueChanges.subscribe(() => this.errorMessage = '');
    const userId = this.userStorage.getUserId();
    if (userId === null) {
      this.isLoading = false;
      return;
    }
    // two lists are needed: the driver's bookings, then the feedback already written
    this.raceService.getRaceBookingsByUserId(userId).subscribe({
      next: (bookings) => this.loadFeedbacks(userId, bookings),
      error: () => this.loadFailed()
    });
  }

  ngOnDestroy(): void {
    this.editSubscription?.unsubscribe();
  }

  private loadFeedbacks(userId: number, bookings: RaceBooking[]): void {
    this.feedbackService.getAllfeedbacksByUserId(userId).subscribe({
      next: (feedbacks) => {
        this.showSessions(this.sessionsToReview(bookings, feedbacks));
        this.isLoading = false;
      },
      error: () => this.loadFailed()
    });
  }

  private loadFailed(): void {
    this.isLoading = false;
    this.loadError = 'Unable to load your sessions';
  }

  // sessions the driver took part in, that are over and not reviewed yet (each one once)
  private sessionsToReview(bookings: RaceBooking[], feedbacks: FeedbackRow[]): RaceSession[] {
    const alreadyReviewed = feedbacks.map(feedback => feedback.session?.sessionId);
    const sessions: RaceSession[] = [];
    for (const booking of bookings) {
      const session = booking.session;
      const tookPart = booking.status === BOOKING_STATUS.APPROVED && session.status === SESSION_STATUS.COMPLETED;
      const alreadyListed = sessions.some(s => s.sessionId === session.sessionId);
      if (tookPart && !alreadyListed && !alreadyReviewed.includes(session.sessionId)) {
        sessions.push(session);
      }
    }
    return sessions;
  }

  private showSessions(sessions: RaceSession[]): void {
    this.sessions = sessions;
    this.sessionOptions = sessions.map(session => ({ value: session.sessionId, text: this.optionText(session) }));
  }

  optionText(session: RaceSession): string {
    return `${session.trackName} · ${sessionLabel(session)} · ${session.sessionDate} · ${sessionTime(session)}`;
  }

  get currentRating(): number {
    return this.feedbackForm.get('rating')?.value || 0;
  }

  // "120 / 1000 characters" under the text box: the box stops taking letters at the limit
  get textCount(): string {
    const text: string = this.feedbackForm.get('feedbackText')?.value || '';
    return `${text.length} / ${TEXT_MAX} characters`;
  }

  setRating(star: number): void {
    const control = this.feedbackForm.get('rating');
    control?.setValue(star);
    control?.markAsTouched();
  }

  // "Improve with AI": up to 3 clearer versions of the text, the driver decides what is sent
  improve(): void {
    this.suggestions = [];
    this.aiMessage = '';
    const control = this.feedbackForm.get('feedbackText');
    // empty or too short: the box shows its own message, nothing is sent
    if (!control || control.invalid) {
      control?.markAsTouched();
      return;
    }
    this.isImproving = true;
    this.feedbackService.getSuggestions(control.value.trim()).subscribe({
      next: (answer) => {
        this.isImproving = false;
        this.suggestions = answer.lines || [];
        // empty list = the backend has no AI right now
        if (this.suggestions.length === 0) {
          this.aiMessage = AI_DOWN;
        }
      },
      error: (err) => {
        this.isImproving = false;
        // 400 = the backend refused the text and says why
        const refused = err.status === 400 && err.error?.message;
        this.aiMessage = refused ? err.error.message : AI_DOWN;
      }
    });
  }

  useSuggestion(text: string): void {
    const control = this.feedbackForm.get('feedbackText');
    control?.setValue(text);
    control?.markAsDirty();
    this.suggestions = [];
  }

  submit(): void {
    if (this.isSaving) {
      return;
    }
    this.errorMessage = '';
    if (this.feedbackForm.invalid) {
      this.feedbackForm.markAllAsTouched();
      return;
    }
    const feedback: Feedback = {
      sessionId: this.feedbackForm.value.sessionId,
      rating: this.feedbackForm.value.rating,
      feedbackText: this.feedbackForm.value.feedbackText.trim(),
      date: new Date()
    };

    this.isSaving = true;
    this.feedbackService.sendFeedback(feedback).subscribe({
      next: () => {
        this.isSaving = false;
        this.showModal = true;
        // one feedback per session: it leaves the list
        this.showSessions(this.sessions.filter(session => session.sessionId !== feedback.sessionId));
        this.feedbackForm.reset({ sessionId: null, rating: null, feedbackText: '' });
        this.suggestions = [];
        this.aiMessage = '';
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = err.error?.message || 'Unable to send feedback. Please try again.';
      }
    });
  }
}
