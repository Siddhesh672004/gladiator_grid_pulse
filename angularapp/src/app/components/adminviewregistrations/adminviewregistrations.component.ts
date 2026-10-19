import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { BookingStatusRequest, RaceBooking, raceStatusText, statusLabel } from '../../models/race-booking.model';
import { RaceSession, sessionLabel, sessionTime } from '../../models/session.model';
import { RaceService } from '../../services/race.service';
import { UserStorageService } from '../../services/user-storage.service';
import { ADMIN_ROLES, BOOKING_STATUS, BOOKING_STATUSES, COMPETITION_PHASE, LIVE_REFRESH_MS, SEARCH_DEBOUNCE_MS, SESSION_STATUS } from '../../constants';
import { pageCount } from '../../shared/paging';
import { paymentLabel } from '../../models/payment.model';
import { FieldOption, toOptions } from '../field/field.component';
import { byKey } from '../../shared/track-by';

// every booking of every driver: search, status filter, approve / reject (the reason is required), details, delete -> RaceService
// one page from the backend, reloaded every 4 s; the approval that fills the last seat rejects everybody still waiting (the backend does that)
const LOAD_ERROR = 'Unable to load registrations';
const REASON_MIN = 5;
const REASON_MAX = 500;

@Component({
  selector: 'app-adminviewregistrations',
  templateUrl: './adminviewregistrations.component.html'
})
export class AdminviewregistrationsComponent implements OnInit, OnDestroy {

  bookings: RaceBooking[] = [];        // the rows of this page only
  total: number = 0;                   // every booking that matches the search + filter
  statusOptions: FieldOption[] = toOptions(BOOKING_STATUSES);
  searchText: string = '';
  statusFilter: string = 'All';
  page: number = 1;
  pageSize: number = 10;
  isLoading: boolean = true;
  errorMessage: string = '';
  busyId: number | null = null;
  selected: RaceBooking | null = null;
  bookingToDelete: RaceBooking | null = null;
  bookingToReject: RaceBooking | null = null;
  rejectNote: string = '';
  rejectChecked: boolean = false;      // typed in the box or pressed Reject Booking: from then on the message shows

  private refreshTimer: number = 0;
  private bookingsRequest?: Subscription;
  private pageClosed: boolean = false;
  private searchChanges = new Subject<void>();
  private searchWatch?: Subscription;

  readonly byBooking = byKey<RaceBooking>('raceBookingId');

  constructor(private raceService: RaceService, private userStorage: UserStorageService) { }

  ngOnInit(): void {
    // typing: one request once the typing stops, back to page 1
    this.searchWatch = this.searchChanges.pipe(debounceTime(SEARCH_DEBOUNCE_MS)).subscribe(() => this.filterChanged());
    this.loadBookings();
  }

  ngOnDestroy(): void {
    this.pageClosed = true;
    this.searchWatch?.unsubscribe();
    this.stopRefresh();
  }

  searchTyped(): void {
    this.searchChanges.next();
  }

  filterChanged(): void {
    this.page = 1;
    this.loadBookings();
  }

  goToPage(page: number): void {
    this.page = page;
    this.loadBookings();
  }

  get hasFilters(): boolean {
    return this.searchText.trim() !== '' || this.statusFilter !== 'All';
  }

  // load the list now, then every 4 s. called again after every action, so the list is fresh at once
  loadBookings(): void {
    this.stopRefresh();
    if (this.pageClosed) {
      return;
    }
    this.refreshBookings();
    // a hidden tab skips the rounds, it reads the list again when it is shown (tabShown)
    this.refreshTimer = window.setInterval(() => {
      if (!document.hidden) {
        this.refreshBookings();
      }
    }, LIVE_REFRESH_MS);
  }

  @HostListener('document:visibilitychange')
  tabShown(): void {
    if (!document.hidden && this.refreshTimer) {
      this.refreshBookings();
    }
  }

  private refreshBookings(): void {
    // another login in this browser (tabs share localStorage) -> stop, never send someone else's token
    if (!ADMIN_ROLES.includes(this.userStorage.getUserRole() ?? '')) {
      this.stopRefresh();
      return;
    }
    // an older answer that is still on its way is dropped
    this.bookingsRequest?.unsubscribe();
    this.bookingsRequest = this.raceService.getBookingPage(this.searchText.trim(), this.statusFilter, this.page - 1, this.pageSize).subscribe({
      next: (result) => this.showBookings(result.content, result.totalElements),
      error: () => {
        this.isLoading = false;
        this.errorMessage = LOAD_ERROR;
      }
    });
  }

