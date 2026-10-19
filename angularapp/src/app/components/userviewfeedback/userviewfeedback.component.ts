import { Component, OnInit } from '@angular/core';
import { FeedbackRow, starText } from '../../models/feedback.model';
import { RaceSession, sessionSummary } from '../../models/session.model';
import { FeedbackService } from '../../services/feedback.service';
import { UserStorageService } from '../../services/user-storage.service';
import { pageOf } from '../../shared/paging';
import { byKey } from '../../shared/track-by';

// the logged-in driver's own feedback, delete with a confirmation popup
@Component({
  selector: 'app-userviewfeedback',
  templateUrl: './userviewfeedback.component.html'
})
export class UserviewfeedbackComponent implements OnInit {

  feedbacks: FeedbackRow[] = [];
  page: number = 1;
  pageSize: number = 10;
  isLoading: boolean = true;
  errorMessage: string = '';
  feedbackToDelete: FeedbackRow | null = null;

  readonly byFeedback = byKey<FeedbackRow>('feedbackId');

  constructor(private feedbackService: FeedbackService, private userStorage: UserStorageService) { }

  ngOnInit(): void {
    const userId = this.userStorage.getUserId();
    if (userId === null) {
      this.isLoading = false;
      return;
    }
    this.feedbackService.getAllfeedbacksByUserId(userId).subscribe({
      next: (data) => {
        // newest first
        this.feedbacks = data.sort((a, b) => (b.feedbackId ?? 0) - (a.feedbackId ?? 0));
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Unable to load your feedback';
      }
    });
  }

  get pagedFeedbacks(): FeedbackRow[] {
    return pageOf(this.feedbacks, this.page, this.pageSize);
  }

  // the admin feedback page uses this too
  sessionText(session: RaceSession | undefined): string {
    return sessionSummary(session);
  }

  stars(rating: number | undefined): string {
    return starText(rating);
  }

  deleteFeedback(): void {
    const feedback = this.feedbackToDelete;
    this.feedbackToDelete = null;
    if (!feedback?.feedbackId) {
      return;
    }
    this.feedbackService.deleteFeedback(feedback.feedbackId).subscribe({
      next: () => this.feedbacks = this.feedbacks.filter(f => f.feedbackId !== feedback.feedbackId),
      error: (err) => this.errorMessage = err.error?.message || 'Unable to delete the feedback'
    });
  }
}
