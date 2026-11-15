package com.fiap.feedback.infra.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for the English (v1) feedback output.
 * 
 * @param id the unique feedback identifier as a UUID
 * @param description the feedback description text
 * @param rating the numeric rating (0-10)
 * @param urgency the urgency level as the enum name (HIGH, MEDIUM, LOW)
 * @param createdAt the creation timestamp
 */
public record FeedbackOutput(
        UUID id,
        String description,
        Integer rating,
        String urgency,
        LocalDateTime createdAt
) {
}
