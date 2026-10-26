package com.fiap.feedback.infra.graphql;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.feedback.application.usecase.ReceiveFeedbackUseCase;
import com.fiap.feedback.domain.entity.Feedback;
import com.fiap.feedback.domain.entity.Urgency;
import com.fiap.feedback.domain.gateway.FeedbackGateway;
import com.fiap.feedback.domain.gateway.UrgencyPublisher;
import com.fiap.feedback.infra.config.FeedbackConfiguration;
import com.fiap.feedback.infra.config.UrgencyCalculator;
import com.fiap.feedback.infra.dto.ReceiveFeedbackOutput;

/**
 * Unit tests for ReceiveFeedbackQueryResolver.
 * 
 * Verifies that the GraphQL query resolver correctly processes feedback data,
 * maps between entities and DTOs, and handles various scenarios.
 */
class ReceiveFeedbackQueryResolverTest {

    @Test
    void testQueryReturnsEmptyList_WhenNoFeedbacksExist() {
        // Arrange: Create a resolver with a use case that returns empty list
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(Collections.emptyList());
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        List<ReceiveFeedbackOutput> result = resolver.listarAvaliacoes();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void testQueryReturnsSingleFeedback() {
        // Arrange: Create one feedback with specific values
        String feedbackId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        Feedback feedback = new Feedback(feedbackId, "Good service", 7, Urgency.MEDIUM, now, false);
        
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(List.of(feedback));
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        List<ReceiveFeedbackOutput> result = resolver.listarAvaliacoes();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        ReceiveFeedbackOutput output = result.get(0);
        assertEquals(UUID.fromString(feedbackId), output.id());
        assertEquals("Good service", output.descricao());
        assertEquals(7, output.nota());
        assertEquals("MEDIUM", output.urgencia());
        assertEquals(now, output.dataCriacao());
    }

    @Test
    void testQueryReturnsMultipleFeedbacks_WithDifferentUrgencies() {
        // Arrange: Create three feedbacks with different urgencies
        LocalDateTime now = LocalDateTime.now();
        
        Feedback feedbackHigh = new Feedback(
            UUID.randomUUID().toString(),
            "Bad experience",
            4,
            Urgency.HIGH,
            now,
            false
        );
        
        Feedback feedbackMedium = new Feedback(
            UUID.randomUUID().toString(),
            "Okay service",
            6,
            Urgency.MEDIUM,
            now,
            false
        );
        
        Feedback feedbackLow = new Feedback(
            UUID.randomUUID().toString(),
            "Excellent service",
            9,
            Urgency.LOW,
            now,
            false
        );
        
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(
            List.of(feedbackHigh, feedbackMedium, feedbackLow)
        );
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        List<ReceiveFeedbackOutput> result = resolver.listarAvaliacoes();

        // Assert: All three feedbacks are returned
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify HIGH urgency
        assertEquals("HIGH", result.get(0).urgencia());
        assertEquals(4, result.get(0).nota());
        assertEquals("Bad experience", result.get(0).descricao());
        
        // Verify MEDIUM urgency
        assertEquals("MEDIUM", result.get(1).urgencia());
        assertEquals(6, result.get(1).nota());
        assertEquals("Okay service", result.get(1).descricao());
        
        // Verify LOW urgency
        assertEquals("LOW", result.get(2).urgencia());
        assertEquals(9, result.get(2).nota());
        assertEquals("Excellent service", result.get(2).descricao());
    }

    @Test
    void testQueryMapsAllOutputFields_Correctly() {
        // Arrange: Create a feedback with all fields set
        String feedbackId = UUID.randomUUID().toString();
        String description = "Test feedback";
        int rating = 5;
        Urgency urgency = Urgency.HIGH;
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 10, 30, 45);
        
        Feedback feedback = new Feedback(feedbackId, description, rating, urgency, createdAt, false);
        
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(List.of(feedback));
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        List<ReceiveFeedbackOutput> result = resolver.listarAvaliacoes();

        // Assert: All fields are mapped correctly
        assertNotNull(result);
        assertEquals(1, result.size());
        
        ReceiveFeedbackOutput output = result.get(0);
        assertEquals(UUID.fromString(feedbackId), output.id());
        assertEquals(description, output.descricao());
        assertEquals(rating, output.nota());
        assertEquals(urgency.name(), output.urgencia());
        assertEquals(createdAt, output.dataCriacao());
    }

    @Test
    void testQueryReturnsList_NotNull() {
        // Arrange
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(Collections.emptyList());
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        List<ReceiveFeedbackOutput> result = resolver.listarAvaliacoes();

        // Assert: Result is never null
        assertNotNull(result);
    }

    @Test
    void testQueryInvokesGateway_FindAllMethod() {
        // Arrange
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(Collections.emptyList());
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        resolver.listarAvaliacoes();

        // Assert: Gateway's findAll was invoked
        assertTrue(gateway.wasQueryCalled());
    }

