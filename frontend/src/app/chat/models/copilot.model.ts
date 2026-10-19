import { PaymentOrder } from '../../shared/models/payment.model';
import { RaceBookingRequest } from '../../shared/models/race-booking.model';

// body of POST /api/ai/copilot (logged in)
export interface CopilotRequest {
  message: string;
  chatId?: string;
}

// a change the Copilot prepared: it happens only when the user presses Confirm (within 5 minutes)
export interface ActionCard {
  token: string;
  summary: string;
  expiresAt: string;
}

export interface CopilotReply {
  reply: string;
  chatId: string;
  actions: ActionCard[];
}

// what Confirm did; a booking also carries the order the payment window opens with
export interface ActionResult {
  ok: boolean;
  message: string;
  paymentOrder?: PaymentOrder;
  booking?: RaceBookingRequest;
}
