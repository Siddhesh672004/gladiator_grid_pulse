import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { RaceTrack } from '../../shared/models/race-track.model';
import { CarCategory, RaceBookingRequest } from '../../shared/models/race-booking.model';
import { PaymentConfirmRequest, PaymentConfirmResult, PaymentOrder, RazorpayResult } from '../../shared/models/payment.model';
import { RaceSession, hasStarted, isBookable, sessionLabel, sessionTime } from '../../shared/models/session.model';
import { RaceService } from '../../core/services/race.service';
import { SessionService } from '../../core/services/session.service';
import { ProfileService } from '../../core/services/profile.service';
import { PaymentService } from '../../core/services/payment.service';
import { BOOKING_STATUS, BOOKING_TERMS, CAR_CATEGORIES } from '../../core/constants';
import { DIGITS_PATTERN, matches, maxChars, monthsAheadIso, notInPast, required, todayIso, withinMonths } from '../../shared/utils/validation';
import { byKey } from '../../shared/utils/track-by';

// /user/register/:trackId -> pick date and session, car, terms, then Razorpay (PaymentService)
// the booking is made only after the payment went through. license comes from the profile
@Component({
  selector: 'app-userregisterrace',
  templateUrl: './user-register-race.component.html'
})
export class UserRegisterRaceComponent implements OnInit, OnDestroy {

  track: RaceTrack | null = null;
  bookingForm: FormGroup;
  sessions: RaceSession[] = [];
  selectedSession: RaceSession | null = null;
  carCategories: CarCategory[] = [];
  termsText: string = BOOKING_TERMS;
  today: string = todayIso();
  lastDay: string = monthsAheadIso(12);

  isLoading: boolean = true;
  loadingSessions: boolean = false;
  sessionsError: string = '';
  submitted: boolean = false;
  isSaving: boolean = false;
  errorMessage: string = '';
  showModal: boolean = false;
  // what the success window says: saved, on the waitlist, or still being confirmed
  modalText: string = '';
  modalHint: string = '';
  profileIncomplete: boolean = false;

  private dateSubscription?: Subscription;
  private editSubscription?: Subscription;
  private sessionsRequest?: Subscription;

  readonly bySession = byKey<RaceSession>('sessionId');
  readonly byCategory = byKey<CarCategory>('name');

  constructor(private fb: FormBuilder, private raceService: RaceService, private sessionService: SessionService,
              private profileService: ProfileService, private paymentService: PaymentService,
              private route: ActivatedRoute, private router: Router) {
    // the race date only picks the day whose sessions are listed, the booking itself goes by the session
    this.bookingForm = this.fb.group({
      raceDate: ['', [required, notInPast('Race date cannot be in the past'),
        withinMonths(12, 'Race date must be within the next 12 months')]],
      carNumber: [null, [required, matches(DIGITS_PATTERN, 'Car number can only have digits'),
        Validators.min(1), Validators.max(999)]],
      carCategory: ['GT3', [required]],
      comments: ['', [maxChars(500)]],
      termsAccepted: [false, [Validators.requiredTrue]]
    });
  }

  ngOnInit(): void {
    this.showCategoryNames();
    this.loadTrack();
    this.loadCategoryPictures();
    this.loadProfileState();
    // a new date in the Race Date box -> the sessions of that day
    this.dateSubscription = this.bookingForm.get('raceDate')?.valueChanges.subscribe(date => this.loadSessions(date));
    // the server's answer next to the pay button goes away when the driver changes something
    this.editSubscription = this.bookingForm.valueChanges.subscribe(() => this.errorMessage = '');
  }

  ngOnDestroy(): void {
    this.dateSubscription?.unsubscribe();
    this.editSubscription?.unsubscribe();
    this.sessionsRequest?.unsubscribe();
  }

