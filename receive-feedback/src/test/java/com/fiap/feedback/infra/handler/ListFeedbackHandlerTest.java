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
import com.fiap.feedback.application.usecase.ListFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.infra.security.BearerTokenAuthenticator;

class ListFeedbackHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ListFeedbackHandler handler;
    private ListFeedbackUseCase useCase;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        handler = new ListFeedbackHandler();
        useCase = mock(ListFeedbackUseCase.class);
        Field field = ListFeedbackHandler.class.getDeclaredField("listFeedbackUseCase");
        field.setAccessible(true);
        field.set(handler, useCase);
        handler.setAuthenticator(new BearerTokenAuthenticator("test-token"));
    }

    private APIGatewayProxyResponseEvent handle(String authorization) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        if (authorization != null) {
            Map<String, String> headers = new HashMap<>();
            headers.put("authorization", authorization);
            event.setHeaders(headers);
        }
        return handler.handleRequest(event, null);
    }

    @Test
    void withoutTokenReturns401AndTouchesNothing() throws Exception {
        APIGatewayProxyResponseEvent response = handle(null);
        assertEquals(401, response.getStatusCode());
        assertEquals("Missing bearer token", objectMapper.readTree(response.getBody()).get("detail").asText());
        verifyNoInteractions(useCase);
    }

    @Test
    void withInvalidTokenReturns401AndTouchesNothing() throws Exception {
        APIGatewayProxyResponseEvent response = handle("Bearer nope");
        assertEquals(401, response.getStatusCode());
        assertEquals("Invalid bearer token", objectMapper.readTree(response.getBody()).get("detail").asText());
        verifyNoInteractions(useCase);
    }

    @Test
    void withValidTokenReturns200() throws Exception {
        Feedback feedback = new Feedback(UUID.randomUUID().toString(), "Good", 7, Urgency.MEDIUM,
                LocalDateTime.of(2024, 3, 15, 10, 30), false);
        when(useCase.listAllFeedbacks()).thenReturn(List.of(feedback));
        APIGatewayProxyResponseEvent response = handle("bearer test-token");
        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(1, body.get("total").asInt());
        verify(useCase).listAllFeedbacks();
    }
}
