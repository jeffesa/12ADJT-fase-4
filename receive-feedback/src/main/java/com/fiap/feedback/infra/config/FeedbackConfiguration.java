package com.fiap.feedback.infra.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

/**
 * Configuration properties for feedback business logic thresholds and validation rules.
 * 
 * This interface is mapped to the {@code feedback.*} prefix in application.yaml
 * via SmallRye Config (Quarkus native config).
 * 
 * Usage in application layer:
 * - Inject this bean into services (e.g., UrgencyCalculator, validators)
 * - Read configuration at application startup or within services
 * 
 * Example application.yaml:
 * <pre>
 * feedback:
 *   urgency:
 *     high:
 *       min: 0
 *       max: 5
 *     medium:
 *       min: 6
 *       max: 7
 *     low:
 *       min: 8
 *       max: 10
 *   validation:
 *     rating_min: 0
 *     rating_max: 10
 *     description_min_length: 5
 *     description_max_length: 1000
 * </pre>
 */
@ConfigMapping(prefix = "feedback")
public interface FeedbackConfiguration {

    /**
     * Urgency threshold configuration.
     * Defines the rating ranges that map to each urgency level (HIGH, MEDIUM, LOW).
     */
    Urgency urgency();

    /**
     * Validation rules configuration.
     * Defines the constraints for feedback rating and description fields.
     */
    Validation validation();

    /**
     * Configuration for urgency thresholds.
     */
    interface Urgency {
        /**
         * HIGH urgency rating range.
         */
        Range high();

        /**
         * MEDIUM urgency rating range.
         */
        Range medium();

        /**
         * LOW urgency rating range.
         */
        Range low();
    }

    /**
     * Min/max range for a rating threshold.
     */
    interface Range {
        /**
         * Minimum rating (inclusive) for this urgency level.
         */
        int min();

        /**
         * Maximum rating (inclusive) for this urgency level.
         */
        int max();
    }

    /**
     * Configuration for feedback validation rules.
     */
    interface Validation {
        /**
         * Minimum allowed rating value.
         */
        @WithName("rating_min")
        int ratingMin();

        /**
         * Maximum allowed rating value.
         */
        @WithName("rating_max")
        int ratingMax();

        /**
         * Minimum allowed description length.
         */
        @WithName("description_min_length")
        int descriptionMinLength();

        /**
         * Maximum allowed description length.
         */
        @WithName("description_max_length")
        int descriptionMaxLength();
    }
}


