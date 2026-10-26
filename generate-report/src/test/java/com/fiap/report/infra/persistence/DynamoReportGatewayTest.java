package com.fiap.report.infra.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import com.fiap.report.domain.entity.Feedback;
import com.fiap.report.domain.entity.Urgency;
import com.fiap.report.domain.gateway.ReportException;
import com.fiap.report.infra.config.ReportConfiguration;

/**
 * Unit tests for DynamoReportGateway persistence adapter.
 *
 * Verifies DynamoDB scanning, item mapping, date filtering, error handling,
 * and proper logging of operations.
 *
 * Note: Uses a custom MockDynamoDbClient to avoid Mockito limitation with sealed AWS SDK v2 classes.
 */
class DynamoReportGatewayTest {

    private DynamoReportGateway gateway;
    private MockDynamoDbClient mockDynamoDbClient;
    private ReportConfiguration config;

    static class MockDynamoDbClient implements DynamoDbClient {
        AtomicReference<ScanRequest> lastRequest = new AtomicReference<>();
        List<Map<String, AttributeValue>> itemsToReturn = new ArrayList<>();
        RuntimeException exceptionToThrow = null;

        @Override
        public ScanResponse scan(ScanRequest request) throws DynamoDbException {
            if (exceptionToThrow != null) {
                if (exceptionToThrow instanceof DynamoDbException) {
                    throw (DynamoDbException) exceptionToThrow;
                } else {
                    throw new RuntimeException(exceptionToThrow);
                }
            }
            lastRequest.set(request);
            return ScanResponse.builder().items(itemsToReturn).build();
        }

        @Override
        public String serviceName() {
            return "DynamoDB";
        }

        @Override
        public void close() {}
    }

    static class TestReportConfiguration implements ReportConfiguration {
        private String tableNameValue = "feedbacks";

        TestReportConfiguration(String tableName) {
            this.tableNameValue = tableName;
        }

        @Override
        public String adminEmail() {
            return "admin@feedback-fase-4.com";
        }

        @Override
        public String senderEmail() {
            return "noreply@feedback-fase-4.com";
        }

        @Override
        public String dynamodbEndpoint() {
            return "http://localhost:4566";
        }

        @Override
        public String tableName() {
            return tableNameValue;
        }

        @Override
        public String reportTitle() {
            return "Weekly Feedback Report";
        }
    }

    @BeforeEach
    void setUp() {
        mockDynamoDbClient = new MockDynamoDbClient();
        config = new TestReportConfiguration("feedbacks");

        gateway = new DynamoReportGateway();
        gateway.dynamoDbClient = mockDynamoDbClient;
        gateway.config = config;
    }

    @Test
    void testFindByPeriod_SuccessfullyRetrievesItems() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("id", AttributeValue.builder().s("1").build());
        item1.put("rating", AttributeValue.builder().n("8").build());
        item1.put("urgency", AttributeValue.builder().s("LOW").build());
        item1.put("createdAt", AttributeValue.builder().s("2024-10-21T10:00:00").build());

        Map<String, AttributeValue> item2 = new HashMap<>();
        item2.put("id", AttributeValue.builder().s("2").build());
        item2.put("rating", AttributeValue.builder().n("9").build());
        item2.put("urgency", AttributeValue.builder().s("HIGH").build());
        item2.put("createdAt", AttributeValue.builder().s("2024-10-22T14:00:00").build());

        mockDynamoDbClient.itemsToReturn.add(item1);
        mockDynamoDbClient.itemsToReturn.add(item2);

        // Act
        List<Feedback> feedbacks = gateway.findByPeriod(startDate, endDate);

