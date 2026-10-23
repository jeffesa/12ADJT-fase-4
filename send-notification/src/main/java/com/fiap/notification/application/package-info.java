/**
 * Application layer: use cases and orchestration.
 * 
 * This layer contains use cases that orchestrate business processes by coordinating
 * domain entities and gateway interfaces. Following Clean Architecture:
 * 
 * - Depends only on domain entities and gateway interfaces (the "ports")
 * - Does not depend on infrastructure implementations
 * - Uses CDI (@ApplicationScoped, @Inject) for dependency injection
 * - Contains no business logic; acts purely as an orchestrator
 * 
 * Sub-packages:
 * - usecase: business process orchestrators (SendNotificationUseCase)
 */
package com.fiap.notification.application;
