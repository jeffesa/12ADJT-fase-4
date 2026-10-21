package com.fiap.feedback.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.domain.gateway.FeedbackGateway;
import com.fiap.feedback.domain.gateway.UrgencyPublisher;
import com.fiap.feedback.infra.config.FeedbackConfiguration;
import com.fiap.feedback.infra.config.UrgencyCalculator;

/**
 * Use case for receiving and processing feedback submissions.
 * 
 * This use case orchestrates the business logic for:
 * - Validating feedback input (description and rating)
 * - Persisting the feedback via the gateway
 * - Calculating urgency based on rating and configuration
 * - Publishing urgent feedbacks (HIGH) to SNS for administrator notification
 * 
 * The class is marked as @ApplicationScoped (CDI singleton) to ensure
 * a single instance is shared across the application.
 */
@ApplicationScoped
public class ReceiveFeedbackUseCase {

    private final FeedbackGateway feedbackGateway;
    private final UrgencyPublisher urgencyPublisher;
    private final UrgencyCalculator urgencyCalculator;
    private final FeedbackConfiguration feedbackConfig;

    /**
     * Constructor for dependency injection.
     * 
     * All dependencies are injected and stored as final fields to ensure
     * immutability and thread-safety.
     * 
     * @param feedbackGateway gateway for persisting feedbacks
     * @param urgencyPublisher publisher for urgent feedbacks
     * @param urgencyCalculator calculator for urgency levels
     * @param feedbackConfig configuration for validation and thresholds
     */
    @Inject
    public ReceiveFeedbackUseCase(
            FeedbackGateway feedbackGateway,
            UrgencyPublisher urgencyPublisher,
            UrgencyCalculator urgencyCalculator,
            FeedbackConfiguration feedbackConfig) {
        this.feedbackGateway = feedbackGateway;
        this.urgencyPublisher = urgencyPublisher;
        this.urgencyCalculator = urgencyCalculator;
        this.feedbackConfig = feedbackConfig;
    }

    /**
     * Receives a feedback submission with description and rating.
     * 
     * This method validates the input according to configured rules,
     * persists the feedback, calculates its urgency, and publishes it
     * to SNS if the urgency level is HIGH.
     * 
     * Validation rules:
     * - Description must not be empty or null
     * - Description length must be >= configured minimum
     * - Rating must be >= configured minimum
     * - Rating must be <= configured maximum
     * 
     * @param description the feedback text
     * @param rating the numeric rating (typically 0-10)
     * @return the persisted Feedback entity with generated ID, urgency, and createdAt
     * @throws IllegalArgumentException if any validation fails
     */
    public Feedback receiveFeedback(String description, int rating) throws IllegalArgumentException {
        // Validate description: not empty
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }

        // Validate description: minimum length
        int minLength = feedbackConfig.validation().descriptionMinLength();
        if (description.length() < minLength) {
            throw new IllegalArgumentException(
                "Description must be at least " + minLength + " characters"
            );
        }

        // Validate rating: minimum value
        int ratingMin = feedbackConfig.validation().ratingMin();
        if (rating < ratingMin) {
            throw new IllegalArgumentException(
                "Rating must be at least " + ratingMin
            );
        }

        // Validate rating: maximum value
        int ratingMax = feedbackConfig.validation().ratingMax();
        if (rating > ratingMax) {
            throw new IllegalArgumentException(
                "Rating must be at most " + ratingMax
            );
        }

        // Create feedback entity (auto-generates id, createdAt, urgency)
        Feedback feedback = new Feedback(description, rating);

        // Calculate urgency using config-driven thresholds
        Urgency urgency = urgencyCalculator.calculateUrgency(rating);

        // Persist feedback via gateway
        Feedback saved = feedbackGateway.save(feedback);

        // Publish to SNS only if urgency is HIGH
        if (urgency == Urgency.HIGH) {
            urgencyPublisher.publish(saved);
        }

        return saved;
    }
}
