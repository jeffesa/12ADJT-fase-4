/**
 * Gateway interfaces: report domain contracts.
 *
 * Contains interfaces that define contracts for external resources and services.
 * These gateways enable the domain layer to remain independent of infrastructure
 * concerns while declaring the dependencies it requires (Clean Architecture principle).
 *
 * Gateways:
 * - ReportGateway: contract for feedback retrieval by period
 * - ReportSender: contract for report delivery (email, messaging, etc.)
 * - ReportException: domain exception for report operation errors
 */
package com.fiap.report.domain.gateway;
