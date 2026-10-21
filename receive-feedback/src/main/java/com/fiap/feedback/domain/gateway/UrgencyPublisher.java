package com.fiap.feedback.domain.gateway;

import com.fiap.feedback.domain.entity.Feedback;

/**
 * Interface that defines the contract for publishing urgent feedbacks.
 * 
 * This interface is part of the domain layer (Clean Architecture) and defines
 * how feedbacks with high urgency should be published for administrator notification.
 * The implementation uses AWS SNS in the infrastructure layer.
 * 
 * Responsibilities:
 * - Publish urgent feedbacks (HIGH) to an SNS topic
 * - Ensure that only relevant feedbacks are published
 */
public interface UrgencyPublisher {

    /**
     * Publishes an urgent feedback (HIGH urgency) for notification.
     * 
     * The implementation should verify if the feedback has HIGH urgency
     * and publish it to an SNS topic so administrators are notified.
     * 
     * @param feedback feedback entity to be published
     */
    void publish(Feedback feedback);
}
