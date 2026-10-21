package com.fiap.feedback.infra.graphql;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.infra.dto.InputOutputMapper;
import com.fiap.feedback.infra.dto.ReceiveFeedbackInput;
import com.fiap.feedback.infra.dto.ReceiveFeedbackOutput;

/**
 * GraphQL API resolver for feedback operations.
 * 
 * This class exposes a single mutation for receiving feedback from GraphQL clients.
 * It orchestrates the request flow: validation (via Bean Validation), use case execution,
 * and response mapping. Exceptions are propagated to the global exception handler.
 */
@GraphQLApi
@ApplicationScoped
public class ReceiveFeedbackResolver {

    private static final Logger LOGGER = Logger.getLogger(ReceiveFeedbackResolver.class.getName());

    private final ReceiveFeedbackUseCase receiveFeedbackUseCase;

    /**
     * Constructor for dependency injection.
     * 
     * @param receiveFeedbackUseCase the use case for processing feedback
     */
    @Inject
    public ReceiveFeedbackResolver(ReceiveFeedbackUseCase receiveFeedbackUseCase) {
        this.receiveFeedbackUseCase = receiveFeedbackUseCase;
    }

    /**
     * Receives and processes feedback via GraphQL mutation.
     * 
     * The input is validated by Bean Validation before this method is called.
     * If validation passes, the use case is invoked to process the feedback.
     * The resulting feedback entity is mapped to a DTO for the response.
     * 
     * @param input the feedback input with description and rating
     * @return the output DTO containing the persisted feedback with generated ID and metadata
     * @throws IllegalArgumentException if the use case validation fails
     * @throws ConstraintViolationException if Bean Validation fails on the input
     */
    @Mutation
    public ReceiveFeedbackOutput receiveFeedback(ReceiveFeedbackInput input) {
        LOGGER.log(Level.INFO, "GraphQL mutation receiveFeedback called");
        
        try {
            Feedback feedback = receiveFeedbackUseCase.receiveFeedback(
                input.descricao(),
                input.nota()
            );
            
            ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);
            LOGGER.log(Level.INFO, "Feedback processed successfully: " + output.id());
            return output;
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Validation error in receiveFeedback: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in receiveFeedback", e);
            throw e;
        }
    }
}
