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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.infra.dto.AvaliacaoInput;
import com.fiap.feedback.infra.dto.FeedbackInput;
import com.fiap.feedback.infra.dto.FeedbackOutput;
import com.fiap.feedback.infra.dto.InputOutputMapper;
import com.fiap.feedback.infra.security.BearerTokenAuthenticator;

/**
 * AWS Lambda handler for REST API: POST /v1/avaliacao.
 *
 * Requires "Authorization: Bearer <token>" (token from the API_TOKEN environment variable).
 * Without a valid token the request is rejected with 401 before any other processing.
 * 
 * POST /v1/avaliacao → Create evaluation (ReceiveFeedbackUseCase)
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
    private Validator validator;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {
        
        String method = event.getHttpMethod();
        LOGGER.log(Level.INFO, "Handler Lambda invocado - " + method + " /v1/avaliacao");
        
        try {
            var denied = authenticator.authenticate(event);
            if (denied.isPresent()) {
                return denied.get();
            }

            if ("POST".equals(method)) {
                return handlePostAvaliacao(event);
            } else {
                return methodNotAllowed();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro: " + e.getMessage(), e);
            return internalServerError();
        }
    }

    void setAuthenticator(BearerTokenAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Handles POST /v1/avaliacao: create feedback from the Portuguese body (descricao, nota).
     */
    private APIGatewayProxyResponseEvent handlePostAvaliacao(APIGatewayProxyRequestEvent event) {
        LOGGER.log(Level.INFO, "POST /v1/avaliacao - criando feedback");
        
        if (receiveFeedbackUseCase == null) {
            throw new IllegalStateException("ReceiveFeedbackUseCase não injetado");
        }
        if (validator == null) {
            throw new IllegalStateException("Validador não injetado");
        }
        
        String body = event.getBody();
        if (body == null || body.isEmpty()) {
            return badRequest("O corpo da requisição não pode estar vazio");
        }
        
        FeedbackInput input;
        try {
            input = toFeedbackInput(OBJECT_MAPPER.readValue(body, AvaliacaoInput.class));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, "Corpo da requisição inválido: " + e.getOriginalMessage());
            return badRequest("O corpo da requisição não é JSON válido ou possui campos com tipo incorreto");
        }
        if (input == null) {
            return badRequest("O corpo da requisição não pode estar vazio");
        }
        if (input.rating() == null) {
            return badRequest("Nota é obrigatória");
        }
        LOGGER.log(Level.FINE, "Requisição interpretada");
        
        var violations = validator.validate(input);
        if (!violations.isEmpty()) {
            return handleValidationViolations(violations);
        }
        
        Feedback feedback = receiveFeedbackUseCase.receiveFeedback(
            input.description(),
            input.rating()
        );
        
        FeedbackOutput output = InputOutputMapper.toFeedbackOutput(feedback);
        
        LOGGER.log(Level.INFO, "Feedback criado: " + output.id());
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(201);
        response.setBody(toAvaliacaoJson(output).toString());
        response.setHeaders(defaultHeaders());
        return response;
    }

    /**
     * Maps the Portuguese body of POST /v1/avaliacao to the input used by the use case.
     */
    private static FeedbackInput toFeedbackInput(AvaliacaoInput input) {
        if (input == null) {
            return null;
        }
        return new FeedbackInput(input.descricao(), input.nota());
    }

    /**
     * Builds the Portuguese JSON object returned by POST /v1/avaliacao.
     */
    private static JsonObject toAvaliacaoJson(FeedbackOutput output) {
        return Json.createObjectBuilder()
            .add("id", output.id().toString())
            .add("descricao", output.description())
            .add("nota", output.rating())
            .add("urgencia", output.urgency())
            .add("dataCriacao", output.createdAt().toString())
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
        
        LOGGER.log(Level.WARNING, "Violação de restrição: " + detail);
        return badRequest(detail.toString());
    }

    /**
     * Creates a 400 Bad Request response.
     */
    private APIGatewayProxyResponseEvent badRequest(String detail) {
        JsonObject problemDetail = Json.createObjectBuilder()
            .add("title", "Erro de validação")
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
            .add("title", "Método não permitido")
            .add("status", 405)
            .add("detail", "Este endpoint aceita apenas os métodos GET e POST")
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
            .add("title", "Erro interno do servidor")
            .add("status", 500)
            .add("detail", "Ocorreu um erro inesperado. Tente novamente mais tarde.")
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
