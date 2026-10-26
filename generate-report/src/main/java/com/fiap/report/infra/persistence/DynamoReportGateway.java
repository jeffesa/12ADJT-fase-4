package com.fiap.report.infra.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import com.fiap.report.domain.entity.Feedback;
import com.fiap.report.domain.gateway.ReportGateway;
import com.fiap.report.domain.gateway.ReportException;
import com.fiap.report.infra.config.ReportConfiguration;

/**
 * DynamoDB implementation of the ReportGateway interface.
 *
 * This adapter is responsible for retrieving feedback records from DynamoDB
 * within a specified date range. It implements the ReportGateway interface,
 * allowing the domain layer to remain independent of persistence concerns
 * (Clean Architecture principle).
 *
 * Features:
 * - Queries the DynamoDB feedbacks table with date range filtering
 * - Uses filter expressions to retrieve records between start and end dates
 * - Maps DynamoDB items to Feedback domain entities
 * - Handles exceptions with appropriate logging
 *
 * Exception handling:
 * - DynamoDbException is caught, logged at SEVERE level, and rethrown
 * - The use case layer can handle database errors appropriately
 */
@ApplicationScoped
public class DynamoReportGateway implements ReportGateway {

    private static final Logger LOGGER = Logger.getLogger(DynamoReportGateway.class.getName());

    @Inject
    DynamoDbClient dynamoDbClient;

    @Inject
    ReportConfiguration config;

    /**
     * Retrieves all feedbacks within a specified date range.
     *
     * The results include feedbacks from both the start and end dates (inclusive).
     * Dates are compared as ISO-8601 formatted strings in DynamoDB.
     *
     * @param startDate the beginning of the search range (inclusive)
     * @param endDate the end of the search range (inclusive)
     * @return a list of feedbacks matching the period; empty list if none found
     * @throws ReportException if the retrieval operation fails
     */
    @Override
    public List<Feedback> findByPeriod(LocalDate startDate, LocalDate endDate) throws ReportException {
        try {
            LOGGER.log(Level.INFO,
                    "Retrieving feedbacks from DynamoDB for period: " + startDate + " to " + endDate);

            String tableName = config.dynamodb().tableName();

            // Filter expression to find feedbacks within the date range
            // Dates are compared as ISO strings (YYYY-MM-DD format)
            String filterExpression = "createdAt BETWEEN :start AND :end";

            // Build the attribute value map for the filter
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":start", AttributeValue.builder()
                    .s(startDate.toString())
                    .build());
            expressionAttributeValues.put(":end", AttributeValue.builder()
                    .s(endDate.toString())
                    .build());

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression(filterExpression)
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

            List<Feedback> feedbacks = scanResponse.items().stream()
                    .map(ReportDynamoMapper::toDomain)
                    .collect(Collectors.toList());

            LOGGER.log(Level.FINE,
                    "Retrieved " + feedbacks.size() + " feedbacks for period: " + startDate + " to " + endDate);

            return feedbacks;
        } catch (DynamoDbException e) {
            LOGGER.log(Level.SEVERE,
                    "DynamoDB error retrieving feedbacks for period: " + startDate + " to " + endDate +
                    ", Error: " + e.getMessage());
            throw e;
        }
    }
}
