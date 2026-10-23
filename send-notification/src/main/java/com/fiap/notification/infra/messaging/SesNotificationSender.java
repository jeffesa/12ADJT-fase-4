package com.fiap.notification.infra.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.logging.Logger;
import java.util.logging.Level;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SesException;

import com.fiap.notification.domain.entity.Notification;
import com.fiap.notification.domain.gateway.NotificationSender;
import com.fiap.notification.domain.gateway.NotificationException;
import com.fiap.notification.infra.config.NotificationConfiguration;

/**
 * AWS SES implementation of the NotificationSender gateway.
 *
 * This class is responsible for sending email notifications via AWS SES.
 * It implements the domain gateway interface and resides in the infrastructure layer,
 * allowing domain logic to remain independent of AWS.
 *
 * The email is constructed with:
 * - From: configured sender email address
 * - To: admin email (from notification or config default)
 * - Subject: rendered from template with urgency and description
 * - Body: static HTML template with values interpolated
 *
 * Failures are logged and wrapped in NotificationException for domain-level handling.
 * Logging uses java.util.logging for consistency across the codebase.
 */
@ApplicationScoped
public class SesNotificationSender implements NotificationSender {

    private static final Logger LOGGER = Logger.getLogger(SesNotificationSender.class.getName());

    @Inject
    NotificationConfiguration config;

    @Inject
    SesClient sesClient;

    /**
     * Sends a notification email via AWS SES.
     *
     * The email includes:
     * - Subject line with urgency and description (templated)
     * - HTML body with feedback details
     * - Sender and recipient addresses from configuration
     *
     * @param notification the notification to send (must not be null)
     * @throws NotificationException if email delivery fails
     */
    @Override
    public void send(Notification notification) throws NotificationException {
        try {
            LOGGER.log(Level.INFO,
                "Sending email notification for feedback " + notification.getFeedbackId() +
                " to " + notification.getAdminEmail()
            );

            String subject = renderSubject(notification.getDescription(), notification.getUrgency());
            String htmlBody = buildHtmlBody(notification);

            SendEmailRequest request = SendEmailRequest.builder()
                .source(config.senderEmail())
                .destination(d -> d.toAddresses(notification.getAdminEmail()))
                .message(Message.builder()
                    .subject(Content.builder().data(subject).build())
                    .body(Body.builder()
                        .html(Content.builder().data(htmlBody).build())
                        .build())
                    .build())
                .build();

            sesClient.sendEmail(request);

            LOGGER.log(Level.INFO,
                "Email sent successfully for feedback " + notification.getFeedbackId()
            );

        } catch (SesException e) {
            LOGGER.log(Level.SEVERE,
                "SES error sending email for feedback " + notification.getFeedbackId() +
                ": " + e.awsErrorDetails().errorMessage()
            );
            throw new NotificationException(
                "Failed to send email via SES: " + e.awsErrorDetails().errorMessage(),
                e
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                "Error sending email for feedback " + notification.getFeedbackId() +
                ": " + e.getMessage()
            );
            throw new NotificationException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Renders the email subject by substituting template placeholders.
     *
     * Replaces:
     * - ${urgency} with the feedback urgency level
     * - ${description} with the feedback description
     *
     * @param description the feedback description
     * @param urgency the urgency level (HIGH, MEDIUM, LOW)
     * @return the rendered subject line
     */
    private String renderSubject(String description, String urgency) {
        String subject = config.subjectTemplate();
        subject = subject.replace("${urgency}", urgency);
        subject = subject.replace("${description}", description);
        return subject;
    }

    /**
     * Builds the HTML email body with notification details.
     *
     * The HTML includes:
     * - Feedback ID
     * - Description
     * - Urgency level
     * - Received timestamp
     * - Call to action for admin review
     *
     * @param notification the notification containing values to interpolate
     * @return the HTML email body
     */
    private String buildHtmlBody(Notification notification) {
        return "<html><body>" +
            "<h2>⚠️ Critical Feedback Received</h2>" +
            "<p><strong>Feedback ID:</strong> " + notification.getFeedbackId() + "</p>" +
            "<p><strong>Description:</strong> " + notification.getDescription() + "</p>" +
            "<p><strong>Urgency:</strong> " + notification.getUrgency() + "</p>" +
            "<p><strong>Received at:</strong> " + notification.getCreatedAt() + "</p>" +
            "<p>Please review and respond as soon as possible.</p>" +
            "</body></html>";
    }
}
