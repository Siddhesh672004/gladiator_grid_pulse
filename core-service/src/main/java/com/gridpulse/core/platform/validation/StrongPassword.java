package com.gridpulse.core.platform.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// password rules for NEW passwords (StrongPasswordValidator, ValidationRules.passwordProblem); empty is left to @NotBlank
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    // never shown, the validator puts the broken rule in its place
    String message() default "Password is not strong enough";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
