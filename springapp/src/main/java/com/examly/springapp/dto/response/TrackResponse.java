package com.examly.springapp.dto.response;

import java.time.LocalTime;
import java.util.List;

public record TrackResponse(
        Long trackId,
        String trackName,
        String description,
        String location,
        String trackCategory,
        Boolean isIndoor,
        LocalTime openTime,
        LocalTime closeTime,
        String facilities,
        boolean active,
        List<String> imageUrls, // the first one is the cover picture
        long upcomingSessions, // 0 = drivers do not see the track
        Double fromPrice) {
}
