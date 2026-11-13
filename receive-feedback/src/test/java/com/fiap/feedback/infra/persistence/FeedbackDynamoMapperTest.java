package com.fiap.feedback.infra.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;

/**
 * Unit tests for FeedbackDynamoMapper.
 */
class FeedbackDynamoMapperTest {

    @Test
    void toAttributeMapWritesEachFieldWithDynamoType() {
        Feedback feedback = new Feedback("feedback-1", "Good service", 7,
                Urgency.MEDIUM, LocalDateTime.of(2024, 3, 15, 10, 30), true);

        Map<String, AttributeValue> item = FeedbackDynamoMapper.toAttributeMap(feedback);

        assertEquals(6, item.size());
        assertEquals("feedback-1", item.get("id").s());
        assertEquals("Good service", item.get("description").s());
        assertEquals("7", item.get("rating").n());
        assertEquals("MEDIUM", item.get("urgency").s());
        assertEquals("2024-03-15T10:30:00", item.get("createdAt").s());
        assertTrue(item.get("notificationSent").bool());
    }

    @Test
    void toDomainRebuildsFeedbackFromItem() {
        Map<String, AttributeValue> item = validItem();

        Feedback feedback = FeedbackDynamoMapper.toDomain(item);

        assertEquals("feedback-1", feedback.getId());
        assertEquals("Terrible experience", feedback.getDescription());
        assertEquals(2, feedback.getRating());
        assertEquals(Urgency.HIGH, feedback.getUrgency());
        assertEquals(LocalDateTime.of(2024, 3, 15, 10, 30), feedback.getCreatedAt());
        assertFalse(feedback.isNotificationSent());
    }

    @Test
    void toDomainRejectsItemMissingField() {
        Map<String, AttributeValue> item = validItem();
        item.remove("rating");

        assertThrows(IllegalArgumentException.class, () -> FeedbackDynamoMapper.toDomain(item));
    }

    @Test
    void toDomainRejectsUnknownUrgency() {
        Map<String, AttributeValue> item = validItem();
        item.put("urgency", AttributeValue.builder().s("CRITICAL").build());

        assertThrows(IllegalArgumentException.class, () -> FeedbackDynamoMapper.toDomain(item));
    }

    private static Map<String, AttributeValue> validItem() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("feedback-1").build());
        item.put("description", AttributeValue.builder().s("Terrible experience").build());
        item.put("rating", AttributeValue.builder().n("2").build());
        item.put("urgency", AttributeValue.builder().s("HIGH").build());
        item.put("createdAt", AttributeValue.builder().s("2024-03-15T10:30:00").build());
        item.put("notificationSent", AttributeValue.builder().bool(false).build());
        return item;
    }
}
