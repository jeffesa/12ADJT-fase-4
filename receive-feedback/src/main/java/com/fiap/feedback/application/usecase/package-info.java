/**
 * Application Layer — Use Cases
 *
 * Contains use cases that orchestrate business logic flows.
 * Depends only on domain interfaces (domain/gateway).
 * Has no external framework dependencies.
 *
 * Expected classes:
 * - ReceiveFeedbackUseCase: validates, persists, and publishes urgency notification if required
 */
package com.fiap.feedback.application.usecase;
