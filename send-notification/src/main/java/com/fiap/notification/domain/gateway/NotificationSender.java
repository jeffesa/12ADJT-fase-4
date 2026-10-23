package com.fiap.notification.domain.gateway;

import com.fiap.notification.domain.entity.Notification;

/**
 * Gateway interface for sending notifications.
 * 
 * This interface defines the contract for delivering notifications to external systems
 * (e.g., AWS SES, email service). The actual implementation resides in the infrastructure layer.
 * 
 * The domain layer depends on this interface but not on any concrete implementation,
 * following the Dependency Inversion Principle of Clean Architecture.
 */
public interface NotificationSender {

    /**
     * Sends a notification via the configured delivery mechanism.
     * 
     * The notification is expected to be already validated and complete.
     * This method handles only the delivery logic, not validation.
     * 
     * @param notification the notification to be sent (must not be null)
     * @throws NotificationException if the notification cannot be delivered
     */
    void send(Notification notification) throws NotificationException;
}
