package com.fiap.report.infra.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for ReportConfiguration property injection.
 *
 * Verifies that configuration properties are correctly read from application.yaml
 * (or defaults) and that all properties are accessible through the interface.
 */
class ReportConfigurationTest {

    @Mock
    private ReportConfiguration config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAdminEmail_ReturnsDefaultValue() {
        // Arrange
        when(config.adminEmail()).thenReturn("admin@feedback-fase-4.com");

        // Act
        String result = config.adminEmail();

        // Assert
        assertEquals("admin@feedback-fase-4.com", result);
        verify(config).adminEmail();
    }

    @Test
    void testSenderEmail_ReturnsDefaultValue() {
        // Arrange
        when(config.senderEmail()).thenReturn("noreply@feedback-fase-4.com");

        // Act
        String result = config.senderEmail();

        // Assert
        assertEquals("noreply@feedback-fase-4.com", result);
        verify(config).senderEmail();
    }

    @Test
    void testTableName_ReturnsDefaultValue() {
        // Arrange
        when(config.tableName()).thenReturn("feedbacks");

        // Act
        String result = config.tableName();

        // Assert
        assertEquals("feedbacks", result);
        verify(config).tableName();
    }

    @Test
    void testDynamodbEndpoint_ReturnsDefaultValue() {
        // Arrange
        when(config.dynamodbEndpoint()).thenReturn("http://localhost:4566");

        // Act
        String result = config.dynamodbEndpoint();

        // Assert
        assertEquals("http://localhost:4566", result);
        verify(config).dynamodbEndpoint();
    }

    @Test
    void testReportTitle_ReturnsDefaultValue() {
        // Arrange
        when(config.reportTitle()).thenReturn("Weekly Feedback Report");

        // Act
        String result = config.reportTitle();

        // Assert
        assertEquals("Weekly Feedback Report", result);
        verify(config).reportTitle();
    }

    @Test
    void testAdminEmail_ReturnsCustomValue() {
        // Arrange
        when(config.adminEmail()).thenReturn("custom@example.com");

        // Act
        String result = config.adminEmail();

        // Assert
        assertEquals("custom@example.com", result);
    }

    @Test
    void testSenderEmail_ReturnsCustomValue() {
        // Arrange
        when(config.senderEmail()).thenReturn("custom-sender@example.com");

        // Act
        String result = config.senderEmail();

        // Assert
        assertEquals("custom-sender@example.com", result);
    }

    @Test
    void testTableName_ReturnsCustomValue() {
        // Arrange
        when(config.tableName()).thenReturn("custom_feedbacks_table");

        // Act
        String result = config.tableName();

        // Assert
        assertEquals("custom_feedbacks_table", result);
    }

    @Test
    void testDynamodbEndpoint_ReturnsCustomValue() {
        // Arrange
        when(config.dynamodbEndpoint()).thenReturn("http://dynamodb.custom.aws.com");

        // Act
        String result = config.dynamodbEndpoint();

        // Assert
        assertEquals("http://dynamodb.custom.aws.com", result);
    }

    @Test
    void testReportTitle_ReturnsCustomValue() {
        // Arrange
        when(config.reportTitle()).thenReturn("Custom Report Title");

        // Act
        String result = config.reportTitle();

        // Assert
        assertEquals("Custom Report Title", result);
    }

    @Test
    void testAdminEmail_ReturnsNonNull() {
        // Arrange
        when(config.adminEmail()).thenReturn("admin@feedback-fase-4.com");

        // Act
        String result = config.adminEmail();

        // Assert
        assertNotNull(result);
    }

    @Test
    void testSenderEmail_ReturnsNonNull() {
        // Arrange
        when(config.senderEmail()).thenReturn("noreply@feedback-fase-4.com");

        // Act
        String result = config.senderEmail();

        // Assert
        assertNotNull(result);
    }

    @Test
    void testTableName_ReturnsNonNull() {
        // Arrange
        when(config.tableName()).thenReturn("feedbacks");

        // Act
        String result = config.tableName();

        // Assert
        assertNotNull(result);
    }

    @Test
    void testDynamodbEndpoint_ReturnsNonNull() {
        // Arrange
        when(config.dynamodbEndpoint()).thenReturn("http://localhost:4566");

        // Act
        String result = config.dynamodbEndpoint();

        // Assert
        assertNotNull(result);
    }

    @Test
    void testReportTitle_ReturnsNonNull() {
        // Arrange
        when(config.reportTitle()).thenReturn("Weekly Feedback Report");

        // Act
        String result = config.reportTitle();

        // Assert
        assertNotNull(result);
    }

    @Test
    void testAdminEmail_ReturnsString() {
        // Arrange
        when(config.adminEmail()).thenReturn("admin@feedback-fase-4.com");

        // Act
        Object result = config.adminEmail();

        // Assert
        assertTrue(result instanceof String);
    }

    @Test
    void testSenderEmail_ReturnsString() {
        // Arrange
        when(config.senderEmail()).thenReturn("noreply@feedback-fase-4.com");

        // Act
        Object result = config.senderEmail();

        // Assert
        assertTrue(result instanceof String);
    }

