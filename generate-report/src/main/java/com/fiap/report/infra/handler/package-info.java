/**
 * Camada de Infraestrutura — Handler (Adapter de Entrada)
 *
 * Contém o handler AWS Lambda disparado pelo EventBridge (cron semanal).
 *
 * Classes esperadas:
 * - GenerateReportHandler: implementa RequestHandler<ScheduledEvent, Void>
 */
package com.fiap.report.infra.handler;
