package com.examly.springapp.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import com.examly.springapp.validation.ValidationRules;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// laps and max cars come from the track category (RaceRules.ruleFor)
public record CompetitionRequest(

        @NotNull(message = "Track is required")
        Long trackId,

        @NotBlank(message = "Competition Name is required")
        @Size(min = 3, message = "Competition Name must be at least 3 characters")
        @Size(max = 100, message = "Competition Name must be at most 100 characters")
        @Pattern(regexp = ValidationRules.HAS_LETTER_PATTERN, message = ValidationRules.COMPETITION_NAME_NO_LETTER)
        String competitionName,

        @NotNull(message = "Date is required")
        LocalDate competitionDate,

        @NotNull(message = "Qualifying Start Time is required")
        LocalTime qualifyingStartTime,

        @NotNull(message = "Qualifying End Time is required")
        LocalTime qualifyingEndTime,

        @NotNull(message = "Race Start Time is required")
        LocalTime raceStartTime,

        @NotNull(message = "Race End Time is required")
        LocalTime raceEndTime,

        @NotNull(message = "Entry fee is required")
        @Min(value = 1, message = "Entry fee must be at least 1")
        @Max(value = ValidationRules.PRICE_MAX, message = "Entry fee must be at most " + ValidationRules.PRICE_MAX)
        @Digits(integer = 9, fraction = 0, message = "Entry fee can only have digits")
        Double entryFee) {

    public CompetitionRequest {
        competitionName = ValidationRules.clean(competitionName);
    }
}
