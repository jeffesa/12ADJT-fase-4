# Monitoring Guide

This guide covers how to monitor the three Lambda functions that power the FIAP Fase-4 serverless feedback platform: **ReceiveFeedback**, **SendNotification**, and **GenerateReport**. Learn how to access logs, set up metrics, create dashboards, and interpret common error patterns.

---

## Table of Contents

1. [CloudWatch Logs](#cloudwatch-logs)
2. [Metrics and Alarms](#metrics-and-alarms)
3. [Dashboard Setup](#dashboard-setup)
4. [Log Interpretation](#log-interpretation)
5. [Troubleshooting Common Issues](#troubleshooting-common-issues)

---

## CloudWatch Logs

### Log Group Naming Pattern

All Lambda functions write logs to CloudWatch Logs under the pattern:

```
/aws/lambda/{environment}-{function-name}
```

Where:
- `{environment}` is either `dev` or `prod` (set via the SAM template parameter)
- `{function-name}` is the Lambda function name

### Lambda 1: ReceiveFeedback

**Log Group Name:**
```
/aws/lambda/dev-receive-feedback     (development)
/aws/lambda/prod-receive-feedback    (production)
```

**Trigger:** HTTP POST request to API Gateway `/avaliacao` endpoint

**Key Operations Logged:**
- Handler invocation start (`Level.INFO`)
- HTTP method and path (`Level.FINE`)
- Request body parsing status
- Input validation results (`Level.FINE`)
- Use case execution start
- Feedback ID upon success (`Level.INFO`)
- Error details on failure (`Level.SEVERE` for exceptions)

**Sample Log Entries:**

```
2026-10-22T10:30:00.123Z	INFO	Lambda handler invoked for receiving feedback
2026-10-22T10:30:00.124Z	FINE	Request method: POST, path: /avaliacao
2026-10-22T10:30:00.125Z	FINE	Request parsed successfully
2026-10-22T10:30:00.130Z	INFO	Feedback received successfully: 123e4567-e89b-12d3-a456-426614174000
```

### Lambda 2: SendNotification

**Log Group Name:**
```
/aws/lambda/dev-send-notification    (development)
/aws/lambda/prod-send-notification   (production)
```

**Trigger:** SNS message from the `{environment}-feedback-urgent-topic`

**Key Operations Logged:**
- Handler invocation start (`Level.INFO`)
- SNS event null check
- SNS message deserialization (`Level.FINE`)
- Per-record processing details
- Use case execution for each message
- Completion status (`Level.INFO`)
- Errors at `Level.SEVERE`

**Sample Log Entries:**

```
2026-10-22T10:30:05.200Z	INFO	Lambda handler invoked for sending notifications
2026-10-22T10:30:05.201Z	FINE	Processing SNS message: {"feedbackId":"123e4567-e89b-12d3-a456-426614174000","description":"Critical issue","urgency":"HIGH","createdAt":"2026-10-22T10:30:00"}
2026-10-22T10:30:05.250Z	INFO	Lambda handler completed
```

### Lambda 3: GenerateReport

**Log Group Name:**
```
/aws/lambda/dev-generate-report      (development)
/aws/lambda/prod-generate-report     (production)
```

**Trigger:** EventBridge cron schedule (Monday 08:00 UTC)

**Key Operations Logged:**
- Handler invocation start (`Level.INFO`)
- Report period calculation (`Level.FINE`): startDate and endDate
- Use case execution start
- Completion status (`Level.INFO`)
- Errors at `Level.SEVERE`

**Sample Log Entries:**

```
2026-10-26T08:00:00.500Z	INFO	Lambda handler invoked for generating and sending report
2026-10-26T08:00:00.501Z	FINE	Report period calculated: startDate=2026-10-19, endDate=2026-10-23
2026-10-26T08:00:01.200Z	INFO	Report generation and sending completed successfully
```

### Accessing Logs in CloudWatch

**Via AWS Console:**
1. Open CloudWatch > Logs > Log Groups
2. Find the log group by name (e.g., `/aws/lambda/dev-receive-feedback`)
3. Click on the log group to view log streams (one per Lambda invocation)
4. Each stream shows all log entries for that invocation in chronological order

**Via AWS CLI:**

```bash
# List recent log entries (last 10 minutes)
aws logs tail /aws/lambda/dev-receive-feedback --follow

# Search for errors
aws logs filter-log-events \
  --log-group-name /aws/lambda/dev-receive-feedback \
  --filter-pattern "ERROR or SEVERE"

# Get logs for a specific time range
aws logs filter-log-events \
  --log-group-name /aws/lambda/dev-receive-feedback \
  --start-time 1698052200000 \
  --end-time 1698052400000
```

**Log Retention:**
All log groups are configured with a 30-day retention policy. Logs older than 30 days are automatically deleted.

---

## Metrics and Alarms

### Available Metrics

CloudWatch automatically publishes metrics for all Lambda functions. Standard metrics include:

| Metric | Description | Unit | Dimensions |
|--------|-------------|------|-----------|
| **Invocations** | Number of times the Lambda was invoked | Count | FunctionName |
| **Errors** | Number of invocations that resulted in an error | Count | FunctionName |
| **Duration** | Time taken to execute the Lambda | Milliseconds | FunctionName |
| **Throttles** | Number of invocations throttled by Lambda | Count | FunctionName |
| **ConcurrentExecutions** | Number of concurrent invocations | Count | FunctionName |
| **UnreservedConcurrentExecutions** | Concurrent executions outside reserved pool | Count | FunctionName |

### Reserved Concurrency

All three Lambdas have reserved concurrency configured:

- **ReceiveFeedback:** 10 concurrent executions
- **SendNotification:** 10 concurrent executions
- **GenerateReport:** 1 concurrent execution (weekly, no parallel runs needed)

### Setting Up Alarms

**Create an Error Alarm:**

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name dev-receive-feedback-errors \
  --alarm-description "Alert when ReceiveFeedback has errors" \
  --metric-name Errors \
  --namespace AWS/Lambda \
  --statistic Sum \
  --period 300 \
  --threshold 5 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --dimensions Name=FunctionName,Value=dev-receive-feedback \
  --alarm-actions arn:aws:sns:us-east-1:ACCOUNT_ID:alert-topic
```

**Create a Duration Alarm (P99 latency):**

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name dev-receive-feedback-latency \
  --alarm-description "Alert when ReceiveFeedback P99 latency exceeds 5 seconds" \
  --metric-name Duration \
  --namespace AWS/Lambda \
  --statistic p99 \
  --period 300 \
  --threshold 5000 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=FunctionName,Value=dev-receive-feedback
```

**Create a Throttling Alarm:**

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name dev-send-notification-throttles \
  --alarm-description "Alert when SendNotification is throttled" \
  --metric-name Throttles \
  --namespace AWS/Lambda \
  --statistic Sum \
  --period 60 \
  --threshold 1 \
  --comparison-operator GreaterThanOrEqualToThreshold \
  --evaluation-periods 1 \
  --dimensions Name=FunctionName,Value=dev-send-notification
```

### Recommended Alarm Thresholds

| Function | Metric | Threshold | Period | Statistic |
|----------|--------|-----------|--------|-----------|
| All | Errors | > 5 in 5 min | 300s | Sum |
| ReceiveFeedback | Duration (P99) | > 5 sec | 300s | p99 |
| SendNotification | Duration (P99) | > 3 sec | 300s | p99 |
| GenerateReport | Duration (P99) | > 30 sec | 300s | p99 |
| All | Throttles | >= 1 | 60s | Sum |

---

## Dashboard Setup

### Create a Custom Dashboard

**Via AWS Console:**

1. Open CloudWatch > Dashboards > Create Dashboard
2. Add the following widgets:

**ReceiveFeedback Widget (Line Graph):**
- Title: "ReceiveFeedback Metrics"
- Metrics:
  - `AWS/Lambda > Invocations` (dev-receive-feedback)
  - `AWS/Lambda > Errors` (dev-receive-feedback)
  - `AWS/Lambda > Duration` (dev-receive-feedback, statistic: Average)

**SendNotification Widget (Line Graph):**
- Title: "SendNotification Metrics"
- Metrics:
  - `AWS/Lambda > Invocations` (dev-send-notification)
  - `AWS/Lambda > Errors` (dev-send-notification)
  - `AWS/Lambda > Duration` (dev-send-notification, statistic: p99)

**GenerateReport Widget (Line Graph):**
- Title: "GenerateReport Metrics"
- Metrics:
  - `AWS/Lambda > Invocations` (dev-generate-report)
  - `AWS/Lambda > Errors` (dev-generate-report)
  - `AWS/Lambda > Duration` (dev-generate-report, statistic: Average)

**Error Rate Widget (Number):**
- Title: "Error Rate (%)"
- Metric: `AWS/Lambda > Errors / AWS/Lambda > Invocations * 100` (all three functions)

**Concurrent Execution Widget (Area Graph):**
- Title: "Reserved vs. Unreserved Concurrency"
- Metrics:
  - `AWS/Lambda > ConcurrentExecutions` (all three functions)

### Dashboard Purpose

A well-designed dashboard answers these questions at a glance:

1. **Is the system healthy?** Monitor invocation counts and error rates
2. **Are there performance issues?** Track duration trends and p99/p95 latencies
3. **Are we hitting limits?** Watch throttle counts and concurrent execution patterns
4. **What changed recently?** Correlate metric spikes with deployments or load increases

### CloudFormation Template for Dashboard

```yaml
FeedbackDashboard:
  Type: AWS::CloudWatch::Dashboard
  Properties:
    DashboardName: !Sub '${Environment}-feedback-dashboard'
    DashboardBody: !Sub |
      {
        "widgets": [
          {
            "type": "metric",
            "properties": {
              "metrics": [
                ["AWS/Lambda", "Invocations", {"stat": "Sum"}, {"label": "ReceiveFeedback"}],
                [".", "Errors", {"stat": "Sum"}, {"label": "ReceiveFeedback Errors"}],
                [".", "Duration", {"stat": "Average"}, {"label": "ReceiveFeedback Avg"}]
              ],
              "period": 300,
              "stat": "Average",
              "region": "${Region}",
              "title": "ReceiveFeedback"
            }
          }
        ]
      }
```

---

## Log Interpretation

### Structured Logging Fields

Logs from all three Lambdas follow a consistent format with key fields:

- **Timestamp:** ISO 8601 format (e.g., `2026-10-22T10:30:00.123Z`)
- **Level:** One of `FINE`, `INFO`, `WARNING`, `SEVERE`
- **Logger:** Class name (e.g., `SendNotificationHandler`)
- **Message:** Human-readable description of the event
- **Trace ID:** Correlation ID from Lambda context (if available)

### Correlation Using Trace IDs

When tracing a request through the system:

1. **ReceiveFeedback logs** include the feedback ID when received
2. **SNS message** to SendNotification carries that same feedback ID
3. **SendNotification logs** repeat the feedback ID during processing
4. **All logs** can be correlated by feedback ID to trace end-to-end flow

**Example Trace:**

```
ReceiveFeedback Log:
  "Feedback received successfully: 123e4567-e89b-12d3-a456-426614174000"

SNS Message (published by ReceiveFeedback):
  {"feedbackId": "123e4567-e89b-12d3-a456-426614174000", ...}

SendNotification Log:
  "Processing SNS message: ... \"feedbackId\":\"123e4567-e89b-12d3-a456-426614174000\" ..."
```

### Common Log Patterns by Function

#### ReceiveFeedback Success Flow

```
INFO    Lambda handler invoked for receiving feedback
FINE    Request method: POST, path: /avaliacao
FINE    Request parsed successfully
INFO    Feedback received successfully: [UUID]
```

#### ReceiveFeedback Validation Error

```
INFO    Lambda handler invoked for receiving feedback
WARNING Constraint violation: [validation message]
```

#### SendNotification Success Flow

```
INFO    Lambda handler invoked for sending notifications
FINE    Processing SNS message: {...}
INFO    Lambda handler completed
```

#### SendNotification Deserialization Error

```
INFO    Lambda handler invoked for sending notifications
SEVERE  Error processing SNS record: [parse error details]
```

#### GenerateReport Success Flow

```
INFO    Lambda handler invoked for generating and sending report
FINE    Report period calculated: startDate=[date], endDate=[date]
INFO    Report generation and sending completed successfully
```

### Severity Levels and Their Meanings

| Level | Usage | Action Required |
|-------|-------|-----------------|
| **FINE** | Detailed diagnostic info | None (informational) |
| **INFO** | Normal operation events | None (tracking) |
| **WARNING** | Recoverable issues | Monitor for patterns |
| **SEVERE** | Errors or failures | Investigate and fix |

### Interpreting Error Messages

**Validation Error (ReceiveFeedback):**
```
"title": "Validation Error",
"detail": "descricao must not be blank; nota must be between 1 and 5"
```
→ Client sent invalid input; validate request structure before retrying

**DynamoDB Error (ReceiveFeedback):**
```
"title": "Internal Server Error",
"detail": "An unexpected error occurred. Please try again later."
SEVERE: Error processing request: User: arn:aws:iam::[ID]:role/[Role] is not authorized to perform: dynamodb:PutItem
```
→ IAM role lacks DynamoDB write permissions; verify role policy

**SES Error (SendNotification):**
```
SEVERE: Notification sending failed: MessageRejected: Email address not verified
```
→ Recipient email not verified in SES; add to verified addresses in dev, or move to production with verified domain

**Parsing Error (SendNotification):**
```
SEVERE: Error processing SNS record: Cannot deserialize value of type UUID
```
→ SNS message format incorrect; verify ReceiveFeedback is publishing correct JSON structure

---

## Troubleshooting Common Issues

### Issue: High Error Rate in ReceiveFeedback

**Symptoms:**
- Errors metric in CloudWatch exceeds threshold
- Alarm triggers frequently
- API returns 4xx or 5xx responses

**Investigation Steps:**

1. **Check error types** in CloudWatch Logs:
   ```bash
   aws logs filter-log-events \
     --log-group-name /aws/lambda/dev-receive-feedback \
     --filter-pattern "SEVERE or WARNING" \
     --start-time $(date -d '1 hour ago' +%s)000
   ```

2. **Look for patterns:**
   - `"Validation Error"` → clients sending bad data; check API contract
   - `"not authorized"` → IAM permission issue; verify DynamoDB policy
   - `"Connection timeout"` → DynamoDB throttling; scale up or use Provisioned Billing

3. **Check metrics:**
   - If Throttles > 0, increase reserved concurrency
   - If Errors coincide with high Invocations, may be load-related

### Issue: SendNotification Not Processing Messages

**Symptoms:**
- SNS topic receives messages but Lambda doesn't trigger
- No logs appearing in SendNotification log group
- Messages pile up unprocessed

**Investigation Steps:**

1. **Verify SNS subscription:**
   ```bash
   aws sns list-subscriptions-by-topic \
     --topic-arn arn:aws:sns:us-east-1:[ACCOUNT]:dev-feedback-urgent-topic
   ```
   → Ensure Lambda is subscribed with correct ARN

2. **Check Lambda permissions:**
   ```bash
   aws lambda get-policy --function-name dev-send-notification
   ```
   → Verify `sns.amazonaws.com` principal has `lambda:InvokeFunction` permission

3. **Check SNS message format:**
   - Manually publish a test message:
   ```bash
   aws sns publish \
     --topic-arn arn:aws:sns:us-east-1:[ACCOUNT]:dev-feedback-urgent-topic \
     --message '{"feedbackId":"test-id","description":"test","urgency":"HIGH","createdAt":"2026-10-22T10:00:00"}'
   ```
   - Check SendNotification logs for processing

4. **Monitor dead-letter queue (if configured):**
   - Check SNS topic configuration for Dead Letter Queue
   - Examine messages in DLQ to understand why processing fails

### Issue: GenerateReport Never Runs

**Symptoms:**
- Monday 08:00 UTC arrives but no logs in GenerateReport log group
- No weekly reports sent
- EventBridge rule appears enabled but doesn't trigger

**Investigation Steps:**

1. **Verify EventBridge rule:**
   ```bash
   aws events describe-rule --name dev-generate-report-schedule
   ```
   → Confirm `State` is `ENABLED` and schedule is `cron(0 8 ? * MON *)`

2. **Check rule targets:**
   ```bash
   aws events list-targets-by-rule --rule dev-generate-report-schedule
   ```
   → Ensure Lambda ARN is listed

3. **Check Lambda permissions:**
   ```bash
   aws lambda get-policy --function-name dev-generate-report
   ```
   → Verify `events.amazonaws.com` principal has permission

4. **Test manually:**
   ```bash
   aws events put-events \
     --entries '[{"Source":"aws.events","DetailType":"Scheduled Event","Detail":"{}"}]'
   ```
   - Publish test event and check logs

5. **Check IAM role for DynamoDB access:**
   - GenerateReport needs `dynamodb:Scan` and `dynamodb:Query` permissions
   - Verify policy attached to `GenerateReportRole`

### Issue: High Latency (Slow Response Times)

**Symptoms:**
- Duration metrics show p99 > 5 seconds
- Latency alarm triggers
- Performance degrades at certain times

**Investigation Steps:**

1. **Identify bottleneck:**
   ```bash
   aws cloudwatch get-metric-statistics \
     --namespace AWS/Lambda \
     --metric-name Duration \
     --dimensions Name=FunctionName,Value=dev-receive-feedback \
     --start-time 2026-10-22T00:00:00Z \
     --end-time 2026-10-22T23:59:59Z \
     --period 300 \
     --statistics Maximum,Average,p99
   ```

2. **Correlate with other metrics:**
   - High Duration + High Invocations → concurrency limit approaching
   - High Duration only → underlying service latency (DynamoDB, SES, etc.)

3. **Check reserved concurrency:**
   - Increase reserved concurrency if hitting concurrent execution limits
   - `aws lambda update-function-configuration --function-name dev-receive-feedback --reserved-concurrent-executions 20`

4. **Check downstream service metrics:**
   - DynamoDB: Check ConsumedWriteCapacity and UserErrors
   - SES: Check Bounce, Complaint, and Delivery rates
   - SNS: Check PublishSize and MessageSize metrics

### Issue: Out of Memory or Timeout

**Symptoms:**
- Logs show `Task timed out` or `OutOfMemory`
- Lambda execution stops abruptly
- Duration metric shows exactly 30 seconds (timeout value)

**Investigation Steps:**

1. **Check Lambda configuration:**
   ```bash
   aws lambda get-function-configuration --function-name dev-receive-feedback
   ```
   → Review `MemorySize` (1024 MB configured) and `Timeout` (30 seconds configured)

2. **Increase timeout for GenerateReport:**
   - Report generation may take > 30 seconds with large datasets
   - Increase to 60 seconds if hitting timeout

3. **Optimize code if OOM:**
   - Stream large result sets instead of loading into memory
   - Use pagination for DynamoDB Scan operations

---

## Summary

Effective monitoring of this serverless platform relies on:

1. **Regular log review** — Check for SEVERE and WARNING entries weekly
2. **Metric trending** — Monitor error rate, duration, and throttle metrics daily
3. **Dashboard at-a-glance** — Verify dashboard during deployments
4. **Alarm response** — React promptly to threshold breaches
5. **Correlation tracing** — Use feedback IDs to trace requests end-to-end

For questions or escalations, consult the application documentation and AWS CloudWatch documentation linked below.

---

## References

- [AWS CloudWatch Logs Documentation](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/)
- [AWS Lambda Metrics Documentation](https://docs.aws.amazon.com/lambda/latest/dg/monitoring-metrics.html)
- [AWS CloudWatch Alarms Documentation](https://docs.aws.amazon.com/AmazonCloudWatch/latest/events/)
- [AWS EventBridge Documentation](https://docs.aws.amazon.com/eventbridge/)
- [AWS SAM Template Reference](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html)
