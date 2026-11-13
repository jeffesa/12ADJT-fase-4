package com.fiap.feedback.infra.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;

/**
 * Unit tests for FeedbackMessageBuilder.
 */
class FeedbackMessageBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildFeedbackJsonContainsAllFeedbackFields() throws Exception {
        Feedback feedback = new Feedback("feedback-1", "Terrible experience", 2,
                Urgency.HIGH, LocalDateTime.of(2024, 3, 15, 10, 30), false);

        JsonNode json = objectMapper.readTree(FeedbackMessageBuilder.buildFeedbackJson(feedback));

        assertEquals("feedback-1", json.get("id").asText());
        assertEquals("Terrible experience", json.get("description").asText());
        assertEquals(2, json.get("rating").asInt());
        assertEquals("HIGH", json.get("urgency").asText());
        assertEquals("2024-03-15T10:30", json.get("createdAt").asText());
        assertEquals(5, json.size());
    }
}
