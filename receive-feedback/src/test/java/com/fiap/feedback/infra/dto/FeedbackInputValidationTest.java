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
        assertEquals(Set.of("Description cannot be empty"), messages(new FeedbackInput("   ", 7)));
    }

    @Test
    void ratingAboveTenIsRejected() {
        assertEquals(Set.of("Rating must be at most 10"), messages(new FeedbackInput("Good service", 11)));
    }

    @Test
    void negativeRatingIsRejected() {
        assertEquals(Set.of("Rating must be at least 0"), messages(new FeedbackInput("Good service", -1)));
    }

    private static Set<String> messages(FeedbackInput input) {
        return VALIDATOR.validate(input).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