  private showBookings(bookings: RaceBooking[], total: number): void {
    // the page got empty (last row deleted, list got shorter): show the new last page
    const lastPage = pageCount(total, this.pageSize);
    if (bookings.length === 0 && this.page > lastPage) {
      this.page = lastPage;
      this.refreshBookings();
      return;
    }
    // newest booking first (the backend sorts)
    this.bookings = bookings;
    this.total = total;
    this.isLoading = false;
    // the details popup may be open: show the fresh copy of that booking (it may sit on another page now)
    if (this.selected) {
      const openId = this.selected.raceBookingId;
      this.selected = this.bookings.find(booking => booking.raceBookingId === openId) || this.selected;
    }
    // loading works again (an action error stays until the next action)
    if (this.errorMessage === LOAD_ERROR) {
      this.errorMessage = '';
    }
  }

  raceStatus(booking: RaceBooking): string {
    return raceStatusText(booking);
  }

  statusText(booking: RaceBooking): string {
    return statusLabel(booking);
  }

  payment(booking: RaceBooking): string {
    return paymentLabel(booking.paymentStatus);
  }

  label(session: RaceSession): string {
    return sessionLabel(session);
  }

  time(session: RaceSession): string {
    return sessionTime(session);
  }

  // approve / reject only before the session starts, and never for a race place (those come from qualifying)
  canDecide(booking: RaceBooking): boolean {
    return booking.session.status === SESSION_STATUS.SCHEDULED && booking.session.phase !== COMPETITION_PHASE.RACING;
  }

  // the money of a rejected booking went back: the driver has to book again. a waitlisted one moves up by itself
  canApprove(booking: RaceBooking): boolean {
    const refunded = booking.status === BOOKING_STATUS.REJECTED && !!booking.paymentStatus && booking.paymentStatus !== 'PAID';
    return this.canDecide(booking) && booking.status !== BOOKING_STATUS.APPROVED
      && booking.status !== BOOKING_STATUS.WAITLISTED && !refunded;
  }

  approve(booking: RaceBooking): void {
    this.sendStatus(booking, { status: BOOKING_STATUS.APPROVED });
  }

  askRejectReason(booking: RaceBooking): void {
    this.bookingToReject = booking;
    this.rejectNote = '';
    this.rejectChecked = false;
  }

  // the first broken rule of the reason, '' = fine. same rules and texts as the backend
  get rejectError(): string {
    if (!this.rejectChecked) {
      return '';
    }
    const note = this.rejectNote.trim();
    if (!note) {
      return 'Please write a reason for rejecting this booking';
    }
    if (note.length < REASON_MIN) {
      return `Reason must be at least ${REASON_MIN} characters`;
    }
    if (note.length > REASON_MAX) {
      return `Reason must be at most ${REASON_MAX} characters`;
    }
    return '';
  }

  reject(): void {
    const booking = this.bookingToReject;
    this.rejectChecked = true;
    if (!booking || this.rejectError) {
      return;
    }
    this.bookingToReject = null;
    this.sendStatus(booking, { status: BOOKING_STATUS.REJECTED, adminNote: this.rejectNote.trim() });
  }

  private sendStatus(booking: RaceBooking, request: BookingStatusRequest): void {
    if (!booking.raceBookingId || this.busyId !== null) {
      return;
    }
    this.errorMessage = '';
    this.busyId = booking.raceBookingId;
    this.raceService.updateRaceBooking(booking.raceBookingId, request).subscribe({
      next: (updated) => {
        this.busyId = null;
        // put the answer of the backend in place of the old row
        const index = this.bookings.findIndex(row => row.raceBookingId === updated.raceBookingId);
        if (index >= 0) {
          this.bookings[index] = updated;
        }
        this.loadBookings();
      },
      error: (err) => {
        this.busyId = null;
        this.errorMessage = err.error?.message || 'Unable to update the registration';
        this.loadBookings();
      }
    });
  }

  deleteBooking(): void {
    const booking = this.bookingToDelete;
    this.bookingToDelete = null;
    if (!booking?.raceBookingId) {
      return;
    }
    this.raceService.deleteRaceBooking(booking.raceBookingId).subscribe({
      next: () => {
        this.bookings = this.bookings.filter(b => b.raceBookingId !== booking.raceBookingId);
        this.loadBookings();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Unable to delete the registration';
        this.loadBookings();
      }
    });
  }

  private stopRefresh(): void {
    window.clearInterval(this.refreshTimer);
    this.refreshTimer = 0;
    this.bookingsRequest?.unsubscribe();
  }
}
