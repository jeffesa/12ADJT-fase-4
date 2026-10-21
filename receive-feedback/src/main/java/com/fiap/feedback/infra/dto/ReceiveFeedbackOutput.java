package com.fiap.feedback.infra.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for feedback output.
 * 
 * This record represents the response format for both the GraphQL mutation
 * and the Lambda handler. It is populated from a Feedback domain entity
 * and contains the normalized representation of the feedback data.
 * 
 * @param id the unique feedback identifier as a UUID
 * @param descricao the feedback description text
 * @param nota the numeric rating (0-10)
 * @param urgencia the urgency level as the enum name (HIGH, MEDIUM, LOW)
 * @param dataCriacao the creation timestamp in ISO 8601 format
 */
public record ReceiveFeedbackOutput(
        UUID id,
        String descricao,
        Integer nota,
        String urgencia,
        LocalDateTime dataCriacao
) {
}
