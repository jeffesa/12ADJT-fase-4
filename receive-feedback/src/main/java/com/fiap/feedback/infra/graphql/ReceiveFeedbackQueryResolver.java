package com.fiap.feedback.infra.graphql;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.infra.dto.InputOutputMapper;
import com.fiap.feedback.infra.dto.ReceiveFeedbackOutput;

/**
 * GraphQL API resolver for feedback queries.
 * 
 * This class exposes a single query for listing all feedbacks from GraphQL clients.
 * It orchestrates the request flow: use case execution, response mapping,
 * and error handling. Exceptions are propagated to the global exception handler.
 */
@GraphQLApi
@ApplicationScoped
public class ReceiveFeedbackQueryResolver {

    private static final Logger LOGGER = Logger.getLogger(ReceiveFeedbackQueryResolver.class.getName());

    private final ReceiveFeedbackUseCase receiveFeedbackUseCase;

    /**
     * Constructor for dependency injection.
     * 
     * @param receiveFeedbackUseCase the use case for retrieving feedbacks
     */
    @Inject
    public ReceiveFeedbackQueryResolver(ReceiveFeedbackUseCase receiveFeedbackUseCase) {
        this.receiveFeedbackUseCase = receiveFeedbackUseCase;
    }

    /**
     * Lists all feedbacks via GraphQL query.
     * 
     * The query invokes the use case to retrieve all feedbacks from the gateway.
     * Each Feedback entity is mapped to a DTO for the response.
     * Returns an empty list if no feedbacks are found.
     * 
     * @return the list of ReceiveFeedbackOutput DTOs containing all feedbacks
     * @throws Exception if the use case or gateway encounters an error
     */
    @Query
    public List<ReceiveFeedbackOutput> listarAvaliacoes() {
        LOGGER.log(Level.INFO, "GraphQL query listarAvaliacoes called");
        
        try {
            List<Feedback> feedbacks = receiveFeedbackUseCase.listarTodos();
            
            List<ReceiveFeedbackOutput> output = feedbacks.stream()
                .map(InputOutputMapper::toOutput)
                .toList();
            
            LOGGER.log(Level.INFO, "Retrieved " + output.size() + " feedbacks");
            return output;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in listarAvaliacoes", e);
            throw e;
        }
    }
}
