package com.fiap.report.domain.gateway;

/**
 * Exception raised during report operations.
 * 
 * This unchecked exception (extends RuntimeException) is used to signal errors in
 * report retrieval, aggregation, or sending operations.
 */
public class ReportException extends RuntimeException {

    /**
     * Constructor with error message.
     * 
     * @param message descriptive error message
     */
    public ReportException(String message) {
        super(message);
    }

    /**
     * Constructor with error message and root cause.
     * 
     * @param message descriptive error message
     * @param cause the underlying exception
     */
    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
