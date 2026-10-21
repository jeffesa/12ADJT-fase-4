/**
 * Infrastructure Layer — Messaging (Output Adapter)
 *
 * Contains the implementation of the urgency publisher using AWS SNS.
 * 
 * Responsibilities:
 * - Implement UrgencyPublisher domain interface
 * - Publish HIGH urgency feedbacks to SNS topic for administrator notification
 * - Convert domain entities to JSON message format
 * - Handle SNS exceptions with appropriate logging
 * 
 * Classes:
 * - SnsUrgencyPublisher: implements UrgencyPublisher, manages SNS publish operations
 * - FeedbackMessageBuilder: utility for building JSON message payloads
 */
package com.fiap.feedback.infra.messaging;
