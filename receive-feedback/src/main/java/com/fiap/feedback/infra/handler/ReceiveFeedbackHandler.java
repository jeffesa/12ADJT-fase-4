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
import com.fiap.feedback.infra.dto.FeedbackInput;
import com.fiap.feedback.infra.dto.FeedbackOutput;
import com.fiap.feedback.infra.dto.InputOutputMapper;
import com.fiap.feedback.infra.security.BearerTokenAuthenticator;

/**
 * AWS Lambda handler for REST API: /v1/feedback (routes POST and GET).
 *
 * Requires "Authorization: Bearer <token>" (token from the API_TOKEN environment variable).
 * Without a valid token the request is rejected with 401 before any other processing.
 * 
 * POST /v1/feedback → Create feedback (ReceiveFeedbackUseCase)
 * GET /v1/feedback → List all feedbacks (ListFeedbackUseCase)
 */
@Named("receiveFeedback")
public class ReceiveFeedbackHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = Logger.getLogger(ReceiveFeedbackHandler.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private BearerTokenAuthenticator authenticator = BearerTokenAuthenticator.fromEnvironment();

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
        LOGGER.log(Level.INFO, "Lambda handler invoked - " + method + " /v1/feedback");
        
        try {
            var denied = authenticator.authenticate(event);
            if (denied.isPresent()) {
                return denied.get();
            }

            if ("GET".equals(method)) {
                return handleGetFeedbacksV1();
            } else if ("POST".equals(method)) {
                return handlePostFeedbackV1(event);
            } else {
                return methodNotAllowed();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error: " + e.getMessage(), e);
            return internalServerError();
        }
    }

    void setAuthenticator(BearerTokenAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Handles GET /v1/feedback: list all feedbacks with English field names.
     */
    private APIGatewayProxyResponseEvent handleGetFeedbacksV1() {
        LOGGER.log(Level.INFO, "GET /v1/feedback - listing all feedbacks");
        
        if (listFeedbackUseCase == null) {
            throw new IllegalStateException("ListFeedbackUseCase not injected");
        }
        
        List<Feedback> feedbacks = listFeedbackUseCase.listAllFeedbacks();
        
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (Feedback feedback : feedbacks) {
            FeedbackOutput output = InputOutputMapper.toFeedbackOutput(feedback);
            arrayBuilder.add(toFeedbackJson(output));
        }
        
        JsonObject responseBody = Json.createObjectBuilder()
            .add("feedbacks", arrayBuilder.build())
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
     * Handles POST /v1/feedback: create feedback from English field names (description, rating).
     */
    private APIGatewayProxyResponseEvent handlePostFeedbackV1(APIGatewayProxyRequestEvent event) {
        LOGGER.log(Level.INFO, "POST /v1/feedback - creating feedback");
        
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
        
        FeedbackInput input;
        try {
            input = OBJECT_MAPPER.readValue(body, FeedbackInput.class);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, "Invalid request body: " + e.getOriginalMessage());
            return badRequest("Request body is not valid JSON or has fields of the wrong type");
        }
        if (input == null) {
            return badRequest("Request body cannot be empty");
        }
        if (input.rating() == null) {
            return badRequest("Rating is required");
        }
        LOGGER.log(Level.FINE, "Request parsed");
        
        var violations = validator.validate(input);
        if (!violations.isEmpty()) {
            return handleValidationViolations(violations);
        }
        
        Feedback feedback = receiveFeedbackUseCase.receiveFeedback(
            input.description(),
            input.rating()
        );
        
        FeedbackOutput output = InputOutputMapper.toFeedbackOutput(feedback);
        
        LOGGER.log(Level.INFO, "Feedback created: " + output.id());
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(201);
        response.setBody(toFeedbackJson(output).toString());
        response.setHeaders(defaultHeaders());
        return response;
    }

    /**
     * Builds the English v1 JSON object for one feedback.
     */
    private static JsonObject toFeedbackJson(FeedbackOutput output) {
        return Json.createObjectBuilder()
            .add("id", output.id().toString())
            .add("description", output.description())
            .add("rating", output.rating())
            .add("urgency", output.urgency())
            .add("createdAt", output.createdAt().toString())
            .build();
    }

    /**
     * Handles Bean Validation constraint violations.
     */
    private APIGatewayProxyResponseEvent handleValidationViolations(
            java.util.Set<? extends ConstraintViolation<?>> violations) {
        
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