        // Assert
        assertEquals(2, feedbacks.size());
        assertEquals("1", feedbacks.get(0).getId());
        assertEquals(8, feedbacks.get(0).getRating());
        assertEquals(Urgency.LOW, feedbacks.get(0).getUrgency());
    }

    @Test
    void testFindByPeriod_CallsScanWithCorrectTableName() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        // Act
        gateway.findByPeriod(startDate, endDate);

        // Assert
        ScanRequest request = mockDynamoDbClient.lastRequest.get();
        assertNotNull(request);
        assertEquals("feedbacks", request.tableName());
    }

    @Test
    void testFindByPeriod_CallsScanWithCorrectFilterExpression() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        // Act
        gateway.findByPeriod(startDate, endDate);

        // Assert
        ScanRequest request = mockDynamoDbClient.lastRequest.get();
        assertTrue(request.filterExpression().contains("createdAt BETWEEN :start AND :end"));
    }

    @Test
    void testFindByPeriod_UsesCorrectAttributeValues() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        // Act
        gateway.findByPeriod(startDate, endDate);

        // Assert
        ScanRequest request = mockDynamoDbClient.lastRequest.get();
        Map<String, AttributeValue> expressionValues = request.expressionAttributeValues();
        assertEquals(startDate.toString(), expressionValues.get(":start").s());
        assertEquals(endDate.toString(), expressionValues.get(":end").s());
    }

    @Test
    void testFindByPeriod_ReturnsEmptyListWhenNoItemsFound() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        // Act
        List<Feedback> feedbacks = gateway.findByPeriod(startDate, endDate);

        // Assert
        assertNotNull(feedbacks);
        assertTrue(feedbacks.isEmpty());
    }

    @Test
    void testFindByPeriod_MapsAllItemsToFeedbackEntities() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("id", AttributeValue.builder().s("1").build());
        item1.put("rating", AttributeValue.builder().n("8").build());
        item1.put("urgency", AttributeValue.builder().s("LOW").build());
        item1.put("createdAt", AttributeValue.builder().s("2024-10-21T10:00:00").build());

        Map<String, AttributeValue> item2 = new HashMap<>();
        item2.put("id", AttributeValue.builder().s("2").build());
        item2.put("rating", AttributeValue.builder().n("9").build());
        item2.put("urgency", AttributeValue.builder().s("HIGH").build());
        item2.put("createdAt", AttributeValue.builder().s("2024-10-22T14:00:00").build());

        Map<String, AttributeValue> item3 = new HashMap<>();
        item3.put("id", AttributeValue.builder().s("3").build());
        item3.put("rating", AttributeValue.builder().n("7").build());
        item3.put("urgency", AttributeValue.builder().s("MEDIUM").build());
        item3.put("createdAt", AttributeValue.builder().s("2024-10-23T09:00:00").build());

        mockDynamoDbClient.itemsToReturn.add(item1);
        mockDynamoDbClient.itemsToReturn.add(item2);
        mockDynamoDbClient.itemsToReturn.add(item3);

        // Act
        List<Feedback> feedbacks = gateway.findByPeriod(startDate, endDate);

        // Assert
        assertEquals(3, feedbacks.size());
    }

    @Test
    void testFindByPeriod_ThrowsExceptionOnDynamoDbException() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        DynamoDbException dynamoException = (DynamoDbException) DynamoDbException.builder()
                .message("Connection timeout")
                .build();
        mockDynamoDbClient.exceptionToThrow = dynamoException;

        // Act & Assert
        assertThrows(
            DynamoDbException.class,
            () -> gateway.findByPeriod(startDate, endDate)
        );
    }

    @Test
    void testFindByPeriod_RetrievesCorrectFeedbackId() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("feedback-123").build());
        item.put("rating", AttributeValue.builder().n("8").build());
        item.put("urgency", AttributeValue.builder().s("LOW").build());
        item.put("createdAt", AttributeValue.builder().s("2024-10-21T10:00:00").build());

        mockDynamoDbClient.itemsToReturn.add(item);

        // Act
        List<Feedback> feedbacks = gateway.findByPeriod(startDate, endDate);

        // Assert
        assertEquals("feedback-123", feedbacks.get(0).getId());
    }

    @Test
    void testFindByPeriod_RetrievesCorrectRating() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("1").build());
        item.put("rating", AttributeValue.builder().n("9").build());
        item.put("urgency", AttributeValue.builder().s("LOW").build());
        item.put("createdAt", AttributeValue.builder().s("2024-10-21T10:00:00").build());

        mockDynamoDbClient.itemsToReturn.add(item);

        // Act
        List<Feedback> feedbacks = gateway.findByPeriod(startDate, endDate);

        // Assert
        assertEquals(9, feedbacks.get(0).getRating());
    }

    @Test
    void testFindByPeriod_RetrievesCorrectUrgency() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("1").build());
        item.put("rating", AttributeValue.builder().n("8").build());
        item.put("urgency", AttributeValue.builder().s("HIGH").build());
        item.put("createdAt", AttributeValue.builder().s("2024-10-21T10:00:00").build());

        mockDynamoDbClient.itemsToReturn.add(item);

        // Act
        List<Feedback> feedbacks = gateway.findByPeriod(startDate, endDate);

        // Assert
        assertEquals(Urgency.HIGH, feedbacks.get(0).getUrgency());
    }

    @Test
    void testFindByPeriod_RetrievesCorrectCreatedAt() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("1").build());
        item.put("rating", AttributeValue.builder().n("8").build());
        item.put("urgency", AttributeValue.builder().s("LOW").build());
        item.put("createdAt", AttributeValue.builder().s("2024-10-21T10:30:45").build());

        mockDynamoDbClient.itemsToReturn.add(item);

        // Act
        List<Feedback> feedbacks = gateway.findByPeriod(startDate, endDate);

        // Assert
        assertEquals(LocalDateTime.of(2024, 10, 21, 10, 30, 45), feedbacks.get(0).getCreatedAt());
    }

    @Test
    void testFindByPeriod_ReturnsNonNullList() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        // Act
        List<Feedback> feedbacks = gateway.findByPeriod(startDate, endDate);

        // Assert
        assertNotNull(feedbacks);
    }
}
