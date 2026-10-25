package com.fiap.report.domain.entity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable value object representing aggregated feedback statistics for a report period.
 * 
 * Contains metrics computed from a collection of feedbacks: total count, average rating,
 * and breakdowns by day and urgency level.
 */
public class FeedbackStatistics {

    private final int totalFeedback;
    private final Double averageRating;
    private final Map<LocalDate, Integer> feedbackByDay;
    private final Map<String, Integer> feedbackByUrgency;

    /**
     * Constructor for FeedbackStatistics.
     * 
     * @param totalFeedback total count of feedbacks in the period
     * @param averageRating mean rating of all feedbacks (nullable if no feedbacks exist)
     * @param feedbackByDay immutable map of feedback counts grouped by day
     * @param feedbackByUrgency immutable map of feedback counts grouped by urgency level
     *                          (keys: "HIGH", "MEDIUM", "LOW")
     */
    public FeedbackStatistics(
            int totalFeedback,
            Double averageRating,
            Map<LocalDate, Integer> feedbackByDay,
            Map<String, Integer> feedbackByUrgency) {
        this.totalFeedback = totalFeedback;
        this.averageRating = averageRating;
        this.feedbackByDay = feedbackByDay != null ? Collections.unmodifiableMap(feedbackByDay) : Collections.emptyMap();
        this.feedbackByUrgency = feedbackByUrgency != null ? Collections.unmodifiableMap(feedbackByUrgency) : Collections.emptyMap();
    }

    /**
     * Retrieves the total count of feedbacks.
     * 
     * @return total feedback count
     */
    public int getTotalFeedback() {
        return totalFeedback;
    }

    /**
     * Retrieves the average rating across all feedbacks.
     * 
     * @return average rating, or null if no feedbacks exist
     */
    public Double getAverageRating() {
        return averageRating;
    }

    /**
     * Retrieves the feedback count grouped by day.
     * 
     * @return immutable map with LocalDate keys and count values
     */
    public Map<LocalDate, Integer> getFeedbackByDay() {
        return feedbackByDay;
    }

    /**
     * Retrieves the feedback count grouped by urgency level.
     * 
     * @return immutable map with urgency level keys ("HIGH", "MEDIUM", "LOW") and count values
     */
    public Map<String, Integer> getFeedbackByUrgency() {
        return feedbackByUrgency;
    }

    @Override
    public String toString() {
        return "FeedbackStatistics{" +
                "totalFeedback=" + totalFeedback +
                ", averageRating=" + averageRating +
                ", feedbackByDay=" + feedbackByDay +
                ", feedbackByUrgency=" + feedbackByUrgency +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeedbackStatistics that = (FeedbackStatistics) o;

        if (totalFeedback != that.totalFeedback) return false;
        if (averageRating != null ? !averageRating.equals(that.averageRating) : that.averageRating != null)
            return false;
        if (!feedbackByDay.equals(that.feedbackByDay)) return false;
        return feedbackByUrgency.equals(that.feedbackByUrgency);
    }

    @Override
    public int hashCode() {
        int result = totalFeedback;
        result = 31 * result + (averageRating != null ? averageRating.hashCode() : 0);
        result = 31 * result + feedbackByDay.hashCode();
        result = 31 * result + feedbackByUrgency.hashCode();
        return result;
    }
}
