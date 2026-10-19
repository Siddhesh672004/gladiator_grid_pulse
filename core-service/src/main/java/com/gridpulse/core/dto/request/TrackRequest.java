package com.gridpulse.core.dto.request;

import java.time.LocalTime;
import java.util.List;

import com.gridpulse.core.validation.ValidationRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// pictures are uploaded first (POST /api/images/track), this body only carries their links
public record TrackRequest(

        @NotBlank(message = "Track Name is required")
        @Size(min = 3, message = "Track Name must be at least 3 characters")
        @Size(max = 100, message = "Track Name must be at most 100 characters")
        @Pattern(regexp = ValidationRules.HAS_LETTER_PATTERN, message = ValidationRules.TRACK_NAME_NO_LETTER)
        String trackName,

        @NotBlank(message = "Location is required")
        @Size(min = 3, message = "Location must be at least 3 characters")
        @Size(max = 200, message = "Location must be at most 200 characters")
        @Pattern(regexp = ValidationRules.HAS_LETTER_PATTERN, message = ValidationRules.LOCATION_NO_LETTER)
        String location,

        @NotBlank(message = "Description is required")
        @Size(min = 10, message = "Description must be at least 10 characters")
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotBlank(message = "Track Category is required")
        String trackCategory,

        @NotNull(message = "Indoor/Outdoor information is required")
        Boolean isIndoor,

        @NotNull(message = "Opening Time is required")
        LocalTime openTime,

        @NotNull(message = "Closing Time is required")
        LocalTime closeTime,

        @NotBlank(message = "Facilities are required")
        @Size(min = 3, message = "Facilities must be at least 3 characters")
        @Size(max = 500, message = "Facilities must be at most 500 characters")
        String facilities,

        @NotNull(message = ValidationRules.TRACK_PICTURES_REQUIRED)
        @Size(min = ValidationRules.TRACK_PICTURES_MIN, message = ValidationRules.TRACK_PICTURES_REQUIRED)
        @Size(max = ValidationRules.TRACK_PICTURES_MAX, message = ValidationRules.TRACK_PICTURES_TOO_MANY)
        List<String> imageUrls) {

    public TrackRequest {
        trackName = ValidationRules.clean(trackName);
        location = ValidationRules.clean(location);
        description = ValidationRules.clean(description);
        trackCategory = ValidationRules.clean(trackCategory);
        facilities = ValidationRules.clean(facilities);
    }
}
