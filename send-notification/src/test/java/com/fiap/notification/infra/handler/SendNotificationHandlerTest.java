package com.fiap.notification.infra.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;

import com.fiap.notification.application.usecase.SendNotificationUseCase;
import com.fiap.notification.domain.entity.Notification;
import com.fiap.notification.domain.gateway.NotificationException;
import com.fiap.notification.infra.config.NotificationConfiguration;

/**
 * Unit tests for SendNotificationHandler.
 *
 * Verifies SNS event processing, message deserialization, resilience to errors,
 * and correct invocation of the SendNotificationUseCase.
 */
@QuarkusTest
class SendNotificationHandlerTest {

    @Inject
    SendNotificationHandler handler;

    @InjectMock
    SendNotificationUseCase useCase;

    @Inject
    NotificationConfiguration config;

    private Context context;

    @BeforeEach
    void setUp() {
        context = mock(Context.class);
    }

    private SNSEvent createSnsEventWithMessage(String messageJson) {
        SNSEvent event = new SNSEvent();

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage(messageJson);

        setSnsByReflection(record, sns);

        event.setRecords(List.of(record));
        return event;
    }

    private void setSnsByReflection(SNSEvent.SNSRecord record, SNSEvent.SNS sns) {
        try {
            var field = SNSEvent.SNSRecord.class.getDeclaredField("sns");
            field.setAccessible(true);
            field.set(record, sns);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set SNS on record", e);
        }
    }

    @Test
    void testHandleRequest_SuccessfulSingleMessageProcessing() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        String messageJson = String.format(
            "{\"id\":\"%s\",\"description\":\"Test feedback\",\"urgency\":\"HIGH\",\"createdAt\":\"2024-10-22T10:00:00\"}",
            feedbackId.toString()
        );

        SNSEvent event = createSnsEventWithMessage(messageJson);

        // Act
        Void result = handler.handleRequest(event, context);

