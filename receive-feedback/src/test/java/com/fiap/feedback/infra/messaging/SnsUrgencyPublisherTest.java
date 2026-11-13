package com.fiap.feedback.infra.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.infra.config.FeedbackConfiguration;
import com.fiap.feedback.infra.config.InfrastructureConfiguration;

/**
 * Unit tests for SnsUrgencyPublisher.
 *
 * The SNS client is mocked with Mockito. The topic ARN comes from a mocked
 * InfrastructureConfiguration.
 */
class SnsUrgencyPublisherTest {

    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic";

    private final SnsClient snsClient = mock(SnsClient.class);
    private final SnsUrgencyPublisher publisher = createPublisher();

    @Test
    void highUrgencyFeedbackIsPublishedToTopicWithAlertSubject() {
        Feedback feedback = new Feedback(UUID.randomUUID().toString(), "Terrible experience", 2,
                Urgency.HIGH, LocalDateTime.of(2024, 3, 15, 10, 30), false);
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("message-1").build());

        publisher.publish(feedback);

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        PublishRequest request = captor.getValue();
        assertEquals(TOPIC_ARN, request.topicArn());
        assertEquals("High Urgency Feedback Alert", request.subject());
        assertEquals(FeedbackMessageBuilder.buildFeedbackJson(feedback), request.message());
    }

    @Test
    void mediumUrgencyFeedbackIsNotPublished() {
        publisher.publish(new Feedback(UUID.randomUUID().toString(), "Average service", 6,
                Urgency.MEDIUM, LocalDateTime.now(), false));

        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void lowUrgencyFeedbackIsNotPublished() {
        publisher.publish(new Feedback(UUID.randomUUID().toString(), "Excellent service", 9,
                Urgency.LOW, LocalDateTime.now(), false));

        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void snsFailureIsRethrown() {
        SnsException failure = (SnsException) SnsException.builder().message("Topic does not exist").build();
        when(snsClient.publish(any(PublishRequest.class))).thenThrow(failure);
        Feedback feedback = new Feedback(UUID.randomUUID().toString(), "Terrible experience", 1,
                Urgency.HIGH, LocalDateTime.now(), false);

        SnsException error = assertThrows(SnsException.class, () -> publisher.publish(feedback));

        assertSame(failure, error);
    }

    private SnsUrgencyPublisher createPublisher() {
        InfrastructureConfiguration infraConfig = mock(InfrastructureConfiguration.class, RETURNS_DEEP_STUBS);
        when(infraConfig.sns().topic().arn()).thenReturn(TOPIC_ARN);
        return new SnsUrgencyPublisher(snsClient, mock(FeedbackConfiguration.class), infraConfig);
    }
}
