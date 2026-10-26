package com.fiap.report.infra.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fiap.report.application.usecase.GenerateReportUseCase;
import com.fiap.report.domain.gateway.ReportException;
import com.fiap.report.infra.config.ReportConfiguration;

/**
 * AWS Lambda handler for generating and sending weekly feedback reports.
 *
 * This handler is triggered by an EventBridge cron schedule (Monday 08:00 UTC)
 * and orchestrates the generation and distribution of weekly feedback reports.
 *
 * EventBridge Trigger:
 * - Schedule: Every Monday at 08:00 UTC (cron(0 8 ? * MON *))
 * - Purpose: Generate and email feedback summary for the previous week
 *
 * Period Calculation:
 * - endDate = today().minusDays(1) → Friday of the previous week (or last working day)
 * - startDate = endDate.minusDays(6) → Monday of the previous week
 * - This captures feedback from the prior complete week
 *
 * Example:
 * - If handler runs on Monday 2026-10-26 at 08:00 UTC
 * - endDate = 2026-10-23 (Friday)
 * - startDate = 2026-10-19 (Monday)
 * - Report covers Mon 2026-10-19 to Fri 2026-10-23
 *
 * Error Handling:
 * - ReportException is caught and logged at SEVERE level without rethrowing
 * - Handler continues to completion (resilient behavior)
 * - Entry and exit are logged at INFO level
 * - Handler always returns null (successful Lambda execution)
 *
 * Logging uses java.util.logging for consistency with domain/application layers.
 */
@ApplicationScoped
public class GenerateReportHandler implements RequestHandler<ScheduledEvent, Void> {

    private static final Logger LOGGER = Logger.getLogger(GenerateReportHandler.class.getName());

    @Inject
    GenerateReportUseCase useCase;

    @Inject
    ReportConfiguration config;

    /**
     * Handles scheduled events to generate and send weekly reports.
     *
     * Flow:
     * 1. Log handler invocation at INFO level
     * 2. Calculate report period: endDate = today() - 1, startDate = endDate - 6
     * 3. Read admin email from configuration
     * 4. Invoke GenerateReportUseCase with calculated period and admin email
     * 5. On ReportException: log at SEVERE level and do NOT rethrow (resilient)
     * 6. Log completion at INFO level
     * 7. Return null
     *
     * @param event EventBridge scheduled event (contains timestamp, detail, etc.)
     * @param context Lambda context (unused)
     * @return null (void return type for Lambda)
     */
    @Override
    public Void handleRequest(ScheduledEvent event, Context context) {
        try {
            LOGGER.log(Level.INFO, "Lambda handler invoked for generating and sending report");

            // Calculate period: endDate = today() - 1 day (previous Friday)
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.minusDays(1);
            
            // startDate = endDate - 6 days (Monday of previous week)
            LocalDate startDate = endDate.minusDays(6);

            LOGGER.log(Level.FINE, "Report period calculated: startDate=" + startDate + ", endDate=" + endDate);

            // Read admin email from configuration
            String adminEmail = config.adminEmail();

            // Invoke use case to generate and send report
            useCase.execute(startDate, endDate, adminEmail);

            LOGGER.log(Level.INFO, "Report generation and sending completed successfully");

        } catch (ReportException e) {
            LOGGER.log(Level.SEVERE,
                "Report generation failed with ReportException: " + e.getMessage(), e
            );
            // Do not rethrow (resilient behavior)
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                "Unexpected error in Lambda handler: " + e.getMessage(), e
            );
            // Do not rethrow (resilient behavior)
        }

        return null;
    }
}
