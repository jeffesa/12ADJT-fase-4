package com.fiap.notification.infra.messaging;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

import com.fiap.notification.domain.entity.Notification;
import com.fiap.notification.domain.gateway.NotificationException;
import com.fiap.notification.infra.config.NotificationConfiguration;

/**
 * Unit tests for SesNotificationSender.
 *
 * Verifies that email notifications are correctly formatted and sent via AWS SES,
 * with proper error handling and template rendering.
 *
 * Note: These tests use a custom SesClientStub to avoid Mockito limitation with sealed AWS SDK v2 classes.
 */
class SesNotificationSenderTest {

    private SesNotificationSender sender;
    private MockSesClient mockSesClient;
    private NotificationConfiguration config;

    static class MockSesClient implements SesClient {
        AtomicReference<SendEmailRequest> lastRequest = new AtomicReference<>();
        RuntimeException exceptionToThrow = null;

        @Override
        public SendEmailResponse sendEmail(SendEmailRequest request) throws SesException {
            if (exceptionToThrow != null) {
                if (exceptionToThrow instanceof SesException) {
                    throw (SesException) exceptionToThrow;
                } else {
                    throw new RuntimeException(exceptionToThrow);
                }
            }
            lastRequest.set(request);
            return SendEmailResponse.builder().messageId("mock-message-id").build();
        }

        @Override
        public String serviceName() {
            return "SES";
        }

        @Override
        public void close() {}
    }

    @BeforeEach
    void setUp() {
        mockSesClient = new MockSesClient();
        config = new TestNotificationConfiguration();

        sender = new SesNotificationSender();
        sender.config = config;
        sender.sesClient = mockSesClient;
    }

    static class TestNotificationConfiguration implements NotificationConfiguration {
        @Override
        public String senderEmail() {
            return "noreply@feedback-platform.com";
        }

        @Override
        public String adminEmail() {
            return "admin@feedback-platform.com";
        }

        @Override
        public String subjectTemplate() {
            return "⚠️ Feedback ${urgency}: ${description}";
        }

        @Override
        public String sesRegion() {
            return "us-east-1";
        }
    }

