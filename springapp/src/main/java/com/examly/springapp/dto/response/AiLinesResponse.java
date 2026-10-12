package com.examly.springapp.dto.response;

import java.util.List;

// lines of text from the ai (feedback suggestions and the admin summary)
// an empty list means the ai could not answer (no key, limit reached, timeout), not an error
public record AiLinesResponse(List<String> lines) {
}
