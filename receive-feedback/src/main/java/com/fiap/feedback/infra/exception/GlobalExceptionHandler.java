package com.fiap.feedback.infra.exception;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Global exception handler for mapping domain and validation exceptions to HTTP responses.
 * 
 * This class implements RFC 7807 (Problem Detail) format for error responses.
 * It maps different exception types to appropriate HTTP status codes:
 * - IllegalArgumentException → 400 (validation failure)
 * - ConstraintViolationException → 400 (bean validation failure)
 * - Any other Exception → 500 (server error)
 * 
 * All exceptions are logged with appropriate levels (WARNING for 400, ERROR for 500).
 */
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

    /**
     * Converts an exception to an HTTP response using RFC 7807 Problem Detail format.
     * 
     * @param exception the exception to map
     * @return a Response with appropriate status code and Problem Detail body
     */
    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return handleIllegalArgumentException((IllegalArgumentException) exception);
        }
        
        if (exception instanceof ConstraintViolationException) {
            return handleConstraintViolationException((ConstraintViolationException) exception);
        }
        
        return handleGenericException(exception);
    }

    /**
     * Handles IllegalArgumentException (validation failures in use case).
     */
    private Response handleIllegalArgumentException(IllegalArgumentException exception) {
        LOGGER.log(Level.WARNING, "Validation error: " + exception.getMessage(), exception);
        
        JsonObject problemDetail = Json.createObjectBuilder()
            .add("title", "Validation Error")
            .add("status", 400)
            .add("detail", exception.getMessage())
            .build();
        
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(problemDetail)
            .build();
    }

    /**
     * Handles ConstraintViolationException (bean validation failures).
     */
    private Response handleConstraintViolationException(ConstraintViolationException exception) {
        LOGGER.log(Level.WARNING, "Constraint violation: " + exception.getMessage(), exception);
        
        // Concatenate all violation messages
        StringBuilder detail = new StringBuilder();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            if (detail.length() > 0) {
                detail.append("; ");
            }
            detail.append(violation.getMessage());
        }
        
        JsonObject problemDetail = Json.createObjectBuilder()
            .add("title", "Validation Error")
            .add("status", 400)
            .add("detail", detail.toString())
            .build();
        
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(problemDetail)
            .build();
    }

    /**
     * Handles any other exception type (generic server error).
     */
    private Response handleGenericException(Exception exception) {
        LOGGER.log(Level.SEVERE, "Unexpected error: " + exception.getMessage(), exception);
        
        JsonObject problemDetail = Json.createObjectBuilder()
            .add("title", "Internal Server Error")
            .add("status", 500)
            .add("detail", "An unexpected error occurred. Please try again later.")
            .build();
        
        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(problemDetail)
            .build();
    }
}
