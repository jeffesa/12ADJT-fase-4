package com.fiap.notification.domain.gateway;

/**
 * Domain exception representing a failure in notification delivery.
 * 
 * This checked exception is thrown by NotificationSender.send() when
 * the notification cannot be delivered (e.g., email send failure, SES error).
 * 
 * The domain layer owns this exception and propagates it to the application layer
 * for handling and logging.
 */
public class NotificationException extends Exception {

    /**
     * Constructs a new NotificationException with the specified detail message.
     * 
     * @param message the detail message explaining the notification failure
     */
    public NotificationException(String message) {
        super(message);
    }

    /**
     * Constructs a new NotificationException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the notification failure
     * @param cause the underlying exception that caused this notification failure
     */
    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
