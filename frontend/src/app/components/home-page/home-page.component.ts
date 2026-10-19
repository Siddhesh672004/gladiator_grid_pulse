import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { UserStorageService } from '../../services/user-storage.service';
import { RaceService } from '../../services/race.service';
import { FeedbackService } from '../../services/feedback.service';
import { RaceBooking } from '../../models/race-booking.model';
import { isLiveOrUpcoming, sessionTime } from '../../models/session.model';
import { BOOKING_STATUS } from '../../constants';
import { byIndex } from '../../shared/track-by';

interface QuickAction {
  title: string;
  text: string;
  link: string;
}

// shown on a number card while its call is on the way / when it failed
const LOADING = '…';
const NO_DATA = '—';

const DRIVER_ACTIONS: QuickAction[] = [
  { title: 'View Tracks', text: 'Pick a track and book a session.', link: '/user/view/tracks' },
  { title: 'My Registrations', text: 'See if your bookings were approved.', link: '/user/view/registrations' },
  { title: 'Live Timing', text: 'Follow the leaderboard lap by lap.', link: '/user/live-timing' },
  { title: 'Add Feedback', text: 'Rate a session you drove in.', link: '/user/add/feedback' },
  { title: 'My Feedback', text: 'Everything you have written so far.', link: '/user/view/feedback' }
];

// one more start light per step
const RACE_DAY = ['Sign up', 'Book and pay', 'Get approved', 'Race', 'Rate the session'];

// the driver's home page: the next session, a few live numbers, one line per page. an admin who opens /home goes to the dashboard
@Component({
  selector: 'app-home-page',
  templateUrl: './home-page.component.html'
})
export class HomePageComponent implements OnInit {

  username: string = '';
  actions: QuickAction[] = DRIVER_ACTIONS;
  raceDay: string[] = RACE_DAY;

  trackCount: string = LOADING;
  feedbackCount: string = LOADING;
  bookingCount: string = LOADING;
  approvedCount: string = LOADING;
  nextBooking: RaceBooking | null = null;
  isLoading: boolean = true;
  private waiting: number = 0;      // answers still on their way

  readonly byIndex = byIndex;

  constructor(private authService: AuthService, private userStorage: UserStorageService, private raceService: RaceService,
              private feedbackService: FeedbackService, private router: Router) { }

  ngOnInit(): void {
    this.username = this.authService.getCustomerName();
    if (this.authService.isAdmin()) {
      this.router.navigate(['/admin/dashboard']);
      return;
    }
    this.loadTracks();
    this.loadDriverNumbers();
  }

  time(booking: RaceBooking): string {
    return sessionTime(booking.session);
  }

  // counts the same tracks as View Tracks: active and with a session still open
  private loadTracks(): void {
    this.counted(this.raceService.getAllTracks()).subscribe({
      next: (tracks) => {
        const open = tracks.filter(track => track.active && (track.upcomingSessions ?? 0) > 0);
        this.trackCount = String(open.length);
      },
      error: () => this.trackCount = NO_DATA
    });
  }

  private loadDriverNumbers(): void {
    const userId = this.userStorage.getUserId();
    if (userId === null) {
      this.bookingCount = NO_DATA;
      this.approvedCount = NO_DATA;
      this.feedbackCount = NO_DATA;
      return;
    }
    this.counted(this.raceService.getRaceBookingsByUserId(userId)).subscribe({
      next: (bookings) => {
        this.bookingCount = String(bookings.length);
        const approved = bookings.filter(booking => booking.status === BOOKING_STATUS.APPROVED);
        this.approvedCount = String(approved.length);
        this.nextBooking = this.findNextBooking(bookings);
      },
      error: () => {
        this.bookingCount = NO_DATA;
        this.approvedCount = NO_DATA;
      }
    });
    this.counted(this.feedbackService.getAllfeedbacksByUserId(userId)).subscribe({
      next: (feedbacks) => this.feedbackCount = String(feedbacks.length),
      error: () => this.feedbackCount = NO_DATA
    });
  }

  // the loader stays until every answer is in, so "Nothing booked yet" is never shown before the bookings came
  private counted<T>(request: Observable<T>): Observable<T> {
    this.waiting++;
    return request.pipe(finalize(() => {
      this.waiting--;
      this.isLoading = this.waiting > 0;
    }));
  }

  // the earliest booking (not rejected) whose session is live or still to come
  private findNextBooking(bookings: RaceBooking[]): RaceBooking | null {
    let next: RaceBooking | null = null;
    for (const booking of bookings) {
      if (booking.status === BOOKING_STATUS.REJECTED || !isLiveOrUpcoming(booking.session)) {
        continue;
      }
      if (next === null || this.startOf(booking) < this.startOf(next)) {
        next = booking;
      }
    }
    return next;
  }

  // "2026-10-28" + "11:30:00": texts in this form sort like dates
  private startOf(booking: RaceBooking): string {
    return booking.session.sessionDate + booking.session.startTime;
  }
}
