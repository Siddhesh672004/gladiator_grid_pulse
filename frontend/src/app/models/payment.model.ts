import { ChartPoint } from './admin-dashboard.model';
import { RaceBooking, RaceBookingRequest } from './race-booking.model';
import { RaceSession } from './session.model';
import { User } from './user.model';

// POST /api/payments/order answer, amount in paise (₹650 = 65000)
export interface PaymentOrder {
  keyId: string;
  orderId: string;
  amount: number;
  currency: string;
  description: string;
  name: string;
  email: string;
  contact: string;
}

export interface RazorpayResult {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
}

// POST /api/payments/confirm
export interface PaymentConfirmRequest {
  booking: RaceBookingRequest;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}

// confirm answer: the booking (201), or only a message while it is still being confirmed (202)
export interface PaymentConfirmResult {
  booking?: RaceBooking;
  pendingMessage?: string;
}

export interface Payment {
  paymentId: number;
  user: User;
  session: RaceSession;
  amount: number;
  status: string;                 // CREATED, CAPTURED, PAID, FAILED, REFUND_PENDING, REFUNDED, REFUND_FAILED
  razorpayOrderId: string;
  razorpayPaymentId?: string;
  razorpayRefundId?: string;
  note?: string;
  createdAt: string;
  paidAt?: string;
  refundedAt?: string;
}

export interface PaymentAudit {
  event: string;
  detail: string;
  createdAt: string;
}

export interface Revenue {
  collected: number;
  refunded: number;
  refundInProgress: number;
  paidBookings: number;
  monthlyRevenue: ChartPoint[];
}

// 'REFUND_PENDING' -> 'Refund initiated', no payment -> ''
const PAYMENT_LABELS: { [status: string]: string } = {
  CREATED: 'Awaiting payment',
  PAID: 'Paid',
  FAILED: 'Not paid',
  REFUND_PENDING: 'Refund initiated',
  REFUNDED: 'Refunded',
  REFUND_FAILED: 'Refund failed'
};

export function paymentLabel(status: string | null | undefined): string {
  if (!status) {
    return '';
  }
  // a status this list does not know is shown as it is
  return PAYMENT_LABELS[status] ?? status;
}
