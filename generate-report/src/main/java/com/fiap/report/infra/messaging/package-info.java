/**
 * Infrastructure layer — Email messaging (Output Adapter).
 *
 * Contains the implementation of the ReportSender interface for sending
 * weekly feedback reports via AWS SES email service.
 *
 * Key classes:
 * - SesReportSender: implements ReportSender sending reports via SES
 * - ReportEmailBuilder: constructs HTML email templates for reports
 */
package com.fiap.report.infra.messaging;
