package com.fiap.feedback.infra.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
 * Unit tests for ReceiveFeedbackHandler English v1 routes (POST and GET /v1/feedback)
 * and for the legacy /avaliacao contract staying unchanged.
 */
class ReceiveFeedbackHandlerV1Test {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String V1_PATH = "/v1/feedback";
    private static final String LEGACY_PATH = "/avaliacao";

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
    void v1PostReturns201WithEnglishFlatBody() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 10, 30);
        Feedback saved = new Feedback(UUID.randomUUID().toString(), "Good service", 7, Urgency.MEDIUM, createdAt, false);
        when(receiveFeedbackUseCase.receiveFeedback("Good service", 7)).thenReturn(saved);

        APIGatewayProxyResponseEvent response = handle("POST", V1_PATH, "{\"description\":\"Good service\",\"rating\":7}", "Bearer test-token");

        assertEquals(201, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertFalse(body.has("receiveFeedback"));
        assertEquals(saved.getId(), body.get("id").asText());
        assertEquals("Good service", body.get("description").asText());
        assertEquals(7, body.get("rating").asInt());
        assertEquals("MEDIUM", body.get("urgency").asText());
        assertEquals(createdAt.toString(), body.get("createdAt").asText());
    }

    @Test
    void v1PostWithPortugueseFieldsReturns400RatingRequired() throws Exception {
        assertBadRequest(handle("POST", V1_PATH, "{\"descricao\":\"Good service\",\"nota\":7}", "Bearer test-token"),
                "Rating is required");
    }

    @Test
    void v1PostBlankDescriptionReturns400() throws Exception {
        assertBadRequest(handle("POST", V1_PATH, "{\"description\":\"\",\"rating\":7}", "Bearer test-token"),
                "Description cannot be empty");
    }

    @Test
    void v1PostRatingAboveTenReturns400() throws Exception {
        assertBadRequest(handle("POST", V1_PATH, "{\"description\":\"Good service\",\"rating\":11}", "Bearer test-token"),
                "Rating must be at most 10");
    }

    @Test
    void v1PostNullBodyReturns400() throws Exception {
        assertBadRequest(handle("POST", V1_PATH, null, "Bearer test-token"), "Request body cannot be empty");
    }

    @Test
    void v1PostEmptyBodyReturns400() throws Exception {
        assertBadRequest(handle("POST", V1_PATH, "", "Bearer test-token"), "Request body cannot be empty");
    }

    @Test
    void v1PostJsonNullBodyReturns400() throws Exception {
        assertBadRequest(handle("POST", V1_PATH, "null", "Bearer test-token"), "Request body cannot be empty");
    }

    @Test
    void v1PostMalformedJsonReturns400() throws Exception {
        assertBadRequest(handle("POST", V1_PATH, "{\"description\":", "Bearer test-token"),
                "Request body is not valid JSON or has fields of the wrong type");
    }

    @Test
    void v1PostWrongRatingTypeReturns400() throws Exception {
        assertBadRequest(handle("POST", V1_PATH, "{\"description\":\"Good service\",\"rating\":\"abc\"}", "Bearer test-token"),
                "Request body is not valid JSON or has fields of the wrong type");
    }

    @Test
    void v1PostUseCaseFailureReturns500WithoutInternalMessage() throws Exception {
        when(receiveFeedbackUseCase.receiveFeedback(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Database connection failed"));

        APIGatewayProxyResponseEvent response = handle("POST", V1_PATH, "{\"description\":\"Good service\",\"rating\":7}", "Bearer test-token");

        assertEquals(500, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Internal Server Error", body.get("title").asText());
        assertFalse(body.get("detail").asText().contains("Database"));
    }

    @Test
    void v1PostWithoutTokenReturns401AndTouchesNothing() throws Exception {
        APIGatewayProxyResponseEvent response = handle("POST", V1_PATH, "{\"description\":\"Good service\",\"rating\":7}", null);
        assertEquals(401, response.getStatusCode());
        assertEquals("Missing bearer token", objectMapper.readTree(response.getBody()).get("detail").asText());
        verifyNoInteractions(receiveFeedbackUseCase, listFeedbackUseCase);
    }

    @Test
    void v1PostWithInvalidTokenReturns401AndTouchesNothing() throws Exception {
        APIGatewayProxyResponseEvent response = handle("POST", V1_PATH, "{\"description\":\"Good service\",\"rating\":7}", "Bearer nope");
        assertEquals(401, response.getStatusCode());
        assertEquals("Invalid bearer token", objectMapper.readTree(response.getBody()).get("detail").asText());
        verifyNoInteractions(receiveFeedbackUseCase, listFeedbackUseCase);
    }

    @Test
    void v1GetWithTokenReturns200WithEnglishList() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 10, 30);
        Feedback feedback = new Feedback(UUID.randomUUID().toString(), "Good service", 9, Urgency.LOW, createdAt, false);
        when(listFeedbackUseCase.listAllFeedbacks()).thenReturn(List.of(feedback));

        APIGatewayProxyResponseEvent response = handle("GET", V1_PATH, null, "Bearer test-token");

        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(1, body.get("total").asInt());
        JsonNode item = body.get("feedbacks").get(0);
        assertEquals(feedback.getId(), item.get("id").asText());
        assertEquals("Good service", item.get("description").asText());
        assertEquals(9, item.get("rating").asInt());
        assertEquals("LOW", item.get("urgency").asText());
        assertEquals(createdAt.toString(), item.get("createdAt").asText());
        assertFalse(item.has("descricao"));
    }

    @Test
    void v1GetEmptyListReturnsZeroTotal() throws Exception {
        when(listFeedbackUseCase.listAllFeedbacks()).thenReturn(List.of());

        APIGatewayProxyResponseEvent response = handle("GET", V1_PATH, null, "Bearer test-token");

        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(0, body.get("total").asInt());
        assertTrue(body.get("feedbacks").isEmpty());
    }

    @Test
    void v1GetWithoutTokenReturns401AndTouchesNothing() {
        assertEquals(401, handle("GET", V1_PATH, null, null).getStatusCode());
        verifyNoInteractions(receiveFeedbackUseCase, listFeedbackUseCase);
    }

    @Test
    void v1GetWithInvalidTokenReturns401AndTouchesNothing() {
        assertEquals(401, handle("GET", V1_PATH, null, "Bearer nope").getStatusCode());
        verifyNoInteractions(receiveFeedbackUseCase, listFeedbackUseCase);
    }

    @Test
    void v1PutReturns405() throws Exception {
        APIGatewayProxyResponseEvent response = handle("PUT", V1_PATH, null, "Bearer test-token");
        assertEquals(405, response.getStatusCode());
        assertEquals("Method Not Allowed", objectMapper.readTree(response.getBody()).get("title").asText());
        verifyNoInteractions(receiveFeedbackUseCase, listFeedbackUseCase);
    }

    @Test
    void v1RouteIsDetectedFromResourceWhenPathIsNull() throws Exception {
        when(listFeedbackUseCase.listAllFeedbacks()).thenReturn(List.of());
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setResource(V1_PATH);
        event.setHeaders(authHeaders("Bearer test-token"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(200, response.getStatusCode());
        assertTrue(objectMapper.readTree(response.getBody()).has("feedbacks"));
    }

    @Test
    void legacyPostStillReturnsPortugueseContract() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 10, 30);
        Feedback saved = new Feedback(UUID.randomUUID().toString(), "Good service", 7, Urgency.MEDIUM, createdAt, false);
        when(receiveFeedbackUseCase.receiveFeedback("Good service", 7)).thenReturn(saved);

        APIGatewayProxyResponseEvent response = handle("POST", LEGACY_PATH, "{\"descricao\":\"Good service\",\"nota\":7}", "Bearer test-token");

        assertEquals(201, response.getStatusCode());
        JsonNode feedback = objectMapper.readTree(response.getBody()).get("receiveFeedback");
        assertEquals("Good service", feedback.get("descricao").asText());
        assertEquals(7, feedback.get("nota").asInt());
        assertEquals("MEDIUM", feedback.get("urgencia").asText());
    }

    private void assertBadRequest(APIGatewayProxyResponseEvent response, String detail) throws Exception {
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Validation Error", body.get("title").asText());
        assertEquals(400, body.get("status").asInt());
        assertEquals(detail, body.get("detail").asText());
        verify(receiveFeedbackUseCase, never()).receiveFeedback(anyString(), anyInt());
    }

    private APIGatewayProxyResponseEvent handle(String method, String path, String body, String authorization) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod(method);
        event.setPath(path);
        event.setBody(body);
        if (authorization != null) {
            event.setHeaders(authHeaders(authorization));
        }
        return handler.handleRequest(event, null);
    }

    private static Map<String, String> authHeaders(String authorization) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", authorization);
        return headers;
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
