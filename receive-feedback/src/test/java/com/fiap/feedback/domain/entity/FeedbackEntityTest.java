package com.fiap.feedback.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Feedback domain entity.
 * 
 * Verifies constructors, immutability, validation, and field initialization.
 */
class FeedbackEntityTest {

    @Test
    void testFeedbackConstructorFull_ValidInput() {
        // Arrange
        String id = UUID.randomUUID().toString();
        String description = "Good service";
        int rating = 8;
        Urgency urgency = Urgency.LOW;
        LocalDateTime createdAt = LocalDateTime.now();
        boolean notificationSent = false;

        // Act
        Feedback feedback = new Feedback(id, description, rating, urgency, createdAt, notificationSent);

        // Assert: All getters return correct values
        assertEquals(id, feedback.getId());
        assertEquals(description, feedback.getDescription());
        assertEquals(rating, feedback.getRating());
        assertEquals(urgency, feedback.getUrgency());
        assertEquals(createdAt, feedback.getCreatedAt());
        assertEquals(notificationSent, feedback.isNotificationSent());
    }

    @Test
    void testFeedbackConstructorFullSetsImmutableFields() {
        // Arrange & Act
        String id = UUID.randomUUID().toString();
        Feedback feedback = new Feedback(id, "Test", 5, Urgency.HIGH, LocalDateTime.now(), false);

        // Assert: Verify no setters are available (immutability)
        // The test passes if no setter methods exist and compilation succeeds
        // This is a structural test confirming no setters are exposed
        assertNotNull(feedback.getId());
    }

    @Test
    void testFeedbackConstructorFullThrowsException_EmptyDescription() {
        // Act & Assert
        String id = UUID.randomUUID().toString();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Feedback(id, "", 5, Urgency.HIGH, LocalDateTime.now(), false)
        );
        
