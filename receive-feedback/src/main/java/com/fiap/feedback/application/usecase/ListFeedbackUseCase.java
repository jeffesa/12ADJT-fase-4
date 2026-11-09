package com.fiap.feedback.application.usecase;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.gateway.FeedbackGateway;

/**
 * Use case: List all feedbacks from the gateway.
 * 
 * This use case retrieves all feedbacks from DynamoDB without any filters.
 * No authentication is required for this operation (public endpoint).
 */
@ApplicationScoped
public class ListFeedbackUseCase {

    @Inject
    private FeedbackGateway feedbackGateway;

    /**
     * Lists all feedbacks from the gateway.
     * 
     * @return List of all Feedback entities, or empty list if none exist
     */
    public List<Feedback> listAllFeedbacks() {
        return feedbackGateway.findAll();
    }
}
