/**
 * Application layer: use cases.
 * 
 * Contains orchestration logic that coordinates business processes.
 * Depends only on domain interfaces and POJOs, not on infrastructure.
 * All classes are @ApplicationScoped CDI beans.
 * 
 * Use cases:
 * - SendNotificationUseCase: receives validated Notification, delegates send to NotificationSender,
 *   logs operations, and propagates NotificationException on failure.
 */
package com.fiap.notification.application.usecase;
