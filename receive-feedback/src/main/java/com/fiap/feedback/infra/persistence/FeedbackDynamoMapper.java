package com.fiap.feedback.infra.persistence;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper utility for converting between Feedback domain entities and DynamoDB AttributeValue maps.
 * 
 * This class provides bidirectional conversion:
 * - toAttributeMap: converts a Feedback entity to a DynamoDB item (Map of AttributeValues)
 * - toDomain: converts a DynamoDB item to a Feedback entity
 * 
 * DynamoDB field mappings:
 * - id (S): unique identifier
 * - description (S): feedback text
 * - rating (N): numeric rating 0-10
 * - urgency (S): HIGH, MEDIUM, or LOW
 * - createdAt (S): ISO-8601 LocalDateTime format
 * - notificationSent (BOOL): whether notification was already sent
 */
public class FeedbackDynamoMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Converts a Feedback entity to a DynamoDB AttributeValue map.
     * 
     * @param feedback the domain entity to convert
     * @return a map of attribute names to AttributeValue objects
     */
    public static Map<String, AttributeValue> toAttributeMap(Feedback feedback) {
        Map<String, AttributeValue> item = new HashMap<>();

        // ID (String)
        item.put("id", AttributeValue.builder()
                .s(feedback.getId())
                .build());

        // Description (String)
        item.put("description", AttributeValue.builder()
                .s(feedback.getDescription())
                .build());

        // Rating (Number)
        item.put("rating", AttributeValue.builder()
                .n(String.valueOf(feedback.getRating()))
                .build());

        // Urgency (String: HIGH, MEDIUM, LOW)
        item.put("urgency", AttributeValue.builder()
                .s(feedback.getUrgency().name())
                .build());

        // Created At (String: ISO-8601 format)
        item.put("createdAt", AttributeValue.builder()
                .s(feedback.getCreatedAt().format(DATE_TIME_FORMATTER))
                .build());

        // Notification Sent (Boolean)
        item.put("notificationSent", AttributeValue.builder()
                .bool(feedback.isNotificationSent())
                .build());

        return item;
    }

    /**
     * Converts a DynamoDB item (AttributeValue map) to a Feedback entity.
     * 
     * @param item the DynamoDB item map
     * @return a Feedback domain entity
     * @throws IllegalArgumentException if required fields are missing or malformed
     */
    public static Feedback toDomain(Map<String, AttributeValue> item) {
        try {
            String id = item.get("id").s();
            String description = item.get("description").s();
            int rating = Integer.parseInt(item.get("rating").n());
            String urgencyStr = item.get("urgency").s();
            Urgency urgency = Urgency.valueOf(urgencyStr);
            LocalDateTime createdAt = LocalDateTime.parse(
                    item.get("createdAt").s(),
                    DATE_TIME_FORMATTER
            );
            boolean notificationSent = item.get("notificationSent").bool();

            return new Feedback(id, description, rating, urgency, createdAt, notificationSent);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Error converting DynamoDB item to Feedback entity: " + e.getMessage(), e
            );
        }
    }
}