    @Test
    void testQueryOutputFieldsAreAccessible() {
        // Arrange: Create a feedback
        Feedback feedback = new Feedback("Test", 5);
        
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(List.of(feedback));
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        List<ReceiveFeedbackOutput> result = resolver.listarAvaliacoes();

        // Assert: All output fields are accessible and non-null
        ReceiveFeedbackOutput output = result.get(0);
        assertNotNull(output.id());
        assertNotNull(output.descricao());
        assertNotNull(output.nota());
        assertNotNull(output.urgencia());
        assertNotNull(output.dataCriacao());
    }

    @Test
    void testQueryPreservesOrderOfFeedbacks() {
        // Arrange: Create feedbacks in a specific order
        Feedback feedback1 = new Feedback("First", 1);
        Feedback feedback2 = new Feedback("Second", 5);
        Feedback feedback3 = new Feedback("Third", 9);
        
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(
            List.of(feedback1, feedback2, feedback3)
        );
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        List<ReceiveFeedbackOutput> result = resolver.listarAvaliacoes();

        // Assert: Order is preserved
        assertEquals("First", result.get(0).descricao());
        assertEquals("Second", result.get(1).descricao());
        assertEquals("Third", result.get(2).descricao());
    }

    @Test
    void testQueryHandlesLargeFeedbackList() {
        // Arrange: Create 100 feedbacks
        List<Feedback> feedbacks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            feedbacks.add(new Feedback("Feedback " + i, i % 10));
        }
        
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(feedbacks);
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        List<ReceiveFeedbackOutput> result = resolver.listarAvaliacoes();

        // Assert
        assertNotNull(result);
        assertEquals(100, result.size());
    }

    @Test
    void testQueryOutputIdIsUUID() {
        // Arrange
        Feedback feedback = new Feedback("Test", 5);
        
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(List.of(feedback));
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        List<ReceiveFeedbackOutput> result = resolver.listarAvaliacoes();

        // Assert: ID is a valid UUID
        UUID id = result.get(0).id();
        assertNotNull(id);
        assertDoesNotThrow(() -> UUID.fromString(id.toString()));
    }

    @Test
    void testQueryOutputUrgencyIsEnumName() {
        // Arrange: Create feedbacks with all urgency levels
        Feedback high = new Feedback("High", 3);
        Feedback medium = new Feedback("Medium", 6);
        Feedback low = new Feedback("Low", 9);
        
        FakeFeedbackGateway gateway = new FakeFeedbackGateway(
            List.of(high, medium, low)
        );
        ReceiveFeedbackUseCase useCase = createUseCase(gateway);
        ReceiveFeedbackQueryResolver resolver = new ReceiveFeedbackQueryResolver(useCase);

        // Act
        List<ReceiveFeedbackOutput> result = resolver.listarAvaliacoes();

        // Assert: Urgency is enum name string (HIGH, MEDIUM, LOW)
        assertTrue(
            result.get(0).urgencia().equals("HIGH") ||
            result.get(0).urgencia().equals("MEDIUM") ||
            result.get(0).urgencia().equals("LOW")
        );
        assertTrue(
            result.get(1).urgencia().equals("HIGH") ||
            result.get(1).urgencia().equals("MEDIUM") ||
            result.get(1).urgencia().equals("LOW")
        );
        assertTrue(
            result.get(2).urgencia().equals("HIGH") ||
            result.get(2).urgencia().equals("MEDIUM") ||
            result.get(2).urgencia().equals("LOW")
        );
    }

    /**
     * Fake implementation of FeedbackGateway for testing.
     * Returns predefined feedbacks without invoking real dependencies.
     */
    private static class FakeFeedbackGateway implements FeedbackGateway {
        private final List<Feedback> feedbacks;
        private boolean queryCalled = false;

        FakeFeedbackGateway(List<Feedback> feedbacks) {
            this.feedbacks = new ArrayList<>(feedbacks);
        }

        @Override
        public Feedback save(Feedback feedback) {
            return feedback;
        }

        @Override
        public List<Feedback> findAll() {
            queryCalled = true;
            return new ArrayList<>(feedbacks);
        }

        @Override
        public List<Feedback> findByPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
            return Collections.emptyList();
        }

        public boolean wasQueryCalled() {
            return queryCalled;
        }
    }

    /**
     * Helper method to create a ReceiveFeedbackUseCase with test dependencies.
     */
    private ReceiveFeedbackUseCase createUseCase(FeedbackGateway gateway) {
        FeedbackConfiguration config = new FeedbackConfiguration() {
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

        UrgencyCalculator calculator = new UrgencyCalculator(config);
        UrgencyPublisher publisher = new FakeUrgencyPublisher();

        return new ReceiveFeedbackUseCase(gateway, publisher, calculator, config);
    }

    /**
     * Fake implementation of UrgencyPublisher for testing.
     */
    private static class FakeUrgencyPublisher implements UrgencyPublisher {
        @Override
        public void publish(Feedback feedback) {
            // No-op for testing
        }
    }
}
