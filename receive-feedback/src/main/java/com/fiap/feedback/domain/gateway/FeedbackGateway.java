package com.fiap.feedback.domain.gateway;

import com.fiap.feedback.domain.entity.Feedback;
import java.time.LocalDate;
import java.util.List;

/**
 * Interface that defines the contract for feedback persistence and retrieval.
 * 
 * This interface is part of the domain layer (Clean Architecture) and defines
 * the operations that the infrastructure layer must implement (e.g., DynamoDB).
 * 
 * Responsibilities:
 * - Save feedbacks
 * - Retrieve all feedbacks
 * - Filter feedbacks by period
 */
public interface FeedbackGateway {

    /**
     * Saves a new feedback or updates an existing one.
     * 
     * @param feedback feedback entity to be persisted
     * @return the persisted feedback (may contain adjustments like generated ID)
     */
    Feedback save(Feedback feedback);

    /**
     * Retrieves all registered feedbacks.
     * 
     * @return list containing all feedbacks
     */
    List<Feedback> findAll();

    /**
     * Retrieves feedbacks within a specific period.
     * 
     * @param startDate start date of the period (inclusive)
     * @param endDate end date of the period (inclusive)
     * @return list of feedbacks created in the specified period
     */
    List<Feedback> findByPeriod(LocalDate startDate, LocalDate endDate);
}
