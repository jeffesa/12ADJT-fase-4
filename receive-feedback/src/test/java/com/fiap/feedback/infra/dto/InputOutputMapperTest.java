package com.fiap.feedback.infra.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;

/**
 * Unit tests for InputOutputMapper.
 * 
 * Verifies that the mapper correctly converts between domain entities
 * and DTOs with proper field mapping and edge case handling.
 */
class InputOutputMapperTest {

    @Test
    void testToOutputMapsAllFieldsCorrectly() {
        // Arrange
        String id = UUID.randomUUID().toString();
        String description = "Excellent service";
        int rating = 9;
        Urgency urgency = Urgency.LOW;
        LocalDateTime createdAt = LocalDateTime.now();
        
        Feedback feedback = new Feedback(id, description, rating, urgency, createdAt, false);
        
        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);
        
        // Assert
        assertNotNull(output);
        assertEquals(UUID.fromString(id), output.id());
        assertEquals(description, output.descricao());
        assertEquals(rating, output.nota());
        assertEquals("LOW", output.urgencia());
        assertEquals(createdAt, output.dataCriacao());
    }

    @Test
    void testToOutputWithHighUrgency() {
        // Arrange
        String id = UUID.randomUUID().toString();
        String description = "Poor service";
        int rating = 2;
        Urgency urgency = Urgency.HIGH;
        LocalDateTime createdAt = LocalDateTime.now();
        
        Feedback feedback = new Feedback(id, description, rating, urgency, createdAt, false);
        
        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);
        
        // Assert
        assertEquals("HIGH", output.urgencia());
    }

    @Test
    void testToOutputWithMediumUrgency() {
        // Arrange
        String id = UUID.randomUUID().toString();
        String description = "Average service";
        int rating = 6;
        Urgency urgency = Urgency.MEDIUM;
        LocalDateTime createdAt = LocalDateTime.now();
        
        Feedback feedback = new Feedback(id, description, rating, urgency, createdAt, false);
        
        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);
        
        // Assert
        assertEquals("MEDIUM", output.urgencia());
    }

    @Test
    void testToOutputWithEdgeCaseValues() {
        // Arrange: Boundary values
        String id = UUID.randomUUID().toString();
        String minDescription = "a";
        int minRating = 0;
        LocalDateTime now = LocalDateTime.now();
        
        Feedback feedback = new Feedback(id, minDescription, minRating, Urgency.HIGH, now, false);
        
        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);
        
        // Assert
        assertEquals(0, output.nota());
        assertEquals("HIGH", output.urgencia());

        // Test max rating
        id = UUID.randomUUID().toString();
        String maxDescription = "x".repeat(1000);
        int maxRating = 10;
        
        Feedback feedbackMax = new Feedback(id, maxDescription, maxRating, Urgency.LOW, now, false);
        ReceiveFeedbackOutput outputMax = InputOutputMapper.toOutput(feedbackMax);
        
        assertEquals(10, outputMax.nota());
        assertEquals("LOW", outputMax.urgencia());
    }

    @Test
    void testToOutputUUIDConversionIsCorrect() {
        // Arrange: Valid UUID string
        String uuidString = UUID.randomUUID().toString();
        UUID expectedUUID = UUID.fromString(uuidString);
        
        Feedback feedback = new Feedback(
            uuidString,
            "Test",
            5,
            Urgency.HIGH,
            LocalDateTime.now(),
            false
        );
        
        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);
        
        // Assert: UUID conversion is correct
        assertEquals(expectedUUID, output.id());
    }

    @Test
    void testToOutputCreatedAtPreserved() {
        // Arrange: Specific LocalDateTime (no truncation)
        LocalDateTime specificTime = LocalDateTime.of(2024, 3, 15, 14, 30, 45, 123456789);
        
        Feedback feedback = new Feedback(
            UUID.randomUUID().toString(),
            "Test feedback",
            5,
            Urgency.MEDIUM,
            specificTime,
            false
        );
        
        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);
        
        // Assert: Timestamp is preserved exactly
        assertEquals(specificTime, output.dataCriacao());
    }

    @Test
    void testToOutputThrowsNPE_FeedbackIsNull() {
        // Act & Assert
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> InputOutputMapper.toOutput(null)
        );
        
        // Exception is expected when feedback is null
        assertNotNull(exception);
    }

    @Test
    void testToOutputAllFieldsMapped() {
        // Arrange: Create feedback with known values
        String id = "550e8400-e29b-41d4-a716-446655440000";
        String description = "Test feedback for mapping";
        int rating = 7;
        Urgency urgency = Urgency.MEDIUM;
        LocalDateTime now = LocalDateTime.now();
        
        Feedback feedback = new Feedback(id, description, rating, urgency, now, false);
        
        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);
        
        // Assert: All 5 fields are populated
        assertNotNull(output.id(), "ID must be mapped");
        assertNotNull(output.descricao(), "Description must be mapped");
        assertNotNull(output.nota(), "Rating must be mapped");
        assertNotNull(output.urgencia(), "Urgency must be mapped");
        assertNotNull(output.dataCriacao(), "CreatedAt must be mapped");
        
        // Verify values
        assertEquals(UUID.fromString(id), output.id());
        assertEquals(description, output.descricao());
        assertEquals(rating, output.nota());
        assertEquals("MEDIUM", output.urgencia());
        assertEquals(now, output.dataCriacao());
    }

    @Test
    void testToOutputDescriptionMapping() {
        // Arrange: Test description with special characters
        String id = UUID.randomUUID().toString();
        String specialDescription = "Feedback: Very good! 5/5 ⭐";
        
        Feedback feedback = new Feedback(
            id,
            specialDescription,
            8,
            Urgency.LOW,
            LocalDateTime.now(),
            false
        );
        
        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);
        
        // Assert: Description is preserved exactly
        assertEquals(specialDescription, output.descricao());
    }

    @Test
    void testToOutputNotificationSentFieldHandled() {
        // Arrange: Test with notificationSent = true
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        
        Feedback feedbackSent = new Feedback(
            id,
            "Processed feedback",
            3,
            Urgency.HIGH,
            now,
            true
        );
        
        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedbackSent);
        
        // Assert: Mapping works regardless of notificationSent value
        assertNotNull(output);
        assertEquals("Processed feedback", output.descricao());
        assertEquals(Urgency.HIGH.name(), output.urgencia());
    }
}
