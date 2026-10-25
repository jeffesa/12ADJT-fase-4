/**
 * Application layer: use cases and business orchestration.
 *
 * Contains use cases that orchestrate domain entities and gateways to implement
 * high-level business logic. Use cases depend only on domain contracts (gateways
 * and entities), maintaining separation from infrastructure concerns
 * (Clean Architecture principle).
 *
 * Use cases:
 * - GenerateReportUseCase: orchestrates feedback retrieval, aggregation, and report delivery
 */
package com.fiap.report.application.usecase;
