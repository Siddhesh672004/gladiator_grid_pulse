package com.gridpulse.ai.entity;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// float[] embedding <-> comma separated text column (mysql has no float array type)
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] vector) {
        // no vector = empty column
        return Optional.ofNullable(vector)
                .filter(values -> values.length > 0)
                .map(FloatArrayConverter::toText)
                .orElse(null);
    }

    @Override
    public float[] convertToEntityAttribute(String text) {
        // empty column = no vector yet
        return Optional.ofNullable(text)
                .filter(column -> !column.isBlank())
                .map(FloatArrayConverter::toVector)
                .orElse(null);
    }

    private static String toText(float[] vector) {
        // "0.12,-0.5,..." in vector order
        return IntStream.range(0, vector.length)
                .mapToObj(i -> String.valueOf(vector[i]))
                .collect(Collectors.joining(","));
    }

    private static float[] toVector(String text) {
        String[] parts = text.split(",");
        float[] vector = new float[parts.length];
        // each part read straight as a float, never through a double
        IntStream.range(0, parts.length)
                .forEach(i -> vector[i] = Float.parseFloat(parts[i].trim()));
        return vector;
    }
}
