package com.fiap.feedback.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Urgency enum.
 * 
 * Tests the fromRating(int) method to verify correct mapping of ratings
 * to urgency levels and proper exception handling for invalid ratings.
 */
class UrgencyEnumTest {

    @Test
    void testFromRating0ReturnsHigh() {
        // Act & Assert: Rating 0 maps to HIGH urgency
        Urgency result = Urgency.fromRating(0);
        assertEquals(Urgency.HIGH, result);
    }

    @Test
    void testFromRating5ReturnsHigh() {
        // Act & Assert: Rating 5 (boundary) maps to HIGH urgency
        Urgency result = Urgency.fromRating(5);
        assertEquals(Urgency.HIGH, result);
    }

    @Test
    void testFromRating6ReturnsMedium() {
        // Act & Assert: Rating 6 maps to MEDIUM urgency
        Urgency result = Urgency.fromRating(6);
        assertEquals(Urgency.MEDIUM, result);
    }

    @Test
    void testFromRating7ReturnsMedium() {
        // Act & Assert: Rating 7 (boundary) maps to MEDIUM urgency
        Urgency result = Urgency.fromRating(7);
        assertEquals(Urgency.MEDIUM, result);
    }

    @Test
    void testFromRating8ReturnsLow() {
        // Act & Assert: Rating 8 maps to LOW urgency
        Urgency result = Urgency.fromRating(8);
        assertEquals(Urgency.LOW, result);
    }

    @Test
    void testFromRating10ReturnsLow() {
        // Act & Assert: Rating 10 (boundary) maps to LOW urgency
        Urgency result = Urgency.fromRating(10);
        assertEquals(Urgency.LOW, result);
    }

    @Test
    void testFromRatingNegativeThrowsException() {
        // Act & Assert: Negative rating throws IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Urgency.fromRating(-1)
        );
        
        assertTrue(exception.getMessage().contains("Rating must be between 0 and 10"));
    }

    @Test
    void testFromRating11ThrowsException() {
        // Act & Assert: Rating above maximum throws IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Urgency.fromRating(11)
        );
        
        assertTrue(exception.getMessage().contains("Rating must be between 0 and 10"));
    }

    @Test
    void testFromRatingExceptionMessageContainsValidRange() {
        // Act & Assert: Exception message includes valid range boundaries
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Urgency.fromRating(-5)
        );
        
        String message = exception.getMessage();
        assertTrue(message.contains("0"), "Exception message should mention minimum value 0");
        assertTrue(message.contains("10"), "Exception message should mention maximum value 10");
    }

    @Test
    void testUrgencyEnumDescriptions() {
        // Assert: Verify all urgency levels have descriptions
        assertEquals("High", Urgency.HIGH.getDescription());
        assertEquals("Medium", Urgency.MEDIUM.getDescription());
        assertEquals("Low", Urgency.LOW.getDescription());
    }

    @Test
    void testUrgencyEnumValues() {
        // Assert: Verify Urgency.values() returns all three urgency levels
        Urgency[] values = Urgency.values();
        
        assertEquals(3, values.length);
        assertTrue(containsUrgency(values, Urgency.HIGH));
        assertTrue(containsUrgency(values, Urgency.MEDIUM));
        assertTrue(containsUrgency(values, Urgency.LOW));
    }

    @Test
    void testFromRatingMiddleValueInRange() {
        // Test middle values to ensure all ranges work correctly
        Urgency resultLow = Urgency.fromRating(3);
        assertEquals(Urgency.HIGH, resultLow);
        
        Urgency resultMedium = Urgency.fromRating(6);
        assertEquals(Urgency.MEDIUM, resultMedium);
        
        Urgency resultHigh = Urgency.fromRating(9);
        assertEquals(Urgency.LOW, resultHigh);
    }

    /**
     * Helper method to check if an urgency level exists in the array.
     */
    private boolean containsUrgency(Urgency[] values, Urgency target) {
        for (Urgency value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }
}
