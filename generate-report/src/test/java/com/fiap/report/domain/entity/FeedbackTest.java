package com.fiap.report.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the report-side Feedback entity.
 */
class FeedbackTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2024, 10, 21, 10, 30);

    @Test
    void gettersReturnConstructorValues() {
        Feedback feedback = new Feedback("feedback-1", 4, Urgency.HIGH, CREATED_AT);

        assertEquals("feedback-1", feedback.getId());
        assertEquals(4, feedback.getRating());
        assertEquals(Urgency.HIGH, feedback.getUrgency());
        assertEquals(CREATED_AT, feedback.getCreatedAt());
    }

    @Test
    void toStringIncludesIdRatingUrgencyAndCreatedAt() {
        Feedback feedback = new Feedback("feedback-1", 4, Urgency.HIGH, CREATED_AT);

        String text = feedback.toString();

        assertTrue(text.contains("id='feedback-1'"));
        assertTrue(text.contains("rating=4"));
        assertTrue(text.contains("urgency=HIGH"));
        assertTrue(text.contains("createdAt=" + CREATED_AT));
    }

    @Test
    void equalsAndHashCodeUseIdOnly() {
        Feedback original = new Feedback("feedback-1", 4, Urgency.HIGH, CREATED_AT);
        Feedback sameFields = new Feedback("feedback-1", 4, Urgency.HIGH, CREATED_AT);
        Feedback sameIdOtherFields = new Feedback("feedback-1", 9, Urgency.LOW, CREATED_AT.plusDays(1));
        Feedback otherId = new Feedback("feedback-2", 4, Urgency.HIGH, CREATED_AT);

        assertEquals(original, sameFields);
        assertEquals(original, sameIdOtherFields);
        assertEquals(original.hashCode(), sameIdOtherFields.hashCode());
        assertNotEquals(original, otherId);
        assertFalse(original.equals(null));
        assertFalse(original.equals("feedback-1"));
    }
}
