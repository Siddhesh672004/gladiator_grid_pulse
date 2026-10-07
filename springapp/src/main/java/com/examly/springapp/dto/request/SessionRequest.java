package com.examly.springapp.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import com.examly.springapp.validation.ValidationRules;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// db rules (opening hours, overlap ...) are checked in RaceSessionServiceImpl
public record SessionRequest(

        @NotNull(message = "Track is required")
        Long trackId,

        @NotNull(message = "Date is required")
        LocalDate sessionDate,

        @NotNull(message = "Start Time is required")
        LocalTime startTime,

        @NotNull(message = "End Time is required")
        LocalTime endTime,

        // whole rupees only, like the box on the form
        @NotNull(message = "Price is required")
        @Min(value = 1, message = "Price must be at least 1")
        @Max(value = ValidationRules.PRICE_MAX, message = "Price must be at most " + ValidationRules.PRICE_MAX)
        @Digits(integer = 9, fraction = 0, message = "Price can only have digits")
        Double price,

        @NotNull(message = "Max Cars is required")
        @Min(value = 1, message = "Max Cars must be at least 1")
        @Max(value = 100, message = "Max Cars must be at most 100")
        Integer maxCars) {
}
