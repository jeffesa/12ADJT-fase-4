package com.fiap.feedback.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity representing student feedback.
 * 
 * This class is immutable after creation. It follows the pure POJO pattern
 * according to Clean Architecture principles, without framework annotations.
 * 
 * Domain validations:
 * - rating must be between 0 and 10
 * - description cannot be empty or null
 */
public class Feedback {
    private final String id;
    private final String description;
    private final int rating;
    private final Urgency urgency;
    private final LocalDateTime createdAt;
    private final boolean notificationSent;

    /**
     * Constructor for the Feedback entity.
     * 
     * @param id unique feedback identifier
     * @param description descriptive text of the feedback
     * @param rating rating from 0 to 10
     * @param urgency urgency level calculated automatically
     * @param createdAt date and time when feedback was created
     * @param notificationSent indicates if notification has already been sent
     * @throws IllegalArgumentException if rating is invalid or description is empty
     */
    public Feedback(
            String id,
            String description,
            int rating,
            Urgency urgency,
            LocalDateTime createdAt,
            boolean notificationSent) {
        
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty or null");
        }
        
        if (rating < 0 || rating > 10) {
            throw new IllegalArgumentException("Rating must be between 0 and 10, received: " + rating);
        }
        
        this.id = id;
        this.description = description.trim();
        this.rating = rating;
        this.urgency = urgency;
        this.createdAt = createdAt;
        this.notificationSent = notificationSent;
    }

    /**
     * Convenience constructor that automatically generates ID and creation date,
     * and calculates urgency based on the rating.
     * 
     * @param description descriptive text of the feedback
     * @param rating rating from 0 to 10
     * @throws IllegalArgumentException if rating is invalid or description is empty
     */
    public Feedback(String description, int rating) {
        this(
            UUID.randomUUID().toString(),
            description,
            rating,
            Urgency.fromRating(rating),
            LocalDateTime.now(),
            false
        );
    }

    // Getters (no setters — immutability)

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public int getRating() {
        return rating;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", rating=" + rating +
                ", urgency=" + urgency +
                ", createdAt=" + createdAt +
                ", notificationSent=" + notificationSent +
                '}';
    }
}
