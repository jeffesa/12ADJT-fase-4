package com.fiap.feedback.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.domain.gateway.FeedbackGateway;

/**
 * Unit tests for ListFeedbackUseCase.
 */
class ListFeedbackUseCaseTest {

    private FeedbackGateway feedbackGateway;
    private ListFeedbackUseCase useCase;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        feedbackGateway = mock(FeedbackGateway.class);
        useCase = new ListFeedbackUseCase();
        Field gatewayField = ListFeedbackUseCase.class.getDeclaredField("feedbackGateway");
        gatewayField.setAccessible(true);
        gatewayField.set(useCase, feedbackGateway);
    }

    @Test
    void listAllFeedbacksReturnsEveryFeedbackFromGateway() {
        List<Feedback> stored = List.of(
                new Feedback("feedback-1", "Terrible experience", 2, Urgency.HIGH, LocalDateTime.now(), false),
                new Feedback("feedback-2", "Excellent service", 9, Urgency.LOW, LocalDateTime.now(), false));
        when(feedbackGateway.findAll()).thenReturn(stored);

        List<Feedback> result = useCase.listAllFeedbacks();

        assertSame(stored, result);
        assertEquals(2, result.size());
        verify(feedbackGateway).findAll();
    }

    @Test
    void listAllFeedbacksReturnsEmptyListWhenGatewayHasNoItems() {
        when(feedbackGateway.findAll()).thenReturn(List.of());

        assertEquals(0, useCase.listAllFeedbacks().size());
    }
}
