import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { RaceBooking, raceStatusText, statusLabel } from '../../models/race-booking.model';
import { RaceSession, sessionLabel, sessionTime } from '../../models/session.model';
import { RaceService } from '../../services/race.service';
import { UserStorageService } from '../../services/user-storage.service';
import { BOOKING_STATUS, COMPETITION_PHASE, LIVE_REFRESH_MS, ROLES, SESSION_STATUS } from '../../constants';
import { paymentLabel } from '../../models/payment.model';
import { pageOf } from '../../shared/paging';
import { blobErrorMessage, fileNameOf, saveFile } from '../../shared/download';

// the driver's own bookings, reloaded every 4 s -> RaceService
// cancel while Pending = full refund, cancel an Approved one = no refund. an Approved one has its e-ticket (pdf with qr)
const LOAD_ERROR = 'Unable to load your registrations';

@Component({
  selector: 'app-userviewmyregistrations',
  templateUrl: './userviewmyregistrations.component.html'
})
export class UserviewmyregistrationsComponent implements OnInit, OnDestroy {

  bookings: RaceBooking[] = [];
  searchText: string = '';
  page: number = 1;
  pageSize: number = 10;
  isLoading: boolean = true;
  errorMessage: string = '';
  selected: RaceBooking | null = null;
  bookingToCancel: RaceBooking | null = null;
  downloadingId: number | null = null;

  private ownerId: number | null = null;
  private refreshTimer: number = 0;
  private bookingsRequest?: Subscription;
  private pageClosed: boolean = false;

  constructor(private raceService: RaceService, private userStorage: UserStorageService) { }

  ngOnInit(): void {
    this.ownerId = this.userStorage.getUserId();
    this.loadBookings();
  }

  ngOnDestroy(): void {
    this.pageClosed = true;
    this.stopRefresh();
  }

  canDownloadTicket(booking: RaceBooking): boolean {
    return booking.status === BOOKING_STATUS.APPROVED;
  }

  downloadTicket(booking: RaceBooking): void {
    const id = booking.raceBookingId;
    if (!id || this.downloadingId !== null) {
      return;
    }
    this.errorMessage = '';
    this.downloadingId = id;
    this.raceService.downloadTicket(id).subscribe({
      next: (response) => {
        this.downloadingId = null;
        if (response.body) {
          saveFile(response.body, fileNameOf(response.headers.get('Content-Disposition'), `gridpulse-ticket-${id}.pdf`));
        }
      },
      error: async (err) => {
        this.downloadingId = null;
        this.errorMessage = await blobErrorMessage(err, 'Unable to download the ticket');
      }
    });
  }

  raceStatus(booking: RaceBooking): string {
    return raceStatusText(booking);
  }

  statusText(booking: RaceBooking): string {
    return statusLabel(booking);
  }

  label(session: RaceSession): string {
    return sessionLabel(session);
  }

  time(session: RaceSession): string {
    return sessionTime(session);
  }

  // an approved car can only be cancelled while its session is still to come
  // a competition race place is never removed by hand: it comes from qualifying
  canCancel(booking: RaceBooking): boolean {
    if (booking.session.phase === COMPETITION_PHASE.RACING) {
      return false;
    }
    return booking.status !== BOOKING_STATUS.APPROVED || booking.session.status === SESSION_STATUS.SCHEDULED;
  }

  cancelWarning(booking: RaceBooking): string {
    if (booking.status === BOOKING_STATUS.APPROVED) {
      return 'Cancelling an approved booking does not refund the amount.';
    }
    if (booking.status === BOOKING_STATUS.PENDING) {
      return 'It is not approved yet, so the full amount is refunded.';
    }
    return '';
  }

  payment(booking: RaceBooking): string {
    return paymentLabel(booking.paymentStatus);
  }

  cancelLabel(booking: RaceBooking): string {
    return booking.status === BOOKING_STATUS.REJECTED ? 'Remove' : 'Cancel';
  }

  // load the list now, then every 4 s. called again after a cancel, so the list is fresh at once
  loadBookings(): void {
    this.stopRefresh();
    if (this.pageClosed) {
      return;
    }
    if (this.ownerId === null) {
      this.isLoading = false;
      return;
    }
    this.refreshBookings();
    this.refreshTimer = window.setInterval(() => this.refreshBookings(), LIVE_REFRESH_MS);
  }

  private refreshBookings(): void {
    const userId = this.ownerId;
    // another login in this browser (tabs share localStorage) -> stop
    if (userId === null || !this.isStillOwner(userId)) {
      this.stopRefresh();
      return;
    }
    // an older answer that is still on its way is dropped
    this.bookingsRequest?.unsubscribe();
    this.bookingsRequest = this.raceService.getRaceBookingsByUserId(userId).subscribe({
      next: (bookings) => this.showBookings(bookings),
      error: () => {
        this.isLoading = false;
        this.errorMessage = LOAD_ERROR;
      }
    });
  }

  private showBookings(bookings: RaceBooking[]): void {
    // newest session first
    this.bookings = bookings.sort((a, b) =>
      (b.session.sessionDate + b.session.startTime).localeCompare(a.session.sessionDate + a.session.startTime));
    this.isLoading = false;
    // the details popup may be open: show the fresh copy of that booking
    if (this.selected) {
      const openId = this.selected.raceBookingId;
      this.selected = this.bookings.find(booking => booking.raceBookingId === openId) || null;
    }
    if (this.errorMessage === LOAD_ERROR) {
      this.errorMessage = '';
    }
  }

  get filteredBookings(): RaceBooking[] {
    const text = this.searchText.trim().toLowerCase();
    return this.bookings.filter(booking => (booking.session?.trackName || '').toLowerCase().includes(text));
  }

  get pagedBookings(): RaceBooking[] {
    return pageOf(this.filteredBookings, this.page, this.pageSize);
  }

  cancelBooking(): void {
    const booking = this.bookingToCancel;
    this.bookingToCancel = null;
    this.errorMessage = '';
    if (!booking?.raceBookingId) {
      return;
    }
    this.raceService.deleteRaceBooking(booking.raceBookingId).subscribe({
      next: () => {
        this.bookings = this.bookings.filter(b => b.raceBookingId !== booking.raceBookingId);
        this.loadBookings();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Unable to cancel the registration';
      }
    });
  }

  private isStillOwner(userId: number): boolean {
    return this.userStorage.getUserId() === userId && this.userStorage.getUserRole() === ROLES.USER;
  }

  private stopRefresh(): void {
    window.clearInterval(this.refreshTimer);
    this.bookingsRequest?.unsubscribe();
  }
}
