import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatBubble, ChatResponse } from '../../models/chat.model';
import { PaymentConfirmRequest, PaymentConfirmResult, PaymentOrder, RazorpayResult } from '../../models/payment.model';
import { RaceBookingRequest } from '../../models/race-booking.model';
import { ChatService } from '../../services/chat.service';
import { PaymentService } from '../../services/payment.service';
import { CHAT_SESSION_KEY } from '../../constants';

const GREETING = "Hi! 🏁 I'm the GridPulse assistant. Ask me about tracks, bookings, the Live Race "
  + 'Control Board or feedback - or just tell me which session to book.';
const NOT_REACHED = 'Sorry, I cannot reach the GridPulse server right now. Please try again in a moment.';
// same number as ChatRequest in spring
const MESSAGE_MAX = 1000;

// floating assistant, mounted once in app.component.html so it is on every page
// a message goes to ChatService; when the answer carries a payment order, the Razorpay window opens (PaymentService)
@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html'
})
export class ChatbotComponent {

  isOpen: boolean = false;
  message: string = '';
  maxLength: number = MESSAGE_MAX;
  isSending: boolean = false;
  bubbles: ChatBubble[] = [{ from: 'bot', text: GREETING, time: new Date() }];

  // the message list (to scroll it) and the text box (to put the cursor in it)
  @ViewChild('messageList') messageList?: ElementRef<HTMLElement>;
  @ViewChild('messageInput') messageInput?: ElementRef<HTMLInputElement>;

  constructor(private chatService: ChatService, private paymentService: PaymentService) { }

  toggle(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      // the text box exists only after the window was drawn
      setTimeout(() => this.messageInput?.nativeElement.focus());
    }
  }

  // the Escape key closes the window
  @HostListener('document:keydown.escape')
  close(): void {
    this.isOpen = false;
  }

  send(): void {
    const text = this.message.trim();
    if (!text || this.isSending) {
      return;
    }
    this.bubbles.push({ from: 'user', text: text, time: new Date() });
    this.message = '';
    this.isSending = true;
    this.scrollToBottom();

    this.chatService.sendMessage({ message: text, sessionId: this.getSessionId() }).subscribe({
      next: (answer) => this.showAnswer(answer),
      error: (err) => this.sendFailed(err)
    });
  }

  private sendFailed(err: any): void {
    this.isSending = false;
    // a chat id the server refuses would stop every next message too: the next one starts a new chat
    if (err.error?.fieldErrors?.sessionId) {
      this.removeSessionId();
    }
    // our backend says why it refused (400, 500 ...). no sentence at all = it was not reached
    this.addBotBubble(err.error?.message || NOT_REACHED);
  }

  private showAnswer(answer: ChatResponse): void {
    this.isSending = false;
    this.saveSessionId(answer.sessionId);
    const bubble: ChatBubble = { from: 'bot', text: answer.reply, source: answer.source, time: new Date() };
    // the grey line names the faq only when one really matched
    if (answer.matched) {
      bubble.matchedQuestion = answer.matchedQuestion;
      bubble.confidence = answer.confidence;
    }
    this.bubbles.push(bubble);
    this.scrollToBottom();
    // the assistant booked a session: it already made the Razorpay order, now the driver pays
    if (answer.paymentOrder && answer.booking) {
      this.openPaymentWindow(answer.paymentOrder, answer.booking);
    }
  }

  // same two steps as the booking page: the driver pays in Razorpay's window ...
  private openPaymentWindow(order: PaymentOrder, booking: RaceBookingRequest): void {
    this.paymentService.openCheckout(order).subscribe({
      next: (paid) => this.confirmPayment(paid, booking),
      error: (err) => this.paymentFailed(err, false)
    });
  }

  // ... then our backend checks the payment and saves the booking
  private confirmPayment(paid: RazorpayResult, booking: RaceBookingRequest): void {
    const request: PaymentConfirmRequest = {
      booking: booking,
      razorpayOrderId: paid.razorpay_order_id,
      razorpayPaymentId: paid.razorpay_payment_id,
      razorpaySignature: paid.razorpay_signature
    };
    this.paymentService.confirm(request).subscribe({
      next: (result) => this.addBotBubble(this.confirmedText(result)),
      error: (err) => this.paymentFailed(err, true)
    });
  }

  private confirmedText(result: PaymentConfirmResult): string {
    const booking = result.booking;
    if (!booking) {
      return result.pendingMessage || 'Payment received. Your booking is being confirmed.';
    }
    if (booking.status === 'Waitlisted') {
      return `Payment received. The session is full: booking #${booking.raceBookingId} for car #${booking.carNumber} `
        + `is number ${booking.waitlistPosition} on the waitlist and moves up when a seat opens (refunded if none does).`;
    }
    return `Payment received. Booking #${booking.raceBookingId} for car #${booking.carNumber} `
      + 'is saved and waiting for approval. You can follow it under My Registrations.';
  }

  private paymentFailed(err: any, hasPaid: boolean): void {
    let text = err.error?.message || 'The payment did not go through. Nothing was booked.';
    // refused after paying: the backend sends the money back
    if (hasPaid && err.error?.message) {
      text += ' Your payment will be refunded.';
    }
    this.addBotBubble(text);
  }

  clear(): void {
    const sessionId = this.getSessionId();
    if (sessionId) {
      this.chatService.clearMemory(sessionId).subscribe({ error: () => { } });
    }
    this.removeSessionId();
    this.bubbles = [{ from: 'bot', text: GREETING, time: new Date() }];
  }

  // small grey line under a bot bubble
  metaLine(bubble: ChatBubble): string {
    if (bubble.source === 'booking') {
      return 'spring ai · booking tool';
    }
    if (bubble.matchedQuestion) {
      const percent = Math.round((bubble.confidence ?? 0) * 100);
      return `matched: "${bubble.matchedQuestion}" · ${percent}% · ${bubble.source}`;
    }
    if (bubble.source === 'fallback') {
      return 'no faq matched · fallback';
    }
    return '';
  }

  private addBotBubble(text: string): void {
    this.bubbles.push({ from: 'bot', text: text, time: new Date() });
    this.scrollToBottom();
  }

  // the chat's id is kept for this browser tab. sessionStorage can throw in private mode:
  // the chat still works then, only without memory
  private getSessionId(): string | undefined {
    try {
      return sessionStorage.getItem(CHAT_SESSION_KEY) ?? undefined;
    } catch {
      return undefined;
    }
  }

  private saveSessionId(sessionId: string): void {
    try {
      sessionStorage.setItem(CHAT_SESSION_KEY, sessionId);
    } catch { }
  }

  private removeSessionId(): void {
    try {
      sessionStorage.removeItem(CHAT_SESSION_KEY);
    } catch { }
  }

  // after the new bubble was drawn
  private scrollToBottom(): void {
    setTimeout(() => {
      const list = this.messageList?.nativeElement;
      if (list) {
        list.scrollTop = list.scrollHeight;
      }
    });
  }
}
