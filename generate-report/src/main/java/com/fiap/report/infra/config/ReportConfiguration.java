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
 *
 * Example usage in a service:
 * <pre>
 * {@literal @}Inject
 * ReportConfiguration config;
 *
 * String sender = config.senderEmail();
 * String table = config.feedbacksTable();
 * </pre>
 */
@ConfigMapping(prefix = "report")
public interface ReportConfiguration {

    /**
     * DynamoDB configuration for report data retrieval.
     *
     * @return the DynamoDB configuration section
     */
    DynamoDb dynamodb();

    /**
     * SES configuration for email delivery.
     *
     * @return the SES configuration section
     */
    Ses ses();

    /**
     * DynamoDB table and connection settings for feedback retrieval.
     */
    interface DynamoDb {
        /**
         * DynamoDB feedbacks table name.
         *
         * This is the table name where feedback records are stored and retrieved.
         * Default: feedbacks
         * Override via: REPORT_DYNAMODB_TABLE_NAME environment variable
         *
         * @return the table name
         */
        @WithName("table-name")
        @WithDefault("feedbacks")
        String tableName();
    }

    /**
     * SES email delivery configuration.
     */
    interface Ses {
        /**
         * Sender email address (From field in SES).
         *
         * This is the email address that appears as the sender in report emails.
         * Must be a verified sender identity in AWS SES.
         * Default: reports@feedback-platform.com
         * Override via: REPORT_SES_SENDER_EMAIL environment variable
         *
         * @return the sender email address
         */
        @WithName("sender-email")
        @WithDefault("reports@feedback-platform.com")
        String senderEmail();
    }
}
