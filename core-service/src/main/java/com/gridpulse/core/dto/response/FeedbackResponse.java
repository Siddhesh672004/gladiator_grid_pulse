package com.gridpulse.core.dto.response;

import java.time.LocalDate;

import com.gridpulse.core.model.Sentiment;
import com.fasterxml.jackson.annotation.JsonInclude;

// sentiment + urgent = the ai label, admins only: null in a driver's answer, NON_NULL leaves it out of the json
// rating is null only for feedback written before ratings existed
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedbackResponse(
        Long feedbackId,
        String feedbackText,
        Integer rating,
        LocalDate date,
        int userId,
        UserResponse user,
        SessionResponse session,
        Sentiment sentiment,
        Boolean urgent) {
}
