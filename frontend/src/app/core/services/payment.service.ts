import { Injectable, NgZone } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, from, map, switchMap } from 'rxjs';
import { Payment, PaymentAudit, PaymentConfirmRequest, PaymentConfirmResult, PaymentOrder, RazorpayResult, Revenue } from '../../shared/models/payment.model';
import { RaceBooking, RaceBookingRequest } from '../../shared/models/race-booking.model';
import { PageResponse } from '../../shared/models/page-response.model';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// set by checkout.js, which loadCheckout() adds to the page
declare const Razorpay: any;
const CHECKOUT_URL = 'https://checkout.razorpay.com/v1/checkout.js';

export const PAYMENT_CANCELLED = 'Payment was not completed. Nothing was booked.';

// driver: createOrder -> openCheckout (Razorpay window) -> confirm makes the booking. admin: payments page
@Injectable({
  providedIn: 'root'
})
export class PaymentService {

  public apiUrl = environment.apiUrl;
  private checkoutScript?: Promise<void>;

  constructor(private http: HttpClient, private zone: NgZone) { }

  createOrder(booking: RaceBookingRequest): Observable<PaymentOrder> {
    return this.http.post<PaymentOrder>(this.apiUrl + API_ENDPOINTS.PAYMENT_ORDER, booking);
  }

  // 201 = the booking is saved; 202 = paid, the booking is still being confirmed (only a message comes back)
  confirm(request: PaymentConfirmRequest): Observable<PaymentConfirmResult> {
    return this.http.post<any>(this.apiUrl + API_ENDPOINTS.PAYMENT_CONFIRM, request, { observe: 'response' }).pipe(
      map(response => response.status === 202
        ? { pendingMessage: response.body?.message }
        : { booking: response.body as RaceBooking }));
  }

  // emits Razorpay's three values once paid, errors when the window is closed without paying
  // a checkout.js that could not be downloaded ends like a missing Razorpay below
  openCheckout(order: PaymentOrder): Observable<RazorpayResult> {
    return from(this.loadCheckout().catch(() => undefined)).pipe(switchMap(() => new Observable<RazorpayResult>(observer => {
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
    })));
  }

  // checkout.js is downloaded when the first payment window opens (not with every page), then kept
  private loadCheckout(): Promise<void> {
    if (!this.checkoutScript) {
      this.checkoutScript = new Promise<void>((resolve, reject) => {
        const script = document.createElement('script');
        script.src = CHECKOUT_URL;
        script.onload = () => resolve();
        script.onerror = () => {
          // the next payment tries again
          script.remove();
          this.checkoutScript = undefined;
          reject();
        };
        document.head.appendChild(script);
      });
    }
    return this.checkoutScript;
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
