/**
 * Infrastructure Layer — Handler (Input Adapter)
 *
 * Contains the AWS Lambda handler that receives API Gateway events,
 * converts them to DTOs, calls the use case, and returns HTTP responses.
 *
 * Expected classes:
 * - ReceiveFeedbackHandler: implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>
 */
package com.fiap.feedback.infra.handler;
