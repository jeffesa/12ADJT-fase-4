package com.fiap.feedback.infra.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for the English (v1) feedback input.
 * 
 * Same constraints as ReceiveFeedbackInput, with English field names.
 * 
 * @param description the feedback description text; must not be blank
 * @param rating the numeric rating; must be between 0 and 10 (inclusive)
 */
public record FeedbackInput(
        @NotBlank(message = "Description cannot be empty")
        String description,

        @Min(value = 0, message = "Rating must be at least 0")
        @Max(value = 10, message = "Rating must be at most 10")
        Integer rating
) {
}
