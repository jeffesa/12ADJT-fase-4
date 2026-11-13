package com.fiap.feedback.infra.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.infra.config.FeedbackConfiguration;
import com.fiap.feedback.infra.config.InfrastructureConfiguration;

/**
 * Unit tests for DynamoFeedbackGateway.
 *
 * The DynamoDB client is mocked with Mockito. The table name comes from a mocked
 * InfrastructureConfiguration.
 */
class DynamoFeedbackGatewayTest {

    private static final String TABLE_NAME = "feedbacks-test";

    private DynamoDbClient dynamoDbClient;
    private DynamoFeedbackGateway gateway;

    @BeforeEach
    void setUp() {
        dynamoDbClient = mock(DynamoDbClient.class);
        InfrastructureConfiguration infraConfig = mock(InfrastructureConfiguration.class, RETURNS_DEEP_STUBS);
        when(infraConfig.dynamodb().table().feedbacks()).thenReturn(TABLE_NAME);
        gateway = new DynamoFeedbackGateway(dynamoDbClient, mock(FeedbackConfiguration.class), infraConfig);
    }

    @Test
    void saveWritesFeedbackItemToConfiguredTable() {
        Feedback feedback = new Feedback(UUID.randomUUID().toString(), "Good service", 7,
                Urgency.MEDIUM, LocalDateTime.of(2024, 3, 15, 10, 30), false);
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        Feedback saved = gateway.save(feedback);

        assertSame(feedback, saved);
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(captor.capture());
        assertEquals(TABLE_NAME, captor.getValue().tableName());
        assertEquals(feedback.getId(), captor.getValue().item().get("id").s());
    }

    @Test
    void saveRethrowsDynamoDbException() {
        DynamoDbException failure = (DynamoDbException) DynamoDbException.builder().message("Throughput exceeded").build();
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenThrow(failure);
        Feedback feedback = new Feedback("Good service", 7);

        DynamoDbException error = assertThrows(DynamoDbException.class, () -> gateway.save(feedback));

        assertSame(failure, error);
    }

    @Test
    void findAllScansTableAndMapsItems() {
        Feedback stored = new Feedback(UUID.randomUUID().toString(), "Terrible experience", 2,
                Urgency.HIGH, LocalDateTime.of(2024, 3, 15, 10, 30), true);
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse(stored));

        List<Feedback> feedbacks = gateway.findAll();

        assertEquals(1, feedbacks.size());
        assertEquals(stored.getId(), feedbacks.get(0).getId());
        assertEquals("Terrible experience", feedbacks.get(0).getDescription());
        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(dynamoDbClient).scan(captor.capture());
        assertEquals(TABLE_NAME, captor.getValue().tableName());
        assertEquals(100, captor.getValue().limit());
    }

    @Test
    void findAllRethrowsDynamoDbException() {
        DynamoDbException failure = (DynamoDbException) DynamoDbException.builder().message("Scan failed").build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenThrow(failure);

        DynamoDbException error = assertThrows(DynamoDbException.class, () -> gateway.findAll());

        assertSame(failure, error);
    }

    @Test
    void findByPeriodScansWithBetweenFilterOnCreatedAt() {
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(ScanResponse.builder().build());

        List<Feedback> feedbacks = gateway.findByPeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        assertEquals(0, feedbacks.size());
        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(dynamoDbClient).scan(captor.capture());
        ScanRequest request = captor.getValue();
        assertEquals(TABLE_NAME, request.tableName());
        assertEquals("createdAt BETWEEN :start AND :end", request.filterExpression());
        Map<String, AttributeValue> values = request.expressionAttributeValues();
        assertEquals("2024-01-01", values.get(":start").s());
        assertEquals("2024-01-31", values.get(":end").s());
    }

    @Test
    void findByPeriodRethrowsDynamoDbException() {
        DynamoDbException failure = (DynamoDbException) DynamoDbException.builder().message("Scan failed").build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenThrow(failure);

        DynamoDbException error = assertThrows(DynamoDbException.class,
                () -> gateway.findByPeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)));

        assertSame(failure, error);
    }

    private static ScanResponse scanResponse(Feedback feedback) {
        return ScanResponse.builder()
                .items(List.of(FeedbackDynamoMapper.toAttributeMap(feedback)))
                .build();
    }
}
