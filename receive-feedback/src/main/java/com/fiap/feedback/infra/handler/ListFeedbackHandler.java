package com.fiap.feedback.infra.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fiap.feedback.application.usecase.ListFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;

/**
 * AWS Lambda handler for REST API: GET /feedback.
 * Returns all feedbacks as a public endpoint.
 */
@Named("listFeedback")
public class ListFeedbackHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = Logger.getLogger(ListFeedbackHandler.class.getName());

    @Inject
    private ListFeedbackUseCase listFeedbackUseCase;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {
        
        LOGGER.log(Level.INFO, "Lambda handler invoked - GET /feedback");
        
        try {
            return handleGetFeedbacks();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error: " + e.getMessage(), e);
            return internalServerError();
        }
    }

    /**
     * Handles GET /feedback: list all feedbacks.
     */
    private APIGatewayProxyResponseEvent handleGetFeedbacks() {
        LOGGER.log(Level.INFO, "GET /feedback - listing all feedbacks");
        
        if (listFeedbackUseCase == null) {
            throw new IllegalStateException("ListFeedbackUseCase not injected");
        }
        
        List<Feedback> feedbacks = listFeedbackUseCase.listAllFeedbacks();
        
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (Feedback feedback : feedbacks) {
            arrayBuilder.add(Json.createObjectBuilder()
                .add("id", feedback.getId())
                .add("descricao", feedback.getDescription())
                .add("nota", feedback.getRating())
                .add("urgencia", feedback.getUrgency().toString())
                .add("dataCriacao", feedback.getCreatedAt().toString())
                .add("deploy", "testando")
                .build());
        }
        
        JsonObject responseBody = Json.createObjectBuilder()
            .add("avaliacoes", arrayBuilder.build())
            .add("total", feedbacks.size())
            .build();
        
        LOGGER.log(Level.INFO, "Returned " + feedbacks.size() + " feedbacks");
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody(responseBody.toString());
        response.setHeaders(defaultHeaders());
        return response;
    }

    /**
     * Creates a 500 Internal Server Error response.
     */
    private APIGatewayProxyResponseEvent internalServerError() {
        JsonObject problemDetail = Json.createObjectBuilder()
            .add("title", "Internal Server Error")
            .add("status", 500)
            .add("detail", "An unexpected error occurred. Please try again later.")
            .build();
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(500);
        response.setBody(problemDetail.toString());
        response.setHeaders(defaultHeaders());
        return response;
    }

    /**
     * Returns default HTTP headers.
     */
    private Map<String, String> defaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
