import { Injectable, NgZone } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Payment, PaymentAudit, PaymentConfirmRequest, PaymentOrder, RazorpayResult, Revenue } from '../models/payment.model';
import { RaceBooking, RaceBookingRequest } from '../models/race-booking.model';
import { PageResponse } from '../models/admin-user.model';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// checkout.js is loaded in index.html
declare const Razorpay: any;

export const PAYMENT_CANCELLED = 'Payment was not completed. Nothing was booked.';

// driver: createOrder -> openCheckout (Razorpay window) -> confirm makes the booking. admin: payments page
@Injectable({
  providedIn: 'root'
})
export class PaymentService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private zone: NgZone) { }

  createOrder(booking: RaceBookingRequest): Observable<PaymentOrder> {
    return this.http.post<PaymentOrder>(this.apiUrl + API_ENDPOINTS.PAYMENT_ORDER, booking);
  }

  confirm(request: PaymentConfirmRequest): Observable<RaceBooking> {
    return this.http.post<RaceBooking>(this.apiUrl + API_ENDPOINTS.PAYMENT_CONFIRM, request);
  }

  // emits Razorpay's three values once paid, errors when the window is closed without paying
  openCheckout(order: PaymentOrder): Observable<RazorpayResult> {
    return new Observable<RazorpayResult>(observer => {
      if (typeof Razorpay === 'undefined') {
        observer.error({ error: { message: 'The payment window could not be loaded. Please check the internet connection.' } });
        return;
      }
      const checkout = new Razorpay({
        key: order.keyId,
        order_id: order.orderId,
        amount: order.amount,
        currency: order.currency,
        name: 'GridPulse',
        description: order.description,
        prefill: { name: order.name, email: order.email, contact: order.contact },
        theme: { color: '#17181c' },
        handler: (result: RazorpayResult) => this.zone.run(() => {
          observer.next(result);
          observer.complete();
        }),
        modal: {
          ondismiss: () => this.zone.run(() => observer.error({ error: { message: PAYMENT_CANCELLED } }))
        }
      });
      checkout.open();
    });
  }

  getAllPayments(): Observable<Payment[]> {
    return this.http.get<Payment[]>(this.apiUrl + API_ENDPOINTS.PAYMENTS);
  }

  // admin list, page counts from 0
  getPaymentPage(search: string, status: string, page: number, size: number): Observable<PageResponse<Payment>> {
    const params = new HttpParams()
      .set('search', search)
      .set('status', status)
      .set('page', page)
      .set('size', size);
    return this.http.get<PageResponse<Payment>>(this.apiUrl + API_ENDPOINTS.PAYMENTS_PAGE, { params });
  }

  getSummary(): Observable<Revenue> {
    return this.http.get<Revenue>(this.apiUrl + API_ENDPOINTS.PAYMENT_SUMMARY);
  }

  getTrail(orderId: string): Observable<PaymentAudit[]> {
    const params = new HttpParams().set('orderId', orderId);
    return this.http.get<PaymentAudit[]>(this.apiUrl + API_ENDPOINTS.PAYMENT_AUDIT, { params });
  }

  retryRefund(paymentId: number): Observable<Payment> {
    return this.http.post<Payment>(this.apiUrl + API_ENDPOINTS.PAYMENT_RETRY_REFUND(paymentId), {});
  }
}
