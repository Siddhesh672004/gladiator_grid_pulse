package com.gridpulse.core.dto.response;

import java.util.List;

// one page of a long list, the database only sent this page (spring data Pageable)
public record PageResponse<T>(List<T> content, long totalElements, int page, int size) {
}
