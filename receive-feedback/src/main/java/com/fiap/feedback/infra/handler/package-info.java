/**
 * Camada de Infraestrutura — Handler (Adapter de Entrada)
 *
 * Contém o handler AWS Lambda que recebe eventos do API Gateway,
 * converte para DTOs, chama o use case e retorna a resposta HTTP.
 *
 * Classes esperadas:
 * - ReceiveFeedbackHandler: implementa RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>
 */
package com.fiap.feedback.infra.handler;
