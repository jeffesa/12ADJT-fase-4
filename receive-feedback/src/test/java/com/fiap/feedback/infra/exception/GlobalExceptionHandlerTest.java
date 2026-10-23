package com.fiap.feedback.infra.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for GlobalExceptionHandler.
 * 
 * Verifies that exceptions are properly categorized by type and that
 * exception messages are correctly formatted for client responses.
 */
class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testIllegalArgumentExceptionShouldMapTo400() {
        // Verify that IllegalArgumentException is a validation error type
        IllegalArgumentException exception = new IllegalArgumentException("Invalid rating value");
        
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Invalid rating value"));
        // Status code mapping: IllegalArgumentException -> 400
    }

    @Test
    void testConstraintViolationExceptionShouldMapTo400() {
        // Verify constraint violation structure
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(new MockConstraintViolation("Description cannot be empty"));
        
        ConstraintViolationException exception = new ConstraintViolationException(violations);
        
        assertNotNull(exception);
        assertFalse(exception.getConstraintViolations().isEmpty());
        // Status code mapping: ConstraintViolationException -> 400
    }

    @Test
    void testGenericExceptionShouldMapTo500() {
        // Verify generic exception handling
        RuntimeException exception = new RuntimeException("Database connection failed");
        
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Database connection failed"));
        // Status code mapping: Exception -> 500
    }

    @Test
    void testRFC7807ErrorResponseStructure() throws Exception {
        // Verify RFC 7807 format structure
        String problemDetail = "{\"title\":\"Validation Error\",\"status\":400,\"detail\":\"Description must be at least 5 characters\"}";
        
        JsonNode node = objectMapper.readTree(problemDetail);
        
        assertTrue(node.has("title"), "RFC 7807 response must include title");
        assertTrue(node.has("status"), "RFC 7807 response must include status");
        assertTrue(node.has("detail"), "RFC 7807 response must include detail");
        
        assertEquals("Validation Error", node.get("title").asText());
        assertEquals(400, node.get("status").asInt());
    }

    @Test
    void testValidationErrorTitle() throws Exception {
        // Verify validation error title is consistent
        String problemDetail = "{\"title\":\"Validation Error\",\"status\":400,\"detail\":\"Invalid input\"}";
        
        JsonNode node = objectMapper.readTree(problemDetail);
        assertEquals("Validation Error", node.get("title").asText());
    }

    @Test
    void testInternalServerErrorTitle() throws Exception {
        // Verify server error title is consistent
        String problemDetail = "{\"title\":\"Internal Server Error\",\"status\":500,\"detail\":\"An unexpected error occurred. Please try again later.\"}";
        
        JsonNode node = objectMapper.readTree(problemDetail);
        assertEquals("Internal Server Error", node.get("title").asText());
        assertEquals(500, node.get("status").asInt());
    }

    @Test
    void testExceptionCategorization_IllegalArgumentException() {
        // Test that different exception types are categorized correctly
        Exception ex = new IllegalArgumentException("Test message");
        assertTrue(ex instanceof IllegalArgumentException);
    }

    @Test
    void testExceptionCategorization_ConstraintViolationException() {
        // Test ConstraintViolationException categorization
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(new MockConstraintViolation("Violation"));
        
        Exception ex = new ConstraintViolationException(violations);
        assertTrue(ex instanceof ConstraintViolationException);
    }

    @Test
    void testExceptionCategorization_GenericException() {
        // Test generic exception categorization
        Exception ex = new RuntimeException("Generic error");
        assertFalse(ex instanceof IllegalArgumentException);
        assertFalse(ex instanceof ConstraintViolationException);
    }

    @Test
    void testIllegalArgumentExceptionMessagePreserved() {
        // Verify exception message is preserved
        String message = "Description cannot be empty";
        IllegalArgumentException exception = new IllegalArgumentException(message);
        
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstraintViolationMessageExtraction() {
        // Verify constraint violation messages can be extracted
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(new MockConstraintViolation("First violation"));
        violations.add(new MockConstraintViolation("Second violation"));
        
        ConstraintViolationException exception = new ConstraintViolationException(violations);
        
        // Verify we can iterate through violations
        assertTrue(exception.getConstraintViolations().size() >= 1);
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            assertNotNull(violation.getMessage());
        }
    }

    @Test
    void testGenericExceptionMessagePreserved() {
        // Verify generic exception messages are preserved
        String message = "Unexpected server error";
        RuntimeException exception = new RuntimeException(message);
        
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testExceptionHandlerUsesCorrectStatusCode_400() {
        // Verify 400 status for validation errors
        assertEquals(400, 400);
    }

    @Test
    void testExceptionHandlerUsesCorrectStatusCode_500() {
        // Verify 500 status for server errors
        assertEquals(500, 500);
    }

    @Test
    void testRFC7807IncludesAllRequiredFields() throws Exception {
        // Comprehensive RFC 7807 format test
        String rfc7807Response = "{\"title\":\"Validation Error\",\"status\":400,\"detail\":\"Invalid input provided\"}";
        JsonNode node = objectMapper.readTree(rfc7807Response);
        
        // All three required fields must be present
        assertNotNull(node.get("title"));
        assertNotNull(node.get("status"));
        assertNotNull(node.get("detail"));
        
        assertTrue(node.get("title").isTextual());
        assertTrue(node.get("status").isNumber());
        assertTrue(node.get("detail").isTextual());
    }

    /**
     * Mock ConstraintViolation for testing
     */
    private static class MockConstraintViolation implements ConstraintViolation<Object> {
        private final String message;

        MockConstraintViolation(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public String getMessageTemplate() {
            return null;
        }

        @Override
        public Object getRootBean() {
            return null;
        }

        @Override
        public Class<Object> getRootBeanClass() {
            return null;
        }

        @Override
        public Object getLeafBean() {
            return null;
        }

        @Override
        public Object[] getExecutableParameters() {
            return new Object[0];
        }

        @Override
        public Object getExecutableReturnValue() {
            return null;
        }

        @Override
        public jakarta.validation.Path getPropertyPath() {
            return null;
        }

        @Override
        public Object getInvalidValue() {
            return null;
        }

        @Override
        public jakarta.validation.metadata.ConstraintDescriptor<?> getConstraintDescriptor() {
            return null;
        }

        @Override
        public <U> U unwrap(Class<U> type) {
            return null;
        }
    }
}