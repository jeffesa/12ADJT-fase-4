/**
 * Lambda handlers for event-driven report generation.
 * 
 * GenerateReportHandler is triggered by EventBridge cron (Monday 08:00 UTC) to generate
 * and send weekly feedback reports based on the previous week's feedback data.
 */
package com.fiap.report.infra.handler;
