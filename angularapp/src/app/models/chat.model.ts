import { PaymentOrder } from './payment.model';
import { RaceBookingRequest } from './race-booking.model';


// body of POST /api/chat
export interface ChatRequest {
  message: string;
  sessionId?: string;
}

export interface ChatResponse {
  reply: string;
  matched: boolean;
  matchedQuestion?: string;
  category?: string;
  confidence: number;
  source: string;            // "gemini" | "faq" | "fallback" | "booking"
  sessionId: string;
  resolvedQuestion?: string;
  // set when the assistant started a payment for a booking
  paymentOrder?: PaymentOrder;
  booking?: RaceBookingRequest;
}

// one bubble in the chat window
export interface ChatBubble {
  from: 'user' | 'bot';
  text: string;
  matchedQuestion?: string;
  confidence?: number;
  source?: string;
  time: Date;
}
