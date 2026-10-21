package com.fiap.feedback.infra.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.infra.dto.InputOutputMapper;
import com.fiap.feedback.infra.dto.ReceiveFeedbackInput;
import com.fiap.feedback.infra.dto.ReceiveFeedbackOutput;

/**
 * AWS Lambda handler for receiving feedback via HTTP/API Gateway.
 * 
 * This handler accepts HTTP POST requests with a JSON body containing
 * feedback data, validates the input, processes it through the use case,
 * and returns an HTTP response in RFC 7807 format.
 * 
 * Response codes:
 * - 201 Created: feedback successfully persisted
 * - 400 Bad Request: validation failed or use case validation failed
 * - 500 Internal Server Error: unexpected server error
 */
@ApplicationScoped
public class ReceiveFeedbackHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = Logger.getLogger(ReceiveFeedbackHandler.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ReceiveFeedbackUseCase receiveFeedbackUseCase;
    private final Validator validator;

    /**
     * Constructor for dependency injection.
     * 
     * @param receiveFeedbackUseCase the use case for processing feedback
     * @param validator the Jakarta Bean Validator
     */
    @Inject
    public ReceiveFeedbackHandler(
            ReceiveFeedbackUseCase receiveFeedbackUseCase,
            Validator validator) {
        this.receiveFeedbackUseCase = receiveFeedbackUseCase;
        this.validator = validator;
    }

    /**
     * Handles an API Gateway event for receiving feedback.
     * 
     * Flow:
     * 1. Parse the request body as JSON into ReceiveFeedbackInput
     * 2. Validate the input using Bean Validation
     * 3. Call the use case to process the feedback
     * 4. Map the result to ReceiveFeedbackOutput
     * 5. Return 201 with the response in { "receiveFeedback": {...} } format
     * 
     * On error, returns RFC 7807 Problem Detail with appropriate status code.
     * 
     * @param event the API Gateway proxy request event
     * @param context the Lambda context
     * @return the HTTP response with status code and body
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {
        
        LOGGER.log(Level.INFO, "Lambda handler invoked for receiving feedback");
        LOGGER.log(Level.FINE, "Request method: " + event.getHttpMethod() + ", path: " + event.getPath());
        
        try {
            // Parse request body
            String body = event.getBody();
            if (body == null || body.isEmpty()) {
                return badRequest("Request body cannot be empty");
            }
            
            ReceiveFeedbackInput input = OBJECT_MAPPER.readValue(body, ReceiveFeedbackInput.class);
            LOGGER.log(Level.FINE, "Request parsed successfully");
            
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
            
            LOGGER.log(Level.INFO, "Feedback received successfully: " + output.id());
            
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(201);
            response.setBody(responseBody.toString());
            response.setHeaders(defaultHeaders());
            return response;
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Validation error: " + e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in handler", e);
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
     * Creates a 400 Bad Request response with RFC 7807 format.
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
     * Creates a 500 Internal Server Error response with RFC 7807 format.
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
     * Returns default HTTP headers for all responses.
     */
    private Map<String, String> defaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
