package com.gridpulse.core.validation;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// the check behind @Adult
public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {

    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        // empty is @NotNull's job, a broken rule becomes the message of the box
        Optional<String> problem = Optional.ofNullable(dateOfBirth)
                .map(ValidationRules::dateOfBirthProblem)
                .filter(text -> !text.isEmpty());
        problem.ifPresent(text -> report(text, context));
        return problem.isEmpty();
    }

    private void report(String problem, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(problem).addConstraintViolation();
    }
}
