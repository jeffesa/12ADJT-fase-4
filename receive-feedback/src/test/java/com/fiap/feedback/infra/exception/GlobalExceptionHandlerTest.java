package com.fiap.feedback.infra.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.json.JsonObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * The JAX-RS Response factory is mocked with Mockito because no RuntimeDelegate
 * implementation is available on the unit test classpath.
 */
class GlobalExceptionHandlerTest {

    private static final String GENERIC_DETAIL = "Ocorreu um erro inesperado. Tente novamente mais tarde.";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgumentExceptionIsMappedTo400WithMessageAsDetail() {
        Response expected = mock(Response.class);

        try (MockedStatic<Response> responses = mockStatic(Response.class)) {
            Response.ResponseBuilder builder = stubBuilder(responses, Response.Status.BAD_REQUEST, expected);

            Response result = handler.toResponse(new IllegalArgumentException("Nota deve estar entre 0 e 10"));

            assertSame(expected, result);
            JsonObject body = capturedEntity(builder);
            assertEquals("Erro de validação", body.getString("title"));
            assertEquals(400, body.getInt("status"));
            assertEquals("Nota deve estar entre 0 e 10", body.getString("detail"));
        }
    }

    @Test
    void constraintViolationExceptionIsMappedTo400WithAllMessages() {
        Response expected = mock(Response.class);

        try (MockedStatic<Response> responses = mockStatic(Response.class)) {
            Response.ResponseBuilder builder = stubBuilder(responses, Response.Status.BAD_REQUEST, expected);

            Set<ConstraintViolation<?>> violations = new LinkedHashSet<>();
            violations.add(violation("Descrição não pode estar vazia"));
            violations.add(violation("Nota deve ser no máximo 10"));

            Response result = handler.toResponse(new ConstraintViolationException(violations));

            assertSame(expected, result);
            JsonObject body = capturedEntity(builder);
            assertEquals("Erro de validação", body.getString("title"));
            assertEquals(400, body.getInt("status"));
            String detail = body.getString("detail");
            assertTrue(detail.contains("Descrição não pode estar vazia"));
            assertTrue(detail.contains("Nota deve ser no máximo 10"));
        }
    }

    @Test
    void genericExceptionIsMappedTo500WithoutLeakingInternalMessage() {
        Response expected = mock(Response.class);

        try (MockedStatic<Response> responses = mockStatic(Response.class)) {
            Response.ResponseBuilder builder = stubBuilder(responses, Response.Status.INTERNAL_SERVER_ERROR, expected);

            Response result = handler.toResponse(new RuntimeException("Database connection failed"));

            assertSame(expected, result);
            JsonObject body = capturedEntity(builder);
            assertEquals("Erro interno do servidor", body.getString("title"));
            assertEquals(500, body.getInt("status"));
            assertEquals(GENERIC_DETAIL, body.getString("detail"));
            assertFalse(body.getString("detail").contains("Database"));
        }
    }

    private static Response.ResponseBuilder stubBuilder(
            MockedStatic<Response> responses,
            Response.Status status,
            Response response) {
        Response.ResponseBuilder builder = mock(Response.ResponseBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(response);
        responses.when(() -> Response.status(status)).thenReturn(builder);
        return builder;
    }

    private static JsonObject capturedEntity(Response.ResponseBuilder builder) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(builder).entity(captor.capture());
        return (JsonObject) captor.getValue();
    }

    private static ConstraintViolation<?> violation(String message) {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }
}
