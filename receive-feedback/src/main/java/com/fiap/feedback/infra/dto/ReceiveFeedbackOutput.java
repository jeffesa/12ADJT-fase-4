package com.fiap.feedback.infra.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.bind.annotation.JsonbProperty;
import org.eclipse.microprofile.graphql.Name;

/**
 * Data Transfer Object for feedback output.
 * 
 * This record represents the response format for both the GraphQL mutation
 * and the Lambda handler. It is populated from a Feedback domain entity
 * and contains the normalized representation of the feedback data.
 * 
 * The components keep the legacy external names "descricao", "nota", "urgencia"
 * and "dataCriacao" in JSON and in the GraphQL schema through the annotations below.
 * 
 * @param id the unique feedback identifier as a UUID
 * @param description the feedback description text (JSON and GraphQL name: descricao)
 * @param rating the numeric rating (0-10) (JSON and GraphQL name: nota)
 * @param urgency the urgency level as the enum name (HIGH, MEDIUM, LOW) (JSON and GraphQL name: urgencia)
 * @param createdAt the creation timestamp in ISO 8601 format (JSON and GraphQL name: dataCriacao)
 */
public record ReceiveFeedbackOutput(
        UUID id,
        @Name("descricao")
        @JsonProperty("descricao")
        @JsonbProperty("descricao")
        String description,
        @Name("nota")
        @JsonProperty("nota")
        @JsonbProperty("nota")
        Integer rating,
        @Name("urgencia")
        @JsonProperty("urgencia")
        @JsonbProperty("urgencia")
        String urgency,
        @Name("dataCriacao")
        @JsonProperty("dataCriacao")
        @JsonbProperty("dataCriacao")
        LocalDateTime createdAt
) {
}
