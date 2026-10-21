package com.fiap.feedback.infra.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.gateway.FeedbackGateway;
import com.fiap.feedback.infra.config.FeedbackConfiguration;
import com.fiap.feedback.infra.config.InfrastructureConfiguration;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DynamoDB implementation of the FeedbackGateway interface.
 * 
 * This adapter is responsible for persisting and retrieving feedback entities
 * from DynamoDB. It follows the dependency inversion principle by implementing
 * the domain's FeedbackGateway interface.
 * 
 * Features:
 * - Save feedback: puts an item in the DynamoDB table
 * - Find all: scans all items from the table
 * - Find by period: scans with filter expression to retrieve feedbacks within a date range
 * 
 * Exception handling:
 * - DynamoDbException is caught, logged, and rethrown to ensure visibility
 * - The use case layer can handle these exceptions appropriately
 */
@ApplicationScoped
public class DynamoFeedbackGateway implements FeedbackGateway {

    private static final Logger LOGGER = Logger.getLogger(DynamoFeedbackGateway.class);

    private final DynamoDbClient dynamoDbClient;
    private final FeedbackConfiguration config;
    private final InfrastructureConfiguration infraConfig;

    /**
     * Constructor for dependency injection.
     * 
     * @param dynamoDbClient the DynamoDB client managed by Quarkus
     * @param config the feedback configuration containing table names
     * @param infraConfig the infrastructure configuration containing AWS resource names
     */
    @Inject
    public DynamoFeedbackGateway(
            DynamoDbClient dynamoDbClient,
            FeedbackConfiguration config,
            InfrastructureConfiguration infraConfig) {
        this.dynamoDbClient = dynamoDbClient;
        this.config = config;
        this.infraConfig = infraConfig;
    }

    @Override
    public Feedback save(Feedback feedback) {
        try {
            String tableName = infraConfig.dynamodb().table().feedbacks();
            
            Map<String, AttributeValue> item = FeedbackDynamoMapper.toAttributeMap(feedback);

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(putItemRequest);
            
            LOGGER.infof("Feedback saved with id: %s", feedback.getId());
            return feedback;
        } catch (DynamoDbException e) {
            LOGGER.errorf("Error saving feedback to DynamoDB: %s", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Feedback> findAll() {
        try {
            String tableName = infraConfig.dynamodb().table().feedbacks();
            
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .limit(100)  // Matches dynamodb.scan.limit from application.yaml
                    .build();

            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

            List<Feedback> feedbacks = scanResponse.items().stream()
                    .map(FeedbackDynamoMapper::toDomain)
                    .collect(Collectors.toList());

            LOGGER.infof("Found %d feedbacks in DynamoDB", feedbacks.size());
            return feedbacks;
        } catch (DynamoDbException e) {
            LOGGER.errorf("Error scanning feedbacks from DynamoDB: %s", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Feedback> findByPeriod(LocalDate startDate, LocalDate endDate) {
        try {
            String tableName = infraConfig.dynamodb().table().feedbacks();

            // Filter expression to find feedbacks within the date range
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
                    .limit(100)
                    .build();

            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

            List<Feedback> feedbacks = scanResponse.items().stream()
                    .map(FeedbackDynamoMapper::toDomain)
                    .collect(Collectors.toList());

            LOGGER.infof("Found %d feedbacks between %s and %s", 
                    feedbacks.size(), startDate, endDate);
            return feedbacks;
        } catch (DynamoDbException e) {
            LOGGER.errorf("Error scanning feedbacks by period from DynamoDB: %s", e.getMessage());
            throw e;
        }
    }
}
