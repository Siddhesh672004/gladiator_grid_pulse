package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.response.AdminDashboardResponse;
import com.examly.springapp.dto.response.LiveRaceResponse;

// read-only numbers for the admin dashboard page, built in AdminDashboardServiceImpl
public interface AdminDashboardService {

    AdminDashboardResponse getDashboard();

    List<LiveRaceResponse> getLiveRaces();
}
