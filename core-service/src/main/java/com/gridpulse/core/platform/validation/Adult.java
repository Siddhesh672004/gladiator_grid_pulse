package com.gridpulse.core.platform.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// date of birth must be in the past, a real date and 18+, counted in India time (AdultValidator, ValidationRules.dateOfBirthProblem)
// empty is left to @NotNull
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AdultValidator.class)
public @interface Adult {

    // never shown, the validator puts the broken rule in its place
    String message() default ValidationRules.TOO_YOUNG;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