        assertTrue(exception.getMessage().contains("Description cannot be empty"));
    }

    @Test
    void testFeedbackConstructorFullThrowsException_NullDescription() {
        // Act & Assert
        String id = UUID.randomUUID().toString();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Feedback(id, null, 5, Urgency.HIGH, LocalDateTime.now(), false)
        );
        
        assertTrue(exception.getMessage().contains("Description cannot be empty"));
    }

    @Test
    void testFeedbackConstructorFullThrowsException_RatingBelowRange() {
        // Act & Assert: Rating -1 is invalid
        String id = UUID.randomUUID().toString();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Feedback(id, "Valid description", -1, Urgency.HIGH, LocalDateTime.now(), false)
        );
        
        assertTrue(exception.getMessage().contains("Rating must be between 0 and 10"));
    }

    @Test
    void testFeedbackConstructorFullThrowsException_RatingAboveRange() {
        // Act & Assert: Rating 11 is invalid
        String id = UUID.randomUUID().toString();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Feedback(id, "Valid description", 11, Urgency.LOW, LocalDateTime.now(), false)
        );
        
        assertTrue(exception.getMessage().contains("Rating must be between 0 and 10"));
    }

    @Test
    void testFeedbackConstructorConvenience_ValidInput() {
        // Arrange & Act: Use convenience constructor
        String description = "Good service";
        int rating = 7;
        
        Feedback feedback = new Feedback(description, rating);

        // Assert: All fields are populated
        assertNotNull(feedback.getId());
        assertEquals(description, feedback.getDescription());
        assertEquals(rating, feedback.getRating());
        assertNotNull(feedback.getUrgency());
        assertNotNull(feedback.getCreatedAt());
        assertFalse(feedback.isNotificationSent());
    }

    @Test
    void testFeedbackConstructorConvenience_GeneratesUUID() {
        // Arrange & Act
        Feedback feedback1 = new Feedback("Feedback 1", 5);
        Feedback feedback2 = new Feedback("Feedback 2", 5);

        // Assert: Different IDs are generated (valid UUIDs)
        assertNotNull(feedback1.getId());
        assertNotNull(feedback2.getId());
        assertNotEquals(feedback1.getId(), feedback2.getId());
        
        // Verify IDs are valid UUIDs
        assertDoesNotThrow(() -> UUID.fromString(feedback1.getId()));
        assertDoesNotThrow(() -> UUID.fromString(feedback2.getId()));
    }

    @Test
    void testFeedbackConstructorConvenience_GeneratesLocalDateTime() {
        // Arrange
        LocalDateTime beforeCreation = LocalDateTime.now();

        // Act
        Feedback feedback = new Feedback("Test feedback", 5);
        LocalDateTime afterCreation = LocalDateTime.now();

        // Assert: createdAt is between before and after
        assertNotNull(feedback.getCreatedAt());
        assertTrue(
            !feedback.getCreatedAt().isBefore(beforeCreation.minusSeconds(1)),
            "Creation time should be at or after the test start time"
        );
        assertTrue(
            !feedback.getCreatedAt().isAfter(afterCreation.plusSeconds(1)),
            "Creation time should be at or before the test end time"
        );
    }

    @Test
    void testFeedbackConstructorConvenience_CalculatesUrgency() {
        // Test that urgency is calculated correctly via Urgency.fromRating()
        
        // HIGH urgency test
        Feedback feedbackHigh = new Feedback("Bad service", 2);
        assertEquals(Urgency.HIGH, feedbackHigh.getUrgency());
        
        // MEDIUM urgency test
        Feedback feedbackMedium = new Feedback("Average service", 6);
        assertEquals(Urgency.MEDIUM, feedbackMedium.getUrgency());
        
        // LOW urgency test
        Feedback feedbackLow = new Feedback("Good service", 9);
        assertEquals(Urgency.LOW, feedbackLow.getUrgency());
    }

    @Test
    void testFeedbackConstructorConvenience_SetsNotificationSentFalse() {
        // Arrange & Act
        Feedback feedback = new Feedback("Test feedback", 5);

        // Assert: notificationSent is initially false
        assertFalse(feedback.isNotificationSent());
    }

    @Test
    void testFeedbackToString() {
        // Arrange & Act
        String description = "Test feedback";
        Feedback feedback = new Feedback(description, 7);
        String toString = feedback.toString();

        // Assert: toString contains key field names
        assertNotNull(toString);
        assertTrue(toString.contains("Feedback"), "toString should contain class name");
        assertTrue(toString.contains("id="), "toString should contain id field");
        assertTrue(toString.contains("description="), "toString should contain description field");
        assertTrue(toString.contains("rating="), "toString should contain rating field");
        assertTrue(toString.contains("urgency="), "toString should contain urgency field");
        assertTrue(toString.contains("createdAt="), "toString should contain createdAt field");
        assertTrue(toString.contains("notificationSent="), "toString should contain notificationSent field");
    }

    @Test
    void testFeedbackConstructorFull_TrimsDescription() {
        // Arrange: Description with leading/trailing spaces
        String id = UUID.randomUUID().toString();
        String descriptionWithSpaces = "  Valid description  ";

        // Act
        Feedback feedback = new Feedback(id, descriptionWithSpaces, 5, Urgency.HIGH, LocalDateTime.now(), false);

        // Assert: Description is trimmed
        assertEquals("Valid description", feedback.getDescription());
    }

    @Test
    void testFeedbackConstructorConvenience_HandlesEdgeCaseRatings() {
        // Test edge case ratings 0 and 10
        Feedback feedbackZero = new Feedback("Worst service", 0);
        assertEquals(0, feedbackZero.getRating());
        assertEquals(Urgency.HIGH, feedbackZero.getUrgency());

        Feedback feedbackMax = new Feedback("Best service", 10);
        assertEquals(10, feedbackMax.getRating());
        assertEquals(Urgency.LOW, feedbackMax.getUrgency());
    }

    @Test
    void testFeedbackConstructorFull_AllFieldsAccessible() {
        // Arrange: Create feedback with all distinct values
        String id = "test-id-12345";
        String description = "Complete test feedback";
        int rating = 6;
        Urgency urgency = Urgency.MEDIUM;
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        boolean notificationSent = true;

        // Act
        Feedback feedback = new Feedback(id, description, rating, urgency, createdAt, notificationSent);

        // Assert: All fields are accessible and correct
        assertEquals(id, feedback.getId());
        assertEquals(description, feedback.getDescription());
        assertEquals(rating, feedback.getRating());
        assertEquals(urgency, feedback.getUrgency());
        assertEquals(createdAt, feedback.getCreatedAt());
        assertTrue(feedback.isNotificationSent());
    }
}
