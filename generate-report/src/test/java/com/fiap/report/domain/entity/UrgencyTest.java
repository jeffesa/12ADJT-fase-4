package com.fiap.report.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the report-side Urgency enum.
 */
class UrgencyTest {

    @Test
    void getDescriptionReturnsHumanReadableLabel() {
        assertEquals("High", Urgency.HIGH.getDescription());
        assertEquals("Medium", Urgency.MEDIUM.getDescription());
        assertEquals("Low", Urgency.LOW.getDescription());
    }
}
