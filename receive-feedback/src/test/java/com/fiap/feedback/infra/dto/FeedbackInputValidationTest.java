package com.fiap.feedback.infra.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

/**
 * Bean Validation tests for FeedbackInput (English v1 input) with the real Hibernate Validator.
 */
class FeedbackInputValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validInputHasNoViolations() {
        assertTrue(VALIDATOR.validate(new FeedbackInput("Good service", 7)).isEmpty());
    }

    @Test
    void blankDescriptionIsRejected() {
        assertEquals(Set.of("Descrição não pode estar vazia"), messages(new FeedbackInput("   ", 7)));
    }

    @Test
    void ratingAboveTenIsRejected() {
        assertEquals(Set.of("Nota deve ser no máximo 10"), messages(new FeedbackInput("Good service", 11)));
    }

    @Test
    void negativeRatingIsRejected() {
        assertEquals(Set.of("Nota deve ser no mínimo 0"), messages(new FeedbackInput("Good service", -1)));
    }

    private static Set<String> messages(FeedbackInput input) {
        return VALIDATOR.validate(input).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
