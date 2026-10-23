package com.fiap.notification.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.fiap.notification.domain.entity.Notification;
import com.fiap.notification.domain.gateway.NotificationSender;
import com.fiap.notification.domain.gateway.NotificationException;

/**
 * Use case for sending notifications to administrators.
 * 
 * This use case orchestrates the business logic for:
 * - Receiving an already-validated Notification entity
 * - Delegating the actual send operation to NotificationSender (infra layer)
 * - Logging the send operation
 * - Propagating NotificationException on delivery failure
 * 
 * The class is marked as @ApplicationScoped (CDI singleton) to ensure
 * a single instance is shared across the application.
 * 
 * Important: Validation of the notification (description, urgency, email format)
 * must be performed by the caller before invoking this use case.
 */
@ApplicationScoped
public class SendNotificationUseCase {

    private static final Logger LOGGER = Logger.getLogger(SendNotificationUseCase.class.getName());

    private final NotificationSender notificationSender;

    /**
     * Constructor for dependency injection.
     * 
     * The NotificationSender is injected and stored as a final field to ensure
     * immutability and thread-safety.
     * 
     * @param notificationSender the gateway responsible for sending notifications
     */
    @Inject
    public SendNotificationUseCase(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    /**
     * Sends a notification to an administrator.
     * 
     * This method receives an already-validated Notification entity and delegates
     * the actual send operation to the NotificationSender gateway.
     * 
     * Flow:
     * 1. Log the send attempt with feedback ID and admin email
     * 2. Call notificationSender.send() to deliver the notification
     * 3. If successful, log the success
     * 4. If NotificationException occurs, propagate it to the caller
     * 
     * @param notification the notification to be sent (must be validated by caller)
     * @throws NotificationException if notification delivery fails
     * @throws NullPointerException if notification is null
     */
    public void execute(Notification notification) throws NotificationException {
        if (notification == null) {
            throw new NullPointerException("Notification cannot be null");
        }

        // Log the send attempt
        LOGGER.log(Level.INFO,
            "Sending notification for feedback " + notification.getFeedbackId() +
            " to " + notification.getAdminEmail()
        );

        // Delegate to the gateway
        try {
            notificationSender.send(notification);
            
            // Log success
            LOGGER.log(Level.INFO,
                "Notification sent successfully for feedback " + notification.getFeedbackId()
            );
        } catch (NotificationException e) {
            // Log the failure
            LOGGER.log(Level.SEVERE,
                "Failed to send notification for feedback " + notification.getFeedbackId() +
                ": " + e.getMessage()
            );
            // Propagate the exception
            throw e;
        }
    }
}
