package com.gridpulse.payment.dto.response;

import java.util.List;

import com.gridpulse.common.paging.ChartPoint;

// the money summary, every number comes from ONE database snapshot (one read-only transaction in PaymentRecordServiceImpl)
public record RevenueResponse(
        double collected,
        double refunded,
        double refundInProgress,     // on its way back (REFUND_PENDING + REFUND_FAILED)
        long paidBookings,
        List<ChartPoint> monthlyRevenue) {
}
