package com.fiap.report.infra.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration properties for report generation and distribution via AWS services.
 *
 * This interface is mapped to the {@code report.*} prefix in application.yaml
 * via SmallRye Config (Quarkus native configuration framework).
 *
 * All properties support environment variable overrides and have sensible defaults.
 * Properties in this interface use camelCase naming, but map to kebab-case in YAML
 * via SmallRye Config convention (e.g., adminEmail → admin-email, senderEmail → sender-email).
 *
 * Example usage in a service:
 * <pre>
 * {@literal @}Inject
 * ReportConfiguration config;
 *
 * String admin = config.adminEmail();
 * String sender = config.senderEmail();
 * </pre>
 */
@ConfigMapping(prefix = "report")
public interface ReportConfiguration {

    /**
     * Administrator email address (To field in SES).
     *
     * This is the email address where weekly feedback reports are sent.
     * Default: admin@feedback-fase-4.com
     * Override via: ADMIN_EMAIL environment variable
     *
     * @return the administrator email address
     */
    @WithName("admin-email")
    @WithDefault("admin@feedback-fase-4.com")
    String adminEmail();

    /**
     * Sender email address (From field in SES).
     *
     * This is the email address that appears as the sender in report emails.
     * Must be a verified sender identity in AWS SES.
     * Default: noreply@feedback-fase-4.com
     * Override via: SENDER_EMAIL environment variable
     *
     * @return the sender email address
     */
    @WithName("sender-email")
    @WithDefault("noreply@feedback-fase-4.com")
    String senderEmail();

    /**
     * DynamoDB endpoint for local testing.
     *
     * Used in development profile to connect to LocalStack on localhost:4566.
     * In production, AWS default endpoint is used (no override needed).
     * Default: http://localhost:4566
     * Override via: DYNAMODB_ENDPOINT environment variable
     *
     * @return the DynamoDB endpoint
     */
    @WithName("dynamodb-endpoint")
    @WithDefault("http://localhost:4566")
    String dynamodbEndpoint();

    /**
     * DynamoDB feedbacks table name.
     *
     * This is the table name where feedback records are stored and retrieved.
     * Default: feedbacks
     * Override via: DYNAMODB_TABLE_NAME environment variable
     *
     * @return the table name
     */
    @WithName("table-name")
    @WithDefault("feedbacks")
    String tableName();

    /**
     * Report title used in email subject and headers.
     *
     * This is the title that appears in email headers and the report body.
     * Default: Weekly Feedback Report
     * Override via: REPORT_TITLE environment variable
     *
     * @return the report title
     */
    @WithName("report-title")
    @WithDefault("Weekly Feedback Report")
    String reportTitle();
}
