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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.application.usecase.ListFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.infra.dto.InputOutputMapper;
import com.fiap.feedback.infra.dto.ReceiveFeedbackInput;
import com.fiap.feedback.infra.dto.ReceiveFeedbackOutput;

/**
 * AWS Lambda handler for REST API: /avaliacao (routes POST and GET).
 * 
 * POST /avaliacao → Create feedback (ReceiveFeedbackUseCase)
 * GET /avaliacao → List all feedbacks (ListFeedbackUseCase)
 */
@Named("receiveFeedback")
public class ReceiveFeedbackHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = Logger.getLogger(ReceiveFeedbackHandler.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Inject
    private ReceiveFeedbackUseCase receiveFeedbackUseCase;

    @Inject
    private ListFeedbackUseCase listFeedbackUseCase;

    @Inject
    private Validator validator;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {
        
        String method = event.getHttpMethod();
        LOGGER.log(Level.INFO, "Lambda handler invoked - " + method + " /avaliacao");
        
        try {
            if ("GET".equals(method)) {
                return handleGetAvaliacoes();
            } else if ("POST".equals(method)) {
                return handlePostAvaliacao(event);
            } else {
                return methodNotAllowed();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error: " + e.getMessage(), e);
            return internalServerError();
        }
    }

    /**
     * Handles GET /avaliacao: list all feedbacks.
     */
    private APIGatewayProxyResponseEvent handleGetAvaliacoes() {
        LOGGER.log(Level.INFO, "GET /avaliacao - listing all feedbacks");
        
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
     * Handles POST /avaliacao: create feedback.
     */
    private APIGatewayProxyResponseEvent handlePostAvaliacao(APIGatewayProxyRequestEvent event) {
        LOGGER.log(Level.INFO, "POST /avaliacao - creating feedback");
        
        if (receiveFeedbackUseCase == null) {
            throw new IllegalStateException("ReceiveFeedbackUseCase not injected");
        }
        if (validator == null) {
            throw new IllegalStateException("Validator not injected");
        }
        
        String body = event.getBody();
        if (body == null || body.isEmpty()) {
            return badRequest("Request body cannot be empty");
        }
        
        ReceiveFeedbackInput input;
        try {
            input = OBJECT_MAPPER.readValue(body, ReceiveFeedbackInput.class);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, "Invalid request body: " + e.getOriginalMessage());
            return badRequest("Request body is not valid JSON or has fields of the wrong type");
        }
        if (input == null) {
            return badRequest("Request body cannot be empty");
        }
        if (input.nota() == null) {
            return badRequest("Rating is required");
        }
        LOGGER.log(Level.FINE, "Request parsed");
        
        var violations = validator.validate(input);
        if (!violations.isEmpty()) {
            return handleValidationViolations(violations);
        }
        
        Feedback feedback = receiveFeedbackUseCase.receiveFeedback(
            input.descricao(),
            input.nota()
        );
        
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);
        
        JsonObject responseBody = Json.createObjectBuilder()
            .add("receiveFeedback", Json.createObjectBuilder()
                .add("id", output.id().toString())
                .add("descricao", output.descricao())
                .add("nota", output.nota())
                .add("urgencia", output.urgencia())
                .add("dataCriacao", output.dataCriacao().toString())
                .build())
            .build();
        
        LOGGER.log(Level.INFO, "Feedback created: " + output.id());
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(201);
        response.setBody(responseBody.toString());
        response.setHeaders(defaultHeaders());
        return response;
    }

    /**
     * Handles Bean Validation constraint violations.
     */
    private APIGatewayProxyResponseEvent handleValidationViolations(
            java.util.Set<ConstraintViolation<ReceiveFeedbackInput>> violations) {
        
        StringBuilder detail = new StringBuilder();
        for (ConstraintViolation<?> violation : violations) {
            if (detail.length() > 0) {
                detail.append("; ");
            }
            detail.append(violation.getMessage());
        }
        
        LOGGER.log(Level.WARNING, "Constraint violation: " + detail);
        return badRequest(detail.toString());
    }

    /**
     * Creates a 400 Bad Request response.
     */
    private APIGatewayProxyResponseEvent badRequest(String detail) {
        JsonObject problemDetail = Json.createObjectBuilder()
            .add("title", "Validation Error")
            .add("status", 400)
            .add("detail", detail)
            .build();
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(400);
        response.setBody(problemDetail.toString());
        response.setHeaders(defaultHeaders());
        return response;
    }

    /**
     * Creates a 405 Method Not Allowed response.
     */
    private APIGatewayProxyResponseEvent methodNotAllowed() {
        JsonObject problemDetail = Json.createObjectBuilder()
            .add("title", "Method Not Allowed")
            .add("status", 405)
            .add("detail", "This endpoint supports only GET and POST methods")
            .build();
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(405);
        response.setBody(problemDetail.toString());
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
