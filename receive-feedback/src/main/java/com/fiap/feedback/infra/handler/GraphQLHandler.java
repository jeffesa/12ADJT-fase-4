package com.fiap.feedback.infra.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.infra.dto.InputOutputMapper;
import com.fiap.feedback.infra.dto.ReceiveFeedbackOutput;

/**
 * AWS Lambda handler for GraphQL API: POST /graphql (internal, requires authentication).
 * 
 * This handler is INTERNAL ONLY and requires API Key authentication via header.
 * It supports GraphQL queries for listing feedbacks.
 * 
 * Authentication: X-API-Key header must match environment variable GRAPHQL_API_KEY
 * 
 * This is a Quarkus CDI bean (named "graphqlHandler") that Lambda invokes through
 * QuarkusStreamHandler. The Quarkus entry point (QuarkusStreamHandler::handleRequest)
 * ensures Arc CDI is fully initialized before delegating to this handler.
 * 
 * Request: POST /graphql with X-API-Key header and {"query": "query { listarAvaliacoes { ... } }"}
 * 
 * Response codes:
 * - 200 OK: GraphQL query executed successfully
 * - 401 Unauthorized: missing or invalid API key
 * - 400 Bad Request: invalid GraphQL query
 * - 500 Internal Server Error: unexpected server error
 */
@Named("graphqlHandler")
public class GraphQLHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = Logger.getLogger(GraphQLHandler.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String EXPECTED_API_KEY = System.getenv().getOrDefault("GRAPHQL_API_KEY", "dev-key-change-in-prod");
    
    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Inject
    private ReceiveFeedbackUseCase receiveFeedbackUseCase;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {
        
        LOGGER.log(Level.INFO, "Lambda handler invoked - POST /graphql (internal GraphQL)");
        LOGGER.log(Level.FINE, "Request method: " + event.getHttpMethod() + ", path: " + event.getPath());
        
        try {
            // Authentication check
            Map<String, String> headers = event.getHeaders();
            if (headers == null || headers.isEmpty()) {
                LOGGER.log(Level.WARNING, "No headers received");
                return unauthorized("Missing X-API-Key header");
            }
            
            // API Gateway sends headers in lowercase
            String providedKey = headers.get("x-api-key");
            if (providedKey == null) {
                providedKey = headers.get("X-API-Key");
            }
            
            if (providedKey == null) {
                LOGGER.log(Level.WARNING, "Missing x-api-key header. Headers: " + headers.keySet());
                return unauthorized("Missing X-API-Key header");
            }
            
            if (!providedKey.equals(EXPECTED_API_KEY)) {
                LOGGER.log(Level.WARNING, "Invalid API key provided: " + providedKey);
                return unauthorized("Invalid API key");
            }
            
            if (receiveFeedbackUseCase == null) {
                throw new IllegalStateException("ReceiveFeedbackUseCase not injected");
            }
            
            String body = event.getBody();
            if (body == null || body.isEmpty()) {
                return badRequest("Request body cannot be empty");
            }
            
            // Parse GraphQL request
            @SuppressWarnings("unchecked")
            Map<String, Object> graphqlRequest = OBJECT_MAPPER.readValue(body, Map.class);
            String query = (String) graphqlRequest.get("query");
            
            if (query == null || query.isEmpty()) {
                return badRequest("GraphQL query cannot be empty");
            }
            
            LOGGER.log(Level.INFO, "GraphQL query received: " + query.substring(0, Math.min(100, query.length())));
            
            // For now, we only support listarAvaliacoes query
            if (!query.toLowerCase().contains("listaravali")) {
                return badRequest("Only listarAvaliacoes query is currently supported");
            }
            
            // Call use case to list all feedbacks
            List<Feedback> feedbacks = receiveFeedbackUseCase.listarTodos();
            
            List<ReceiveFeedbackOutput> outputList = feedbacks.stream()
                .map(InputOutputMapper::toOutput)
                .toList();
            
            // Build GraphQL response
            var avaliacoesArray = outputList.stream()
                .map(output -> Json.createObjectBuilder()
                    .add("id", output.id().toString())
                    .add("descricao", output.descricao())
                    .add("nota", output.nota())
                    .add("urgencia", output.urgencia())
                    .add("dataCriacao", output.dataCriacao().toString())
                    .build())
                .toList();
            
            JsonObject responseBody = Json.createObjectBuilder()
                .add("data", Json.createObjectBuilder()
                    .add("listarAvaliacoes", Json.createArrayBuilder(avaliacoesArray).build())
                    .build())
                .build();
            
            LOGGER.log(Level.INFO, "GraphQL query executed successfully, returned " + outputList.size() + " feedbacks");
            
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setBody(responseBody.toString());
            response.setHeaders(defaultHeaders());
            return response;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in GraphQL handler: " + e.getMessage(), e);
            e.printStackTrace();
            return internalServerError();
        }
    }

    /**
     * Creates a 401 Unauthorized response.
     */
    private APIGatewayProxyResponseEvent unauthorized(String detail) {
        JsonObject errorDetail = Json.createObjectBuilder()
            .add("title", "Unauthorized")
            .add("status", 401)
            .add("detail", detail)
            .build();
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(401);
        response.setBody(errorDetail.toString());
        response.setHeaders(defaultHeaders());
        return response;
    }

    /**
     * Creates a 400 Bad Request response.
     */
    private APIGatewayProxyResponseEvent badRequest(String detail) {
        JsonObject errorDetail = Json.createObjectBuilder()
            .add("title", "Bad Request")
            .add("status", 400)
            .add("detail", detail)
            .build();
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(400);
        response.setBody(errorDetail.toString());
        response.setHeaders(defaultHeaders());
        return response;
    }

    /**
     * Creates a 500 Internal Server Error response.
     */
    private APIGatewayProxyResponseEvent internalServerError() {
        JsonObject errorDetail = Json.createObjectBuilder()
            .add("title", "Internal Server Error")
            .add("status", 500)
            .add("detail", "An unexpected error occurred. Please try again later.")
            .build();
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(500);
        response.setBody(errorDetail.toString());
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
