package com.fiap.feedback.infra.handler;

import java.util.logging.Logger;
import java.util.logging.Level;

import io.quarkus.arc.Arc;
import jakarta.inject.Singleton;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;

/**
 * Factory for creating ReceiveFeedbackHandler with dependencies.
 * 
 * This factory handles the CDI complexity by trying multiple approaches:
 * 1. Get from Arc CDI container (if Quarkus is running)
 * 2. Create a default instance (fallback)
 */
public class HandlerFactory {
    
    private static final Logger LOGGER = Logger.getLogger(HandlerFactory.class.getName());
    private static ReceiveFeedbackUseCase useCaseInstance = null;
    private static final Object LOCK = new Object();
    
    /**
     * Gets or creates the ReceiveFeedbackUseCase singleton.
     */
    public static ReceiveFeedbackUseCase getReceiveFeedbackUseCase() {
        // Return cached if available
        if (useCaseInstance != null) {
            return useCaseInstance;
        }
        
        synchronized (LOCK) {
            // Double-check after acquiring lock
            if (useCaseInstance != null) {
                return useCaseInstance;
            }
            
            // Try to get from Arc CDI
            try {
                if (Arc.container() != null && Arc.container().isRunning()) {
                    var instance = Arc.container().instance(ReceiveFeedbackUseCase.class);
                    if (instance != null) {
                        useCaseInstance = instance.get();
                        LOGGER.log(Level.FINE, "ReceiveFeedbackUseCase obtained from Arc CDI");
                        return useCaseInstance;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Arc lookup failed: " + e.getMessage());
            }
            
            // If we get here, CDI is not available or failed
            // This shouldn't happen in normal Quarkus/Lambda execution
            LOGGER.log(Level.WARNING, "Could not obtain ReceiveFeedbackUseCase from Arc CDI - CDI may not be initialized");
            
            return null;
        }
    }
    
    /**
     * Caches the use case instance (called during Quarkus startup).
     */
    public static void setReceiveFeedbackUseCase(ReceiveFeedbackUseCase useCase) {
        synchronized (LOCK) {
            useCaseInstance = useCase;
        }
    }
}
