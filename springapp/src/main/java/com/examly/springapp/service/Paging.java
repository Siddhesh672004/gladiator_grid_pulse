package com.examly.springapp.service;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.examly.springapp.dto.response.PageResponse;

// shared rules of the admin list pages: page from 0, 1 - 50 rows, empty or "All" = no filter
public final class Paging {

    private static final int MAX_SIZE = 50;

    private Paging() {
    }

    public static Pageable pageable(int page, int size, Sort sort) {
        Optional.of(size)
                .filter(rows -> page >= 0 && rows >= 1 && rows <= MAX_SIZE)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Page must be 0 or more and size between 1 and " + MAX_SIZE));
        return PageRequest.of(page, size, sort);
    }

    // "%text%" in small letters, no text = every row
    public static String likePattern(String search) {
        return "%" + Optional.ofNullable(search).map(String::trim).map(String::toLowerCase).orElse("") + "%";
    }

    public static String filter(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(text -> !text.isEmpty() && !text.equalsIgnoreCase("All"))
                .orElse(null);
    }

    // "completed" -> COMPLETED, an unknown word -> 400 with the allowed ones
    public static <E extends Enum<E>> E enumFilter(Class<E> type, String value, String name) {
        return Optional.ofNullable(filter(value))
                .map(text -> Arrays.stream(type.getEnumConstants())
                        .filter(constant -> constant.name().equalsIgnoreCase(text))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(name + " must be one of "
                                + Arrays.toString(type.getEnumConstants()) + " or All")))
                .orElse(null);
    }

    public static <T, R> PageResponse<R> toResponse(Page<T> rows, Function<T, R> mapper) {
        return new PageResponse<>(rows.map(mapper).getContent(), rows.getTotalElements(), rows.getNumber(),
                rows.getSize());
    }
}
