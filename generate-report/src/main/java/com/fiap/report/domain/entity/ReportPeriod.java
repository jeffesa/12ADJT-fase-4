package com.fiap.report.domain.entity;

import java.time.LocalDate;

/**
 * Immutable value object representing a time period for report generation.
 * 
 * A report period defines the start and end dates for aggregating feedback statistics.
 * The period is immutable and validates that the start date does not exceed the end date.
 */
public class ReportPeriod {

    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * Constructor for ReportPeriod.
     * 
     * Validates that the start date is less than or equal to the end date.
     * 
     * @param startDate the beginning of the report period (inclusive)
     * @param endDate the end of the report period (inclusive)
     * @throws IllegalArgumentException if startDate is after endDate
     */
    public ReportPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                "Start date must be less than or equal to end date. Start: " + startDate + ", End: " + endDate
            );
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Retrieves the start date of the report period.
     * 
     * @return the start date (inclusive)
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Retrieves the end date of the report period.
     * 
     * @return the end date (inclusive)
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Calculates the duration of the report period in days.
     * 
     * @return the number of days between start and end date (inclusive)
     */
    public long getDurationDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    @Override
    public String toString() {
        return "ReportPeriod{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReportPeriod that = (ReportPeriod) o;

        if (!startDate.equals(that.startDate)) return false;
        return endDate.equals(that.endDate);
    }

    @Override
    public int hashCode() {
        int result = startDate.hashCode();
        result = 31 * result + endDate.hashCode();
        return result;
    }
}
