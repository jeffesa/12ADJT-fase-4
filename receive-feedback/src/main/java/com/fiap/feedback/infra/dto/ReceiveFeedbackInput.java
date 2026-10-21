package com.fiap.feedback.infra.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for feedback input.
 * 
 * This record is used as input for both the GraphQL mutation and the Lambda handler.
 * It carries the user-provided feedback data with validation constraints applied
 * via Jakarta Bean Validation annotations.
 * 
 * @param descricao the feedback description text; must not be blank
 * @param nota the numeric rating; must be between 0 and 10 (inclusive)
 */
public record ReceiveFeedbackInput(
        @NotBlank(message = "Description cannot be empty")
        String descricao,
        
        @Min(value = 0, message = "Rating must be at least 0")
        @Max(value = 10, message = "Rating must be at most 10")
        Integer nota
) {
}
