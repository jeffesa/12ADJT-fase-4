package com.fiap.feedback.infra.dto;

import java.util.UUID;

import com.fiap.feedback.domain.entity.Feedback;

/**
 * Utility mapper for converting between domain entities and DTOs.
 * 
 * This class provides static methods to transform domain objects
 * into Data Transfer Objects suitable for API responses.
 */
public final class InputOutputMapper {

    private InputOutputMapper() {
        // Utility class, no instances
    }

    /**
     * Converts a Feedback domain entity to a ReceiveFeedbackOutput DTO.
     * 
     * The mapping includes:
     * - String ID is converted to UUID
     * - Description is copied as-is
     * - Rating is copied as-is
     * - Urgency enum is converted to its name (HIGH, MEDIUM, LOW)
     * - Creation timestamp is copied as-is
     * 
     * @param feedback the domain entity to convert
     * @return a new ReceiveFeedbackOutput DTO with all fields populated
     * @throws NullPointerException if feedback is null
     */
    public static ReceiveFeedbackOutput toOutput(Feedback feedback) {
        return new ReceiveFeedbackOutput(
            UUID.fromString(feedback.getId()),
            feedback.getDescription(),
            feedback.getRating(),
            feedback.getUrgency().name(),
            feedback.getCreatedAt()
        );
    }
}
