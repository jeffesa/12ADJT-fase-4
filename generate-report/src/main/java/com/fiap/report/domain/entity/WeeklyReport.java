package com.fiap.report.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable domain entity representing a generated weekly report.
 * 
 * A WeeklyReport encapsulates the complete report for a period: the time period covered,
 * aggregated feedback statistics, generation timestamp, and the recipient email address.
 * 
 * The report is auto-generated with a unique identifier and timestamp upon construction.
 */
public class WeeklyReport {

    private final UUID id;
    private final ReportPeriod period;
    private final FeedbackStatistics statistics;
    private final LocalDateTime generatedAt;
    private final String adminEmail;

    /**
     * Constructor for WeeklyReport.
     * 
     * Auto-generates a unique identifier (UUID) and captures the generation timestamp.
     * 
     * @param startDate the beginning of the report period (inclusive)
     * @param endDate the end of the report period (inclusive)
     * @param statistics the aggregated feedback statistics for the period
     * @param adminEmail the email address of the administrator to receive the report
     */
    public WeeklyReport(LocalDate startDate, LocalDate endDate, FeedbackStatistics statistics, String adminEmail) {
        this.id = UUID.randomUUID();
        this.period = new ReportPeriod(startDate, endDate);
        this.statistics = statistics;
        this.generatedAt = LocalDateTime.now();
        this.adminEmail = adminEmail;
    }

    /**
     * Retrieves the unique identifier of the report.
     * 
     * @return the report UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retrieves the report period.
     * 
     * @return the time period covered by the report
     */
    public ReportPeriod getPeriod() {
        return period;
    }

    /**
     * Retrieves the aggregated feedback statistics.
     * 
     * @return the statistics object
     */
    public FeedbackStatistics getStatistics() {
        return statistics;
    }

    /**
     * Retrieves the timestamp when the report was generated.
     * 
     * @return the generation timestamp
     */
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    /**
     * Retrieves the email address of the administrator.
     * 
     * @return the admin email
     */
    public String getAdminEmail() {
        return adminEmail;
    }

    @Override
    public String toString() {
        return "WeeklyReport{" +
                "id=" + id +
                ", period=" + period +
                ", statistics=" + statistics +
                ", generatedAt=" + generatedAt +
                ", adminEmail='" + adminEmail + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeeklyReport that = (WeeklyReport) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
