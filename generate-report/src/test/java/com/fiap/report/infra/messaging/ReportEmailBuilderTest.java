package com.fiap.report.infra.messaging;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fiap.report.domain.entity.FeedbackStatistics;
import com.fiap.report.domain.entity.WeeklyReport;

/**
 * Unit tests for ReportEmailBuilder, focused on the "Feedback by Urgency" table.
 */
class ReportEmailBuilderTest {

    @Test
    void urgencyTableListsHighMediumAndLowInFixedOrderWithCounts() {
        Map<String, Integer> byUrgency = new HashMap<>();
        byUrgency.put("LOW", 3);
        byUrgency.put("HIGH", 2);
        WeeklyReport report = reportWith(byUrgency);

        String html = ReportEmailBuilder.buildHtmlBody(report);

        String table = html.substring(html.indexOf("<h2>Feedback by Urgency</h2>"));
        int high = table.indexOf("<td>HIGH</td>");
        int medium = table.indexOf("<td>MEDIUM</td>");
        int low = table.indexOf("<td>LOW</td>");
        assertTrue(high > 0 && high < medium && medium < low);
        assertTrue(table.contains("<td>HIGH</td>\n      <td>2</td>"));
        assertTrue(table.contains("<td>MEDIUM</td>\n      <td>0</td>"));
        assertTrue(table.contains("<td>LOW</td>\n      <td>3</td>"));
    }

    @Test
    void dayTableListsDatesInAscendingOrderWithCounts() {
        Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
        byDay.put(LocalDate.of(2024, 10, 23), 1);
        byDay.put(LocalDate.of(2024, 10, 21), 4);
        WeeklyReport report = new WeeklyReport(
                LocalDate.of(2024, 10, 21),
                LocalDate.of(2024, 10, 25),
                new FeedbackStatistics(5, 7.5, byDay, new HashMap<>()),
                "admin@feedback-fase-4.com");

        String html = ReportEmailBuilder.buildHtmlBody(report);

        String table = html.substring(html.indexOf("<h2>Feedback by Day</h2>"), html.indexOf("<h2>Feedback by Urgency</h2>"));
        int first = table.indexOf("<td>2024-10-21</td>\n      <td>4</td>");
        int second = table.indexOf("<td>2024-10-23</td>\n      <td>1</td>");
        assertTrue(first > 0 && first < second);
    }

    @Test
    void summaryShowsNotAvailableWhenAverageRatingIsMissing() {
        WeeklyReport report = new WeeklyReport(
                LocalDate.of(2024, 10, 21),
                LocalDate.of(2024, 10, 25),
                new FeedbackStatistics(0, null, new HashMap<>(), new HashMap<>()),
                "admin@feedback-fase-4.com");

        String html = ReportEmailBuilder.buildHtmlBody(report);

        assertTrue(html.contains("<strong>Average Rating:</strong> N/A"));
    }

    private static WeeklyReport reportWith(Map<String, Integer> byUrgency) {
        return new WeeklyReport(
                LocalDate.of(2024, 10, 21),
                LocalDate.of(2024, 10, 25),
                new FeedbackStatistics(5, 7.5, new HashMap<>(), byUrgency),
                "admin@feedback-fase-4.com");
    }
}
