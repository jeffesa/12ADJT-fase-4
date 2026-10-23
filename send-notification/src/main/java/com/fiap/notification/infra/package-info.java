/**
 * Infrastructure Layer — SES Sender and Lambda Handler
 *
 * Contains the infrastructure components for notification delivery:
 * - SES email sender implementation
 * - Lambda SNS event handler
 * - Configuration mapping from application properties
 *
 * This layer implements the domain gateways and handles external integrations
 * (AWS SES, Lambda runtime), while the domain layer remains framework-independent.
 */
package com.fiap.notification.infra;
