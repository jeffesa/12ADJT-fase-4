/**
 * Domain layer: pure entities and gateway interfaces.
 * 
 * This layer contains business logic and domain models that are independent of
 * any framework or technology. According to Clean Architecture principles:
 * 
 * - Contains no framework annotations (@Entity, @Repository, @Service, etc.)
 * - Contains pure POJOs (Notification entity) and interface contracts (NotificationSender)
 * - Does not depend on the application or infrastructure layers
 * - Defines domain exceptions (NotificationException)
 * 
 * Sub-packages:
 * - entity: domain entities (Notification)
 * - gateway: interface contracts for external dependencies (NotificationSender)
 */
package com.fiap.notification.domain;
