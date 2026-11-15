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

/**
 * Tests for the GraphQLHandler listFeedbacks query (English v1 fields).
 */
class GraphQLHandlerListFeedbacksTest {

    private static final String LIST_FEEDBACKS = "{\"query\":\"query { listFeedbacks { id description rating urgency createdAt } }\"}";

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

    private APIGatewayProxyResponseEvent handle(String authorization, String body) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setBody(body);
        if (authorization != null) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", authorization);
            event.setHeaders(headers);
        }
        return handler.handleRequest(event, null);
    }

    @Test
    void listFeedbacksWithTokenReturnsEnglishFields() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 10, 30);
        Feedback feedback = new Feedback(UUID.randomUUID().toString(), "Good", 7, Urgency.MEDIUM, createdAt, false);
        when(useCase.listAllFeedbacks()).thenReturn(List.of(feedback));

        APIGatewayProxyResponseEvent response = handle("Bearer test-token", LIST_FEEDBACKS);

        assertEquals(200, response.getStatusCode());
        JsonNode list = objectMapper.readTree(response.getBody()).get("data").get("listFeedbacks");
        assertEquals(1, list.size());
        JsonNode item = list.get(0);
        assertEquals(feedback.getId(), item.get("id").asText());
        assertEquals("Good", item.get("description").asText());
        assertEquals(7, item.get("rating").asInt());
        assertEquals("MEDIUM", item.get("urgency").asText());
        assertEquals(createdAt.toString(), item.get("createdAt").asText());
        verify(useCase).listAllFeedbacks();
    }

    @Test
    void listFeedbacksWithoutTokenReturns401AndTouchesNothing() throws Exception {
        APIGatewayProxyResponseEvent response = handle(null, LIST_FEEDBACKS);

        assertEquals(401, response.getStatusCode());
        assertEquals("Missing bearer token", objectMapper.readTree(response.getBody()).get("detail").asText());
        verifyNoInteractions(useCase);
    }

    @Test
    void unsupportedQueryReturns400() throws Exception {
        APIGatewayProxyResponseEvent response = handle("Bearer test-token", "{\"query\":\"query { somethingElse { id } }\"}");

        assertEquals(400, response.getStatusCode());
        assertEquals("Consulta GraphQL não suportada. Use a consulta listFeedbacks.",
                objectMapper.readTree(response.getBody()).get("detail").asText());
        verifyNoInteractions(useCase);
    }
}
