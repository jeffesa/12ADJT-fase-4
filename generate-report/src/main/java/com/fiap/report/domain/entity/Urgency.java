package com.fiap.report.domain.entity;

/**
 * Enum representing the urgency levels of feedback.
 * 
 * Urgency categories are used to classify feedbacks for reporting and analysis:
 * - HIGH: Critical feedbacks requiring immediate attention (ratings 0-5)
 * - MEDIUM: Moderate feedbacks for review (ratings 6-7)
 * - LOW: Positive feedbacks indicating satisfaction (ratings 8-10)
 */
public enum Urgency {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    private final String description;

    /**
     * Constructor for Urgency enum.
     * 
     * @param description human-readable description of the urgency level
     */
    Urgency(String description) {
        this.description = description;
    }

    /**
     * Retrieves the human-readable description of this urgency level.
     * 
     * @return the description string
     */
    public String getDescription() {
        return description;
    }
}
