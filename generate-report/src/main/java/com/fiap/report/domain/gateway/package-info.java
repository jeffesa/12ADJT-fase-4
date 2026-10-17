/**
 * Camada de Domínio — Gateways (Ports)
 *
 * Contém as interfaces que definem contratos de recursos externos.
 *
 * Interfaces esperadas:
 * - ReportGateway: consulta de feedbacks por período (findByPeriod)
 * - ReportSender: envio do relatório por e-mail (send)
 */
package com.fiap.report.domain.gateway;
