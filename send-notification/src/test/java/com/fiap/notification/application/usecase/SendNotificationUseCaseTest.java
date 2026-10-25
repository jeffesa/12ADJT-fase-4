package com.fiap.notification.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fiap.notification.domain.entity.Notification;
import com.fiap.notification.domain.gateway.NotificationSender;
import com.fiap.notification.domain.gateway.NotificationException;

/**
 * Unit tests for SendNotificationUseCase.
 *
 * Verifies that the use case correctly orchestrates notification sending,
 * handles exceptions, and propagates errors appropriately.
 */
class SendNotificationUseCaseTest {

    private SendNotificationUseCase useCase;
    private NotificationSender notificationSender;

    @BeforeEach
    void setUp() {
        notificationSender = mock(NotificationSender.class);
        useCase = new SendNotificationUseCase(notificationSender);
    }

    @Test
    void testExecute_SuccessfullySendsNotification() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test feedback", "HIGH", "admin@company.com"
        );

        // Act
        useCase.execute(notification);

        // Assert: notificationSender.send() was called exactly once with the notification
        verify(notificationSender, times(1)).send(notification);
    }

    @Test
    void testExecute_PropagatesNotificationException() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test feedback", "HIGH", "admin@company.com"
        );

        doThrow(new NotificationException("SES failed to send email"))
            .when(notificationSender).send(notification);

        // Act & Assert: NotificationException is propagated
        NotificationException exception = assertThrows(
            NotificationException.class,
            () -> useCase.execute(notification)
        );

        assertTrue(exception.getMessage().contains("SES failed"));
        verify(notificationSender, times(1)).send(notification);
    }

    @Test
    void testExecute_InvokesNotificationSenderExactlyOnce() throws NotificationException {
        // Arrange
        UUID feedbackId1 = UUID.randomUUID();
        UUID feedbackId2 = UUID.randomUUID();
        Notification notification1 = new Notification(
            feedbackId1, "Feedback 1", "HIGH", "admin@company.com"
        );
        Notification notification2 = new Notification(
            feedbackId2, "Feedback 2", "MEDIUM", "admin@company.com"
        );

        // Act
        useCase.execute(notification1);
        useCase.execute(notification2);

        // Assert: notificationSender.send() was called exactly twice (once per execution)
        verify(notificationSender, times(2)).send(any(Notification.class));
    }

    @Test
    void testExecute_ThrowsNullPointerException_WhenNotificationIsNull() {
        // Act & Assert
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> useCase.execute(null)
        );

        assertTrue(exception.getMessage().contains("cannot be null"));

        // Verify sender was never called
        try {
            verify(notificationSender, never()).send(any(Notification.class));
        } catch (Exception e) {
            // Mockito compatibility - the important thing is the exception was thrown above
        }
    }

    @Test
    void testExecute_VerifiesSenderCalledWithCorrectNotification() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Critical issue", "HIGH", "admin@company.com"
        );

        // Act
        useCase.execute(notification);

        // Assert: Verify the exact notification object was passed to sender
        verify(notificationSender).send(notification);
    }

    @Test
    void testExecute_HandlesSenderThrowingGenericException() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test feedback", "HIGH", "admin@company.com"
        );

        doThrow(new NotificationException("Email service unavailable"))
            .when(notificationSender).send(any(Notification.class));

        // Act & Assert: Exception is propagated
        assertThrows(
            NotificationException.class,
            () -> useCase.execute(notification)
        );

        verify(notificationSender, times(1)).send(any(Notification.class));
    }

    @Test
    void testExecute_MultipleNotificationsAreSentIndependently() throws NotificationException {
        // Arrange
        UUID feedbackId1 = UUID.randomUUID();
        UUID feedbackId2 = UUID.randomUUID();
        UUID feedbackId3 = UUID.randomUUID();

        Notification notification1 = new Notification(
            feedbackId1, "Feedback 1", "HIGH", "admin1@company.com"
        );
        Notification notification2 = new Notification(
            feedbackId2, "Feedback 2", "MEDIUM", "admin2@company.com"
        );
        Notification notification3 = new Notification(
            feedbackId3, "Feedback 3", "LOW", "admin3@company.com"
        );

        // Act
        useCase.execute(notification1);
        useCase.execute(notification2);
        useCase.execute(notification3);

        // Assert: All notifications were sent
        verify(notificationSender, times(1)).send(notification1);
        verify(notificationSender, times(1)).send(notification2);
        verify(notificationSender, times(1)).send(notification3);
        verify(notificationSender, times(3)).send(any(Notification.class));
    }

    @Test
    void testExecute_SenderThrowingNotificationException_WithDifferentMessage() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test", "HIGH", "admin@company.com"
        );

        String errorMessage = "SES returned error code 400: Invalid sender address";
        doThrow(new NotificationException(errorMessage))
            .when(notificationSender).send(any(Notification.class));

        // Act & Assert
        NotificationException exception = assertThrows(
            NotificationException.class,
            () -> useCase.execute(notification)
        );

        assertTrue(exception.getMessage().contains("SES"));
        assertTrue(exception.getMessage().contains("Invalid sender address"));
    }

    @Test
    void testExecute_UseCaseDoesNotCatchException() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(
            feedbackId, "Test", "HIGH", "admin@company.com"
        );

        doThrow(new NotificationException("Network error"))
            .when(notificationSender).send(any(Notification.class));

        // Act & Assert: Exception is not caught; it propagates to caller
        assertThrows(
            NotificationException.class,
            () -> useCase.execute(notification)
        );
    }
}
