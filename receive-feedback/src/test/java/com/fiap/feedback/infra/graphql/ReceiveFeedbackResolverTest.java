package com.fiap.feedback.infra.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.infra.dto.ReceiveFeedbackInput;
import com.fiap.feedback.infra.dto.ReceiveFeedbackOutput;

/**
 * Unit tests for ReceiveFeedbackResolver.
 *
 * The use case is mocked with Mockito, so these tests cover the resolver's
 * mapping of the use case result and its exception propagation.
 */
class ReceiveFeedbackResolverTest {

    private ReceiveFeedbackUseCase receiveFeedbackUseCase;
    private ReceiveFeedbackResolver resolver;

    @BeforeEach
    void setUp() {
        receiveFeedbackUseCase = mock(ReceiveFeedbackUseCase.class);
        resolver = new ReceiveFeedbackResolver(receiveFeedbackUseCase);
    }

    @Test
    void receiveFeedbackMapsUseCaseResultToOutput() {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 10, 30);
        Feedback saved = new Feedback(id, "Terrible experience", 2, Urgency.HIGH, createdAt, false);
        when(receiveFeedbackUseCase.receiveFeedback("Terrible experience", 2)).thenReturn(saved);

        ReceiveFeedbackOutput output = resolver.receiveFeedback(new ReceiveFeedbackInput("Terrible experience", 2));

        assertEquals(UUID.fromString(id), output.id());
        assertEquals("Terrible experience", output.descricao());
        assertEquals(2, output.nota());
        assertEquals("HIGH", output.urgencia());
        assertEquals(createdAt, output.dataCriacao());
        verify(receiveFeedbackUseCase).receiveFeedback("Terrible experience", 2);
    }

    @Test
    void receiveFeedbackRethrowsValidationError() {
        when(receiveFeedbackUseCase.receiveFeedback(anyString(), anyInt()))
                .thenThrow(new IllegalArgumentException("Rating must be at most 10"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> resolver.receiveFeedback(new ReceiveFeedbackInput("Good service", 11)));

        assertEquals("Rating must be at most 10", error.getMessage());
    }

    @Test
    void receiveFeedbackRethrowsUnexpectedError() {
        when(receiveFeedbackUseCase.receiveFeedback(anyString(), anyInt()))
                .thenThrow(new IllegalStateException("Database unavailable"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> resolver.receiveFeedback(new ReceiveFeedbackInput("Good service", 7)));

        assertEquals("Database unavailable", error.getMessage());
    }
}
