package com.fiap.feedback.domain.entity;

/**
 * Enum representing the urgency levels of a feedback.
 * 
 * Urgency is calculated automatically based on the assigned rating:
 * - HIGH: rating 0 to 5 (very negative feedback)
 * - MEDIUM: rating 6 to 7 (moderate feedback)
 * - LOW: rating 8 to 10 (positive feedback)
 */
public enum Urgency {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    private final String description;

    Urgency(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Calculates the urgency level based on the provided rating.
     * 
     * @param rating value from 0 to 10
     * @return Urgency corresponding to the rating
     * @throws IllegalArgumentException if the rating is invalid (< 0 or > 10)
     */
    public static Urgency fromRating(int rating) {
        if (rating < 0 || rating > 10) {
            throw new IllegalArgumentException("Rating must be between 0 and 10, received: " + rating);
        }
        
        if (rating <= 5) {
            return HIGH;
        } else if (rating <= 7) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }
}
