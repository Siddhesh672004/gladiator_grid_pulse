package com.gridpulse.core.service;

import java.util.List;

import com.gridpulse.core.dto.response.AdminDashboardResponse;
import com.gridpulse.core.dto.response.LiveRaceResponse;

// read-only numbers for the admin dashboard page, built in AdminDashboardServiceImpl
public interface AdminDashboardService {

    AdminDashboardResponse getDashboard();

    List<LiveRaceResponse> getLiveRaces();
}
