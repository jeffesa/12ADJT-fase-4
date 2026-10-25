/**
 * Domain layer: report entities.
 *
 * Contains pure domain entities (POJOs without framework annotations).
 * Entities are immutable value objects representing core business concepts.
 *
 * Entities:
 * - WeeklyReport: aggregated report data (period, total, average rating, by day, by urgency)
 * - FeedbackStatistics: aggregated statistics from feedback collection
 * - ReportPeriod: immutable time period for reports
 * - Feedback: report-side feedback representation from persistence layer
 * - Urgency: feedback urgency enum (HIGH, MEDIUM, LOW)
 */
package com.fiap.report.domain.entity;
