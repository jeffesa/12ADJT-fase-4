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
import com.fiap.feedback.infra.dto.FeedbackOutput;
import com.fiap.feedback.infra.dto.InputOutputMapper;
import com.fiap.feedback.infra.dto.ReceiveFeedbackOutput;
import com.fiap.feedback.infra.security.BearerTokenAuthenticator;

/**
 * AWS Lambda handler for GraphQL API: POST /graphql (requires Bearer authentication).
 * 
 * Requires Bearer token authentication via header.
 * It supports GraphQL queries for listing feedbacks.
 * 
 * Authentication: "Authorization: Bearer <token>" must match environment variable API_TOKEN
 * 
 * This is a Quarkus CDI bean (named "graphqlHandler") that Lambda invokes through
 * QuarkusStreamHandler. The Quarkus entry point (QuarkusStreamHandler::handleRequest)
 * ensures Arc CDI is fully initialized before delegating to this handler.
 * 
 * Request: POST /graphql with Authorization: Bearer header and {"query": "query { listarAvaliacoes { ... } }"}
 * 
 * Response codes:
 * - 200 OK: GraphQL query executed successfully
 * - 401 Unauthorized: missing or invalid bearer token
 * - 400 Bad Request: invalid GraphQL query
 * - 500 Internal Server Error: unexpected server error
 */
@Named("graphqlHandler")
public class GraphQLHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = Logger.getLogger(GraphQLHandler.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private BearerTokenAuthenticator authenticator = BearerTokenAuthenticator.fromEnvironment();

    @Inject
    private ReceiveFeedbackUseCase receiveFeedbackUseCase;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {
        
        LOGGER.log(Level.INFO, "Lambda handler invoked - POST /graphql (internal GraphQL)");
        LOGGER.log(Level.FINE, "Request method: " + event.getHttpMethod() + ", path: " + event.getPath());
        
        try {
            var denied = authenticator.authenticate(event);
            if (denied.isPresent()) {
                return denied.get();
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
            
            // English v1 query: listFeedbacks
            if (query.toLowerCase().contains("listfeedbacks")) {
                List<Feedback> feedbacks = receiveFeedbackUseCase.listarTodos();
                
                var feedbacksArray = feedbacks.stream()
                    .map(InputOutputMapper::toFeedbackOutput)
                    .map(GraphQLHandler::toFeedbackJson)
                    .toList();
                
                JsonObject feedbacksResponse = Json.createObjectBuilder()
                    .add("data", Json.createObjectBuilder()
                        .add("listFeedbacks", Json.createArrayBuilder(feedbacksArray).build())
                        .build())
                    .build();
                
                LOGGER.log(Level.INFO, "GraphQL query executed successfully, returned " + feedbacks.size() + " feedbacks");
                
                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(200);
                response.setBody(feedbacksResponse.toString());
                response.setHeaders(defaultHeaders());
                return response;
            }
            
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
            return internalServerError();
        }
    }

    void setAuthenticator(BearerTokenAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Builds the English JSON object for one feedback (listFeedbacks).
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
