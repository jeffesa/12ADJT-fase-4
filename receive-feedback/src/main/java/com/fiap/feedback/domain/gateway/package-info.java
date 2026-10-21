/**
 * Domain Layer — Gateways (Ports)
 *
 * Contains interfaces that define contracts for accessing external resources.
 * Implemented by the infrastructure layer. Has no framework dependencies.
 *
 * Expected interfaces:
 * - FeedbackGateway: persistence contract (save, findAll, findByPeriod)
 * - UrgencyPublisher: contract for publishing urgent events (publish)
 */
package com.fiap.feedback.domain.gateway;