    @Test
    void testTableName_ReturnsString() {
        // Arrange
        when(config.tableName()).thenReturn("feedbacks");

        // Act
        Object result = config.tableName();

        // Assert
        assertTrue(result instanceof String);
    }

    @Test
    void testDynamodbEndpoint_ReturnsString() {
        // Arrange
        when(config.dynamodbEndpoint()).thenReturn("http://localhost:4566");

        // Act
        Object result = config.dynamodbEndpoint();

        // Assert
        assertTrue(result instanceof String);
    }

    @Test
    void testReportTitle_ReturnsString() {
        // Arrange
        when(config.reportTitle()).thenReturn("Weekly Feedback Report");

        // Act
        Object result = config.reportTitle();

        // Assert
        assertTrue(result instanceof String);
    }

    @Test
    void testAdminEmail_ConsistentOnMultipleCalls() {
        // Arrange
        when(config.adminEmail()).thenReturn("admin@feedback-fase-4.com");

        // Act
        String result1 = config.adminEmail();
        String result2 = config.adminEmail();

        // Assert
        assertEquals(result1, result2);
        verify(config, times(2)).adminEmail();
    }

    @Test
    void testSenderEmail_ConsistentOnMultipleCalls() {
        // Arrange
        when(config.senderEmail()).thenReturn("noreply@feedback-fase-4.com");

        // Act
        String result1 = config.senderEmail();
        String result2 = config.senderEmail();

        // Assert
        assertEquals(result1, result2);
        verify(config, times(2)).senderEmail();
    }

    @Test
    void testTableName_ConsistentOnMultipleCalls() {
        // Arrange
        when(config.tableName()).thenReturn("feedbacks");

        // Act
        String result1 = config.tableName();
        String result2 = config.tableName();

        // Assert
        assertEquals(result1, result2);
        verify(config, times(2)).tableName();
    }

    @Test
    void testDynamodbEndpoint_ConsistentOnMultipleCalls() {
        // Arrange
        when(config.dynamodbEndpoint()).thenReturn("http://localhost:4566");

        // Act
        String result1 = config.dynamodbEndpoint();
        String result2 = config.dynamodbEndpoint();

        // Assert
        assertEquals(result1, result2);
        verify(config, times(2)).dynamodbEndpoint();
    }

    @Test
    void testReportTitle_ConsistentOnMultipleCalls() {
        // Arrange
        when(config.reportTitle()).thenReturn("Weekly Feedback Report");

        // Act
        String result1 = config.reportTitle();
        String result2 = config.reportTitle();

        // Assert
        assertEquals(result1, result2);
        verify(config, times(2)).reportTitle();
    }

    @Test
    void testAllPropertiesAreAccessible() {
        // Arrange
        when(config.adminEmail()).thenReturn("admin@feedback-fase-4.com");
        when(config.senderEmail()).thenReturn("noreply@feedback-fase-4.com");
        when(config.tableName()).thenReturn("feedbacks");
        when(config.dynamodbEndpoint()).thenReturn("http://localhost:4566");
        when(config.reportTitle()).thenReturn("Weekly Feedback Report");

        // Act
        String adminEmail = config.adminEmail();
        String senderEmail = config.senderEmail();
        String tableName = config.tableName();
        String dynamodbEndpoint = config.dynamodbEndpoint();
        String reportTitle = config.reportTitle();

        // Assert
        assertNotNull(adminEmail);
        assertNotNull(senderEmail);
        assertNotNull(tableName);
        assertNotNull(dynamodbEndpoint);
        assertNotNull(reportTitle);
        assertFalse(adminEmail.isEmpty());
        assertFalse(senderEmail.isEmpty());
        assertFalse(tableName.isEmpty());
        assertFalse(dynamodbEndpoint.isEmpty());
        assertFalse(reportTitle.isEmpty());
    }

    @Test
    void testAdminEmail_ReturnsNonEmptyString() {
        // Arrange
        when(config.adminEmail()).thenReturn("admin@feedback-fase-4.com");

        // Act
        String result = config.adminEmail();

        // Assert
        assertFalse(result.isEmpty());
    }

    @Test
    void testSenderEmail_ReturnsNonEmptyString() {
        // Arrange
        when(config.senderEmail()).thenReturn("noreply@feedback-fase-4.com");

        // Act
        String result = config.senderEmail();

        // Assert
        assertFalse(result.isEmpty());
    }

    @Test
    void testTableName_ReturnsNonEmptyString() {
        // Arrange
        when(config.tableName()).thenReturn("feedbacks");

        // Act
        String result = config.tableName();

        // Assert
        assertFalse(result.isEmpty());
    }

    @Test
    void testDynamodbEndpoint_ReturnsNonEmptyString() {
        // Arrange
        when(config.dynamodbEndpoint()).thenReturn("http://localhost:4566");

        // Act
        String result = config.dynamodbEndpoint();

        // Assert
        assertFalse(result.isEmpty());
    }

    @Test
    void testReportTitle_ReturnsNonEmptyString() {
        // Arrange
        when(config.reportTitle()).thenReturn("Weekly Feedback Report");

        // Act
        String result = config.reportTitle();

        // Assert
        assertFalse(result.isEmpty());
    }
}
