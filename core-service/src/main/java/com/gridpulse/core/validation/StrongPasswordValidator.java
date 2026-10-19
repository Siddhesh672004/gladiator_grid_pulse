package com.gridpulse.core.validation;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// the check behind @StrongPassword
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // empty is @NotBlank's job, a broken rule becomes the message of the box
        Optional<String> problem = Optional.ofNullable(password)
                .filter(typed -> !typed.isBlank())
                .map(this::problemOf)
                .filter(text -> !text.isEmpty());
        problem.ifPresent(text -> report(text, context));
        return problem.isEmpty();
    }

    // every rule is fine, but BCrypt refuses to hash more than 72 bytes
    private String problemOf(String password) {
        String problem = ValidationRules.passwordProblem(password);
        boolean tooManyBytes = problem.isEmpty()
                && password.getBytes(StandardCharsets.UTF_8).length > ValidationRules.PASSWORD_MAX_BYTES;
        return tooManyBytes ? ValidationRules.PASSWORD_TOO_MANY_BYTES : problem;
    }

    private void report(String problem, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(problem).addConstraintViolation();
    }
}
