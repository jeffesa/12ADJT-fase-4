/**
 * Domain layer: gateway interfaces (ports).
 * 
 * Contains pure interface contracts that define communication with external systems.
 * Implementations reside in the infrastructure layer, not the domain.
 * 
 * Interfaces:
 * - NotificationSender: contract for delivering notifications (send method).
 * 
 * Exceptions:
 * - NotificationException: checked exception thrown on notification delivery failure.
 */
package com.fiap.notification.domain.gateway;
