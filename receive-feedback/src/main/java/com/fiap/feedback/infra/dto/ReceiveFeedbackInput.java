package com.fiap.feedback.infra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.graphql.Name;

/**
 * Data Transfer Object for feedback input.
 * 
 * This record is used as input for both the GraphQL mutation and the Lambda handler.
 * It carries the user-provided feedback data with validation constraints applied
 * via Jakarta Bean Validation annotations.
 * 
 * The components keep the legacy external names "descricao" and "nota" in JSON
 * and in the GraphQL schema through the annotations below.
 * 
 * @param description the feedback description text; must not be blank (JSON and GraphQL name: descricao)
 * @param rating the numeric rating; must be between 0 and 10 (inclusive) (JSON and GraphQL name: nota)
 */
public record ReceiveFeedbackInput(
        @NotBlank(message = "Description cannot be empty")
        @Name("descricao")
        @JsonProperty("descricao")
        @JsonbProperty("descricao")
        String description,
        @Min(value = 0, message = "Rating must be at least 0")
        @Max(value = 10, message = "Rating must be at most 10")
        @Name("nota")
        @JsonProperty("nota")
        @JsonbProperty("nota")
        Integer rating
) {
}
