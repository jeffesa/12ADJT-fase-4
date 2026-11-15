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
 * The components use the English names "description", "rating", "urgency" and "createdAt"
 * in JSON and in the GraphQL schema through the annotations below, the same format as FeedbackOutput.
 * 
 * @param id the unique feedback identifier as a UUID
 * @param description the feedback description text (JSON and GraphQL name: description)
 * @param rating the numeric rating (0-10) (JSON and GraphQL name: rating)
 * @param urgency the urgency level as the enum name (HIGH, MEDIUM, LOW) (JSON and GraphQL name: urgency)
 * @param createdAt the creation timestamp in ISO 8601 format (JSON and GraphQL name: createdAt)
 */
public record ReceiveFeedbackOutput(
        UUID id,
        @Name("description")
        @JsonProperty("description")
        @JsonbProperty("description")
        String description,
        @Name("rating")
        @JsonProperty("rating")
        @JsonbProperty("rating")
        Integer rating,
        @Name("urgency")
        @JsonProperty("urgency")
        @JsonbProperty("urgency")
        String urgency,
        @Name("createdAt")
        @JsonProperty("createdAt")
        @JsonbProperty("createdAt")
        LocalDateTime createdAt
) {
}
