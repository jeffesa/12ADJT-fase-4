package com.fiap.report.infra.messaging;

import com.fiap.report.domain.entity.Feedback;
import com.fiap.report.domain.entity.WeeklyReport;
import com.fiap.report.domain.entity.FeedbackStatistics;
import com.fiap.report.domain.entity.ReportPeriod;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builder utility for constructing HTML email templates for weekly reports.
 *
 * This class provides a static method to generate a complete, well-formatted HTML
 * email body containing report period, aggregated statistics, and feedback breakdowns
 * by day and urgency level.
 *
 * The generated HTML includes:
 * - Report header
 * - Period information
 * - Summary statistics (total feedback count, average rating)
 * - Feedback breakdown table by day
 * - Feedback breakdown table by urgency level
 * - "Feedbacks do período" table: Descrição, Urgência, Data de envio
 *
 * The feedback table lists the period's feedbacks sorted by createdAt ascending. Dates use
 * the pattern dd/MM/yyyy HH:mm, taken from the stored LocalDateTime without a time zone
 * conversion. Descriptions are HTML-escaped (&, &lt;, &gt;, &quot;, &#39;). An empty period
 * shows "Nenhum feedback no período".
 *
 * Output is a valid, standalone HTML document suitable for email delivery via SES.
 */
public class ReportEmailBuilder {

    private static final DateTimeFormatter SEND_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Builds a complete HTML email body for a weekly feedback report.
     *
     * The HTML includes:
     * - Header with "Weekly Feedback Report" title
     * - Period display showing start and end dates
     * - Summary section with total feedback count and average rating (formatted to 2 decimals)
     * - Feedback by Day table with dates sorted ascending and corresponding counts
     * - Feedback by Urgency table with rows for HIGH, MEDIUM, and LOW with their counts
     *
     * @param report the WeeklyReport containing period and statistics data
     * @return a complete valid HTML string ready for email delivery
     */
    public static String buildHtmlBody(WeeklyReport report) {
        ReportPeriod period = report.getPeriod();
        FeedbackStatistics statistics = report.getStatistics();

        StringBuilder html = new StringBuilder();

        // HTML header and opening
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("    h1 { color: #333; }\n");
        html.append("    .period { margin: 20px 0; font-size: 16px; }\n");
        html.append("    .summary { background-color: #f0f0f0; padding: 15px; margin: 20px 0; border-radius: 5px; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        html.append("    th { background-color: #4CAF50; color: white; padding: 10px; text-align: left; }\n");
        html.append("    td { border: 1px solid #ddd; padding: 10px; }\n");
        html.append("    tr:nth-child(even) { background-color: #f9f9f9; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Report header
        html.append("  <h1>Relatório semanal de feedback</h1>\n");

        // Period information
        html.append("  <div class=\"period\">\n");
        html.append("    <strong>Período:</strong> ");
        html.append(period.getStartDate());
        html.append(" a ");
        html.append(period.getEndDate());
        html.append("\n");
        html.append("  </div>\n");

        // Summary section
        html.append("  <div class=\"summary\">\n");
        html.append("    <h2>Resumo</h2>\n");
        html.append("    <p><strong>Total de feedbacks:</strong> ");
        html.append(statistics.getTotalFeedback());
        html.append("</p>\n");

        // Average rating (formatted to 2 decimals, or "N/D" if null)
        String averageRatingStr = "N/D";
        if (statistics.getAverageRating() != null) {
            averageRatingStr = String.format("%.2f", statistics.getAverageRating());
        }
        html.append("    <p><strong>Nota média:</strong> ");
        html.append(averageRatingStr);
        html.append("</p>\n");
        html.append("  </div>\n");

        // Feedback by Day table
        html.append("  <h2>Feedbacks por dia</h2>\n");
        html.append("  <table>\n");
        html.append("    <tr>\n");
        html.append("      <th>Data</th>\n");
        html.append("      <th>Quantidade</th>\n");
        html.append("    </tr>\n");

        Map<LocalDate, Integer> feedbackByDay = statistics.getFeedbackByDay();
        // Sort by date ascending
        feedbackByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    html.append("    <tr>\n");
                    html.append("      <td>");
                    html.append(entry.getKey());
                    html.append("</td>\n");
                    html.append("      <td>");
                    html.append(entry.getValue());
                    html.append("</td>\n");
                    html.append("    </tr>\n");
                });

        html.append("  </table>\n");

        // Feedback by Urgency table
        html.append("  <h2>Feedbacks por urgência</h2>\n");
        html.append("  <table>\n");
        html.append("    <tr>\n");
        html.append("      <th>Urgência</th>\n");
        html.append("      <th>Quantidade</th>\n");
        html.append("    </tr>\n");

        Map<String, Integer> feedbackByUrgency = statistics.getFeedbackByUrgency();
        // Iterate in fixed order: HIGH, MEDIUM, LOW
        String[] urgencyOrder = {"HIGH", "MEDIUM", "LOW"};
        for (String urgency : urgencyOrder) {
            Integer count = feedbackByUrgency.getOrDefault(urgency, 0);
            html.append("    <tr>\n");
            html.append("      <td>");
            html.append(urgency);
            html.append("</td>\n");
            html.append("      <td>");
            html.append(count);
            html.append("</td>\n");
            html.append("    </tr>\n");
        }

        html.append("  </table>\n");

        // Feedbacks of the period
        appendFeedbacksSection(html, report.getFeedbacks());

        // HTML closing
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * Appends the "Feedbacks do período" section, ordered by creation date.
     * Descriptions are HTML-escaped. An empty period shows a placeholder message.
     */
    private static void appendFeedbacksSection(StringBuilder html, List<Feedback> feedbacks) {
        html.append("  <h2>Feedbacks do período</h2>\n");

        if (feedbacks.isEmpty()) {
            html.append("  <p>Nenhum feedback no período</p>\n");
            return;
        }

        html.append("  <table>\n");
        html.append("    <tr>\n");
        html.append("      <th>Descrição</th>\n");
        html.append("      <th>Urgência</th>\n");
        html.append("      <th>Data de envio</th>\n");
        html.append("    </tr>\n");

        feedbacks.stream()
                .sorted(Comparator.comparing(Feedback::getCreatedAt))
                .forEach(feedback -> {
                    html.append("    <tr>\n");
                    html.append("      <td>");
                    html.append(escapeHtml(feedback.getDescription()));
                    html.append("</td>\n");
                    html.append("      <td>");
                    html.append(feedback.getUrgency().name());
                    html.append("</td>\n");
                    html.append("      <td>");
                    html.append(feedback.getCreatedAt().format(SEND_DATE_FORMATTER));
                    html.append("</td>\n");
                    html.append("    </tr>\n");
                });

        html.append("  </table>\n");
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
