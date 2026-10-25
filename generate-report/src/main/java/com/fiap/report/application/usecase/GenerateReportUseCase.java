package com.fiap.report.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fiap.report.domain.entity.Feedback;
import com.fiap.report.domain.entity.FeedbackStatistics;
import com.fiap.report.domain.entity.WeeklyReport;
import com.fiap.report.domain.gateway.ReportException;
import com.fiap.report.domain.gateway.ReportGateway;
import com.fiap.report.domain.gateway.ReportSender;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Use case for generating and sending weekly feedback reports.
 * 
 * This use case orchestrates the business logic for:
 * - Validating the report period (start date must not exceed end date)
 * - Retrieving feedbacks for the specified period via the gateway
 * - Aggregating feedback statistics (total count, average rating, grouping by day and urgency)
 * - Creating a WeeklyReport entity with aggregated metrics
 * - Sending the report via the configured sender
 * 
 * The class is marked as @ApplicationScoped (CDI singleton) to ensure
 * a single instance is shared across the application.
 */
@ApplicationScoped
public class GenerateReportUseCase {

    private static final Logger LOGGER = Logger.getLogger(GenerateReportUseCase.class.getName());

    private final ReportGateway reportGateway;
    private final ReportSender reportSender;

    /**
     * Constructor for dependency injection.
     * 
     * All dependencies are injected and stored as final fields to ensure
     * immutability and thread-safety.
     * 
     * @param reportGateway gateway for retrieving feedbacks from persistence
     * @param reportSender gateway for sending generated reports
     */
    @Inject
    public GenerateReportUseCase(ReportGateway reportGateway, ReportSender reportSender) {
        this.reportGateway = reportGateway;
        this.reportSender = reportSender;
    }

    /**
     * Generates a weekly report for the specified period and sends it.
     * 
     * This method:
     * 1. Validates that startDate <= endDate
     * 2. Retrieves all feedbacks within the period from the gateway
     * 3. Aggregates statistics: count, average rating, grouping by day and urgency
     * 4. Creates a WeeklyReport entity with the aggregated data
     * 5. Sends the report via the configured sender
     * 6. Returns the generated report
     * 
     * @param startDate the beginning of the report period (inclusive)
     * @param endDate the end of the report period (inclusive)
     * @param adminEmail the email address of the administrator to receive the report
     * @return the generated WeeklyReport
     * @throws IllegalArgumentException if startDate > endDate
     * @throws ReportException if retrieval or sending fails
     */
    public WeeklyReport execute(LocalDate startDate, LocalDate endDate, String adminEmail) {
        // Validate period: startDate <= endDate
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                "Start date must be less than or equal to end date. Start: " + startDate + ", End: " + endDate
            );
        }

        LOGGER.log(Level.INFO, "Generating report for period " + startDate + " to " + endDate);

        // Retrieve feedbacks for the period
        List<Feedback> feedbacks = reportGateway.findByPeriod(startDate, endDate);

        LOGGER.log(Level.FINE, "Found " + feedbacks.size() + " feedbacks for period");

        // Aggregate statistics
        FeedbackStatistics statistics = aggregateStatistics(feedbacks);

        LOGGER.log(Level.FINE, "Aggregated statistics: total=" + statistics.getTotalFeedback() +
                ", average rating=" + statistics.getAverageRating() +
                ", by day=" + statistics.getFeedbackByDay() +
                ", by urgency=" + statistics.getFeedbackByUrgency());

        // Create WeeklyReport
        WeeklyReport report = new WeeklyReport(startDate, endDate, statistics, adminEmail);

        // Send report
        try {
            reportSender.send(report, adminEmail);
            LOGGER.log(Level.INFO, "Report generated and sent successfully");
        } catch (ReportException e) {
            LOGGER.log(Level.SEVERE, "Error generating report: " + e.getMessage(), e);
            throw e;
        }

        return report;
    }

    /**
     * Aggregates feedback data into statistics.
     * 
     * Computes:
     * - Total feedback count
     * - Average rating (null if no feedbacks)
     * - Feedback count per day
     * - Feedback count per urgency level (HIGH, MEDIUM, LOW)
     * 
     * @param feedbacks list of feedbacks to aggregate
     * @return aggregated FeedbackStatistics
     */
    private FeedbackStatistics aggregateStatistics(List<Feedback> feedbacks) {
        int totalFeedback = feedbacks.size();
        
        // Calculate average rating
        Double averageRating = null;
        if (totalFeedback > 0) {
            int sum = 0;
            for (Feedback feedback : feedbacks) {
                sum += feedback.getRating();
            }
            averageRating = (double) sum / totalFeedback;
        }

        // Group by day
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        for (Feedback feedback : feedbacks) {
            LocalDate day = feedback.getCreatedAt().toLocalDate();
            feedbackByDay.put(day, feedbackByDay.getOrDefault(day, 0) + 1);
        }

        // Group by urgency
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        feedbackByUrgency.put("HIGH", 0);
        feedbackByUrgency.put("MEDIUM", 0);
        feedbackByUrgency.put("LOW", 0);
        
        for (Feedback feedback : feedbacks) {
            String urgencyName = feedback.getUrgency().name();
            feedbackByUrgency.put(urgencyName, feedbackByUrgency.get(urgencyName) + 1);
        }

        return new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);
    }
}
