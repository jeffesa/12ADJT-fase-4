/**
 * Application layer - Use cases and orchestration.
 * 
 * This layer implements the business logic of the application through use cases.
 * Each use case coordinates domain entities, gateways, and services to perform
 * a specific business function.
 * 
 * Responsibilities:
 * - Orchestrate domain entities (business logic)
 * - Depend on domain interfaces (FeedbackGateway, UrgencyPublisher, UrgencyCalculator)
 * - Validate input according to business rules and configuration
 * - Coordinate interactions between domain layer and infrastructure layer
 * - Throw domain-appropriate exceptions (IllegalArgumentException for validation)
 * 
 * Dependencies:
 * - Domain layer (entities, gateways, interfaces)
 * - Infrastructure layer (CDI beans, configuration)
 * - Quarkus CDI only (@ApplicationScoped, @Inject)
 * 
 * No direct framework annotations on entities (no @Entity, @Table, etc.)
 * No ORM dependencies - persistence delegated to FeedbackGateway implementation.
 */
package com.fiap.feedback.application;
