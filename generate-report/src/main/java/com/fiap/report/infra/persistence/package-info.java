/**
 * Infrastructure layer — DynamoDB persistence (Output Adapter).
 *
 * Contains the implementation of the ReportGateway interface using DynamoDB
 * for retrieving feedback records within a specified date range.
 *
 * Key classes:
 * - DynamoReportGateway: implements ReportGateway querying DynamoDB by period
 * - ReportDynamoMapper: maps DynamoDB items to Feedback domain entities
 */
package com.fiap.report.infra.persistence;
