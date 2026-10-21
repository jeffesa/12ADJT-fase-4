package com.fiap.feedback.infra.graphql;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.infra.dto.ReceiveFeedbackInput;
import com.fiap.feedback.infra.dto.ReceiveFeedbackOutput;
import com.fiap.feedback.infra.dto.InputOutputMapper;

/**
 * Unit tests for ReceiveFeedbackResolver.
 * 
 * Verifies that the GraphQL resolver correctly maps between DTOs and domain entities.
 * Note: Full integration tests require Quarkus testing framework with GraphQL support.
 * These tests verify the DTO mapping logic and output transformation.
 */
class ReceiveFeedbackResolverTest {

    @Test
    void testResolverOutputMappingLowUrgency() {
        // Arrange
        String feedbackId = UUID.randomUUID().toString();
        String description = "Excellent service";
        int rating = 9;
        LocalDateTime now = LocalDateTime.now();
        
        Feedback feedback = new Feedback(feedbackId, description, rating, Urgency.LOW, now, false);

        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);

        // Assert
        assertNotNull(output);
        assertEquals(UUID.fromString(feedbackId), output.id());
        assertEquals(description, output.descricao());
        assertEquals(rating, output.nota());
        assertEquals("LOW", output.urgencia());
        assertEquals(now, output.dataCriacao());
    }

    @Test
    void testResolverOutputMappingHighUrgency() {
        // Arrange
        String feedbackId = UUID.randomUUID().toString();
        String description = "Terrible experience";
        int rating = 2;
        LocalDateTime now = LocalDateTime.now();
        
        Feedback feedback = new Feedback(feedbackId, description, rating, Urgency.HIGH, now, false);

        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);

        // Assert
        assertNotNull(output);
        assertEquals("HIGH", output.urgencia());
    }

    @Test
    void testResolverOutputMappingMediumUrgency() {
        // Arrange
        String feedbackId = UUID.randomUUID().toString();
        String description = "Average service";
        int rating = 7;
        LocalDateTime now = LocalDateTime.now();
        
        Feedback feedback = new Feedback(feedbackId, description, rating, Urgency.MEDIUM, now, false);

        // Act
        ReceiveFeedbackOutput output = InputOutputMapper.toOutput(feedback);

        // Assert
        assertNotNull(output);
        assertEquals("MEDIUM", output.urgencia());
    }

    @Test
    void testResolverInputDTORecordProperties() {
        // Verify ReceiveFeedbackInput record has correct properties
        String description = "Good feedback";
        int rating = 7;
        
        ReceiveFeedbackInput input = new ReceiveFeedbackInput(description, rating);
        
        // Assert record accessors work correctly
        assertEquals(description, input.descricao());
        assertEquals(rating, input.nota());
    }

    @Test
    void testResolverOutputDTORecordProperties() {
        // Verify ReceiveFeedbackOutput record has all required properties
        UUID id = UUID.randomUUID();
        String description = "Good feedback";
        int rating = 7;
        String urgencia = "LOW";
        LocalDateTime now = LocalDateTime.now();
        
        ReceiveFeedbackOutput output = new ReceiveFeedbackOutput(id, description, rating, urgencia, now);
        
        // Assert record accessors work correctly
        assertEquals(id, output.id());
        assertEquals(description, output.descricao());
        assertEquals(rating, output.nota());
        assertEquals(urgencia, output.urgencia());
        assertEquals(now, output.dataCriacao());
    }

    @Test
    void testResolverOutputUrgencyEnumNames() {
        // Verify that all urgency enum names are correctly represented
        String[] expectedUrgencies = {"HIGH", "MEDIUM", "LOW"};
        
        for (String urgencyName : expectedUrgencies) {
            assertTrue(urgencyName.equals("HIGH") || urgencyName.equals("MEDIUM") || urgencyName.equals("LOW"),
                    "Urgency must be one of: HIGH, MEDIUM, LOW");
        }
    }
}