package com.gridpulse.core.stats.dto;

import java.util.List;

import com.gridpulse.common.paging.ChartPoint;

// the money summary from payment-service, every number from ONE database snapshot there
public record RevenueResponse(
        double collected,
        double refunded,
        double refundInProgress,     // on its way back (REFUND_PENDING + REFUND_FAILED)
        long paidBookings,
        List<ChartPoint> monthlyRevenue) {
}
