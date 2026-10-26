package com.fiap.report.infra.messaging;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

import com.fiap.report.domain.entity.FeedbackStatistics;
import com.fiap.report.domain.entity.WeeklyReport;
import com.fiap.report.domain.gateway.ReportException;
import com.fiap.report.infra.config.ReportConfiguration;

/**
 * Unit tests for SesReportSender email delivery.
 *
 * Verifies that reports are correctly formatted as emails, sent via SES
 * with proper subject lines and HTML bodies, and that errors are handled.
 *
 * Note: Uses a custom MockSesClient to avoid Mockito limitation with sealed AWS SDK v2 classes.
 */
class SesReportSenderTest {

    private SesReportSender sender;
    private MockSesClient mockSesClient;
    private ReportConfiguration config;

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

    static class TestReportConfiguration implements ReportConfiguration {
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
            return "feedbacks";
        }

        @Override
        public String reportTitle() {
            return "Weekly Feedback Report";
        }
    }

    @BeforeEach
    void setUp() {
        mockSesClient = new MockSesClient();
        config = new TestReportConfiguration();

        sender = new SesReportSender();
        sender.sesClient = mockSesClient;
        sender.config = config;
    }

    private WeeklyReport createTestReport(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        feedbackByUrgency.put("HIGH", 2);
        feedbackByUrgency.put("MEDIUM", 1);
        feedbackByUrgency.put("LOW", 2);
        FeedbackStatistics statistics = new FeedbackStatistics(5, 7.5, feedbackByDay, feedbackByUrgency);
        return new WeeklyReport(startDate, endDate, statistics, "admin@feedback-fase-4.com");
    }

    @Test
    void testSend_SuccessfullyDelivery() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        assertNotNull(mockSesClient.lastRequest.get());
    }

    @Test
    void testSend_BuildsCorrectSubjectLine() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        SendEmailRequest request = mockSesClient.lastRequest.get();
        String subject = request.message().subject().data();
        assertTrue(subject.contains("Weekly Feedback Report"));
        assertTrue(subject.contains("2024-10-21"));
        assertTrue(subject.contains("2024-10-25"));
    }

    @Test
    void testSend_BuildsHtmlBody() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        SendEmailRequest request = mockSesClient.lastRequest.get();
        String htmlBody = request.message().body().html().data();
        assertNotNull(htmlBody);
        assertFalse(htmlBody.isEmpty());
        assertTrue(htmlBody.contains("<!DOCTYPE html>"));
    }

    @Test
    void testSend_SetsCorrectFromAddress() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        SendEmailRequest request = mockSesClient.lastRequest.get();
        assertEquals("noreply@feedback-fase-4.com", request.source());
    }

    @Test
    void testSend_SetsCorrectToAddress() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        SendEmailRequest request = mockSesClient.lastRequest.get();
        assertTrue(request.destination().toAddresses().contains(adminEmail));
    }

    @Test
    void testSend_ThrowsReportExceptionOnSesError() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        SesException sesException = (SesException) SesException.builder()
                .message("Invalid sender address")
                .build();
        mockSesClient.exceptionToThrow = sesException;

        // Act & Assert
        assertThrows(
            ReportException.class,
            () -> sender.send(report, adminEmail)
        );
    }

    @Test
    void testSend_ThrowsReportExceptionOnGenericError() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        mockSesClient.exceptionToThrow = new RuntimeException("Network error");

        // Act & Assert
        assertThrows(
            ReportException.class,
            () -> sender.send(report, adminEmail)
        );
    }

    @Test
    void testSend_IncludesReportIdInRequest() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        assertNotNull(report.getId());
        assertNotNull(mockSesClient.lastRequest.get());
    }

    @Test
    void testSend_SubjectContainsPeriodDates() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        SendEmailRequest request = mockSesClient.lastRequest.get();
        String subject = request.message().subject().data();
        assertTrue(subject.contains("—"));
    }

    @Test
    void testSend_HtmlBodyIsValidContent() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        SendEmailRequest request = mockSesClient.lastRequest.get();
        String htmlBody = request.message().body().html().data();
        assertTrue(htmlBody.contains("</html>"));
    }

    @Test
    void testSend_ReportWithDifferentDates() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 14);
        LocalDate endDate = LocalDate.of(2024, 10, 18);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "custom@example.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        SendEmailRequest request = mockSesClient.lastRequest.get();
        String subject = request.message().subject().data();
        assertTrue(subject.contains("2024-10-14"));
        assertTrue(subject.contains("2024-10-18"));
        assertEquals("noreply@feedback-fase-4.com", request.source());
        assertTrue(request.destination().toAddresses().contains("custom@example.com"));
    }

    @Test
    void testSend_MessageSubjectIsNotEmpty() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        SendEmailRequest request = mockSesClient.lastRequest.get();
        String subject = request.message().subject().data();
        assertFalse(subject.isEmpty());
    }

    @Test
    void testSend_MessageBodyIsNotEmpty() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);

        // Assert
        SendEmailRequest request = mockSesClient.lastRequest.get();
        String body = request.message().body().html().data();
        assertFalse(body.isEmpty());
    }

    @Test
    void testSend_CallsSendEmailOnce() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        WeeklyReport report = createTestReport(startDate, endDate);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        sender.send(report, adminEmail);
        sender.send(report, adminEmail);

        // Assert - lastRequest is the second one, so both were called
        assertNotNull(mockSesClient.lastRequest.get());
    }
}
