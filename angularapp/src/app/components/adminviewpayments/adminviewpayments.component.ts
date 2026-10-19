import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { Payment, PaymentAudit, Revenue, paymentLabel } from '../../models/payment.model';
import { RaceSession, sessionLabel, sessionTime } from '../../models/session.model';
import { PaymentService } from '../../services/payment.service';
import { pageCount } from '../../shared/paging';
import { SEARCH_DEBOUNCE_MS } from '../../constants';
import { FieldOption, toOptions } from '../field/field.component';
import { byIndex, byKey } from '../../shared/track-by';

// Payments (/admin/view/payments): money summary, every payment (newest first, one page from the backend), Show More = details + audit trail -> PaymentService
// Retry refund is for a refund Razorpay refused; refunds move on by themselves (backend job), Refresh shows the latest state
@Component({
  selector: 'app-adminviewpayments',
  templateUrl: './adminviewpayments.component.html'
})
export class AdminviewpaymentsComponent implements OnInit, OnDestroy {

  payments: Payment[] = [];            // the rows of this page only
  total: number = 0;                   // every payment that matches the search + filter
  summary: Revenue | null = null;
  statuses: string[] = ['PAID', 'REFUND_PENDING', 'REFUNDED', 'REFUND_FAILED', 'CREATED', 'FAILED'];
  statusOptions: FieldOption[] = this.statuses.map(status => ({ value: status, text: paymentLabel(status) }));
  statusFilter: string = 'All';
  searchText: string = '';
  page: number = 1;
  pageSize: number = 10;
  isLoading: boolean = true;
  errorMessage: string = '';
  busyId: number | null = null;

  selected: Payment | null = null;
  trail: PaymentAudit[] = [];
  trailLoading: boolean = false;

  private pageRequest?: Subscription;
  private searchChanges = new Subject<void>();
  private searchWatch?: Subscription;

  readonly byIndex = byIndex;
  readonly byPayment = byKey<Payment>('paymentId');

  constructor(private paymentService: PaymentService) { }

  ngOnInit(): void {
    // typing: one request once the typing stops, back to page 1
    this.searchWatch = this.searchChanges.pipe(debounceTime(SEARCH_DEBOUNCE_MS)).subscribe(() => this.filterChanged());
    this.load();
  }

  ngOnDestroy(): void {
    this.searchWatch?.unsubscribe();
    this.pageRequest?.unsubscribe();
  }

  // Refresh: the page on screen + the summary
  load(): void {
    this.errorMessage = '';
    this.loadPage();
    this.paymentService.getSummary().subscribe({
      next: (summary) => this.summary = summary,
      error: () => this.summary = null
    });
  }

  // search looks at the driver's name, email and the track (the backend does it)
  private loadPage(): void {
    // an older answer that is still on its way is dropped
    this.pageRequest?.unsubscribe();
    this.pageRequest = this.paymentService.getPaymentPage(this.searchText.trim(), this.statusFilter, this.page - 1, this.pageSize).subscribe({
      next: (result) => {
        // the page got empty (the list got shorter): show the new last page
        const lastPage = pageCount(result.totalElements, this.pageSize);
        if (result.content.length === 0 && this.page > lastPage) {
          this.page = lastPage;
          this.loadPage();
          return;
        }
        this.payments = result.content;
        this.total = result.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Unable to load the payments';
      }
    });
  }

  searchTyped(): void {
    this.searchChanges.next();
  }

  filterChanged(): void {
    this.page = 1;
    this.loadPage();
  }

  goToPage(page: number): void {
    this.page = page;
    this.loadPage();
  }

  get hasFilters(): boolean {
    return this.searchText.trim() !== '' || this.statusFilter !== 'All';
  }

  label(status: string): string {
    return paymentLabel(status);
  }

  sessionText(session: RaceSession): string {
    return session.trackName + ' · ' + sessionLabel(session) + ' · ' + session.sessionDate + ' ' + sessionTime(session);
  }

  showMore(payment: Payment): void {
    this.selected = payment;
    this.trail = [];
    this.trailLoading = true;
    this.paymentService.getTrail(payment.razorpayOrderId).subscribe({
      next: (trail) => {
        this.trail = trail;
        this.trailLoading = false;
      },
      error: () => this.trailLoading = false
    });
  }

  retryRefund(payment: Payment): void {
    this.errorMessage = '';
    this.busyId = payment.paymentId;
    this.paymentService.retryRefund(payment.paymentId).subscribe({
      next: (updated) => {
        this.busyId = null;
        // put the answer of the backend in place of the old row
        const index = this.payments.findIndex(row => row.paymentId === updated.paymentId);
        if (index >= 0) {
          this.payments[index] = updated;
        }
      },
      error: (err) => {
        this.busyId = null;
        this.errorMessage = err.error?.message || 'Unable to retry the refund';
      }
    });
  }
}
