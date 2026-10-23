package com.fiap.notification.infra.handler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import com.fiap.notification.domain.entity.Notification;
import com.fiap.notification.domain.gateway.NotificationException;
import com.fiap.notification.application.usecase.SendNotificationUseCase;
import com.fiap.notification.infra.config.NotificationConfiguration;

/**
 * AWS Lambda handler for processing SNS events and sending notifications.
 *
 * This handler receives SNS events containing feedback information,
 * deserializes the messages, and triggers the send notification use case.
 *
 * SNS Message Format (JSON):
 * <pre>
 * {
 *   "feedbackId": "uuid-string",
 *   "description": "feedback text",
 *   "rating": 5,
 *   "urgency": "HIGH",
 *   "createdAt": "2026-10-22T10:30:00"
 * }
 * </pre>
 *
 * Error Handling:
 * - Messages that fail to deserialize are logged at SEVERE level
 * - Messages that fail to send are logged at SEVERE level
 * - Handler continues processing remaining messages (resilient behavior)
 * - Always returns null to indicate successful Lambda execution
 *
 * Logging uses java.util.logging for consistency with domain/application layers.
 */
@ApplicationScoped
public class SendNotificationHandler implements RequestHandler<SNSEvent, Void> {

    private static final Logger LOGGER = Logger.getLogger(SendNotificationHandler.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    SendNotificationUseCase useCase;

    @Inject
    NotificationConfiguration config;

    /**
     * Handles SNS events containing feedback notifications.
     *
     * Flow:
     * 1. Log handler invocation
     * 2. Iterate over each SNS record in the event
     * 3. For each record:
     *    a. Extract and deserialize the SNS message JSON
     *    b. Create a Notification entity from deserialized data
     *    c. Invoke the use case to send the notification
     *    d. On failure: log and continue to next message
     * 4. Log completion
     * 5. Return null (Lambda success)
     *
     * @param event SNS event containing feedback records
     * @param context Lambda context (unused)
     * @return null (void return type for Lambda)
     */
    @Override
    public Void handleRequest(SNSEvent event, Context context) {
        try {
            LOGGER.log(Level.INFO, "Lambda handler invoked for sending notifications");

            if (event == null || event.getRecords() == null) {
                LOGGER.log(Level.WARNING, "Received null or empty SNS event");
                return null;
            }

            for (SNSEvent.SNSRecord record : event.getRecords()) {
                processRecord(record);
            }

            LOGGER.log(Level.INFO, "Lambda handler completed");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                "Unexpected error in Lambda handler: " + e.getMessage()
            );
        }

        return null;
    }

    /**
     * Processes a single SNS record.
     *
     * Deserializes the SNS message JSON, creates a Notification entity,
     * and invokes the use case. Errors are logged and not re-thrown.
     *
     * @param record the SNS record to process
     */
    private void processRecord(SNSEvent.SNSRecord record) {
        try {
            SNSEvent.SNS sns = record.getSNS();
            String messageJson = sns.getMessage();

            LOGGER.log(Level.FINE, "Processing SNS message: " + messageJson);

            // Deserialize JSON to extract feedback fields
            JsonNode messageNode = OBJECT_MAPPER.readTree(messageJson);

            String feedbackIdStr = messageNode.get("feedbackId").asText();
            String description = messageNode.get("description").asText();
            String urgency = messageNode.get("urgency").asText();
            String createdAtStr = messageNode.get("createdAt").asText();

            UUID feedbackId = UUID.fromString(feedbackIdStr);

            // Create notification with admin email from config
            Notification notification = new Notification(
                feedbackId,
                description,
                urgency,
                config.adminEmail()
            );

            // Invoke use case
            useCase.execute(notification);

        } catch (NotificationException e) {
            LOGGER.log(Level.SEVERE,
                "Notification sending failed: " + e.getMessage()
            );
            // Continue to next message
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                "Error processing SNS record: " + e.getMessage()
            );
            // Continue to next message
        }
    }
}
