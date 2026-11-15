package com.fiap.feedback.infra.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class BearerTokenAuthenticatorTest {

    private static final String TOKEN = "expected-token-value-123";
    private static final String WRONG = "wrong-token-value-456";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(TOKEN);

    private static APIGatewayProxyRequestEvent eventWith(String headerName, String headerValue) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put(headerName, headerValue);
        event.setHeaders(headers);
        return event;
    }

    private void assertUnauthorized(Optional<APIGatewayProxyResponseEvent> result, String detail) throws Exception {
        assertTrue(result.isPresent());
        APIGatewayProxyResponseEvent response = result.get();
        assertEquals(401, response.getStatusCode());
        assertEquals("Bearer", response.getHeaders().get("WWW-Authenticate"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Não autorizado", body.get("title").asText());
        assertEquals(401, body.get("status").asInt());
        assertEquals(detail, body.get("detail").asText());
    }

    @Test
    void nullHeadersMapIsMissing() throws Exception {
        assertUnauthorized(authenticator.authenticate(new APIGatewayProxyRequestEvent()), "Token Bearer ausente");
    }

    @Test
    void emptyHeadersMapIsMissing() throws Exception {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHeaders(new HashMap<>());
        assertUnauthorized(authenticator.authenticate(event), "Token Bearer ausente");
    }

    @Test
    void emptyHeaderValueIsMissing() throws Exception {
        assertUnauthorized(authenticator.authenticate(eventWith("Authorization", "")), "Token Bearer ausente");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic abc123", "Bearer", "Bearer ", "Bearer a b", "Bearerexpected-token-value-123"})
    void malformedSchemeIsMissing(String value) throws Exception {
        assertUnauthorized(authenticator.authenticate(eventWith("Authorization", value)), "Token Bearer ausente");
    }

    @Test
    void wrongTokenIsInvalid() throws Exception {
        assertUnauthorized(authenticator.authenticate(eventWith("Authorization", "Bearer " + WRONG)),
                "Token Bearer inválido");
    }

    @Test
    void rightTokenIsAuthorized() {
        assertTrue(authenticator.authenticate(eventWith("Authorization", "Bearer " + TOKEN)).isEmpty());
    }

    @Test
    void extraSpaceBeforeTokenIsAccepted() {
        assertTrue(authenticator.authenticate(eventWith("Authorization", "Bearer  " + TOKEN)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Authorization", "authorization", "AUTHORIZATION"})
    void headerNameIsCaseInsensitive(String name) {
        assertTrue(authenticator.authenticate(eventWith(name, "Bearer " + TOKEN)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"bearer", "BEARER", "Bearer"})
    void schemeIsCaseInsensitive(String scheme) {
        assertTrue(authenticator.authenticate(eventWith("Authorization", scheme + " " + TOKEN)).isEmpty());
    }

    @Test
    void duplicatedHeaderWithDifferentValuesIsMissing() throws Exception {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + TOKEN);
        headers.put("authorization", "Bearer " + WRONG);
        event.setHeaders(headers);
        assertUnauthorized(authenticator.authenticate(event), "Token Bearer ausente");
    }

    @Test
    void duplicatedHeaderWithSameValuesIsAuthorized() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + TOKEN);
        headers.put("authorization", "Bearer " + TOKEN);
        event.setHeaders(headers);
        assertTrue(authenticator.authenticate(event).isEmpty());
    }

    @Test
    void doesNotLeakTokensInLogs() {
        List<String> logged = new ArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(String.valueOf(record.getMessage()));
                if (record.getParameters() != null) {
                    for (Object p : record.getParameters()) {
                        logged.add(String.valueOf(p));
                    }
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(BearerTokenAuthenticator.class.getName());
        logger.addHandler(capture);
        try {
            authenticator.authenticate(eventWith("Authorization", "Bearer " + WRONG));
            authenticator.authenticate(eventWith("Authorization", "Basic " + WRONG));
        } finally {
            logger.removeHandler(capture);
        }
        assertFalse(logged.isEmpty());
        for (String line : logged) {
            assertFalse(line.contains(WRONG));
            assertFalse(line.contains(TOKEN));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void blankConfiguredTokenFailsClosed(String configured) throws Exception {
        assertFailsClosed(new BearerTokenAuthenticator(configured), configured);
    }

    @Test
    void nullConfiguredTokenFailsClosed() throws Exception {
        assertFailsClosed(new BearerTokenAuthenticator(null), "null");
    }

    private void assertFailsClosed(BearerTokenAuthenticator closed, String configured) throws Exception {
        for (String header : new String[] {"Bearer " + TOKEN, "", "Bearer ", "Bearer  "}) {
            Optional<APIGatewayProxyResponseEvent> result = closed.authenticate(eventWith("Authorization", header));
            assertTrue(result.isPresent());
            assertEquals(500, result.get().getStatusCode());
            JsonNode body = objectMapper.readTree(result.get().getBody());
            assertEquals("Autenticação não configurada", body.get("detail").asText());
            assertFalse(result.get().getBody().contains(TOKEN));
        }
        assertTrue(closed.authenticate(new APIGatewayProxyRequestEvent()).isPresent());
    }
}
