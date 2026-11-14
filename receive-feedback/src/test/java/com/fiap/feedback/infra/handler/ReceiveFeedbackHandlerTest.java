package com.fiap.feedback.infra.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fiap.feedback.application.usecase.ListFeedbackUseCase;
import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.infra.security.BearerTokenAuthenticator;

/**
 * Unit tests for ReceiveFeedbackHandler POST /avaliacao.
 *
 * The use case is mocked with Mockito. Bean Validation runs with the real
 * Hibernate Validator so the constraint messages come from ReceiveFeedbackInput.
 */
class ReceiveFeedbackHandlerTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ReceiveFeedbackHandler handler;
    private ReceiveFeedbackUseCase receiveFeedbackUseCase;
    private ListFeedbackUseCase listFeedbackUseCase;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        handler = new ReceiveFeedbackHandler();
        receiveFeedbackUseCase = mock(ReceiveFeedbackUseCase.class);
        setField(handler, "receiveFeedbackUseCase", receiveFeedbackUseCase);
        listFeedbackUseCase = mock(ListFeedbackUseCase.class);
        setField(handler, "listFeedbackUseCase", listFeedbackUseCase);
        setField(handler, "validator", VALIDATOR);
        handler.setAuthenticator(new BearerTokenAuthenticator("test-token"));
    }

    @Test
    void getWithTokenReturns200() throws Exception {
        when(listFeedbackUseCase.listAllFeedbacks()).thenReturn(List.of());
        APIGatewayProxyResponseEvent response = handle("GET", null, "Bearer test-token");
        assertEquals(200, response.getStatusCode());
        assertEquals(0, objectMapper.readTree(response.getBody()).get("total").asInt());
    }

    @Test
    void postWithoutTokenReturns401AndTouchesNothing() throws Exception {
        APIGatewayProxyResponseEvent response = handle("POST", "{\"descricao\":\"Good service\",\"nota\":7}", null);
        assertEquals(401, response.getStatusCode());
        assertEquals("Missing bearer token", objectMapper.readTree(response.getBody()).get("detail").asText());
        verifyNoInteractions(receiveFeedbackUseCase, listFeedbackUseCase);
    }

    @Test
    void postWithInvalidTokenReturns401AndTouchesNothing() throws Exception {
        APIGatewayProxyResponseEvent response = handle("POST", "{\"descricao\":\"Good service\",\"nota\":7}", "Bearer nope");
        assertEquals(401, response.getStatusCode());
        assertEquals("Invalid bearer token", objectMapper.readTree(response.getBody()).get("detail").asText());
        verifyNoInteractions(receiveFeedbackUseCase, listFeedbackUseCase);
    }

    @Test
    void getWithoutTokenReturns401AndTouchesNothing() {
        assertEquals(401, handle("GET", null, null).getStatusCode());
        verifyNoInteractions(receiveFeedbackUseCase, listFeedbackUseCase);
    }

    @Test
    void getWithInvalidTokenReturns401AndTouchesNothing() {
        assertEquals(401, handle("GET", null, "Bearer nope").getStatusCode());
        verifyNoInteractions(receiveFeedbackUseCase, listFeedbackUseCase);
    }

    @Test
    void validPostReturns201WithCreatedFeedback() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 10, 30);
        Feedback saved = new Feedback(UUID.randomUUID().toString(), "Good service", 7, Urgency.MEDIUM, createdAt, false);
        when(receiveFeedbackUseCase.receiveFeedback("Good service", 7)).thenReturn(saved);

        APIGatewayProxyResponseEvent response = handle("{\"descricao\":\"Good service\",\"nota\":7}");

        assertEquals(201, response.getStatusCode());
        JsonNode feedback = objectMapper.readTree(response.getBody()).get("receiveFeedback");
        assertEquals(saved.getId(), feedback.get("id").asText());
        assertEquals("Good service", feedback.get("descricao").asText());
        assertEquals(7, feedback.get("nota").asInt());
        assertEquals("MEDIUM", feedback.get("urgencia").asText());
        assertEquals(createdAt.toString(), feedback.get("dataCriacao").asText());
    }

    @Test
    void blankDescriptionReturns400WithValidationDetail() throws Exception {
        APIGatewayProxyResponseEvent response = handle("{\"descricao\":\"\",\"nota\":7}");

        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Validation Error", body.get("title").asText());
        assertEquals(400, body.get("status").asInt());
        assertEquals("Description cannot be empty", body.get("detail").asText());
        verify(receiveFeedbackUseCase, never()).receiveFeedback(anyString(), anyInt());
    }

    @Test
    void ratingOutOfRangeReturns400WithValidationDetail() throws Exception {
        APIGatewayProxyResponseEvent response = handle("{\"descricao\":\"Good service\",\"nota\":11}");

        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Rating must be at most 10", body.get("detail").asText());
        verify(receiveFeedbackUseCase, never()).receiveFeedback(anyString(), anyInt());
    }

    @Test
    void nullBodyReturns400() throws Exception {
        APIGatewayProxyResponseEvent response = handle(null);

        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Validation Error", body.get("title").asText());
        assertEquals("Request body cannot be empty", body.get("detail").asText());
        verify(receiveFeedbackUseCase, never()).receiveFeedback(anyString(), anyInt());
    }

    @Test
    void useCaseFailureReturns500WithoutInternalMessage() throws Exception {
        when(receiveFeedbackUseCase.receiveFeedback(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Database connection failed"));

        APIGatewayProxyResponseEvent response = handle("{\"descricao\":\"Good service\",\"nota\":7}");

        assertEquals(500, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Internal Server Error", body.get("title").asText());
        assertEquals(500, body.get("status").asInt());
        assertFalse(body.get("detail").asText().contains("Database"));
    }

    @Test
    void missingRatingReturns400() throws Exception {
        assertBadRequest(handle("{\"descricao\":\"Good service\"}"), "Rating is required");
    }

    @Test
    void stringRatingReturns400() throws Exception {
        assertBadRequest(handle("{\"descricao\":\"Good service\",\"nota\":\"abc\"}"),
                "Request body is not valid JSON or has fields of the wrong type");
    }

    @Test
    void missingDescriptionReturns400() throws Exception {
        assertBadRequest(handle("{\"nota\":7}"), "Description cannot be empty");
    }

    @Test
    void emptyBodyReturns400() throws Exception {
        assertBadRequest(handle(""), "Request body cannot be empty");
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        assertBadRequest(handle("{\"descricao\":"),
                "Request body is not valid JSON or has fields of the wrong type");
    }

    @Test
    void jsonNullBodyReturns400() throws Exception {
        assertBadRequest(handle("null"), "Request body cannot be empty");
    }

    private void assertBadRequest(APIGatewayProxyResponseEvent response, String detail) throws Exception {
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Validation Error", body.get("title").asText());
        assertEquals(400, body.get("status").asInt());
        assertEquals(detail, body.get("detail").asText());
        verify(receiveFeedbackUseCase, never()).receiveFeedback(anyString(), anyInt());
    }

    private APIGatewayProxyResponseEvent handle(String body) {
        return handle("POST", body, "Bearer test-token");
    }

    private APIGatewayProxyResponseEvent handle(String method, String body, String authorization) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod(method);
        event.setBody(body);
        if (authorization != null) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", authorization);
            event.setHeaders(headers);
        }
        return handler.handleRequest(event, null);
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
