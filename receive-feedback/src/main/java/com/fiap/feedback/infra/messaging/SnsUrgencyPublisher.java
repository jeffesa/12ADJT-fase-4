package com.fiap.feedback.infra.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.domain.gateway.UrgencyPublisher;
import com.fiap.feedback.infra.config.FeedbackConfiguration;
import com.fiap.feedback.infra.config.InfrastructureConfiguration;

/**
 * AWS SNS implementation of the UrgencyPublisher interface.
 * 
 * This adapter publishes feedback with HIGH urgency to an SNS topic to notify
 * administrators of critical feedback. It follows the dependency inversion principle
 * by implementing the domain's UrgencyPublisher interface.
 * 
 * Features:
 * - Publishes only HIGH urgency feedbacks to SNS
 * - Constructs a JSON message payload containing feedback details
 * - Logs all publish operations for audit and debug purposes
 * - Handles and rethrows SNS exceptions with detailed logging
 * 
 * Configuration:
 * - SNS topic ARN is read from environment variable FEEDBACK_TOPIC_ARN
 * - Fallback ARN is provided for LocalStack development
 */
@ApplicationScoped
public class SnsUrgencyPublisher implements UrgencyPublisher {

    private static final Logger LOGGER = Logger.getLogger(SnsUrgencyPublisher.class);

    private final SnsClient snsClient;
    private final FeedbackConfiguration config;
    private final InfrastructureConfiguration infraConfig;

    /**
     * Constructor for dependency injection.
     * 
     * @param snsClient the SNS client managed by Quarkus
     * @param config the feedback configuration (for potential future use)
     * @param infraConfig the infrastructure configuration containing SNS topic ARN
     */
    @Inject
    public SnsUrgencyPublisher(
            SnsClient snsClient,
            FeedbackConfiguration config,
            InfrastructureConfiguration infraConfig) {
        this.snsClient = snsClient;
        this.config = config;
        this.infraConfig = infraConfig;
    }

    @Override
    public void publish(Feedback feedback) {
        try {
            // Only publish if urgency is HIGH
            if (feedback.getUrgency() != Urgency.HIGH) {
                LOGGER.debugf(
                        "Feedback %s has urgency %s, not publishing to SNS (only HIGH urgency is published)",
                        feedback.getId(),
                        feedback.getUrgency()
                );
                return;
            }

            // Get topic ARN from configuration
            String topicArn = infraConfig.sns().topic().arn();

            // Build the JSON message payload
            String messageBody = FeedbackMessageBuilder.buildFeedbackJson(feedback);

            // Create and send the publish request
            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(messageBody)
                    .subject("High Urgency Feedback Alert")
                    .build();

            PublishResponse publishResponse = snsClient.publish(publishRequest);

            LOGGER.infof(
                    "Published feedback %s to SNS topic. MessageId: %s",
                    feedback.getId(),
                    publishResponse.messageId()
            );
        } catch (SnsException e) {
            LOGGER.errorf(
                    "Error publishing feedback %s to SNS: %s",
                    feedback.getId(),
                    e.getMessage()
            );
            throw e;
        }
    }
}
