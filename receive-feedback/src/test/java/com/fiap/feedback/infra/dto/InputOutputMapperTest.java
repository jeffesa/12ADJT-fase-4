package com.fiap.feedback.infra.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;

/**
 * Unit tests for InputOutputMapper.
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
}
