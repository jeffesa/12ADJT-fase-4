package com.fiap.feedback.infra.handler;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;

/**
 * Unit tests for HandlerFactory.
 *
 * Outside a running Quarkus container there is no Arc CDI instance, so the factory
 * only returns the use case that was cached with setReceiveFeedbackUseCase.
 */
class HandlerFactoryTest {

    @AfterEach
    void clearCachedUseCase() {
        HandlerFactory.setReceiveFeedbackUseCase(null);
    }

    @Test
    void returnsNullWhenNoUseCaseIsCachedAndCdiIsNotRunning() {
        HandlerFactory.setReceiveFeedbackUseCase(null);

        assertNull(HandlerFactory.getReceiveFeedbackUseCase());
    }

    @Test
    void returnsCachedUseCaseAfterSetup() {
        ReceiveFeedbackUseCase useCase = mock(ReceiveFeedbackUseCase.class);
        HandlerFactory.setReceiveFeedbackUseCase(useCase);

        assertSame(useCase, HandlerFactory.getReceiveFeedbackUseCase());
        assertSame(useCase, HandlerFactory.getReceiveFeedbackUseCase());
    }
}
