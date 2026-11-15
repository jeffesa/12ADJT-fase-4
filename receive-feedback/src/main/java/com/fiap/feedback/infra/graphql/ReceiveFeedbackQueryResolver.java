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
import com.fiap.feedback.infra.dto.FeedbackOutput;
import com.fiap.feedback.infra.dto.InputOutputMapper;

/**
 * GraphQL API resolver for feedback queries.
 * 
 * This class exposes the query for listing all feedbacks from GraphQL clients (listFeedbacks).
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
     * Lists all feedbacks with English field names (English v1 contract).
     * 
     * @return the list of FeedbackOutput DTOs containing all feedbacks
     * @throws Exception if the use case or gateway encounters an error
     */
    @Query
    public List<FeedbackOutput> listFeedbacks() {
        LOGGER.log(Level.INFO, "Consulta GraphQL listFeedbacks chamada");
        
        try {
            List<Feedback> feedbacks = receiveFeedbackUseCase.listAllFeedbacks();
            
            List<FeedbackOutput> output = feedbacks.stream()
                .map(InputOutputMapper::toFeedbackOutput)
                .toList();
            
            LOGGER.log(Level.INFO, "Recuperados " + output.size() + " feedbacks");
            return output;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro inesperado na consulta listFeedbacks", e);
            throw e;
        }
    }
}
