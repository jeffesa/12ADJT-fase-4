package com.fiap.report.infra.messaging;

import com.fiap.report.domain.entity.WeeklyReport;
import com.fiap.report.domain.entity.FeedbackStatistics;
import com.fiap.report.domain.entity.ReportPeriod;

import java.time.LocalDate;
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
 *
 * Output is a valid, standalone HTML document suitable for email delivery via SES.
 */
public class ReportEmailBuilder {

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
        html.append("  <h1>Weekly Feedback Report</h1>\n");

        // Period information
        html.append("  <div class=\"period\">\n");
        html.append("    <strong>Period:</strong> ");
        html.append(period.getStartDate());
        html.append(" to ");
        html.append(period.getEndDate());
        html.append("\n");
        html.append("  </div>\n");

        // Summary section
        html.append("  <div class=\"summary\">\n");
        html.append("    <h2>Summary</h2>\n");
        html.append("    <p><strong>Total Feedback:</strong> ");
        html.append(statistics.getTotalFeedback());
        html.append("</p>\n");

        // Average rating (formatted to 2 decimals, or "N/A" if null)
        String averageRatingStr = "N/A";
        if (statistics.getAverageRating() != null) {
            averageRatingStr = String.format("%.2f", statistics.getAverageRating());
        }
        html.append("    <p><strong>Average Rating:</strong> ");
        html.append(averageRatingStr);
        html.append("</p>\n");
        html.append("  </div>\n");

        // Feedback by Day table
        html.append("  <h2>Feedback by Day</h2>\n");
        html.append("  <table>\n");
        html.append("    <tr>\n");
        html.append("      <th>Date</th>\n");
        html.append("      <th>Count</th>\n");
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
        html.append("  <h2>Feedback by Urgency</h2>\n");
        html.append("  <table>\n");
        html.append("    <tr>\n");
        html.append("      <th>Urgency</th>\n");
        html.append("      <th>Count</th>\n");
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

        // HTML closing
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }
}
