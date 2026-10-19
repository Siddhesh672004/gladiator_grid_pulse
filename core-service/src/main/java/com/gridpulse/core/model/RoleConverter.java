package com.gridpulse.core.model;

import java.util.Optional;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// stores the label ("Admin" / "User"), the same text the column already held
@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        // no role = an empty column
        return Optional.ofNullable(role)
                .map(Role::getLabel)
                .orElse(null);
    }

    @Override
    public Role convertToEntityAttribute(String label) {
        // an empty column = no role
        return Optional.ofNullable(label)
                .map(Role::fromLabel)
                .orElse(null);
    }
}
