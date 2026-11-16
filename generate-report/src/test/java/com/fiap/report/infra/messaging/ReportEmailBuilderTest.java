package com.fiap.report.infra.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fiap.report.domain.entity.Feedback;
import com.fiap.report.domain.entity.FeedbackStatistics;
import com.fiap.report.domain.entity.Urgency;
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

        String table = html.substring(html.indexOf("<h2>Avaliações por urgência</h2>"));
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

        String table = html.substring(html.indexOf("<h2>Avaliações por dia</h2>"), html.indexOf("<h2>Avaliações por urgência</h2>"));
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

        assertTrue(html.contains("<strong>Nota média:</strong> N/D"));
    }

    @Test
    void feedbackSectionListsDescriptionUrgencyAndSendDateOrderedByDate() {
        List<Feedback> feedbacks = List.of(
                new Feedback("2", 6, Urgency.MEDIUM, LocalDateTime.of(2024, 10, 23, 14, 30), "Segunda aula"),
                new Feedback("1", 2, Urgency.HIGH, LocalDateTime.of(2024, 10, 21, 9, 5), "Primeira aula"));
        WeeklyReport report = reportWithFeedbacks(feedbacks);

        String html = ReportEmailBuilder.buildHtmlBody(report);
        String section = html.substring(html.indexOf("<h2>Avaliações do período</h2>"));

        assertTrue(section.contains("<th>Descrição</th>"));
        assertTrue(section.contains("<th>Urgência</th>"));
        assertTrue(section.contains("<th>Data de envio</th>"));
        int first = section.indexOf("<td>Primeira aula</td>\n      <td>HIGH</td>\n      <td>21/10/2024 09:05</td>");
        int second = section.indexOf("<td>Segunda aula</td>\n      <td>MEDIUM</td>\n      <td>23/10/2024 14:30</td>");
        assertTrue(first > 0 && first < second);
    }

    @Test
    void feedbackDescriptionIsHtmlEscaped() {
        List<Feedback> feedbacks = List.of(
                new Feedback("1", 1, Urgency.HIGH, LocalDateTime.of(2024, 10, 21, 9, 0),
                        "<script>alert('x')</script> & \"aspas\""));
        WeeklyReport report = reportWithFeedbacks(feedbacks);

        String html = ReportEmailBuilder.buildHtmlBody(report);

        assertFalse(html.contains("<script>alert"));
        assertTrue(html.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;aspas&quot;"));
    }

    @Test
    void feedbackSectionShowsPlaceholderWhenPeriodHasNoFeedback() {
        WeeklyReport report = reportWithFeedbacks(List.of());

        String html = ReportEmailBuilder.buildHtmlBody(report);

        assertTrue(html.contains("<h2>Avaliações do período</h2>\n  <p>Nenhuma avaliação no período</p>"));
    }

    private static WeeklyReport reportWithFeedbacks(List<Feedback> feedbacks) {
        return new WeeklyReport(
                LocalDate.of(2024, 10, 21),
                LocalDate.of(2024, 10, 25),
                new FeedbackStatistics(feedbacks.size(), null, new HashMap<>(), new HashMap<>()),
                "admin@feedback-fase-4.com",
                feedbacks);
    }

    private static WeeklyReport reportWith(Map<String, Integer> byUrgency) {
        return new WeeklyReport(
                LocalDate.of(2024, 10, 21),
                LocalDate.of(2024, 10, 25),
                new FeedbackStatistics(5, 7.5, new HashMap<>(), byUrgency),
                "admin@feedback-fase-4.com");
    }
}
