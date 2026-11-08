package com.fiap.feedback.infra.handler;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.runtime.StartupEvent;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;

/**
 * Listens to Quarkus startup event and initializes shared Lambda handler state.
 * 
 * This ensures that when Quarkus CDI container starts, we capture references
 * to key beans that the ReceiveFeedbackHandler can use when Lambda calls it
 * via reflection with a no-arg constructor.
 */
@ApplicationScoped
public class LambdaStartupHandler {
    
    private static final Logger LOGGER = Logger.getLogger(LambdaStartupHandler.class.getName());
    
    @Inject
    ReceiveFeedbackUseCase receiveFeedbackUseCase;
    
    void onStart(@Observes StartupEvent ev, ReceiveFeedbackUseCase useCase) {
        // Cache in HandlerFactory so no-arg constructor can use it
        LOGGER.log(Level.INFO, "Quarkus application started - caching ReceiveFeedbackUseCase in HandlerFactory");
        HandlerFactory.setReceiveFeedbackUseCase(useCase);
        LOGGER.log(Level.INFO, "Lambda handler ReceiveFeedbackUseCase cached");
    }
}
