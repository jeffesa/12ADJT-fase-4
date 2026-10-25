package com.fiap.report.domain.gateway;

import com.fiap.report.domain.entity.WeeklyReport;

/**
 * Gateway interface for sending reports to recipients.
 * 
 * This interface defines the contract for delivering generated reports via external
 * services (e.g., email service, messaging system).
 * 
 * Implementations handle the infrastructure details of message delivery while the
 * domain layer remains independent of transport mechanism details (Clean Architecture
 * principle).
 */
public interface ReportSender {

    /**
     * Sends a generated report to the specified email address.
     * 
     * @param report the report to send
     * @param adminEmail the recipient email address
     * @throws ReportException if the send operation fails (e.g., mail service error,
     *                         invalid email address, or permission denied)
     */
    void send(WeeklyReport report, String adminEmail) throws ReportException;
}