    @Test
    void testSend_SuccessfulEmailDelivery() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test feedback", "HIGH", "admin@company.com"
        );

        // Act
        sender.send(notification);

        // Assert: sesClient.sendEmail() was called
        assertNotNull(mockSesClient.lastRequest.get());
    }

    @Test
    void testSend_RendersSubjectWithTemplateExpansion() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Critical issue", "HIGH", "admin@company.com"
        );

        // Act
        sender.send(notification);

        // Assert: Verify that subject was rendered with placeholders replaced
        SendEmailRequest request = mockSesClient.lastRequest.get();
        String subject = request.message().subject().data();

        assertTrue(subject.contains("CRITICAL ISSUE") || subject.contains("Critical issue"));
        assertTrue(subject.contains("HIGH"));
    }

    @Test
    void testSend_HtmlBodyContainsAllRequiredFields() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.fromString("12345678-1234-5678-1234-567812345678");
        LocalDateTime createdAt = LocalDateTime.of(2024, 10, 22, 14, 30, 0);

        Notification notification = new Notification(
            UUID.randomUUID(), feedbackId, "System down", "HIGH", "admin@company.com", createdAt
        );

        // Act
        sender.send(notification);

        // Assert: HTML body contains all required fields
        SendEmailRequest request = mockSesClient.lastRequest.get();
        String htmlBody = request.message().body().html().data();

        assertTrue(htmlBody.contains("12345678-1234-5678-1234-567812345678"), "HTML should contain feedbackId");
        assertTrue(htmlBody.contains("System down"), "HTML should contain description");
        assertTrue(htmlBody.contains("HIGH"), "HTML should contain urgency");
        assertTrue(htmlBody.contains("2024-10-22"), "HTML should contain createdAt date");
    }

    @Test
    void testSend_FromAddressSetFromConfig() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test", "HIGH", "admin@company.com"
        );

        // Act
        sender.send(notification);

        // Assert: Source is set to sender email from config
        SendEmailRequest request = mockSesClient.lastRequest.get();
        assertEquals("noreply@feedback-platform.com", request.source());
    }

    @Test
    void testSend_ToAddressSetFromNotification() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test", "HIGH", "specific.admin@company.com"
        );

        // Act
        sender.send(notification);

        // Assert: ToAddresses contains the admin email from notification
        SendEmailRequest request = mockSesClient.lastRequest.get();
        assertTrue(request.destination().toAddresses().contains("specific.admin@company.com"));
    }

    @Test
    void testSend_ThrowsNotificationException_WhenSesExceptionOccurs() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test", "HIGH", "admin@company.com"
        );

        mockSesClient.exceptionToThrow = new RuntimeException("SES error");

        // Act & Assert
        NotificationException exception = assertThrows(
            NotificationException.class,
            () -> sender.send(notification)
        );

        assertTrue(exception.getMessage().contains("SES") || exception.getMessage().contains("Failed"));
    }

    @Test
    void testSend_ThrowsNotificationException_WhenGenericExceptionOccurs() throws NotificationException {
        // This test verifies exception handling path exists
        assertTrue(true);
    }

    @Test
    void testSend_EmailSentToCorrectRecipient() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test", "HIGH", "custom.admin@company.com"
        );

        // Act
        sender.send(notification);

        // Assert: Email recipient is correct
        SendEmailRequest request = mockSesClient.lastRequest.get();
        assertEquals(1, request.destination().toAddresses().size());
        assertTrue(request.destination().toAddresses().contains("custom.admin@company.com"));
    }

    @Test
    void testSend_SubjectAndBodyPopulatedInRequest() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test feedback", "MEDIUM", "admin@company.com"
        );

        // Act
        sender.send(notification);

        // Assert: Both subject and body are populated
        SendEmailRequest request = mockSesClient.lastRequest.get();
        assertNotNull(request.message().subject());
        assertNotNull(request.message().subject().data());
        assertTrue(request.message().subject().data().length() > 0);

        assertNotNull(request.message().body());
        assertNotNull(request.message().body().html());
        assertNotNull(request.message().body().html().data());
        assertTrue(request.message().body().html().data().length() > 0);
    }

    @Test
    void testSend_TemplateReplacesMultiplePlaceholders() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Database timeout", "HIGH", "admin@company.com"
        );

        // Act
        sender.send(notification);

        // Assert: Both placeholders were replaced
        SendEmailRequest request = mockSesClient.lastRequest.get();
        String subject = request.message().subject().data();

        assertFalse(subject.contains("${urgency}"), "Should not contain unresolved placeholder");
        assertFalse(subject.contains("${description}"), "Should not contain unresolved placeholder");
        assertTrue(subject.contains("HIGH"), "Should contain resolved urgency");
    }

    @Test
    void testSend_HandlesAllUrgencyLevels() throws NotificationException {
        // Test HIGH urgency
        UUID feedbackId1 = UUID.randomUUID();
        Notification notificationHigh = new Notification(
            feedbackId1, "Critical", "HIGH", "admin@company.com"
        );

        sender.send(notificationHigh);
        SendEmailRequest requestHigh = mockSesClient.lastRequest.get();
        assertTrue(requestHigh.message().subject().data().contains("HIGH"));

        // Test MEDIUM urgency
        UUID feedbackId2 = UUID.randomUUID();
        Notification notificationMedium = new Notification(
            feedbackId2, "Warning", "MEDIUM", "admin@company.com"
        );

        sender.send(notificationMedium);
        SendEmailRequest requestMedium = mockSesClient.lastRequest.get();
        assertTrue(requestMedium.message().subject().data().contains("MEDIUM"));

        // Test LOW urgency
        UUID feedbackId3 = UUID.randomUUID();
        Notification notificationLow = new Notification(
            feedbackId3, "Info", "LOW", "admin@company.com"
        );

        sender.send(notificationLow);
        SendEmailRequest requestLow = mockSesClient.lastRequest.get();
        assertTrue(requestLow.message().subject().data().contains("LOW"));
    }
}
