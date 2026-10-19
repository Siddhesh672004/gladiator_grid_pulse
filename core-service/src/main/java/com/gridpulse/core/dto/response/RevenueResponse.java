package com.gridpulse.core.dto.response;

import java.util.List;

// the money summary from payment-service, every number from ONE database snapshot there
public record RevenueResponse(
        double collected,
        double refunded,
        double refundInProgress,     // on its way back (REFUND_PENDING + REFUND_FAILED)
        long paidBookings,
        List<ChartPoint> monthlyRevenue) {
}
