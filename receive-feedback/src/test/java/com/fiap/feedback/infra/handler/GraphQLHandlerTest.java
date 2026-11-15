package com.fiap.feedback.infra.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.infra.security.BearerTokenAuthenticator;

class GraphQLHandlerTest {

    private static final String QUERY = "{\"query\":\"query { listFeedbacks { id } }\"}";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GraphQLHandler handler;
    private ReceiveFeedbackUseCase useCase;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        handler = new GraphQLHandler();
        useCase = mock(ReceiveFeedbackUseCase.class);
        Field field = GraphQLHandler.class.getDeclaredField("receiveFeedbackUseCase");
        field.setAccessible(true);
        field.set(handler, useCase);
        handler.setAuthenticator(new BearerTokenAuthenticator("test-token"));
    }

    private APIGatewayProxyResponseEvent handle(String headerName, String headerValue, String body) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setBody(body);
        if (headerName != null) {
            Map<String, String> headers = new HashMap<>();
            headers.put(headerName, headerValue);
            event.setHeaders(headers);
        }
        return handler.handleRequest(event, null);
    }

    @Test
    void withoutTokenReturns401AndTouchesNothing() throws Exception {
        APIGatewayProxyResponseEvent response = handle(null, null, QUERY);
        assertEquals(401, response.getStatusCode());
        assertEquals("Token Bearer ausente", objectMapper.readTree(response.getBody()).get("detail").asText());
        verifyNoInteractions(useCase);
    }

    @Test
    void withInvalidTokenReturns401AndTouchesNothing() throws Exception {
        APIGatewayProxyResponseEvent response = handle("Authorization", "Bearer nope", QUERY);
        assertEquals(401, response.getStatusCode());
        assertEquals("Token Bearer inválido", objectMapper.readTree(response.getBody()).get("detail").asText());
        verifyNoInteractions(useCase);
    }

    @Test
    void legacyApiKeyHeaderAloneReturns401() {
        APIGatewayProxyResponseEvent response = handle("x-api-key", "test-token", QUERY);
        assertEquals(401, response.getStatusCode());
        verifyNoInteractions(useCase);
    }

    @Test
    void withValidTokenReturns200() throws Exception {
        Feedback feedback = new Feedback(UUID.randomUUID().toString(), "Good", 7, Urgency.MEDIUM,
                LocalDateTime.of(2024, 3, 15, 10, 30), false);
        when(useCase.listAllFeedbacks()).thenReturn(List.of(feedback));
        APIGatewayProxyResponseEvent response = handle("authorization", "Bearer test-token", QUERY);
        assertEquals(200, response.getStatusCode());
        JsonNode list = objectMapper.readTree(response.getBody()).get("data").get("listFeedbacks");
        assertEquals(1, list.size());
        verify(useCase).listAllFeedbacks();
    }

    @Test
    void validTokenWithEmptyBodyReturns400() {
        APIGatewayProxyResponseEvent response = handle("Authorization", "Bearer test-token", "");
        assertEquals(400, response.getStatusCode());
        verifyNoInteractions(useCase);
    }
}