        // Assert
        assertNull(result, "Handler should return null");
        verify(useCase, times(1)).execute(any(Notification.class));
    }

    @Test
    void testHandleRequest_ProcessesMultipleMessages() throws NotificationException {
        // Arrange
        UUID feedbackId1 = UUID.randomUUID();
        UUID feedbackId2 = UUID.randomUUID();

        SNSEvent event = new SNSEvent();
        List<SNSEvent.SNSRecord> records = new ArrayList<>();

        SNSEvent.SNSRecord record1 = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns1 = new SNSEvent.SNS();
        sns1.setMessage(String.format(
            "{\"id\":\"%s\",\"description\":\"Feedback 1\",\"urgency\":\"HIGH\",\"createdAt\":\"2024-10-22T10:00:00\"}",
            feedbackId1.toString()
        ));
        setSnsByReflection(record1, sns1);
        records.add(record1);

        SNSEvent.SNSRecord record2 = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns2 = new SNSEvent.SNS();
        sns2.setMessage(String.format(
            "{\"id\":\"%s\",\"description\":\"Feedback 2\",\"urgency\":\"MEDIUM\",\"createdAt\":\"2024-10-22T11:00:00\"}",
            feedbackId2.toString()
        ));
        setSnsByReflection(record2, sns2);
        records.add(record2);

        event.setRecords(records);

        // Act
        Void result = handler.handleRequest(event, context);

        // Assert
        assertNull(result);
        verify(useCase, times(2)).execute(any(Notification.class));
    }

    @Test
    void testHandleRequest_ResilienceToErrorInOneMessage() throws NotificationException {
        // Arrange: First message fails, second succeeds
        doThrow(new NotificationException("SES error")).when(useCase).execute(any());

        UUID feedbackId1 = UUID.randomUUID();
        UUID feedbackId2 = UUID.randomUUID();

        SNSEvent event = new SNSEvent();
        List<SNSEvent.SNSRecord> records = new ArrayList<>();

        SNSEvent.SNSRecord record1 = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns1 = new SNSEvent.SNS();
        sns1.setMessage(String.format(
            "{\"id\":\"%s\",\"description\":\"Feedback 1\",\"urgency\":\"HIGH\",\"createdAt\":\"2024-10-22T10:00:00\"}",
            feedbackId1.toString()
        ));
        setSnsByReflection(record1, sns1);
        records.add(record1);

        SNSEvent.SNSRecord record2 = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns2 = new SNSEvent.SNS();
        sns2.setMessage(String.format(
            "{\"id\":\"%s\",\"description\":\"Feedback 2\",\"urgency\":\"MEDIUM\",\"createdAt\":\"2024-10-22T11:00:00\"}",
            feedbackId2.toString()
        ));
        setSnsByReflection(record2, sns2);
        records.add(record2);

        event.setRecords(records);

        // Act
        Void result = handler.handleRequest(event, context);

        // Assert: Handler returns null and attempts to process both messages despite error
        assertNull(result);
        verify(useCase, times(2)).execute(any(Notification.class));
    }

    @Test
    void testHandleRequest_ReturnsNull() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        String messageJson = String.format(
            "{\"id\":\"%s\",\"description\":\"Test\",\"urgency\":\"HIGH\",\"createdAt\":\"2024-10-22T10:00:00\"}",
            feedbackId.toString()
        );

        SNSEvent event = createSnsEventWithMessage(messageJson);

        // Act
        Void result = handler.handleRequest(event, context);

        // Assert: Result is null (Void type)
        assertNull(result);
    }

    @Test
    void testHandleRequest_HandlesNullEvent() throws NotificationException {
        // Act
        Void result = handler.handleRequest(null, context);

        // Assert: Returns null without crashing
        assertNull(result);
        verify(useCase, never()).execute(any(Notification.class));
    }

    @Test
    void testHandleRequest_HandlesNullRecords() throws NotificationException {
        // Arrange
        SNSEvent event = new SNSEvent();
        event.setRecords(null);

        // Act
        Void result = handler.handleRequest(event, context);

        // Assert: Returns null without crashing
        assertNull(result);
        verify(useCase, never()).execute(any(Notification.class));
    }

    @Test
    void testHandleRequest_HandlesMessageWithMissingFields() throws NotificationException {
        // Arrange: Message missing "description" field
        String messageJson = "{\"id\":\"12345678-1234-5678-1234-567812345678\",\"urgency\":\"HIGH\"}";

        SNSEvent event = createSnsEventWithMessage(messageJson);

        // Act
        Void result = handler.handleRequest(event, context);

        // Assert: Returns null and continues (error is handled)
        assertNull(result);
    }

    @Test
    void testHandleRequest_DeserializesNotificationCorrectly() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        String description = "Critical system failure";
        String urgency = "HIGH";

        String messageJson = String.format(
            "{\"id\":\"%s\",\"description\":\"%s\",\"urgency\":\"%s\",\"createdAt\":\"2024-10-22T14:30:00\"}",
            feedbackId.toString(), description, urgency
        );

        SNSEvent event = createSnsEventWithMessage(messageJson);

        // Act
        handler.handleRequest(event, context);

        // Assert: Verify deserialized notification values
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(useCase).execute(captor.capture());
        Notification capturedNotification = captor.getValue();

        assertEquals(feedbackId, capturedNotification.getFeedbackId());
        assertEquals(description, capturedNotification.getDescription());
        assertEquals(urgency, capturedNotification.getUrgency());
    }

    @Test
    void testHandleRequest_UsesConfigAdminEmail() throws NotificationException {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        String messageJson = String.format(
            "{\"id\":\"%s\",\"description\":\"Test\",\"urgency\":\"HIGH\",\"createdAt\":\"2024-10-22T10:00:00\"}",
            feedbackId.toString()
        );

        SNSEvent event = createSnsEventWithMessage(messageJson);

        // Act
        handler.handleRequest(event, context);

        // Assert: Admin email from config was used (uses default from @WithDefault annotation)
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(useCase).execute(captor.capture());
        Notification capturedNotification = captor.getValue();

        assertEquals("admin@feedback-fase-4.com", capturedNotification.getAdminEmail());
    }

    @Test
    void testHandleRequest_NoExceptionThrownOnMessageError() {
        // Arrange: Invalid message format
        String invalidMessageJson = "{ invalid json }";

        SNSEvent event = createSnsEventWithMessage(invalidMessageJson);

        // Act & Assert: No exception is thrown
        assertDoesNotThrow(() -> handler.handleRequest(event, context));
    }

    @Test
    void testHandleRequest_ContinuesProcessingAfterFirstMessageError() throws NotificationException {
        // Arrange: First record has invalid message, second is valid
        SNSEvent event = new SNSEvent();
        List<SNSEvent.SNSRecord> records = new ArrayList<>();

        SNSEvent.SNSRecord invalidRecord = new SNSEvent.SNSRecord();
        SNSEvent.SNS invalidSns = new SNSEvent.SNS();
        invalidSns.setMessage("{ invalid json }");
        setSnsByReflection(invalidRecord, invalidSns);
        records.add(invalidRecord);

        UUID feedbackId2 = UUID.randomUUID();
        SNSEvent.SNSRecord validRecord = new SNSEvent.SNSRecord();
        SNSEvent.SNS validSns = new SNSEvent.SNS();
        validSns.setMessage(String.format(
            "{\"id\":\"%s\",\"description\":\"Valid feedback\",\"urgency\":\"HIGH\",\"createdAt\":\"2024-10-22T10:00:00\"}",
            feedbackId2.toString()
        ));
        setSnsByReflection(validRecord, validSns);
        records.add(validRecord);

        event.setRecords(records);

        // Act
        Void result = handler.handleRequest(event, context);

        // Assert: Valid message was still processed
        assertNull(result);
        verify(useCase, times(1)).execute(any(Notification.class));
    }

    @Test
    void testHandleRequest_HandlesUrgencyCorrectly() throws NotificationException {
        // Arrange: Test all urgency levels
        for (String urgency : new String[]{"HIGH", "MEDIUM", "LOW"}) {
            reset(useCase);

            UUID feedbackId = UUID.randomUUID();
            SNSEvent event = new SNSEvent();
            List<SNSEvent.SNSRecord> records = new ArrayList<>();

            SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
            SNSEvent.SNS sns = new SNSEvent.SNS();
            sns.setMessage(String.format(
                "{\"id\":\"%s\",\"description\":\"Test\",\"urgency\":\"%s\",\"createdAt\":\"2024-10-22T10:00:00\"}",
                feedbackId.toString(), urgency
            ));
            setSnsByReflection(record, sns);
            records.add(record);
            event.setRecords(records);

            // Act
            handler.handleRequest(event, context);

            // Assert
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(useCase).execute(captor.capture());
            Notification capturedNotification = captor.getValue();

            assertEquals(urgency, capturedNotification.getUrgency());
        }
    }

    @Test
    void testHandleRequest_HandlesEmptyRecordsList() throws NotificationException {
        // Arrange
        SNSEvent event = new SNSEvent();
        event.setRecords(new ArrayList<>());

        // Act
        Void result = handler.handleRequest(event, context);

        // Assert
        assertNull(result);
        verify(useCase, never()).execute(any(Notification.class));
    }

    @Test
    void testHandleRequest_UsesCaseExecutedOncePerMessage() throws NotificationException {
        // Arrange: Three valid messages
        SNSEvent event = new SNSEvent();
        List<SNSEvent.SNSRecord> records = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            UUID feedbackId = UUID.randomUUID();
            SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
            SNSEvent.SNS sns = new SNSEvent.SNS();
            sns.setMessage(String.format(
                "{\"id\":\"%s\",\"description\":\"Feedback %d\",\"urgency\":\"HIGH\",\"createdAt\":\"2024-10-22T10:00:00\"}",
                feedbackId.toString(), i
            ));
            setSnsByReflection(record, sns);
            records.add(record);
        }

        event.setRecords(records);

        // Act
        handler.handleRequest(event, context);

        // Assert: useCase.execute() called exactly 3 times
        verify(useCase, times(3)).execute(any(Notification.class));
    }
}
