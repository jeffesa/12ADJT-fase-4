package com.fiap.report.infra.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import com.fiap.report.domain.entity.Feedback;
import com.fiap.report.domain.entity.Urgency;

/**
 * Unit tests for ReportDynamoMapper.
 */
class ReportDynamoMapperTest {

    @Test
    void toDomainMapsValidItem() {
        Feedback feedback = ReportDynamoMapper.toDomain(validItem());

        assertEquals("feedback-1", feedback.getId());
        assertEquals(8, feedback.getRating());
        assertEquals(Urgency.LOW, feedback.getUrgency());
        assertEquals(LocalDateTime.of(2024, 10, 21, 10, 30, 45), feedback.getCreatedAt());
    }

    @Test
    void toDomainRejectsItemMissingRequiredField() {
        Map<String, AttributeValue> item = validItem();
        item.remove("createdAt");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ReportDynamoMapper.toDomain(item));

        assertEquals("Error converting DynamoDB item to Feedback entity: missing required field",
                error.getMessage());
    }

    @Test
    void toDomainRejectsNonNumericRating() {
        Map<String, AttributeValue> item = validItem();
        item.put("rating", AttributeValue.builder().n("eight").build());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ReportDynamoMapper.toDomain(item));

        assertEquals("Error converting DynamoDB item to Feedback entity: invalid rating format",
                error.getMessage());
    }

    @Test
    void toDomainRejectsUnknownUrgency() {
        Map<String, AttributeValue> item = validItem();
        item.put("urgency", AttributeValue.builder().s("CRITICAL").build());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ReportDynamoMapper.toDomain(item));

        assertEquals("Error converting DynamoDB item to Feedback entity: invalid urgency or datetime format",
                error.getMessage());
    }

    @Test
    void toDomainRejectsInvalidDateTime() {
        Map<String, AttributeValue> item = validItem();
        item.put("createdAt", AttributeValue.builder().s("21/10/2024").build());

        assertThrows(IllegalArgumentException.class, () -> ReportDynamoMapper.toDomain(item));
    }

    private static Map<String, AttributeValue> validItem() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("feedback-1").build());
        item.put("rating", AttributeValue.builder().n("8").build());
        item.put("urgency", AttributeValue.builder().s("LOW").build());
        item.put("createdAt", AttributeValue.builder().s("2024-10-21T10:30:45").build());
        return item;
    }
}
