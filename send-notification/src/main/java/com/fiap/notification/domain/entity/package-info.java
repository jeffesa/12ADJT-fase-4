/**
 * Domain layer: entity POJOs.
 * 
 * Contains pure domain entities without framework annotations, following Clean Architecture.
 * 
 * Entities:
 * - Notification: immutable POJO representing a notification to be sent to an administrator,
 *   with fields: id (UUID), feedbackId (UUID), description, urgency (HIGH/MEDIUM/LOW),
 *   adminEmail, and createdAt (LocalDateTime). Auto-generates id and createdAt.
 */
package com.fiap.notification.domain.entity;
