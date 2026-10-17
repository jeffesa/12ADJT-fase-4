/**
 * Camada de Domínio — Gateways (Ports)
 *
 * Contém as interfaces que definem os contratos de acesso a recursos externos.
 * Implementadas pela camada de infra. Não possui dependência de framework.
 *
 * Interfaces esperadas:
 * - FeedbackGateway: contrato de persistência (save, findAll, findByPeriod)
 * - UrgencyPublisher: contrato de publicação de eventos urgentes (publish)
 */
package com.fiap.feedback.domain.gateway;
