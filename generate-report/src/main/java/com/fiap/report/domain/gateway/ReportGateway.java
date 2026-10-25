package com.fiap.report.domain.gateway;

import com.fiap.report.domain.entity.Feedback;

import java.time.LocalDate;
import java.util.List;

/**
 * Gateway interface for feedback data retrieval and persistence.
 * 
 * This interface defines the contract for retrieving feedback records from the
 * persistence layer (e.g., DynamoDB) within a specified time period.
 * 
 * Implementations of this gateway handle the infrastructure details of accessing
 * the feedback storage system while the domain layer remains independent of
 * persistence concerns (Clean Architecture principle).
 */
public interface ReportGateway {

    /**
     * Retrieves all feedbacks within a specified date range.
     * 
     * The results include feedbacks from both the start and end dates (inclusive).
     * If no feedbacks are found for the period, an empty list is returned.
     * 
     * @param startDate the beginning of the search range (inclusive)
     * @param endDate the end of the search range (inclusive)
     * @return a list of feedbacks matching the period; empty list if none found
     * @throws ReportException if the retrieval operation fails (e.g., database error,
     *                         connectivity issue, or data access permission denied)
     */
    List<Feedback> findByPeriod(LocalDate startDate, LocalDate endDate) throws ReportException;
}
