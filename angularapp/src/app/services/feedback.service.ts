import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AiLines, Feedback, FeedbackRow } from '../models/feedback.model';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// feedback calls + the AI helpers
@Injectable({
  providedIn: 'root'
})
export class FeedbackService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  sendFeedback(feedback: Feedback): Observable<FeedbackRow> {
    const body = {
      sessionId: feedback.sessionId,
      feedbackText: feedback.feedbackText,
      rating: feedback.rating,
      date: this.toIsoDate(feedback.date)
    };
    return this.http.post<FeedbackRow>(this.apiUrl + API_ENDPOINTS.FEEDBACK, body);
  }

  getAllfeedbacksByUserId(userId: number): Observable<FeedbackRow[]> {
    return this.http.get<FeedbackRow[]>(this.apiUrl + API_ENDPOINTS.FEEDBACK_BY_USER + '/' + userId);
  }

  deleteFeedback(feedbackId: number): Observable<FeedbackRow> {
    return this.http.delete<FeedbackRow>(this.apiUrl + API_ENDPOINTS.FEEDBACK + '/' + feedbackId);
  }

  getFeedbacks(): Observable<FeedbackRow[]> {
    return this.http.get<FeedbackRow[]>(this.apiUrl + API_ENDPOINTS.FEEDBACK);
  }

  // driver: up to 3 clearer versions of the text, [] = AI not available
  getSuggestions(feedbackText: string): Observable<AiLines> {
    return this.http.post<AiLines>(this.apiUrl + API_ENDPOINTS.FEEDBACK_SUGGESTIONS, { feedbackText: feedbackText });
  }

  analyseFeedback(feedbackId: number): Observable<FeedbackRow> {
    return this.http.post<FeedbackRow>(this.apiUrl + API_ENDPOINTS.FEEDBACK_ANALYSE(feedbackId), {});
  }

  // admin: 3-line summary, [] = no AI or no feedback
  getSummary(): Observable<AiLines> {
    return this.http.get<AiLines>(this.apiUrl + API_ENDPOINTS.FEEDBACK_SUMMARY);
  }

  // local date: toISOString gives yesterday before 5:30 am in India
  private toIsoDate(date: Date | string | undefined): string | undefined {
    if (!date) {
      return undefined;
    }
    if (typeof date === 'string') {
      return date.substring(0, 10);
    }
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }
}
