package com.gridpulse.core.stats.dto;

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
