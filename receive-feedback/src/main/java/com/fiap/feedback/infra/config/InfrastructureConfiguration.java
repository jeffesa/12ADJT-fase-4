package com.fiap.feedback.infra.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration properties for AWS infrastructure services.
 * 
 * This interface is mapped to the root level configuration via SmallRye Config.
 * It provides access to infrastructure-specific settings like table names and topic ARNs.
 * 
 * Usage in infrastructure layer:
 * - Inject this bean into persistence/messaging adapters
 * - Read AWS resource names at initialization time
 * 
 * Example application.yaml:
 * <pre>
 * dynamodb:
 *   table:
 *     feedbacks: feedbacks
 * 
 * sns:
 *   topic:
 *     arn: arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic
 * </pre>
 */
@ConfigMapping(prefix = "")
public interface InfrastructureConfiguration {

    /**
     * DynamoDB configuration section.
     */
    DynamoDb dynamodb();

    /**
     * SNS configuration section.
     */
    Sns sns();

    /**
     * DynamoDB resource configuration.
     */
    interface DynamoDb {
        /**
         * Table configuration.
         */
        Table table();
    }

    /**
     * DynamoDB table names.
     */
    interface Table {
        /**
         * The DynamoDB table name for storing feedbacks.
         * Default: feedbacks
         */
        @WithDefault("feedbacks")
        String feedbacks();
    }

    /**
     * SNS resource configuration.
     */
    interface Sns {
        /**
         * Topic configuration.
         */
        Topic topic();
    }

    /**
     * SNS topic configuration.
     */
    interface Topic {
        /**
         * The SNS topic ARN for publishing urgent feedbacks.
         * Can be overridden via environment variable FEEDBACK_TOPIC_ARN.
         * Default: arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic (LocalStack)
         */
        @WithDefault("arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic")
        String arn();
    }
}
