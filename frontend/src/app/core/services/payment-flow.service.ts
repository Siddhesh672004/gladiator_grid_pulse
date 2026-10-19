import { Injectable } from '@angular/core';
import { PaymentConfirmRequest, PaymentConfirmResult, PaymentOrder, RazorpayResult } from '../../shared/models/payment.model';
import { RaceBookingRequest } from '../../shared/models/race-booking.model';
import { PaymentService } from './payment.service';

// the two steps after a Razorpay order, for the booking page and the Copilot chat: the driver pays in Razorpay's
// window, then our backend checks the payment and saves the booking. each page words the outcome itself
@Injectable({
  providedIn: 'root'
})
export class PaymentFlowService {

  constructor(private paymentService: PaymentService) { }

  // done: 201 (the booking) or 202 (only a message). failed: hasPaid = the money was already taken when it failed
  pay(order: PaymentOrder, booking: RaceBookingRequest, done: (result: PaymentConfirmResult) => void,
      failed: (err: any, hasPaid: boolean) => void): void {
    // closing the window comes back as an error
    this.paymentService.openCheckout(order).subscribe({
      next: (paid) => this.confirm(paid, booking, done, failed),
      error: (err) => failed(err, false)
    });
  }

  private confirm(paid: RazorpayResult, booking: RaceBookingRequest, done: (result: PaymentConfirmResult) => void,
                  failed: (err: any, hasPaid: boolean) => void): void {
    const request: PaymentConfirmRequest = {
      booking: booking,
      razorpayOrderId: paid.razorpay_order_id,
      razorpayPaymentId: paid.razorpay_payment_id,
      razorpaySignature: paid.razorpay_signature
    };
    this.paymentService.confirm(request).subscribe({
      next: (result) => done(result),
      error: (err) => failed(err, true)
    });
  }
}
