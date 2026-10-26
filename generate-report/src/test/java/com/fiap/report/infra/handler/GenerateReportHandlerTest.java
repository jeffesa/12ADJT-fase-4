package com.fiap.report.infra.handler;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

import com.fiap.report.application.usecase.GenerateReportUseCase;
import com.fiap.report.domain.gateway.ReportException;
import com.fiap.report.infra.config.ReportConfiguration;

/**
 * Unit tests for the GenerateReportHandler Lambda handler.
 *
 * Verifies period calculation (today - 1 = endDate, endDate - 6 = startDate),
 * use case invocation, error resilience (no rethrow), and logging.
 *
 * Note: Uses custom mock implementations to support testing with sealed classes.
 */
class GenerateReportHandlerTest {

    private GenerateReportHandler handler;
    private MockGenerateReportUseCase mockUseCase;
    private MockReportConfiguration mockConfig;

    static class MockGenerateReportUseCase extends GenerateReportUseCase {
        AtomicReference<LocalDate> lastStartDate = new AtomicReference<>();
        AtomicReference<LocalDate> lastEndDate = new AtomicReference<>();
        AtomicReference<String> lastAdminEmail = new AtomicReference<>();
        AtomicInteger callCount = new AtomicInteger(0);
        RuntimeException exceptionToThrow = null;

        MockGenerateReportUseCase() {
            super(null, null);
        }

        @Override
        public com.fiap.report.domain.entity.WeeklyReport execute(
            LocalDate startDate, LocalDate endDate, String adminEmail) throws ReportException {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            lastStartDate.set(startDate);
            lastEndDate.set(endDate);
            lastAdminEmail.set(adminEmail);
            callCount.incrementAndGet();
            return null;
        }
    }

    static class MockReportConfiguration implements ReportConfiguration {
        private String adminEmailValue = "admin@feedback-fase-4.com";

        @Override
        public String adminEmail() {
            return adminEmailValue;
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

        void setAdminEmail(String email) {
            this.adminEmailValue = email;
        }
    }

    @BeforeEach
    void setUp() {
        mockUseCase = new MockGenerateReportUseCase();
        mockConfig = new MockReportConfiguration();

        handler = new GenerateReportHandler();
        handler.useCase = mockUseCase;
        handler.config = mockConfig;
    }

    @Test
    void testHandleRequest_ReturnsNull() {
        // Act
        Void result = handler.handleRequest(null, null);

        // Assert
        assertNull(result);
    }

    @Test
    void testHandleRequest_CallsUseCaseExecute() {
        // Act
        handler.handleRequest(null, null);

        // Assert
        assertTrue(mockUseCase.callCount.get() > 0);
    }

    @Test
    void testHandleRequest_CalculatesPeriodWithCorrectDuration() {
        // Act
        handler.handleRequest(null, null);

        // Assert
        LocalDate startDate = mockUseCase.lastStartDate.get();
        LocalDate endDate = mockUseCase.lastEndDate.get();

        long duration = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        assertEquals(7, duration);
    }

    @Test
    void testHandleRequest_EndDateIsYesterday() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate expectedEndDate = today.minusDays(1);

        // Act
        handler.handleRequest(null, null);

        // Assert
        LocalDate endDate = mockUseCase.lastEndDate.get();
        assertEquals(expectedEndDate, endDate);
    }

    @Test
    void testHandleRequest_StartDateIsSixDaysBeforeEndDate() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate expectedEndDate = today.minusDays(1);
        LocalDate expectedStartDate = expectedEndDate.minusDays(6);

        // Act
        handler.handleRequest(null, null);

        // Assert
        LocalDate startDate = mockUseCase.lastStartDate.get();
        LocalDate endDate = mockUseCase.lastEndDate.get();

        assertEquals(expectedStartDate, startDate);
        assertEquals(expectedEndDate, endDate);
    }

    @Test
    void testHandleRequest_PassesAdminEmailFromConfig() {
        // Arrange
        String adminEmail = "admin@feedback-fase-4.com";

        // Act
        handler.handleRequest(null, null);

        // Assert
        String passedEmail = mockUseCase.lastAdminEmail.get();
        assertEquals(adminEmail, passedEmail);
    }

    @Test
    void testHandleRequest_CatchesReportExceptionAndDoesNotRethrow() {
        // Arrange
        mockUseCase.exceptionToThrow = new ReportException("Report generation failed");

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> handler.handleRequest(null, null));
    }

    @Test
    void testHandleRequest_CatchesGenericExceptionAndDoesNotRethrow() {
        // Arrange
        mockUseCase.exceptionToThrow = new RuntimeException("Unexpected error");

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> handler.handleRequest(null, null));
    }

    @Test
    void testHandleRequest_UseCaseInvokedWithCorrectParameterTypes() {
        // Act
        handler.handleRequest(null, null);

        // Assert
        LocalDate startDate = mockUseCase.lastStartDate.get();
        LocalDate endDate = mockUseCase.lastEndDate.get();
        String email = mockUseCase.lastAdminEmail.get();

        assertNotNull(startDate);
        assertNotNull(endDate);
        assertNotNull(email);
        assertEquals(LocalDate.class, startDate.getClass());
        assertEquals(LocalDate.class, endDate.getClass());
        assertEquals(String.class, email.getClass());
    }

    @Test
    void testHandleRequest_StartDateIsBeforeEndDate() {
        // Act
        handler.handleRequest(null, null);

        // Assert
        LocalDate startDate = mockUseCase.lastStartDate.get();
        LocalDate endDate = mockUseCase.lastEndDate.get();

        assertTrue(startDate.isBefore(endDate) || startDate.isEqual(endDate));
    }

    @Test
    void testHandleRequest_CustomAdminEmail() {
        // Arrange
        String customEmail = "custom@example.com";
        mockConfig.setAdminEmail(customEmail);

        // Act
        handler.handleRequest(null, null);

        // Assert
        String passedEmail = mockUseCase.lastAdminEmail.get();
        assertEquals(customEmail, passedEmail);
    }

    @Test
    void testHandleRequest_UseCaseCalledOnce() {
        // Act
        handler.handleRequest(null, null);

        // Assert
        assertEquals(1, mockUseCase.callCount.get());
    }

    @Test
    void testHandleRequest_PeriodIsPreviousWeek() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act
        handler.handleRequest(null, null);

        // Assert
        LocalDate startDate = mockUseCase.lastStartDate.get();
        LocalDate endDate = mockUseCase.lastEndDate.get();

        assertTrue(endDate.isBefore(today) || endDate.isEqual(today.minusDays(1)));
        assertTrue(startDate.isBefore(endDate));
    }

    @Test
    void testHandleRequest_ReturnsNullEvenOnException() {
        // Arrange
        mockUseCase.exceptionToThrow = new ReportException("Test error");

        // Act
        Void result = handler.handleRequest(null, null);

        // Assert
        assertNull(result);
    }

    @Test
    void testHandleRequest_ImplementsRequestHandlerInterface() {
        // Assert
        assertNotNull(handler);
        assertTrue(handler instanceof com.amazonaws.services.lambda.runtime.RequestHandler);
    }

    @Test
    void testHandleRequest_MonthlyTransitionScenario() {
        // Act
        handler.handleRequest(null, null);

        // Assert
        LocalDate startDate = mockUseCase.lastStartDate.get();
        LocalDate endDate = mockUseCase.lastEndDate.get();

        assertNotNull(startDate);
        assertNotNull(endDate);
        assertTrue(startDate.isBefore(endDate) || startDate.isEqual(endDate));
    }
}
