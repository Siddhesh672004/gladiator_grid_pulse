import { ActionCard } from './copilot.model';
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
  source: string;            // "gemini" | "faq" | "fallback"
  sessionId: string;
  resolvedQuestion?: string;
  // always empty since the faq chat stopped booking (the Copilot books now), kept for the json shape
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
  // Copilot: the Confirm cards under this bubble
  cards?: CardView[];
}

// a card on screen: open until Confirm / Cancel / its 5 minutes are over
export interface CardView {
  card: ActionCard;
  state: 'open' | 'busy' | 'done' | 'cancelled' | 'expired';
  secondsLeft: number;
}
