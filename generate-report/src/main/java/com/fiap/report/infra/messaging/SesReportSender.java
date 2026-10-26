package com.fiap.report.infra.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.logging.Logger;
import java.util.logging.Level;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SesException;

import com.fiap.report.domain.entity.WeeklyReport;
import com.fiap.report.domain.gateway.ReportSender;
import com.fiap.report.domain.gateway.ReportException;
import com.fiap.report.infra.config.ReportConfiguration;

/**
 * AWS SES implementation of the ReportSender gateway.
 *
 * This adapter is responsible for sending weekly feedback reports via AWS SES.
 * It implements the ReportSender domain interface, allowing the domain layer to
 * remain independent of email service concerns (Clean Architecture principle).
 *
 * Features:
 * - Builds HTML email body using ReportEmailBuilder
 * - Constructs formatted subject line with report period
 * - Sends email via AWS SES with proper error handling
 * - Logs operations at appropriate levels for debugging and monitoring
 *
 * The email includes:
 * - From: configured sender email address
 * - To: admin email address
 * - Subject: "Weekly Feedback Report — {startDate} to {endDate}"
 * - Body: complete HTML report template with statistics and tables
 *
 * Failures are logged at SEVERE level and wrapped in ReportException for
 * domain-level handling.
 */
@ApplicationScoped
public class SesReportSender implements ReportSender {

    private static final Logger LOGGER = Logger.getLogger(SesReportSender.class.getName());

    @Inject
    SesClient sesClient;

    @Inject
    ReportConfiguration config;

    /**
     * Sends a weekly feedback report via AWS SES email.
     *
     * The email includes:
     * - Subject line with report period dates
     * - HTML body with complete report structure and statistics
     * - Sender and recipient addresses from configuration
     *
     * @param report the WeeklyReport to send (must not be null)
     * @param adminEmail the recipient email address (must not be null)
     * @throws ReportException if email delivery fails or SES returns an error
     */
    @Override
    public void send(WeeklyReport report, String adminEmail) throws ReportException {
        try {
            LOGGER.log(Level.INFO,
                    "Sending weekly report " + report.getId() + " to " + adminEmail);

            // Build HTML body
            String htmlBody = ReportEmailBuilder.buildHtmlBody(report);

            // Construct subject line
            LocalDate startDate = report.getPeriod().getStartDate();
            LocalDate endDate = report.getPeriod().getEndDate();
            String subject = "Weekly Feedback Report — " + startDate + " to " + endDate;

            // Build SES request
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(config.ses().senderEmail())
                    .destination(d -> d.toAddresses(adminEmail))
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).build())
                                    .build())
                            .build())
                    .build();

            // Send email
            sesClient.sendEmail(request);

            LOGGER.log(Level.INFO,
                    "Weekly report " + report.getId() + " sent successfully to " + adminEmail);

        } catch (SesException e) {
            String errorMsg = e.awsErrorDetails() != null && e.awsErrorDetails().errorMessage() != null
                    ? e.awsErrorDetails().errorMessage()
                    : e.getMessage();

            LOGGER.log(Level.SEVERE,
                    "SES error sending report " + report.getId() + " to " + adminEmail +
                    ": " + errorMsg);

            throw new ReportException(
                    "Failed to send report via SES: " + errorMsg,
                    e
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Error sending report " + report.getId() + " to " + adminEmail +
                    ": " + e.getMessage());

            throw new ReportException(
                    "Failed to send report: " + e.getMessage(),
                    e
            );
        }
    }
}
