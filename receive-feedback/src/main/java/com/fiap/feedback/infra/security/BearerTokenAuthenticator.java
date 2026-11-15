package com.fiap.feedback.infra.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

/**
 * Validates the {@code Authorization: Bearer <token>} header against the token
 * configured in the {@code API_TOKEN} environment variable.
 *
 * Fails closed: if no token is configured, every request is rejected with 500.
 * Never logs the received token, the expected token or the header value.
 */
public final class BearerTokenAuthenticator {

    private static final Logger LOGGER = Logger.getLogger(BearerTokenAuthenticator.class.getName());
    private static final String AUTHORIZATION_HEADER = "authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final String expectedToken;

    public BearerTokenAuthenticator(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public static BearerTokenAuthenticator fromEnvironment() {
        return new BearerTokenAuthenticator(System.getenv("API_TOKEN"));
    }

    /**
     * @return empty if the request is authorized, otherwise the error response to return.
     */
    public Optional<APIGatewayProxyResponseEvent> authenticate(APIGatewayProxyRequestEvent event) {
        if (expectedToken == null || expectedToken.isBlank()) {
            LOGGER.log(Level.SEVERE, "API_TOKEN is not configured; rejecting all requests");
            return Optional.of(error(500, "Erro interno do servidor", "Autenticação não configurada", false));
        }

        String headerValue = findAuthorizationHeader(event.getHeaders());
        if (headerValue == null) {
            return Optional.of(missing());
        }

        String value = headerValue.trim();
        if (value.length() <= BEARER_PREFIX.length()
                || !value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return Optional.of(missing());
        }
        String token = value.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty() || containsWhitespace(token)) {
            return Optional.of(missing());
        }

        if (!MessageDigest.isEqual(sha256(token), sha256(expectedToken))) {
            LOGGER.log(Level.WARNING, "Unauthorized request: invalid bearer token");
            return Optional.of(error(401, "Não autorizado", "Token Bearer inválido", true));
        }
        return Optional.empty();
    }

    /**
     * Returns the Authorization header value, ignoring the header name case.
     * Returns null if absent/empty, or if keys differing only by case carry different values.
     */
    private static String findAuthorizationHeader(Map<String, String> headers) {
        if (headers == null) {
            return null;
        }
        String found = null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || !AUTHORIZATION_HEADER.equalsIgnoreCase(key) || value == null || value.isBlank()) {
                continue;
            }
            if (found != null && !found.equals(value)) {
                return null;
            }
            found = value;
        }
        return found;
    }

    private static boolean containsWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available");
        }
    }

    private static APIGatewayProxyResponseEvent missing() {
        LOGGER.log(Level.WARNING, "Unauthorized request: missing bearer token");
        return error(401, "Não autorizado", "Token Bearer ausente", true);
    }

    private static APIGatewayProxyResponseEvent error(int status, String title, String detail, boolean challenge) {
        String body = "{\"title\":\"" + title + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (challenge) {
            headers.put("WWW-Authenticate", "Bearer");
        }
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(status);
        response.setBody(body);
        response.setHeaders(headers);
        return response;
    }
}
