package com.gridpulse.core.stats.dto;

import java.util.List;

import com.gridpulse.common.paging.ChartPoint;

// everything the admin dashboard shows in ONE answer, the live monitor has its own endpoint (refreshed every 4 seconds)
public record AdminDashboardResponse(
        DashboardMetrics metrics,
        List<ChartPoint> registrationStatus,
        List<ChartPoint> trackCategories,
        List<BookingRaceStatistic> bookingRaceStatistics,
        List<RecentRegistrationResponse> recentRegistrations,
        List<LiveRaceResponse> liveRaces,
        RevenueResponse statement) {
}
