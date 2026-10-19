package com.gridpulse.core.dto.response;

public record DashboardMetrics(
        long totalUsers,
        long registrations,
        long totalTracks,
        long totalBookings,
        long activeRaces,             // sessions that are LIVE right now
        double totalRevenue,          // money paid and kept (payments in status PAID)
        long totalFeedback,
        long feedbackNeedsAttention) {
}
