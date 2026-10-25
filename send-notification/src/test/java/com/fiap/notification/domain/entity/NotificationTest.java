package com.fiap.notification.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Notification domain entity.
 *
 * Verifies constructors, auto-generation of id and createdAt,
 * validation, immutability, and object equality.
 */
class NotificationTest {

    @Test
    void testConvenienceConstructor_GeneratesIdAndCreatedAt() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        String description = "Critical system failure";
        String urgency = "HIGH";
        String adminEmail = "admin@company.com";

        LocalDateTime beforeCreation = LocalDateTime.now();

        // Act
        Notification notification = new Notification(feedbackId, description, urgency, adminEmail);

        LocalDateTime afterCreation = LocalDateTime.now();

        // Assert: id and createdAt are auto-generated
        assertNotNull(notification.getId());
        assertNotNull(notification.getCreatedAt());

        // Verify id is a valid UUID
        assertDoesNotThrow(() -> UUID.fromString(notification.getId().toString()));

        // Verify createdAt is within ±1 second of test execution time
        assertTrue(
            !notification.getCreatedAt().isBefore(beforeCreation.minusSeconds(1)),
            "Creation time should be at or after test start time"
        );
        assertTrue(
            !notification.getCreatedAt().isAfter(afterCreation.plusSeconds(1)),
            "Creation time should be at or before test end time"
        );
    }

    @Test
    void testConvenienceConstructor_SetsFeedbackIdAndDescription() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        String description = "Database connection timeout";
        String urgency = "MEDIUM";
        String adminEmail = "admin@company.com";

        // Act
        Notification notification = new Notification(feedbackId, description, urgency, adminEmail);

        // Assert
        assertEquals(feedbackId, notification.getFeedbackId());
        assertEquals(description, notification.getDescription());
    }

    @Test
    void testConvenienceConstructor_NormalizesUrgencyToUppercase() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        String description = "Test notification";
        String adminEmail = "admin@company.com";

        // Act
        Notification notificationHigh = new Notification(feedbackId, description, "high", adminEmail);
        Notification notificationMedium = new Notification(feedbackId, description, "medium", adminEmail);
        Notification notificationLow = new Notification(feedbackId, description, "low", adminEmail);

        // Assert: Urgency is normalized to uppercase
        assertEquals("HIGH", notificationHigh.getUrgency());
        assertEquals("MEDIUM", notificationMedium.getUrgency());
        assertEquals("LOW", notificationLow.getUrgency());
    }

    @Test
    void testFullConstructor_StoresAllFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();
        String description = "Critical system failure";
        String urgency = "HIGH";
        String adminEmail = "admin@company.com";
        LocalDateTime createdAt = LocalDateTime.of(2024, 10, 22, 14, 30, 0);

        // Act
        Notification notification = new Notification(id, feedbackId, description, urgency, adminEmail, createdAt);

        // Assert: All fields are correctly stored
        assertEquals(id, notification.getId());
        assertEquals(feedbackId, notification.getFeedbackId());
        assertEquals(description, notification.getDescription());
        assertEquals(urgency, notification.getUrgency());
        assertEquals(adminEmail, notification.getAdminEmail());
        assertEquals(createdAt, notification.getCreatedAt());
    }

    @Test
    void testFullConstructor_ThrowsNullPointerException_WhenIdIsNull() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        // Act & Assert
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new Notification(null, feedbackId, "Test", "HIGH", "admin@company.com", createdAt)
        );

        assertTrue(exception.getMessage().contains("id must not be null"));
    }

    @Test
    void testFullConstructor_ThrowsNullPointerException_WhenFeedbackIdIsNull() {
        // Arrange
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        // Act & Assert
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new Notification(id, null, "Test", "HIGH", "admin@company.com", createdAt)
        );

        assertTrue(exception.getMessage().contains("feedbackId must not be null"));
    }

    @Test
    void testFullConstructor_ThrowsNullPointerException_WhenCreatedAtIsNull() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();

        // Act & Assert
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new Notification(id, feedbackId, "Test", "HIGH", "admin@company.com", null)
        );

        assertTrue(exception.getMessage().contains("createdAt must not be null"));
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentException_WhenDescriptionIsEmpty() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Notification(feedbackId, "", "HIGH", "admin@company.com")
        );

        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentException_WhenDescriptionIsNull() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Notification(feedbackId, null, "HIGH", "admin@company.com")
        );

        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentException_WhenDescriptionIsWhitespaceOnly() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Notification(feedbackId, "   ", "HIGH", "admin@company.com")
        );

        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void testConstructor_TrimsDescription() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        String descriptionWithSpaces = "  System failure  ";

        // Act
        Notification notification = new Notification(feedbackId, descriptionWithSpaces, "HIGH", "admin@company.com");

        // Assert: Description is trimmed
        assertEquals("System failure", notification.getDescription());
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentException_WhenUrgencyIsInvalid() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Notification(feedbackId, "Test", "INVALID", "admin@company.com")
        );

        assertTrue(exception.getMessage().contains("must be one of HIGH, MEDIUM, or LOW"));
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentException_WhenUrgencyIsNull() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Notification(feedbackId, "Test", null, "admin@company.com")
        );

        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentException_WhenUrgencyIsEmpty() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Notification(feedbackId, "Test", "", "admin@company.com")
        );

        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentException_WhenAdminEmailIsEmpty() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Notification(feedbackId, "Test", "HIGH", "")
        );

        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentException_WhenAdminEmailIsNull() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Notification(feedbackId, "Test", "HIGH", null)
        );

        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void testConstructor_TrimsAdminEmail() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        String emailWithSpaces = "  admin@company.com  ";

        // Act
        Notification notification = new Notification(feedbackId, "Test", "HIGH", emailWithSpaces);

        // Assert: Email is trimmed
        assertEquals("admin@company.com", notification.getAdminEmail());
    }

    @Test
    void testGetters_ReturnCorrectValues() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();
        String description = "Critical issue";
        String urgency = "HIGH";
        String adminEmail = "admin@company.com";
        LocalDateTime createdAt = LocalDateTime.of(2024, 10, 22, 14, 30, 0);

        Notification notification = new Notification(id, feedbackId, description, urgency, adminEmail, createdAt);

        // Act & Assert: Each getter returns the correct value
        assertEquals(id, notification.getId());
        assertEquals(feedbackId, notification.getFeedbackId());
        assertEquals(description, notification.getDescription());
        assertEquals(urgency, notification.getUrgency());
        assertEquals(adminEmail, notification.getAdminEmail());
        assertEquals(createdAt, notification.getCreatedAt());
    }

    @Test
    void testToString_IsNonEmpty() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(feedbackId, "Test", "HIGH", "admin@company.com");

        // Act
        String toString = notification.toString();

        // Assert: toString is not empty
        assertNotNull(toString);
        assertTrue(toString.length() > 0);
    }

    @Test
    void testToString_ContainsAllFieldNames() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(feedbackId, "Test", "HIGH", "admin@company.com");

        // Act
        String toString = notification.toString();

        // Assert: toString contains all field names
        assertTrue(toString.contains("id"), "toString should contain 'id'");
        assertTrue(toString.contains("feedbackId"), "toString should contain 'feedbackId'");
        assertTrue(toString.contains("description"), "toString should contain 'description'");
        assertTrue(toString.contains("urgency"), "toString should contain 'urgency'");
        assertTrue(toString.contains("adminEmail"), "toString should contain 'adminEmail'");
        assertTrue(toString.contains("createdAt"), "toString should contain 'createdAt'");
    }

    @Test
    void testEquals_TwoNotificationsWithSameIdAreEqual() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Notification notification1 = new Notification(
            id, feedbackId, "Description", "HIGH", "admin@company.com", now
        );
        Notification notification2 = new Notification(
            id, feedbackId, "Description", "HIGH", "admin@company.com", now
        );

        // Act & Assert: Both have same all fields, so they are equal
        assertEquals(notification1, notification2);
    }

    @Test
    void testEquals_TwoNotificationsWithDifferentIdAreNotEqual() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Notification notification1 = new Notification(
            UUID.randomUUID(), feedbackId, "Description", "HIGH", "admin@company.com", now
        );
        Notification notification2 = new Notification(
            UUID.randomUUID(), feedbackId, "Description", "HIGH", "admin@company.com", now
        );

        // Act & Assert
        assertNotEquals(notification1, notification2);
    }

    @Test
    void testEquals_EqualsNullReturnsFalse() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(feedbackId, "Test", "HIGH", "admin@company.com");

        // Act & Assert
        assertNotEquals(null, notification);
        assertFalse(notification.equals(null));
    }

    @Test
    void testEquals_EqualsDifferentClassReturnsFalse() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(feedbackId, "Test", "HIGH", "admin@company.com");
        Object other = new Object();

        // Act & Assert
        assertNotEquals(notification, other);
        assertFalse(notification.equals(other));
    }

    @Test
    void testEquals_EqualsSelfReturnsTrue() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(feedbackId, "Test", "HIGH", "admin@company.com");

        // Act & Assert
        assertEquals(notification, notification);
        assertTrue(notification.equals(notification));
    }

    @Test
    void testHashCode_IsConsistent() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        Notification notification = new Notification(feedbackId, "Test", "HIGH", "admin@company.com");

        // Act
        int hashCode1 = notification.hashCode();
        int hashCode2 = notification.hashCode();

        // Assert: hashCode is consistent across calls
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    void testHashCode_EqualObjectsHaveSameHashCode() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Notification notification1 = new Notification(
            id, feedbackId, "Description", "HIGH", "admin@company.com", now
        );
        Notification notification2 = new Notification(
            id, feedbackId, "Description", "HIGH", "admin@company.com", now
        );

        // Act & Assert: Equal objects have the same hashCode
        assertEquals(notification1.hashCode(), notification2.hashCode());
    }

    @Test
    void testIdIsValidUUID() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        // Act
        Notification notification = new Notification(feedbackId, "Test", "HIGH", "admin@company.com");

        // Assert: id is a valid UUID
        assertNotNull(notification.getId());
        assertDoesNotThrow(() -> UUID.fromString(notification.getId().toString()));
    }

    @Test
    void testCreatedAtIsValidLocalDateTime() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();

        // Act
        Notification notification = new Notification(feedbackId, "Test", "HIGH", "admin@company.com");

        // Assert: createdAt is a valid LocalDateTime
        assertNotNull(notification.getCreatedAt());
        assertDoesNotThrow(() -> {
            notification.getCreatedAt().toString();
        });
    }
}
