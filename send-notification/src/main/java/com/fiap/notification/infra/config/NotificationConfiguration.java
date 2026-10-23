package com.fiap.notification.infra.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration properties for notification delivery via AWS SES.
 *
 * This interface is mapped to the {@code notification.*} prefix in application.properties
 * via SmallRye Config (Quarkus native configuration framework).
 *
 * All properties support environment variable overrides and have sensible defaults.
 * The defaults can be overridden at runtime via environment variables:
 * - SENDER_EMAIL: sender email address
 * - ADMIN_EMAIL: admin email address
 * - SUBJECT_TEMPLATE: subject template with ${urgency} and ${description} placeholders
 * - SES_REGION: AWS SES region
 *
 * Example usage in a service:
 * <pre>
 * {@literal @}Inject
 * NotificationConfiguration config;
 *
 * String sender = config.senderEmail();
 * String admin = config.adminEmail();
 * </pre>
 */
@ConfigMapping(prefix = "notification")
public interface NotificationConfiguration {

    /**
     * Sender email address (From field in SES).
     *
     * This is the email address that appears as the sender in notifications.
     * Default: noreply@feedback-platform.com
     * Override via: SENDER_EMAIL environment variable
     *
     * @return the sender email address
     */
    @WithName("sender-email")
    @WithDefault("noreply@feedback-platform.com")
    String senderEmail();

    /**
     * Admin email address (To field in SES).
     *
     * This is the default recipient email address for notifications when
     * the notification does not specify a custom admin email.
     * Default: admin@feedback-platform.com
     * Override via: ADMIN_EMAIL environment variable
     *
     * @return the admin email address
     */
    @WithName("admin-email")
    @WithDefault("admin@feedback-platform.com")
    String adminEmail();

    /**
     * Email subject template with placeholders.
     *
     * Supported placeholders:
     * - ${urgency}: replaced with the feedback urgency (HIGH, MEDIUM, LOW)
     * - ${description}: replaced with the feedback description
     *
     * Default: "⚠️ Feedback ${urgency}: ${description}"
     * Override via: SUBJECT_TEMPLATE environment variable
     *
     * @return the subject template
     */
    @WithName("subject-template")
    @WithDefault("⚠️ Feedback ${urgency}: ${description}")
    String subjectTemplate();

    /**
     * AWS SES region for email delivery.
     *
     * This region must match the region where the SES domain is verified.
     * Default: us-east-1
     * Override via: SES_REGION environment variable
     *
     * @return the SES region
     */
    @WithName("ses-region")
    @WithDefault("us-east-1")
    String sesRegion();
}
