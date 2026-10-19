import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { CardView, ChatBubble, ChatResponse } from '../models/chat.model';
import { ActionResult, CopilotReply } from '../models/copilot.model';
import { PaymentConfirmRequest, PaymentConfirmResult, PaymentOrder, RazorpayResult } from '../../shared/models/payment.model';
import { RaceBookingRequest } from '../../shared/models/race-booking.model';
import { ChatService } from '../services/chat.service';
import { CopilotService } from '../services/copilot.service';
import { PaymentService } from '../../core/services/payment.service';
import { UserStorageService } from '../../core/services/user-storage.service';
import { CHAT_SESSION_KEY, COPILOT_CHAT_KEY } from '../../core/constants';
import { byIndex } from '../../shared/utils/track-by';

const GREETING = "Hi! 🏁 I'm the GridPulse assistant. Ask me about tracks, bookings, the Live Race "
  + 'Control Board or feedback. Log in to book or manage things right here.';
const COPILOT_GREETING = "Hi! I'm the GridPulse Copilot. Ask me anything about your GridPulse data, or tell me what "
  + 'to do - every change shows a card and happens only when you press Confirm.';
const NOT_REACHED = 'Sorry, I cannot reach the GridPulse server right now. Please try again in a moment.';
// same number as ChatRequest / CopilotRequest in spring
const MESSAGE_MAX = 1000;

// floating assistant on every page, created (open) by the chat button (ChatLauncherComponent) on its first click
// logged out: the faq chat (ChatService). logged in: the Copilot (CopilotService), whose changes come as cards;
// a confirmed booking opens the Razorpay window like the booking page (PaymentService)
@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html'
})
export class ChatbotComponent implements OnInit, OnDestroy {

  isOpen: boolean = false;
  message: string = '';
  maxLength: number = MESSAGE_MAX;
  isSending: boolean = false;
  copilot: boolean = false;
  role: string = '';
  bubbles: ChatBubble[] = [];

  private loginWatch?: Subscription;
  private countdown?: ReturnType<typeof setInterval>;

  // the message list (to scroll it) and the text box (to put the cursor in it)
  @ViewChild('messageList') messageList?: ElementRef<HTMLElement>;
  @ViewChild('messageInput') messageInput?: ElementRef<HTMLInputElement>;

  readonly byIndex = byIndex;

  constructor(private chatService: ChatService, private copilotService: CopilotService,
    private paymentService: PaymentService, private userStorage: UserStorageService) { }

  ngOnInit(): void {
    // logging in or out switches the mode and starts a fresh chat
    this.loginWatch = this.userStorage.loggedIn$.subscribe(() => this.startChat());
  }

  ngOnDestroy(): void {
    this.loginWatch?.unsubscribe();
    this.stopCountdown();
  }

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

