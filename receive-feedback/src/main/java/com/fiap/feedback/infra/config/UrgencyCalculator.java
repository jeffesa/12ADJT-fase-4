package com.fiap.feedback.infra.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fiap.feedback.domain.entity.Urgency;

/**
 * Service for calculating feedback urgency levels based on configured thresholds.
 * 
 * This is a stateless, CDI-managed bean that delegates urgency calculation to 
 * configuration-driven thresholds rather than hardcoded logic.
 * 
 * Usage:
 * - Inject this service into application layer use cases
 * - Call calculateUrgency(rating) to get the urgency level based on config
 * 
 * Separation of concerns:
 * - Domain layer (Urgency enum): pure POJO with hardcoded fallback logic
 * - Application layer: uses UrgencyCalculator for config-driven thresholds
 * - Infrastructure layer: FeedbackConfiguration provides the threshold values
 */
@ApplicationScoped
public class UrgencyCalculator {

    private final FeedbackConfiguration config;

    /**
     * Constructor for dependency injection.
     * 
     * @param config the injected feedback configuration
     */
    @Inject
    public UrgencyCalculator(FeedbackConfiguration config) {
        this.config = config;
    }

    /**
     * Calculates the urgency level based on the provided rating and configured thresholds.
     * 
     * The calculation uses the min/max ranges defined in the configuration:
     * - If rating <= high.max → HIGH
     * - Else if rating <= medium.max → MEDIUM
     * - Else → LOW
     * 
     * @param rating the feedback rating (0 to 10)
     * @return the calculated urgency level
     * @throws IllegalArgumentException if the rating is outside the valid range (0-10)
     */
    public Urgency calculateUrgency(int rating) {
        FeedbackConfiguration.Validation validation = config.validation();
        
        if (rating < validation.ratingMin() || rating > validation.ratingMax()) {
            throw new IllegalArgumentException(
                "Rating must be between " + validation.ratingMin() + " and " + validation.ratingMax() 
                + ", received: " + rating
            );
        }

        FeedbackConfiguration.Urgency urgencyThresholds = config.urgency();

        // Apply configured thresholds: HIGH → MEDIUM → LOW
        if (rating <= urgencyThresholds.high().max()) {
            return Urgency.HIGH;
        } else if (rating <= urgencyThresholds.medium().max()) {
            return Urgency.MEDIUM;
        } else {
            return Urgency.LOW;
        }
    }
}
