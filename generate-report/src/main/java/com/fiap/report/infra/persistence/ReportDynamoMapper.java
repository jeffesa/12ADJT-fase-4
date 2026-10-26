package com.fiap.report.infra.persistence;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import com.fiap.report.domain.entity.Feedback;
import com.fiap.report.domain.entity.Urgency;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mapper utility for converting DynamoDB items to Feedback domain entities.
 *
 * This class provides one-way conversion from DynamoDB AttributeValue maps to
 * Feedback entities. It handles field extraction, type conversion, and ISO-8601
 * datetime parsing.
 *
 * DynamoDB field mappings:
 * - id (S): unique identifier
 * - feedbackId (S): original feedback identifier (retained from source)
 * - description (S): feedback text
 * - rating (N): numeric rating 0-10
 * - urgency (S): HIGH, MEDIUM, or LOW
 * - createdAt (S): ISO-8601 LocalDateTime format
 */
public class ReportDynamoMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Converts a DynamoDB item to a Feedback domain entity.
     *
     * Extracts and maps the following fields:
     * - id: unique feedback identifier
     * - rating: numeric rating
     * - urgency: urgency level (parsed from enum string)
     * - createdAt: creation timestamp (parsed from ISO-8601 format)
     *
     * @param item the DynamoDB item map with AttributeValue entries
     * @return a Feedback domain entity with extracted data
     * @throws IllegalArgumentException if required fields are missing, null, or malformed
     */
    public static Feedback toDomain(Map<String, AttributeValue> item) {
        try {
            String id = item.get("id").s();
            int rating = Integer.parseInt(item.get("rating").n());
            String urgencyStr = item.get("urgency").s();
            Urgency urgency = Urgency.valueOf(urgencyStr);
            LocalDateTime createdAt = LocalDateTime.parse(
                    item.get("createdAt").s(),
                    DATE_TIME_FORMATTER
            );

            return new Feedback(id, rating, urgency, createdAt);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(
                    "Error converting DynamoDB item to Feedback entity: missing required field", e
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Error converting DynamoDB item to Feedback entity: invalid rating format", e
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Error converting DynamoDB item to Feedback entity: invalid urgency or datetime format", e
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Error converting DynamoDB item to Feedback entity: " + e.getMessage(), e
            );
        }
    }
}