  private loadTrack(): void {
    const trackId = Number(this.route.snapshot.paramMap.get('trackId'));
    this.raceService.getTrackById(trackId).subscribe({
      next: (track) => {
        this.track = track;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 404) {
          this.errorMessage = 'This track does not exist any more.';
        } else {
          this.errorMessage = 'Unable to load the track.';
        }
      }
    });
  }

  // the car tiles are there at once (names only), their pictures come from the backend
  private showCategoryNames(): void {
    this.carCategories = [];
    for (const name of CAR_CATEGORIES) {
      this.carCategories.push({ name: name, imageUrl: null });
    }
  }

  private loadCategoryPictures(): void {
    this.raceService.getCarCategories().subscribe({
      next: (categories) => this.carCategories = categories,
      error: () => { }     // no pictures: the names stay
    });
  }

  // a driver without date of birth + license cannot book: the page then shows a link to the profile
  private loadProfileState(): void {
    this.profileService.getProfile().subscribe({
      next: (profile) => this.profileIncomplete = !profile.profileComplete,
      error: () => this.profileIncomplete = false
    });
  }

  // the old list and the picked session belong to the old date, both go
  loadSessions(date: string): void {
    this.sessionsRequest?.unsubscribe();
    this.sessions = [];
    this.selectedSession = null;
    this.loadingSessions = false;
    this.sessionsError = '';
    // an empty, past or too far date has no list
    if (this.bookingForm.get('raceDate')?.invalid || !this.track?.trackId) {
      return;
    }
    this.loadingSessions = true;
    this.sessionsRequest = this.sessionService.getSessionsForTrackOnDate(this.track.trackId, date).subscribe({
      next: (sessions) => {
        this.sessions = sessions.filter(isBookable);
        this.loadingSessions = false;
      },
      error: () => {
        this.loadingSessions = false;
        this.sessionsError = 'Unable to load the sessions. Please pick the date again.';
      }
    });
  }

  // only approved cars take a seat
  isFull(session: RaceSession): boolean {
    return (session.approvedCars ?? 0) >= session.maxCars;
  }

  // a full session can be picked too: the booking joins its waitlist
  selectSession(session: RaceSession): void {
    this.selectedSession = session;
    this.errorMessage = '';
  }

  // text on a session chip
  chipText(session: RaceSession): string {
    if (this.selectedSession === session) {
      return 'Selected ✓';
    }
    return this.isFull(session) ? 'Full · Waitlist' : 'Select';
  }

  hidePicture(category: CarCategory): void {
    category.imageUrl = null;
  }

  label(session: RaceSession): string {
    return sessionLabel(session);
  }

  time(session: RaceSession): string {
    return sessionTime(session);
  }

  // one start light per finished part: date, session, car, terms. the fifth is the payment
  get lightsOn(): number {
    let lights = 0;
    if (this.bookingForm.get('raceDate')?.valid) {
      lights++;
    }
    if (this.selectedSession) {
      lights++;
    }
    if (this.bookingForm.get('carNumber')?.valid) {
      lights++;
    }
    if (this.bookingForm.get('termsAccepted')?.valid) {
      lights++;
    }
    if (this.showModal) {
      lights++;
    }
    return lights;
  }

  // under the page title: "Dirt Track, Pune · Circuit · open 06:00 - 23:00"
  get trackLine(): string {
    const track = this.track;
    if (!track) {
      return '';
    }
    return track.trackName + ', ' + track.location + ' · ' + track.trackCategory
      + ' · open ' + track.openTime.substring(0, 5) + ' - ' + track.closeTime.substring(0, 5);
  }

  get amountText(): string {
    if (this.selectedSession) {
      return '₹' + this.selectedSession.price;
    }
    return '-';
  }

  // text on the pay button
  get payText(): string {
    if (this.isSaving) {
      return 'Paying...';
    }
    if (this.selectedSession) {
      return 'Pay ₹' + this.selectedSession.price + ' & Register';
    }
    return 'Pay & Register';
  }

  // Pay & Register: step 1 of 3, our backend checks every booking rule and makes a Razorpay order
  register(): void {
    // a second click while the first payment is still running does nothing
    if (this.isSaving) {
      return;
    }
    this.submitted = true;
    this.errorMessage = '';
    this.bookingForm.markAllAsTouched();
    if (this.profileIncomplete || this.bookingForm.invalid || !this.selectedSession) {
      return;
    }
    if (hasStarted(this.selectedSession)) {
      this.dropStartedSessions();
      return;
    }
    const value = this.bookingForm.value;
    const booking: RaceBookingRequest = {
      sessionId: this.selectedSession.sessionId,
      carNumber: Number(value.carNumber),
      carCategory: value.carCategory,
      comments: value.comments?.trim(),
      termsAccepted: value.termsAccepted
    };
    this.isSaving = true;
    this.paymentService.createOrder(booking).subscribe({
      next: (order) => this.openPaymentWindow(order, booking),
      error: (err) => this.paymentFailed(err, false)
    });
  }

  // the page stayed open and the picked session began meanwhile (same text as the backend)
  private dropStartedSessions(): void {
    this.sessions = this.sessions.filter(isBookable);
    this.selectedSession = null;
    this.errorMessage = 'Booking is closed: this session has already started';
  }

  // step 2: the driver pays in Razorpay's window (closing it comes back as an error)
  private openPaymentWindow(order: PaymentOrder, booking: RaceBookingRequest): void {
    this.paymentService.openCheckout(order).subscribe({
      next: (paid) => this.confirmPayment(paid, booking),
      error: (err) => this.paymentFailed(err, false)
    });
  }

  // step 3: our backend checks the payment and saves the booking
  private confirmPayment(paid: RazorpayResult, booking: RaceBookingRequest): void {
    const request: PaymentConfirmRequest = {
      booking: booking,
      razorpayOrderId: paid.razorpay_order_id,
      razorpayPaymentId: paid.razorpay_payment_id,
      razorpaySignature: paid.razorpay_signature
    };
    this.paymentService.confirm(request).subscribe({
      next: (result) => {
        this.isSaving = false;
        this.showSuccess(result);
      },
      error: (err) => this.paymentFailed(err, true)
    });
  }

  private showSuccess(result: PaymentConfirmResult): void {
    if (result.pendingMessage) {
      this.modalText = result.pendingMessage;
      this.modalHint = 'If the booking can\'t be saved, the amount is refunded.';
    } else if (result.booking?.status === BOOKING_STATUS.WAITLISTED) {
      this.modalText = 'Payment received. The session is full: you are number ' + result.booking.waitlistPosition
        + ' on the waitlist.';
      this.modalHint = 'You move up by yourself when a seat opens. If none opens before the start, the amount is refunded.';
    } else {
      this.modalText = 'Payment received and race registration submitted successfully!';
      this.modalHint = 'Your booking now waits for the admin\'s approval. If it is rejected, the amount is refunded.';
    }
    this.showModal = true;
  }

  // window closed, car number taken meanwhile ... "hasPaid" = the money was already taken when it failed
  private paymentFailed(err: any, hasPaid: boolean): void {
    this.isSaving = false;
    this.errorMessage = err.error?.message || 'Unable to register. Please try again.';
    if (!hasPaid) {
      return;
    }
    // refused after paying (seat or car number taken meanwhile): the backend sends the money back
    if (err.error?.message) {
      this.errorMessage += ' Your payment will be refunded.';
    } else {
      this.errorMessage += ' Please look at My Registrations before you pay again.';
    }
  }

  goBack(): void {
    this.router.navigate(['/user/view/tracks']);
  }

  closeModal(): void {
    this.showModal = false;
    this.router.navigate(['/user/view/registrations']);
  }
}
