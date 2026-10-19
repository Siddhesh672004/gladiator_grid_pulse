package com.gridpulse.core.dto.response;

import java.util.List;

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
