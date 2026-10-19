package com.gridpulse.core.stats.service;

import java.util.List;

import com.gridpulse.core.stats.dto.AdminDashboardResponse;
import com.gridpulse.core.stats.dto.LiveRaceResponse;

// read-only numbers for the admin dashboard page, built in AdminDashboardServiceImpl
public interface AdminDashboardService {

    AdminDashboardResponse getDashboard();

    List<LiveRaceResponse> getLiveRaces();
}
