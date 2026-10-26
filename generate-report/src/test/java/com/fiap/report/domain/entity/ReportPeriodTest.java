package com.fiap.report.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ReportPeriod value object.
 *
 * Verifies constructor validation, date calculations, equality, hashing,
 * and string representation.
 */
class ReportPeriodTest {

    @Test
    void testConstructor_SucceedsWhenStartDateLessThanEndDate() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        // Act
        ReportPeriod period = new ReportPeriod(startDate, endDate);

        // Assert
        assertEquals(startDate, period.getStartDate());
        assertEquals(endDate, period.getEndDate());
    }

    @Test
    void testConstructor_SucceedsWhenStartDateEqualsEndDate() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 10, 21);

        // Act
        ReportPeriod period = new ReportPeriod(date, date);

        // Assert
        assertEquals(date, period.getStartDate());
        assertEquals(date, period.getEndDate());
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentExceptionWhenStartDateGreaterThanEndDate() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 25);
        LocalDate endDate = LocalDate.of(2024, 10, 21);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ReportPeriod(startDate, endDate)
        );
        assertTrue(exception.getMessage().contains("Start date must be less than or equal to end date"));
        assertTrue(exception.getMessage().contains(startDate.toString()));
        assertTrue(exception.getMessage().contains(endDate.toString()));
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentExceptionWhenStartDateIsNull() {
        // Arrange
        LocalDate endDate = LocalDate.of(2024, 10, 25);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ReportPeriod(null, endDate)
        );
        assertTrue(exception.getMessage().contains("must not be null"));
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentExceptionWhenEndDateIsNull() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ReportPeriod(startDate, null)
        );
        assertTrue(exception.getMessage().contains("must not be null"));
    }

    @Test
    void testConstructor_ThrowsIllegalArgumentExceptionWhenBothDatesAreNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ReportPeriod(null, null)
        );
        assertTrue(exception.getMessage().contains("must not be null"));
    }

    @Test
    void testGetDurationDays_ReturnsCorrectCountForFiveDayPeriod() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);  // Monday
        LocalDate endDate = LocalDate.of(2024, 10, 25);    // Friday
        ReportPeriod period = new ReportPeriod(startDate, endDate);

        // Act
        long duration = period.getDurationDays();

        // Assert
        assertEquals(5, duration);
    }

    @Test
    void testGetDurationDays_ReturnsOneForSameDayPeriod() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 10, 21);
        ReportPeriod period = new ReportPeriod(date, date);

        // Act
        long duration = period.getDurationDays();

        // Assert
        assertEquals(1, duration);
    }

    @Test
    void testGetDurationDays_ReturnsCorrectCountForWeekPeriod() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);  // Monday
        LocalDate endDate = LocalDate.of(2024, 10, 27);    // Sunday
        ReportPeriod period = new ReportPeriod(startDate, endDate);

        // Act
        long duration = period.getDurationDays();

        // Assert
        assertEquals(7, duration);
    }

    @Test
    void testGetStartDate_ReturnsCorrectValue() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        ReportPeriod period = new ReportPeriod(startDate, endDate);

        // Act & Assert
        assertEquals(startDate, period.getStartDate());
    }

    @Test
    void testGetEndDate_ReturnsCorrectValue() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        ReportPeriod period = new ReportPeriod(startDate, endDate);

        // Act & Assert
        assertEquals(endDate, period.getEndDate());
    }

    @Test
    void testEquals_ReturnsTrueForIdenticalPeriods() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        ReportPeriod period1 = new ReportPeriod(startDate, endDate);
        ReportPeriod period2 = new ReportPeriod(startDate, endDate);

        // Act & Assert
        assertEquals(period1, period2);
    }

    @Test
    void testEquals_ReturnsFalseForDifferentStartDates() {
        // Arrange
        LocalDate startDate1 = LocalDate.of(2024, 10, 21);
        LocalDate startDate2 = LocalDate.of(2024, 10, 22);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        ReportPeriod period1 = new ReportPeriod(startDate1, endDate);
        ReportPeriod period2 = new ReportPeriod(startDate2, endDate);

        // Act & Assert
        assertNotEquals(period1, period2);
    }

    @Test
    void testEquals_ReturnsFalseForDifferentEndDates() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate1 = LocalDate.of(2024, 10, 25);
        LocalDate endDate2 = LocalDate.of(2024, 10, 26);
        ReportPeriod period1 = new ReportPeriod(startDate, endDate1);
        ReportPeriod period2 = new ReportPeriod(startDate, endDate2);

        // Act & Assert
        assertNotEquals(period1, period2);
    }

    @Test
    void testEquals_ReturnsFalseWhenComparedWithNull() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        ReportPeriod period = new ReportPeriod(startDate, endDate);

        // Act & Assert
        assertNotEquals(period, null);
    }

    @Test
    void testEquals_ReturnsFalseWhenComparedWithDifferentType() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        ReportPeriod period = new ReportPeriod(startDate, endDate);

        // Act & Assert
        assertNotEquals(period, "not a period");
    }

    @Test
    void testHashCode_IsConsistentForSamePeriod() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        ReportPeriod period = new ReportPeriod(startDate, endDate);

        // Act
        int hash1 = period.hashCode();
        int hash2 = period.hashCode();

        // Assert
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashCode_IsEqualForEqualPeriods() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        ReportPeriod period1 = new ReportPeriod(startDate, endDate);
        ReportPeriod period2 = new ReportPeriod(startDate, endDate);

        // Act & Assert
        assertEquals(period1.hashCode(), period2.hashCode());
    }

    @Test
    void testToString_IsNotEmpty() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        ReportPeriod period = new ReportPeriod(startDate, endDate);

        // Act
        String result = period.toString();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testToString_ContainsBothDates() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 10, 21);
        LocalDate endDate = LocalDate.of(2024, 10, 25);
        ReportPeriod period = new ReportPeriod(startDate, endDate);

        // Act
        String result = period.toString();

        // Assert
        assertTrue(result.contains(startDate.toString()));
        assertTrue(result.contains(endDate.toString()));
    }
}
