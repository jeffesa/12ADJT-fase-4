/**
 * Application layer: use cases and business orchestration.
 *
 * Contains use cases that orchestrate domain entities and gateways to implement
 * high-level business logic. This layer depends on the domain layer but remains
 * independent of infrastructure concerns (Clean Architecture principle).
 * 
 * The application layer uses Jakarta CDI for dependency injection (@ApplicationScoped, @Inject)
 * to coordinate the execution of business workflows.
 */
package com.fiap.report.application;
