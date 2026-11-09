package com.fiap.feedback.infra.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.infra.dto.InputOutputMapper;
import com.fiap.feedback.infra.dto.ReceiveFeedbackInput;
import com.fiap.feedback.infra.dto.ReceiveFeedbackOutput;

/**
 * AWS Lambda handler for REST API: POST /avaliacao (create feedback only).
 * 
 * This is a Quarkus CDI bean (named "receiveFeedback") that Lambda invokes through
 * QuarkusStreamHandler. The Quarkus entry point (QuarkusStreamHandler::handleRequest)
 * ensures Arc CDI is fully initialized before delegating to this handler.
 * 
 * All @Inject fields are guaranteed to be available at invocation time.
 * 
 * Request: POST /avaliacao with {"descricao": "...", "nota": N}
 * Response codes:
 * - 201 Created: feedback successfully persisted
 * - 400 Bad Request: validation failed
 * - 500 Internal Server Error: unexpected server error
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
    private Validator validator;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {
        
        LOGGER.log(Level.INFO, "Lambda handler invoked - POST /avaliacao (create feedback)");
        LOGGER.log(Level.FINE, "Request method: " + event.getHttpMethod() + ", path: " + event.getPath());
        
        try {
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
            
            ReceiveFeedbackInput input = OBJECT_MAPPER.readValue(body, ReceiveFeedbackInput.class);
            LOGGER.log(Level.FINE, "Request parsed");
            
            // Validate input
            var violations = validator.validate(input);
            if (!violations.isEmpty()) {
                return handleValidationViolations(violations);
            }
            
            // Call use case
            Feedback feedback = receiveFeedbackUseCase.receiveFeedback(
                input.descricao(),
                input.nota()
            );
            
            // Map and return response
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
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error: " + e.getMessage(), e);
            e.printStackTrace();
            return internalServerError();
        }
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
