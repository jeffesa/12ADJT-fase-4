package com.fiap.report.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the FeedbackStatistics immutable value object.
 *
 * Verifies constructor behavior, immutability of internal maps, field access,
 * equality, hashing, and string representation.
 */
class FeedbackStatisticsTest {

    @Test
    void testConstructor_StoresTotalFeedback() {
        // Arrange
        int totalFeedback = 10;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();

        // Act
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Assert
        assertEquals(10, stats.getTotalFeedback());
    }

    @Test
    void testConstructor_StoresAverageRating() {
        // Arrange
        int totalFeedback = 10;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();

        // Act
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Assert
        assertEquals(7.5, stats.getAverageRating());
    }

    @Test
    void testConstructor_StoresAverageRatingAsNull() {
        // Arrange
        int totalFeedback = 0;
        Double averageRating = null;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();

        // Act
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Assert
        assertNull(stats.getAverageRating());
    }

    @Test
    void testConstructor_StoresFeedbackByDay() {
        // Arrange
        int totalFeedback = 5;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        feedbackByDay.put(LocalDate.of(2024, 10, 21), 2);
        feedbackByDay.put(LocalDate.of(2024, 10, 22), 3);
        Map<String, Integer> feedbackByUrgency = new HashMap<>();

        // Act
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Assert
        Map<LocalDate, Integer> result = stats.getFeedbackByDay();
        assertEquals(2, result.get(LocalDate.of(2024, 10, 21)));
        assertEquals(3, result.get(LocalDate.of(2024, 10, 22)));
    }

    @Test
    void testConstructor_StoresFeedbackByUrgency() {
        // Arrange
        int totalFeedback = 10;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        feedbackByUrgency.put("HIGH", 4);
        feedbackByUrgency.put("MEDIUM", 3);
        feedbackByUrgency.put("LOW", 3);

        // Act
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Assert
        Map<String, Integer> result = stats.getFeedbackByUrgency();
        assertEquals(4, result.get("HIGH"));
        assertEquals(3, result.get("MEDIUM"));
        assertEquals(3, result.get("LOW"));
    }

    @Test
    void testConstructor_MakesFeedbackByDayUnmodifiable() {
        // Arrange
        int totalFeedback = 5;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        feedbackByDay.put(LocalDate.of(2024, 10, 21), 2);
        Map<String, Integer> feedbackByUrgency = new HashMap<>();

        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Act & Assert
        assertThrows(
            UnsupportedOperationException.class,
            () -> stats.getFeedbackByDay().put(LocalDate.of(2024, 10, 23), 1)
        );
    }

    @Test
    void testConstructor_MakesFeedbackByUrgencyUnmodifiable() {
        // Arrange
        int totalFeedback = 10;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        feedbackByUrgency.put("HIGH", 4);

        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Act & Assert
        assertThrows(
            UnsupportedOperationException.class,
            () -> stats.getFeedbackByUrgency().put("CRITICAL", 1)
        );
    }

    @Test
    void testConstructor_ReturnsFeedbackByDayAsEmptyMapWhenNull() {
        // Arrange
        int totalFeedback = 0;
        Double averageRating = null;
        Map<String, Integer> feedbackByUrgency = new HashMap<>();

        // Act
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, null, feedbackByUrgency);

        // Assert
        Map<LocalDate, Integer> result = stats.getFeedbackByDay();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testConstructor_ReturnsFeedbackByUrgencyAsEmptyMapWhenNull() {
        // Arrange
        int totalFeedback = 0;
        Double averageRating = null;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();

        // Act
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, null);

        // Assert
        Map<String, Integer> result = stats.getFeedbackByUrgency();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTotalFeedback_ReturnsCorrectValue() {
        // Arrange
        int totalFeedback = 10;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Act & Assert
        assertEquals(10, stats.getTotalFeedback());
    }

    @Test
    void testGetAverageRating_ReturnsCorrectValue() {
        // Arrange
        int totalFeedback = 10;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Act & Assert
        assertEquals(7.5, stats.getAverageRating());
    }

    @Test
    void testGetFeedbackByDay_ReturnsCorrectMap() {
        // Arrange
        int totalFeedback = 5;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        feedbackByDay.put(LocalDate.of(2024, 10, 21), 2);
        feedbackByDay.put(LocalDate.of(2024, 10, 22), 3);
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Act
        Map<LocalDate, Integer> result = stats.getFeedbackByDay();

        // Assert
        assertEquals(2, result.size());
        assertEquals(2, result.get(LocalDate.of(2024, 10, 21)));
        assertEquals(3, result.get(LocalDate.of(2024, 10, 22)));
    }

