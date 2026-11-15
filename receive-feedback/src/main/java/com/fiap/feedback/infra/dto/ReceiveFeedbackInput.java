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
 * The components use the English names "description" and "rating" in JSON
 * and in the GraphQL schema through the annotations below.
 * 
 * @param description the feedback description text; must not be blank (JSON and GraphQL name: description)
 * @param rating the numeric rating; must be between 0 and 10 (inclusive) (JSON and GraphQL name: rating)
 */
public record ReceiveFeedbackInput(
        @NotBlank(message = "Descrição não pode estar vazia")
        @Name("description")
        @JsonProperty("description")
        @JsonbProperty("description")
        String description,
        @Min(value = 0, message = "Nota deve ser no mínimo 0")
        @Max(value = 10, message = "Nota deve ser no máximo 10")
        @Name("rating")
        @JsonProperty("rating")
        @JsonbProperty("rating")
        Integer rating
) {
}
