package com.fiap.feedback.infra.handler;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;

/**
 * Service factory for creating service instances in Lambda environment.
 * 
 * This factory attempts to use Quarkus CDI container (Arc) when available,
 * and gracefully falls back to providing a helpful error message if CDI is not initialized.
 * 
 * The Lambda environment may not initialize CDI automatically when using RequestHandler,
 * so this factory helps diagnose and work around that limitation.
 */
public class ServiceFactory {
    
    private static final Logger LOGGER = Logger.getLogger(ServiceFactory.class.getName());
    private static volatile ReceiveFeedbackUseCase cachedUseCase;
    
    /**
     * Gets the ReceiveFeedbackUseCase instance from CDI container.
     * 
     * This method attempts to retrieve the use case from Quarkus Arc.
     * If successful, the instance is cached for subsequent calls.
     * If the CDI container is not available, a detailed error is logged.
     * 
     * @return the ReceiveFeedbackUseCase instance
     * @throws RuntimeException if the CDI container is not initialized or the service cannot be retrieved
     */
    public static ReceiveFeedbackUseCase getReceiveFeedbackUseCase() {
        if (cachedUseCase != null) {
            return cachedUseCase;
        }
        
        try {
            ArcContainer container = Arc.container();
            
            if (container == null) {
                String errorMsg = "Arc.container() returned null - CDI container not initialized in Lambda environment. " +
                    "This occurs when RequestHandler is instantiated by Lambda runtime before Quarkus initializes CDI. " +
                    "Solution: Use GraphQL endpoint (auto-handles CDI) or rewrite handler as StreamHandler.";
                LOGGER.log(Level.SEVERE, errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            // Successfully retrieve from CDI
            cachedUseCase = container.instance(ReceiveFeedbackUseCase.class).get();
            LOGGER.log(Level.INFO, "ReceiveFeedbackUseCase retrieved from CDI container");
            return cachedUseCase;
            
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to get ReceiveFeedbackUseCase from CDI container", e);
            throw e;
        } catch (Exception e) {
            String errorMsg = "Unexpected error retrieving ReceiveFeedbackUseCase from CDI container: " + e.getMessage();
            LOGGER.log(Level.SEVERE, errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    /**
     * Clears the cached use case instance.
     * Useful for testing or resetting the factory state.
     */
    public static void clearCache() {
        cachedUseCase = null;
    }
}
