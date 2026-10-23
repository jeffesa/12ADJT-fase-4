package com.fiap.feedback.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.domain.gateway.FeedbackGateway;
import com.fiap.feedback.domain.gateway.UrgencyPublisher;
import com.fiap.feedback.infra.config.FeedbackConfiguration;
import com.fiap.feedback.infra.config.UrgencyCalculator;

/**
 * Unit tests for ReceiveFeedbackUseCase.
 * 
 * Verifies that the core business logic of feedback reception, validation,
 * persistence, and urgency publishing works correctly.
 */
class ReceiveFeedbackUseCaseTest {

    private ReceiveFeedbackUseCase useCase;
    private FakeFeedbackGateway feedbackGateway;
    private FakeUrgencyPublisher urgencyPublisher;
    private UrgencyCalculator urgencyCalculator;
    private FeedbackConfiguration feedbackConfig;

    @BeforeEach
    void setUp() {
        // Create mock configuration with complete urgency thresholds
        feedbackConfig = new FeedbackConfiguration() {
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
                    public int ratingMin() {
                        return 0;
                    }

                    @Override
                    public int ratingMax() {
                        return 10;
                    }

                    @Override
                    public int descriptionMinLength() {
                        return 5;
                    }

                    @Override
                    public int descriptionMaxLength() {
                        return 1000;
                    }
                };
            }
        };
        
        // Create fake dependencies that track calls
        feedbackGateway = new FakeFeedbackGateway();
        urgencyPublisher = new FakeUrgencyPublisher();
        
        // Use real urgency calculator with complete config
        urgencyCalculator = new UrgencyCalculator(feedbackConfig);
        
        useCase = new ReceiveFeedbackUseCase(
            feedbackGateway,
            urgencyPublisher,
            urgencyCalculator,
            feedbackConfig
        );
    }

    @Test
    void testReceiveFeedbackSuccessWithValidInput_RatingLow() {
        // Arrange: Valid feedback with LOW urgency (rating 8)
        String description = "Excellent service provided";
        int rating = 8;

        // Act
        Feedback result = useCase.receiveFeedback(description, rating);

        // Assert: save called once, publish NOT called (LOW urgency)
        assertEquals(1, feedbackGateway.getSaveCallCount());
        assertEquals(0, urgencyPublisher.getPublishCallCount());
        assertNotNull(result);
        assertEquals(description, result.getDescription());
        assertEquals(rating, result.getRating());
        assertEquals(Urgency.LOW, result.getUrgency());
    }

    @Test
    void testReceiveFeedbackSuccessWithValidInput_RatingHigh() {
        // Arrange: Valid feedback with HIGH urgency (rating 3)
        String description = "Very poor experience";
        int rating = 3;

        // Act
        Feedback result = useCase.receiveFeedback(description, rating);

        // Assert: save called once, publish called once (HIGH urgency)
        assertEquals(1, feedbackGateway.getSaveCallCount());
        assertEquals(1, urgencyPublisher.getPublishCallCount());
        assertNotNull(result);
        assertEquals(Urgency.HIGH, result.getUrgency());
    }

    @Test
    void testReceiveFeedbackSuccessWithValidInput_RatingMedium() {
        // Arrange: Valid feedback with MEDIUM urgency (rating 7)
        String description = "Service was average";
        int rating = 7;

        // Act
        Feedback result = useCase.receiveFeedback(description, rating);

        // Assert: save called once, publish NOT called (MEDIUM urgency)
        assertEquals(1, feedbackGateway.getSaveCallCount());
        assertEquals(0, urgencyPublisher.getPublishCallCount());
        assertNotNull(result);
        assertEquals(Urgency.MEDIUM, result.getUrgency());
    }

    @Test
    void testReceiveFeedbackThrowsException_EmptyDescription() {
        // Act & Assert: Empty description throws IllegalArgumentException
        String emptyDescription = "";
        int rating = 5;
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.receiveFeedback(emptyDescription, rating)
        );
        
        assertEquals("Description cannot be empty", exception.getMessage());
        assertEquals(0, feedbackGateway.getSaveCallCount());
        assertEquals(0, urgencyPublisher.getPublishCallCount());
    }

    @Test
    void testReceiveFeedbackThrowsException_NullDescription() {
        // Act & Assert: Null description throws IllegalArgumentException
        int rating = 5;
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.receiveFeedback(null, rating)
        );
        
        assertEquals("Description cannot be empty", exception.getMessage());
        assertEquals(0, feedbackGateway.getSaveCallCount());
        assertEquals(0, urgencyPublisher.getPublishCallCount());
    }

    @Test
    void testReceiveFeedbackThrowsException_DescriptionTooShort() {
        // Arrange: Description shorter than configured minimum (5 characters)
        String shortDescription = "Bad";
        int rating = 5;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.receiveFeedback(shortDescription, rating)
        );
        
        assertTrue(exception.getMessage().contains("Description must be at least"));
        assertEquals(0, feedbackGateway.getSaveCallCount());
        assertEquals(0, urgencyPublisher.getPublishCallCount());
    }

    @Test
    void testReceiveFeedbackThrowsException_RatingBelowMin() {
        // Act & Assert: Rating below minimum (0)
        String description = "Valid description";
        int ratingBelowMin = -1;
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.receiveFeedback(description, ratingBelowMin)
        );
        
        assertTrue(exception.getMessage().contains("Rating must be at least"));
        assertEquals(0, feedbackGateway.getSaveCallCount());
        assertEquals(0, urgencyPublisher.getPublishCallCount());
    }

    @Test
    void testReceiveFeedbackThrowsException_RatingAboveMax() {
        // Act & Assert: Rating above maximum (10)
        String description = "Valid description";
        int ratingAboveMax = 11;
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.receiveFeedback(description, ratingAboveMax)
        );
        
        assertTrue(exception.getMessage().contains("Rating must be at most"));
        assertEquals(0, feedbackGateway.getSaveCallCount());
        assertEquals(0, urgencyPublisher.getPublishCallCount());
    }

    @Test
    void testReceiveFeedbackPublishesOnlyWhenUrgencyIsHigh() {
        // Test three scenarios: HIGH (publish), MEDIUM (no publish), LOW (no publish)
        
        // Scenario 1: HIGH urgency
        useCase.receiveFeedback("Critical issue", 2);
        assertEquals(1, feedbackGateway.getSaveCallCount());
        assertEquals(1, urgencyPublisher.getPublishCallCount());

        // Reset counter
        feedbackGateway.reset();
        urgencyPublisher.reset();

        // Scenario 2: MEDIUM urgency
        useCase.receiveFeedback("Average", 6);
        assertEquals(1, feedbackGateway.getSaveCallCount());
        assertEquals(0, urgencyPublisher.getPublishCallCount());

        // Reset counter
        feedbackGateway.reset();
        urgencyPublisher.reset();

        // Scenario 3: LOW urgency
        useCase.receiveFeedback("Good service", 9);
        assertEquals(1, feedbackGateway.getSaveCallCount());
        assertEquals(0, urgencyPublisher.getPublishCallCount());
    }

    @Test
    void testReceiveFeedbackReturnsSavedFeedback() {
        // Arrange: Prepare a feedback with all fields populated
        String description = "Good feedback";
        int rating = 7;

        // Act
        Feedback result = useCase.receiveFeedback(description, rating);

        // Assert: All fields are present
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(description, result.getDescription());
        assertEquals(rating, result.getRating());
        assertEquals(Urgency.MEDIUM, result.getUrgency());
        assertNotNull(result.getCreatedAt());
        assertFalse(result.isNotificationSent());
    }

    @Test
    void testReceiveFeedbackSaveCalledExactlyOnce() {
        // Arrange
        String description = "Test feedback";
        int rating = 5;

        // Act
        useCase.receiveFeedback(description, rating);

        // Assert: save was called exactly once
        assertEquals(1, feedbackGateway.getSaveCallCount());
    }

    /**
     * Fake FeedbackGateway for testing without mocking
     */
    private static class FakeFeedbackGateway implements FeedbackGateway {
        private int saveCallCount = 0;

        @Override
        public Feedback save(Feedback feedback) {
            saveCallCount++;
            return feedback;
        }

        @Override
        public java.util.List<Feedback> findAll() {
            return java.util.Collections.emptyList();
        }

        @Override
        public java.util.List<Feedback> findByPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
            return java.util.Collections.emptyList();
        }

        public int getSaveCallCount() {
            return saveCallCount;
        }

        public void reset() {
            saveCallCount = 0;
        }
    }

    /**
     * Fake UrgencyPublisher for testing without mocking
     */
    private static class FakeUrgencyPublisher implements UrgencyPublisher {
        private int publishCallCount = 0;

        @Override
        public void publish(Feedback feedback) {
            publishCallCount++;
        }

        public int getPublishCallCount() {
            return publishCallCount;
        }

        public void reset() {
            publishCallCount = 0;
        }
    }
}
