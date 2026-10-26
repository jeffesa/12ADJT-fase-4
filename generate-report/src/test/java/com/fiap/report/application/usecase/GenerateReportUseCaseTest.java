package com.fiap.report.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fiap.report.domain.entity.Feedback;
import com.fiap.report.domain.entity.FeedbackStatistics;
import com.fiap.report.domain.entity.Urgency;
import com.fiap.report.domain.entity.WeeklyReport;
import com.fiap.report.domain.gateway.ReportException;
import com.fiap.report.domain.gateway.ReportGateway;
import com.fiap.report.domain.gateway.ReportSender;

/**
 * Unit tests for the GenerateReportUseCase orchestration logic.
 *
 * Verifies period validation, gateway/sender invocation, and correct aggregation
 * of feedback statistics (total count, average rating, grouping by day and urgency).
 */
class GenerateReportUseCaseTest {

    @Mock
    private ReportGateway reportGateway;

    @Mock
    private ReportSender reportSender;

    private GenerateReportUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GenerateReportUseCase(reportGateway, reportSender);
    }

    @Test
    void testExecute_SuccessfullyGeneratesAndSendsReport() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback("1", 8, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 10, 0)));
        feedbacks.add(new Feedback("2", 9, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 14, 0)));
        feedbacks.add(new Feedback("3", 7, Urgency.MEDIUM, LocalDateTime.of(2024, 10, 22, 10, 0)));

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(feedbacks);

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert
        assertNotNull(report);
        assertEquals(startDate, report.getPeriod().getStartDate());
        assertEquals(endDate, report.getPeriod().getEndDate());
        assertEquals(adminEmail, report.getAdminEmail());
        verify(reportGateway).findByPeriod(startDate, endDate);
        verify(reportSender).send(report, adminEmail);
    }

    @Test
    void testExecute_CallsGatewayWithCorrectDates() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(new ArrayList<>());

        // Act
        useCase.execute(startDate, endDate, adminEmail);

        // Assert
        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(reportGateway).findByPeriod(startCaptor.capture(), endCaptor.capture());

        assertEquals(startDate, startCaptor.getValue());
        assertEquals(endDate, endCaptor.getValue());
    }

    @Test
    void testExecute_AggregatesCorrectTotalFeedback() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback("1", 8, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 10, 0)));
        feedbacks.add(new Feedback("2", 9, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 14, 0)));
        feedbacks.add(new Feedback("3", 7, Urgency.MEDIUM, LocalDateTime.of(2024, 10, 22, 10, 0)));
        feedbacks.add(new Feedback("4", 6, Urgency.HIGH, LocalDateTime.of(2024, 10, 23, 10, 0)));
        feedbacks.add(new Feedback("5", 5, Urgency.HIGH, LocalDateTime.of(2024, 10, 24, 10, 0)));

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(feedbacks);

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert
        assertEquals(5, report.getStatistics().getTotalFeedback());
    }

    @Test
    void testExecute_CalculatesCorrectAverageRating() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback("1", 8, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 10, 0)));
        feedbacks.add(new Feedback("2", 9, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 14, 0)));
        feedbacks.add(new Feedback("3", 7, Urgency.MEDIUM, LocalDateTime.of(2024, 10, 22, 10, 0)));
        feedbacks.add(new Feedback("4", 6, Urgency.HIGH, LocalDateTime.of(2024, 10, 23, 10, 0)));
        feedbacks.add(new Feedback("5", 10, Urgency.LOW, LocalDateTime.of(2024, 10, 24, 10, 0)));

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(feedbacks);

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert - average: (8+9+7+6+10)/5 = 40/5 = 8.0
        assertEquals(8.0, report.getStatistics().getAverageRating());
    }

    @Test
    void testExecute_AggregatesFeedbackByDay() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback("1", 8, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 10, 0)));
        feedbacks.add(new Feedback("2", 9, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 14, 0)));
        feedbacks.add(new Feedback("3", 7, Urgency.MEDIUM, LocalDateTime.of(2024, 10, 22, 10, 0)));
        feedbacks.add(new Feedback("4", 6, Urgency.HIGH, LocalDateTime.of(2024, 10, 22, 15, 0)));
        feedbacks.add(new Feedback("5", 5, Urgency.HIGH, LocalDateTime.of(2024, 10, 23, 10, 0)));

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(feedbacks);

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert
        Map<LocalDate, Integer> feedbackByDay = report.getStatistics().getFeedbackByDay();
        assertEquals(2, feedbackByDay.get(LocalDate.of(2024, 10, 21)));
        assertEquals(2, feedbackByDay.get(LocalDate.of(2024, 10, 22)));
        assertEquals(1, feedbackByDay.get(LocalDate.of(2024, 10, 23)));
    }

    @Test
    void testExecute_AggregatesFeedbackByUrgency() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback("1", 8, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 10, 0)));
        feedbacks.add(new Feedback("2", 9, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 14, 0)));
        feedbacks.add(new Feedback("3", 9, Urgency.LOW, LocalDateTime.of(2024, 10, 22, 10, 0)));
        feedbacks.add(new Feedback("4", 7, Urgency.MEDIUM, LocalDateTime.of(2024, 10, 22, 15, 0)));
        feedbacks.add(new Feedback("5", 6, Urgency.MEDIUM, LocalDateTime.of(2024, 10, 23, 10, 0)));
        feedbacks.add(new Feedback("6", 5, Urgency.HIGH, LocalDateTime.of(2024, 10, 23, 14, 0)));
        feedbacks.add(new Feedback("7", 4, Urgency.HIGH, LocalDateTime.of(2024, 10, 24, 10, 0)));
        feedbacks.add(new Feedback("8", 3, Urgency.HIGH, LocalDateTime.of(2024, 10, 24, 15, 0)));
        feedbacks.add(new Feedback("9", 2, Urgency.HIGH, LocalDateTime.of(2024, 10, 25, 10, 0)));

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(feedbacks);

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert
        Map<String, Integer> feedbackByUrgency = report.getStatistics().getFeedbackByUrgency();
        assertEquals(3, feedbackByUrgency.get("LOW"));
        assertEquals(2, feedbackByUrgency.get("MEDIUM"));
        assertEquals(4, feedbackByUrgency.get("HIGH"));
    }

    @Test
    void testExecute_HandlesEmptyFeedbackList() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(new ArrayList<>());

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert
        assertEquals(0, report.getStatistics().getTotalFeedback());
        assertNull(report.getStatistics().getAverageRating());
        assertTrue(report.getStatistics().getFeedbackByDay().isEmpty());
        assertEquals(0, report.getStatistics().getFeedbackByUrgency().get("HIGH"));
        assertEquals(0, report.getStatistics().getFeedbackByUrgency().get("MEDIUM"));
        assertEquals(0, report.getStatistics().getFeedbackByUrgency().get("LOW"));
    }

    @Test
    void testExecute_HandlesSingleFeedback() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback("1", 7, Urgency.MEDIUM, LocalDateTime.of(2024, 10, 21, 10, 0)));

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(feedbacks);

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert
        assertEquals(1, report.getStatistics().getTotalFeedback());
        assertEquals(7.0, report.getStatistics().getAverageRating());
        assertEquals(1, report.getStatistics().getFeedbackByDay().get(LocalDate.of(2024, 10, 21)));
        assertEquals(1, report.getStatistics().getFeedbackByUrgency().get("MEDIUM"));
    }

    @Test
    void testExecute_HandlesAllUrgenciesAbsent() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(new ArrayList<>());

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert
        Map<String, Integer> feedbackByUrgency = report.getStatistics().getFeedbackByUrgency();
        assertEquals(0, feedbackByUrgency.get("HIGH"));
        assertEquals(0, feedbackByUrgency.get("MEDIUM"));
        assertEquals(0, feedbackByUrgency.get("LOW"));
    }

    @Test
    void testExecute_ThrowsIllegalArgumentExceptionWhenStartDateGreaterThanEndDate() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 25);
        LocalDate endDate = LocalDate.of(2024, 10, 21);
        String adminEmail = "admin@feedback-fase-4.com";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(startDate, endDate, adminEmail)
        );
        assertTrue(exception.getMessage().contains("Start date must be less than or equal to end date"));
    }

    @Test
    void testExecute_PropagatesGatewayException() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        ReportException gatewayException = new ReportException("Database connection failed");
        when(reportGateway.findByPeriod(startDate, endDate)).thenThrow(gatewayException);

        // Act & Assert
        assertThrows(
            ReportException.class,
            () -> useCase.execute(startDate, endDate, adminEmail)
        );
    }

    @Test
    void testExecute_PropagateSenderException() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback("1", 8, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 10, 0)));

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(feedbacks);
        
        ReportException senderException = new ReportException("SES service unavailable");
        doThrow(senderException).when(reportSender).send(any(WeeklyReport.class), eq(adminEmail));

        // Act & Assert
        assertThrows(
            ReportException.class,
            () -> useCase.execute(startDate, endDate, adminEmail)
        );
    }

    @Test
    void testExecute_SenderCalledWithCorrectArguments() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        List<Feedback> feedbacks = new ArrayList<>();
        feedbacks.add(new Feedback("1", 8, Urgency.LOW, LocalDateTime.of(2024, 10, 21, 10, 0)));

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(feedbacks);

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert
        ArgumentCaptor<WeeklyReport> reportCaptor = ArgumentCaptor.forClass(WeeklyReport.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(reportSender).send(reportCaptor.capture(), emailCaptor.capture());

        assertEquals(report, reportCaptor.getValue());
        assertEquals(adminEmail, emailCaptor.getValue());
    }

    @Test
    void testExecute_ReturnsReportWithCorrectPeriodBounds() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(new ArrayList<>());

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert
        assertEquals(startDate, report.getPeriod().getStartDate());
        assertEquals(endDate, report.getPeriod().getEndDate());
    }

    @Test
    void testExecute_ReturnsReportWithCorrectAdminEmail() throws ReportException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        String adminEmail = "admin@feedback-fase-4.com";

        when(reportGateway.findByPeriod(startDate, endDate)).thenReturn(new ArrayList<>());

        // Act
        WeeklyReport report = useCase.execute(startDate, endDate, adminEmail);

        // Assert
        assertEquals(adminEmail, report.getAdminEmail());
    }
}
