package com.fiap.feedback.infra.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.infra.dto.FeedbackOutput;

/**
 * Unit tests for ReceiveFeedbackQueryResolver.listFeedbacks (English GraphQL query).
 */
class ReceiveFeedbackQueryResolverListFeedbacksTest {

    @Test
    void returnsEnglishOutputForEachFeedback() {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 10, 30, 45);
        Feedback feedback = new Feedback(id, "Good service", 7, Urgency.MEDIUM, createdAt, false);
        ReceiveFeedbackUseCase useCase = mock(ReceiveFeedbackUseCase.class);
        when(useCase.listarTodos()).thenReturn(List.of(feedback));

        List<FeedbackOutput> result = new ReceiveFeedbackQueryResolver(useCase).listFeedbacks();

        assertEquals(1, result.size());
        FeedbackOutput output = result.get(0);
        assertEquals(UUID.fromString(id), output.id());
        assertEquals("Good service", output.description());
        assertEquals(7, output.rating());
        assertEquals("MEDIUM", output.urgency());
        assertEquals(createdAt, output.createdAt());
    }

    @Test
    void returnsEmptyListWhenNoFeedbacksExist() {
        ReceiveFeedbackUseCase useCase = mock(ReceiveFeedbackUseCase.class);
        when(useCase.listarTodos()).thenReturn(List.of());

        List<FeedbackOutput> result = new ReceiveFeedbackQueryResolver(useCase).listFeedbacks();

        assertTrue(result.isEmpty());
    }

    @Test
    void rethrowsWhenUseCaseFails() {
        ReceiveFeedbackUseCase useCase = mock(ReceiveFeedbackUseCase.class);
        when(useCase.listarTodos()).thenThrow(new IllegalStateException("Scan failed"));
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        IllegalStateException error = assertThrows(IllegalStateException.class, resolver::listFeedbacks);

        assertEquals("Scan failed", error.getMessage());
    }
}
