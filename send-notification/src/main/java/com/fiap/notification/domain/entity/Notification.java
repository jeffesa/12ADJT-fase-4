package com.fiap.notification.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

/**
 * Domain entity representing a notification to be sent to an administrator.
 * 
 * This class is immutable after creation and follows the pure POJO pattern
 * according to Clean Architecture principles, without framework annotations.
 * 
 * The id and createdAt fields are auto-generated during construction.
 * All fields are final and accessible only through getters (no setters).
 * 
 * Validation is performed in the constructor to ensure data integrity:
 * - description must not be null or empty
 * - urgency must be one of: HIGH, MEDIUM, LOW
 * - adminEmail must not be null or empty
 */
public class Notification {
    private final UUID id;
    private final UUID feedbackId;
    private final String description;
    private final String urgency;
    private final String adminEmail;
    private final LocalDateTime createdAt;

    /**
     * Full constructor for creating a Notification with all fields specified.
     * 
     * This constructor is used primarily for internal and test purposes.
     * 
     * @param id the unique identifier for the notification
     * @param feedbackId the identifier of the associated feedback
     * @param description the notification description
     * @param urgency the urgency level (HIGH, MEDIUM, or LOW)
     * @param adminEmail the email address of the recipient administrator
     * @param createdAt the date and time when the notification was created
     * @throws IllegalArgumentException if any field fails validation
     */
    public Notification(
            UUID id,
            UUID feedbackId,
            String description,
            String urgency,
            String adminEmail,
            LocalDateTime createdAt) {
        
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.feedbackId = Objects.requireNonNull(feedbackId, "feedbackId must not be null");
        this.description = validateDescription(description);
        this.urgency = validateUrgency(urgency);
        this.adminEmail = validateAdminEmail(adminEmail);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Convenience constructor that automatically generates id and createdAt.
     * 
     * The id is generated using UUID.randomUUID() and createdAt is set to LocalDateTime.now().
     * This constructor is the primary way to create new Notification instances.
     * 
     * @param feedbackId the identifier of the associated feedback
     * @param description the notification description
     * @param urgency the urgency level (HIGH, MEDIUM, or LOW)
     * @param adminEmail the email address of the recipient administrator
     * @throws IllegalArgumentException if any field fails validation
     */
    public Notification(
            UUID feedbackId,
            String description,
            String urgency,
            String adminEmail) {
        this(
            UUID.randomUUID(),
            feedbackId,
            description,
            urgency,
            adminEmail,
            LocalDateTime.now()
        );
    }

    /**
     * Validates the description field.
     * 
     * @param description the description to validate
     * @return the validated description
     * @throws IllegalArgumentException if description is null or empty
     */
    private static String validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty or null");
        }
        return description.trim();
    }

    /**
     * Validates the urgency field.
     * 
     * @param urgency the urgency level to validate
     * @return the validated urgency
     * @throws IllegalArgumentException if urgency is not one of HIGH, MEDIUM, or LOW
     */
    private static String validateUrgency(String urgency) {
        if (urgency == null || urgency.trim().isEmpty()) {
            throw new IllegalArgumentException("Urgency cannot be empty or null");
        }
        
        String upper = urgency.trim().toUpperCase();
        if (!upper.equals("HIGH") && !upper.equals("MEDIUM") && !upper.equals("LOW")) {
            throw new IllegalArgumentException(
                "Urgency must be one of HIGH, MEDIUM, or LOW, received: " + urgency
            );
        }
        return upper;
    }

    /**
     * Validates the adminEmail field.
     * 
     * @param adminEmail the email address to validate
     * @return the validated email address
     * @throws IllegalArgumentException if adminEmail is null or empty
     */
    private static String validateAdminEmail(String adminEmail) {
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin email cannot be empty or null");
        }
        return adminEmail.trim();
    }

    // Getters (no setters — immutability)

    /**
     * Returns the unique identifier of this notification.
     * 
     * @return the notification ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the identifier of the associated feedback.
     * 
     * @return the feedback ID
     */
    public UUID getFeedbackId() {
        return feedbackId;
    }

    /**
     * Returns the description of this notification.
     * 
     * @return the notification description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the urgency level of this notification.
     * 
     * @return the urgency level (HIGH, MEDIUM, or LOW)
     */
    public String getUrgency() {
        return urgency;
    }

    /**
     * Returns the email address of the recipient administrator.
     * 
     * @return the admin email address
     */
    public String getAdminEmail() {
        return adminEmail;
    }

    /**
     * Returns the date and time when this notification was created.
     * 
     * @return the creation date and time
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", feedbackId=" + feedbackId +
                ", description='" + description + '\'' +
                ", urgency='" + urgency + '\'' +
                ", adminEmail='" + adminEmail + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(feedbackId, that.feedbackId) &&
               Objects.equals(description, that.description) &&
               Objects.equals(urgency, that.urgency) &&
               Objects.equals(adminEmail, that.adminEmail) &&
               Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, feedbackId, description, urgency, adminEmail, createdAt);
    }
}
