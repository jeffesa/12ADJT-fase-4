package com.fiap.feedback.infra.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;

/**
 * Unit tests for InputOutputMapper.toFeedbackOutput (English v1 mapping).
 */
class InputOutputMapperV1Test {

    @Test
    void mapsEveryFieldToEnglishOutput() {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 10, 30, 45);
        Feedback feedback = new Feedback(id, "Good service", 7, Urgency.MEDIUM, createdAt, false);

        FeedbackOutput output = InputOutputMapper.toFeedbackOutput(feedback);

        assertEquals(UUID.fromString(id), output.id());
        assertEquals("Good service", output.description());
        assertEquals(7, output.rating());
        assertEquals("MEDIUM", output.urgency());
        assertEquals(createdAt, output.createdAt());
    }

    @Test
    void urgencyIsEnumName() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals("HIGH", InputOutputMapper.toFeedbackOutput(
                new Feedback(UUID.randomUUID().toString(), "High", 1, Urgency.HIGH, now, false)).urgency());
        assertEquals("LOW", InputOutputMapper.toFeedbackOutput(
                new Feedback(UUID.randomUUID().toString(), "Low", 9, Urgency.LOW, now, false)).urgency());
    }

    @Test
    void nullFeedbackThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> InputOutputMapper.toFeedbackOutput(null));
    }
}
