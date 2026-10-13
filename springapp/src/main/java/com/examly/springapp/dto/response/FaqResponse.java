package com.examly.springapp.dto.response;

public record FaqResponse(Long id, String category, String question, String answer) {
}