    @Test
    void testGetFeedbackByUrgency_ReturnsCorrectMap() {
        // Arrange
        int totalFeedback = 10;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        feedbackByUrgency.put("HIGH", 4);
        feedbackByUrgency.put("MEDIUM", 3);
        feedbackByUrgency.put("LOW", 3);
        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Act
        Map<String, Integer> result = stats.getFeedbackByUrgency();

        // Assert
        assertEquals(3, result.size());
        assertEquals(4, result.get("HIGH"));
        assertEquals(3, result.get("MEDIUM"));
        assertEquals(3, result.get("LOW"));
    }

    @Test
    void testEquals_ReturnsTrueForIdenticalStatistics() {
        // Arrange
        int totalFeedback = 10;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        feedbackByDay.put(LocalDate.of(2024, 10, 21), 2);
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        feedbackByUrgency.put("HIGH", 4);

        FeedbackStatistics stats1 = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);
        FeedbackStatistics stats2 = new FeedbackStatistics(totalFeedback, averageRating, new HashMap<>(feedbackByDay), new HashMap<>(feedbackByUrgency));

        // Act & Assert
        assertEquals(stats1, stats2);
    }

    @Test
    void testEquals_ReturnsFalseForDifferentTotalFeedback() {
        // Arrange
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        FeedbackStatistics stats1 = new FeedbackStatistics(10, 7.5, feedbackByDay, feedbackByUrgency);
        FeedbackStatistics stats2 = new FeedbackStatistics(11, 7.5, new HashMap<>(feedbackByDay), new HashMap<>(feedbackByUrgency));

        // Act & Assert
        assertNotEquals(stats1, stats2);
    }

    @Test
    void testEquals_ReturnsFalseForDifferentAverageRating() {
        // Arrange
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        FeedbackStatistics stats1 = new FeedbackStatistics(10, 7.5, feedbackByDay, feedbackByUrgency);
        FeedbackStatistics stats2 = new FeedbackStatistics(10, 8.0, new HashMap<>(feedbackByDay), new HashMap<>(feedbackByUrgency));

        // Act & Assert
        assertNotEquals(stats1, stats2);
    }

    @Test
    void testEquals_ReturnsFalseWhenComparedWithNull() {
        // Arrange
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        FeedbackStatistics stats = new FeedbackStatistics(10, 7.5, feedbackByDay, feedbackByUrgency);

        // Act & Assert
        assertNotEquals(stats, null);
    }

    @Test
    void testHashCode_IsConsistentForSameStatistics() {
        // Arrange
        int totalFeedback = 10;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        feedbackByDay.put(LocalDate.of(2024, 10, 21), 2);
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        feedbackByUrgency.put("HIGH", 4);

        FeedbackStatistics stats = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);

        // Act
        int hash1 = stats.hashCode();
        int hash2 = stats.hashCode();

        // Assert
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashCode_IsEqualForEqualStatistics() {
        // Arrange
        int totalFeedback = 10;
        Double averageRating = 7.5;
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        feedbackByDay.put(LocalDate.of(2024, 10, 21), 2);
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        feedbackByUrgency.put("HIGH", 4);

        FeedbackStatistics stats1 = new FeedbackStatistics(totalFeedback, averageRating, feedbackByDay, feedbackByUrgency);
        FeedbackStatistics stats2 = new FeedbackStatistics(totalFeedback, averageRating, new HashMap<>(feedbackByDay), new HashMap<>(feedbackByUrgency));

        // Act & Assert
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void testToString_IsNotEmpty() {
        // Arrange
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        FeedbackStatistics stats = new FeedbackStatistics(10, 7.5, feedbackByDay, feedbackByUrgency);

        // Act
        String result = stats.toString();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testToString_ContainsAllFieldNames() {
        // Arrange
        Map<LocalDate, Integer> feedbackByDay = new HashMap<>();
        Map<String, Integer> feedbackByUrgency = new HashMap<>();
        FeedbackStatistics stats = new FeedbackStatistics(10, 7.5, feedbackByDay, feedbackByUrgency);

        // Act
        String result = stats.toString();

        // Assert
        assertTrue(result.contains("totalFeedback"));
        assertTrue(result.contains("averageRating"));
        assertTrue(result.contains("feedbackByDay"));
        assertTrue(result.contains("feedbackByUrgency"));
    }
}
