package com.fiap.report.domain.entity;

import java.time.LocalDateTime;

/**
 * Immutable domain entity representing a feedback record retrieved from persistence.
 * 
 * This is a report-side representation of feedback data. It captures the essential
 * information needed for report aggregation: unique identifier, rating, urgency level,
 * and creation timestamp.
 * 
 * Note: This is distinct from the receive-feedback module's Feedback entity to maintain
 * Clean Architecture separation. The infrastructure layer handles mapping between
 * the two representations when reading from or writing to persistence.
 */
public class Feedback {

    private final String id;
    private final int rating;
    private final Urgency urgency;
    private final LocalDateTime createdAt;

    /**
     * Constructor for Feedback.
     * 
     * @param id unique identifier for the feedback record
     * @param rating numeric rating value (typically 0-10)
     * @param urgency the urgency level (HIGH, MEDIUM, or LOW)
     * @param createdAt timestamp when the feedback was created
     */
    public Feedback(String id, int rating, Urgency urgency, LocalDateTime createdAt) {
        this.id = id;
        this.rating = rating;
        this.urgency = urgency;
        this.createdAt = createdAt;
    }

    /**
     * Retrieves the unique identifier of the feedback.
     * 
     * @return the feedback ID
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the numeric rating of the feedback.
     * 
     * @return the rating value
     */
    public int getRating() {
        return rating;
    }

    /**
     * Retrieves the urgency level of the feedback.
     * 
     * @return the urgency (HIGH, MEDIUM, or LOW)
     */
    public Urgency getUrgency() {
        return urgency;
    }

    /**
     * Retrieves the timestamp when the feedback was created.
     * 
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "id='" + id + '\'' +
                ", rating=" + rating +
                ", urgency=" + urgency +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Feedback feedback = (Feedback) o;

        return id.equals(feedback.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
