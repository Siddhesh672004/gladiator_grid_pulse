package com.examly.springapp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// the picture links of a track in ONE text column, one link per line (no table of their own)
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> items) {
        // no pictures = an empty column
        return Optional.ofNullable(items)
                .filter(list -> !list.isEmpty())
                .map(list -> String.join("\n", list))
                .orElse(null);
    }

    @Override
    public List<String> convertToEntityAttribute(String text) {
        // an empty column = no pictures, the list stays changeable either way
        return Optional.ofNullable(text)
                .filter(column -> !column.isBlank())
                .map(column -> new ArrayList<>(column.lines().toList()))
                .orElseGet(ArrayList::new);
    }
}