    if (this.copilot) {
      this.copilotService.ask({ message: text, chatId: this.getChatId(COPILOT_CHAT_KEY) }).subscribe({
        next: (reply) => this.showReply(reply),
        error: (err) => this.sendFailed(err, COPILOT_CHAT_KEY)
      });
      return;
    }
    this.chatService.sendMessage({ message: text, sessionId: this.getChatId(CHAT_SESSION_KEY) }).subscribe({
      next: (answer) => this.showAnswer(answer),
      error: (err) => this.sendFailed(err, CHAT_SESSION_KEY)
    });
  }

  confirm(view: CardView): void {
    if (view.state !== 'open') {
      return;
    }
    view.state = 'busy';
    this.copilotService.confirm(view.card.token).subscribe({
      next: (result) => this.showResult(view, result),
      error: (err) => {
        // used, expired or not yours: the card is done either way
        view.state = 'expired';
        this.addBotBubble(err.error?.message || NOT_REACHED);
      }
    });
  }

  cancel(view: CardView): void {
    if (view.state !== 'open') {
      return;
    }
    view.state = 'cancelled';
    this.copilotService.cancel(view.card.token).subscribe({ error: () => { } });
  }

  clear(): void {
    const key = this.copilot ? COPILOT_CHAT_KEY : CHAT_SESSION_KEY;
    const chatId = this.getChatId(key);
    if (chatId) {
      const forget = this.copilot ? this.copilotService.clearMemory(chatId) : this.chatService.clearMemory(chatId);
      forget.subscribe({ error: () => { } });
    }
    this.removeChatId(key);
    this.startChat();
  }

  // "4:59" left on a card
  timeLeft(view: CardView): string {
    const minutes = Math.floor(view.secondsLeft / 60);
    const seconds = view.secondsLeft % 60;
    return minutes + ':' + String(seconds).padStart(2, '0');
  }

  // small grey line under a bot bubble
  metaLine(bubble: ChatBubble): string {
    if (bubble.matchedQuestion) {
      const percent = Math.round((bubble.confidence ?? 0) * 100);
      return `matched: "${bubble.matchedQuestion}" · ${percent}% · ${bubble.source}`;
    }
    if (bubble.source === 'fallback') {
      return 'no faq matched · fallback';
    }
    return '';
  }

  private startChat(): void {
    const token = this.userStorage.decodeToken();
    this.copilot = token !== null && !this.userStorage.isTokenExpired();
    this.role = this.copilot ? (this.userStorage.getUserRole() ?? '') : '';
    this.bubbles = [{ from: 'bot', text: this.copilot ? COPILOT_GREETING : GREETING, time: new Date() }];
    this.stopCountdown();
  }

  private sendFailed(err: any, key: string): void {
    this.isSending = false;
    // a chat id the server refuses would stop every next message too: the next one starts a new chat
    if (err.error?.fieldErrors?.sessionId || err.error?.fieldErrors?.chatId) {
      this.removeChatId(key);
    }
    // our backend says why it refused (400, 500 ...). no sentence at all = it was not reached
    this.addBotBubble(err.error?.message || NOT_REACHED);
  }

  private showAnswer(answer: ChatResponse): void {
    this.isSending = false;
    this.saveChatId(CHAT_SESSION_KEY, answer.sessionId);
    const bubble: ChatBubble = { from: 'bot', text: answer.reply, source: answer.source, time: new Date() };
    // the grey line names the faq only when one really matched
    if (answer.matched) {
      bubble.matchedQuestion = answer.matchedQuestion;
      bubble.confidence = answer.confidence;
    }
    this.bubbles.push(bubble);
    this.scrollToBottom();
  }

  private showReply(reply: CopilotReply): void {
    this.isSending = false;
    this.saveChatId(COPILOT_CHAT_KEY, reply.chatId);
    const cards: CardView[] = reply.actions.map(card => ({ card: card, state: 'open', secondsLeft: 0 }));
    this.bubbles.push({ from: 'bot', text: reply.reply, time: new Date(), cards: cards });
    this.tick();
    this.scrollToBottom();
  }

  private showResult(view: CardView, result: ActionResult): void {
    view.state = 'done';
    this.addBotBubble(result.message);
    // a confirmed booking: the payment window opens, then the same confirm as the booking page
    if (result.ok && result.paymentOrder && result.booking) {
      this.openPaymentWindow(result.paymentOrder, result.booking);
    }
  }

  // every second while a card counts down: the time left on the open cards, an open card at 0 is over
  private tick(): void {
    const now = Date.now();
    const openCards = this.bubbles.reduce<CardView[]>((all, bubble) => all.concat(bubble.cards ?? []), [])
      .filter(view => view.state === 'open');
    openCards.forEach(view => {
      view.secondsLeft = Math.max(0, Math.floor((new Date(view.card.expiresAt).getTime() - now) / 1000));
      if (view.secondsLeft === 0) {
        view.state = 'expired';
      }
    });
    // the clock only runs while a countdown is on screen (each round redraws the page)
    if (openCards.some(view => view.state === 'open')) {
      this.countdown = this.countdown ?? setInterval(() => this.tick(), 1000);
    } else {
      this.stopCountdown();
    }
  }

  private stopCountdown(): void {
    clearInterval(this.countdown);
    this.countdown = undefined;
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

  private addBotBubble(text: string): void {
    this.bubbles.push({ from: 'bot', text: text, time: new Date() });
    this.scrollToBottom();
  }

  // the chat's id is kept for this browser tab. sessionStorage can throw in private mode:
  // the chat still works then, only without memory
  private getChatId(key: string): string | undefined {
    try {
      return sessionStorage.getItem(key) ?? undefined;
    } catch {
      return undefined;
    }
  }

  private saveChatId(key: string, chatId: string): void {
    try {
      sessionStorage.setItem(key, chatId);
    } catch { }
  }

  private removeChatId(key: string): void {
    try {
      sessionStorage.removeItem(key);
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
