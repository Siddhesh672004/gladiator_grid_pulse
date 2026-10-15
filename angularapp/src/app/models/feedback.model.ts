import { RaceSession } from './session.model';
import { User } from './user.model';

// POST /api/feedback, the author comes from the token
export interface Feedback {
  feedbackId?: number;
  sessionId?: number;
  feedbackText: string;
  rating?: number;          // 1 to 5
  date: Date;
}

// sentiment + urgent are the AI label, only the admin lists carry them
export interface FeedbackRow extends Feedback {
  userId: number;
  user?: User;
  session?: RaceSession;
  sentiment?: string;       // POSITIVE, NEGATIVE, NEUTRAL or NOT_ANALYSED
  urgent?: boolean;
}

// the counters above the admin table, over all feedback
export interface FeedbackCounts {
  total: number;
  sentiments: { [sentiment: string]: number };
  urgent: number;
}

// answer of the AI helpers, an empty list means the AI is not available
export interface AiLines {
  lines: string[];
}

export function sentimentLabel(sentiment: string | undefined): string {
  if (sentiment === 'POSITIVE') {
    return 'Positive';
  }
  if (sentiment === 'NEGATIVE') {
    return 'Negative';
  }
  if (sentiment === 'NEUTRAL') {
    return 'Neutral';
  }
  return 'Not analysed';
}

export function starText(rating: number | undefined): string {
  if (!rating) {
    return '—';
  }
  return '★'.repeat(rating) + '☆'.repeat(5 - rating);
}
