import { Component, OnInit } from '@angular/core';
import { Payment, PaymentAudit, Revenue, paymentLabel } from '../../models/payment.model';
import { RaceSession, sessionLabel, sessionTime } from '../../models/session.model';
import { PaymentService } from '../../services/payment.service';
import { pageOf } from '../../shared/paging';
import { FieldOption, toOptions } from '../field/field.component';

// Payments (/admin/view/payments): money summary, every payment (newest first), Show More = details + audit trail -> PaymentService
// Retry refund is for a refund Razorpay refused; refunds move on by themselves (backend job), Refresh shows the latest state
@Component({
  selector: 'app-adminviewpayments',
  templateUrl: './adminviewpayments.component.html'
})
export class AdminviewpaymentsComponent implements OnInit {

  payments: Payment[] = [];
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

  constructor(private paymentService: PaymentService) { }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.errorMessage = '';
    this.paymentService.getAllPayments().subscribe({
      next: (payments) => {
        this.payments = payments;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Unable to load the payments';
      }
    });
    this.paymentService.getSummary().subscribe({
      next: (summary) => this.summary = summary,
      error: () => this.summary = null
    });
  }

  get filteredPayments(): Payment[] {
    const text = this.searchText.trim().toLowerCase();
    return this.payments.filter(payment =>
      (this.statusFilter === 'All' || payment.status === this.statusFilter)
      && (payment.user.username + ' ' + payment.user.email + ' ' + payment.session.trackName).toLowerCase().includes(text));
  }

  get pagedPayments(): Payment[] {
    return pageOf(this.filteredPayments, this.page, this.pageSize);
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
