package com.fiap.feedback.infra.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fiap.feedback.domain.entity.Urgency;

/**
 * Unit tests for UrgencyCalculator.
 * 
 * Verifies that the calculator correctly maps ratings to urgency levels
 * based on the configured thresholds.
 */
class UrgencyCalculatorTest {

    private UrgencyCalculator calculator;
    private FeedbackConfiguration mockConfig;

    @BeforeEach
    void setUp() {
        // Create a mock configuration with the default thresholds
        mockConfig = new FeedbackConfiguration() {
            @Override
            public Urgency urgency() {
                return new Urgency() {
                    @Override
                    public Range high() {
                        return new Range() {
                            @Override
                            public int min() { return 0; }
                            @Override
                            public int max() { return 5; }
                        };
                    }
                    @Override
                    public Range medium() {
                        return new Range() {
                            @Override
                            public int min() { return 6; }
                            @Override
                            public int max() { return 7; }
                        };
                    }
                    @Override
                    public Range low() {
                        return new Range() {
                            @Override
                            public int min() { return 8; }
                            @Override
                            public int max() { return 10; }
                        };
                    }
                };
            }
            @Override
            public Validation validation() {
                return new Validation() {
                    @Override
                    public int ratingMin() { return 0; }
                    @Override
                    public int ratingMax() { return 10; }
                    @Override
                    public int descriptionMinLength() { return 5; }
                    @Override
                    public int descriptionMaxLength() { return 1000; }
                };
            }
        };

        calculator = new UrgencyCalculator(mockConfig);
    }

    @Test
    void testRating0ReturnsHigh() {
        Urgency result = calculator.calculateUrgency(0);
        assertEquals(Urgency.HIGH, result);
    }

    @Test
    void testRating5ReturnsHigh() {
        Urgency result = calculator.calculateUrgency(5);
        assertEquals(Urgency.HIGH, result);
    }

    @Test
    void testRating6ReturnsMedium() {
        Urgency result = calculator.calculateUrgency(6);
        assertEquals(Urgency.MEDIUM, result);
    }

    @Test
    void testRating7ReturnsMedium() {
        Urgency result = calculator.calculateUrgency(7);
        assertEquals(Urgency.MEDIUM, result);
    }

    @Test
    void testRating8ReturnsLow() {
        Urgency result = calculator.calculateUrgency(8);
        assertEquals(Urgency.LOW, result);
    }

    @Test
    void testRating10ReturnsLow() {
        Urgency result = calculator.calculateUrgency(10);
        assertEquals(Urgency.LOW, result);
    }

    @Test
    void testRatingBelowMinThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculateUrgency(-1);
        });
    }

    @Test
    void testRatingAboveMaxThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.calculateUrgency(11);
        });
    }
}
