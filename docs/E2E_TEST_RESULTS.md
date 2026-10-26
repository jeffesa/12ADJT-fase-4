# End-to-End Test Results

This document is a **template** for recording the execution results of the feedback platform E2E tests with LocalStack.

## Instructions

1. Run `./e2e-test.sh` to execute all three flows (see LOCAL_SETUP.md for setup details).
2. Run `./e2e-verify.sh` to validate results and capture LocalStack evidence.
3. Copy this template and fill in all [FILL:] placeholders with actual test data.
4. Save the completed document as `E2E_TEST_RESULTS_YYYY-MM-DD.md` in the docs/ directory.
5. Commit the result as evidence of successful testing.

For reference, see `E2E_TEST_RESULTS_SAMPLE.md` for an example of a filled-in result.

---

## Template

This document records the execution results of the feedback platform E2E tests with LocalStack.

## Test Execution Environment

**Test Date**: [FILL: YYYY-MM-DD HH:MM UTC]

**Tester**: [FILL: Name or CI/CD Pipeline]

### System Information

| Item | Value |
|------|-------|
| Operating System | [FILL: e.g., macOS 14.1, Linux Ubuntu 22.04, Windows 11] |
| Docker Version | [FILL: e.g., 25.0.1] |
| Docker Compose Version | [FILL: e.g., 2.24.0] |
| Java Version | [FILL: e.g., 17.0.8] |
| Maven Version | [FILL: e.g., 3.9.6] |
| LocalStack Image | [FILL: e.g., localstack/localstack:3.0] |

### Environment Configuration

| Variable | Value |
|----------|-------|
| AWS_REGION | us-east-1 |
| AWS_ACCESS_KEY_ID | test |
| AWS_ENDPOINT_URL | http://localhost:4566 |
| Quarkus Mode | development (mvn quarkus:dev) |

## Test Flows Checklist

- [ ] Flow 1: HIGH Urgency Feedback (rating 4) → DynamoDB + SNS + Email
- [ ] Flow 2: LOW Urgency Feedback (rating 9) → DynamoDB only (no SNS/Email)
- [ ] Flow 3: Generate Weekly Report → Query aggregation + Email

## Flow 1: HIGH Urgency Feedback

### Objective
Submit feedback with rating 4 (HIGH urgency), verify it triggers SNS publish, send-notification Lambda, and SES email to admin@feedback-fase-4.com.

### Test Input

```graphql
mutation {
  receiveFeedback(input: {descricao: "Test critical feedback", nota: 4}) {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
```

### Test Execution

**Start Time**: [FILL: HH:MM:SS UTC]

**HTTP Request**:
```
POST http://localhost:8080/graphql
Content-Type: application/json

{"query": "mutation { receiveFeedback(input: {descricao: \"Test critical feedback\", nota: 4}) { id descricao nota urgencia dataCriacao } }"}
```

**HTTP Response Status**: [FILL: e.g., 200]

### Test Output

**Response JSON**:
```json
[FILL: Paste entire GraphQL response here]
```

**Extracted Values**:
| Field | Value |
|-------|-------|
| Feedback ID | [FILL: UUID] |
| Description | [FILL: Should be "Test critical feedback"] |
| Rating (nota) | [FILL: Should be 4] |
| Urgency (urgencia) | [FILL: Should be "HIGH"] |
| Created At (dataCriacao) | [FILL: ISO 8601 timestamp] |

### DynamoDB Verification

**Command Executed**:
```bash
docker compose exec localstack awslocal dynamodb scan \
  --table-name feedbacks \
  --region us-east-1 \
  --output json | jq '.Items[] | select(.feedbackId.S == "[FILL: Feedback ID from above]")'
```

**DynamoDB Response**:
```json
[FILL: Paste DynamoDB item here]
```

**Verification Results**:
- [ ] **PASS**: Feedback found in DynamoDB table
- [ ] **PASS**: `feedbackId` matches submitted value
- [ ] **PASS**: `nota` = 4
- [ ] **PASS**: `urgencia` = "HIGH"
- [ ] **PASS**: `createdAt` timestamp is recent

**Evidence** (DynamoDB query output):
```
[PASTE ACTUAL QUERY RESULT]
```

### SNS Verification

**Command Executed**:
```bash
docker compose exec localstack awslocal sns get-topic-attributes \
  --topic-arn arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic \
  --region us-east-1 \
  --output json
```

**SNS Response**:
```json
[FILL: Paste SNS attributes here]
```

**Verification Results**:
- [ ] **PASS**: Topic `feedback-urgent-topic` exists
- [ ] **PASS**: At least 1 confirmed subscription (send-notification Lambda)

**Evidence** (SNS attributes):
```
[PASTE ACTUAL QUERY RESULT]
```

### SES Email Verification

**Module Logs** (from send-notification Terminal 2):

Look for log entries like:
```
INFO: Sending email notification for feedback [ID] to admin@feedback-fase-4.com
INFO: Email sent successfully for feedback [ID]
```

**Paste relevant log excerpt**:
```
[FILL: Paste log entries showing email sent to admin@feedback-fase-4.com]
```

**Verification Results**:
- [ ] **PASS**: Email sent to `admin@feedback-fase-4.com`
- [ ] **PASS**: No SEVERE or ERROR logs
- [ ] **PASS**: Execution time visible in logs

**Email Details**:
| Item | Value |
|------|-------|
| From (sender) | [FILL: Should be noreply@feedback-fase-4.com] |
| To (recipient) | [FILL: Should be admin@feedback-fase-4.com] |
| Subject | [FILL: Should contain feedback rating and urgency] |
| Execution Time | [FILL: milliseconds, should be < 1000] |

### Overall Flow 1 Result

**Status**: [ ] PASS / [ ] FAIL / [ ] PARTIAL

**Summary**: [FILL: Brief description of test outcome]

---

## Flow 2: LOW Urgency Feedback

### Objective
Submit feedback with rating 9 (LOW urgency), verify it is saved to DynamoDB but does NOT trigger SNS or email.

### Test Input

```graphql
mutation {
  receiveFeedback(input: {descricao: "Test normal feedback", nota: 9}) {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
```

### Test Execution

**Start Time**: [FILL: HH:MM:SS UTC]

### Test Output

**Response JSON**:
```json
[FILL: Paste entire GraphQL response here]
```

**Extracted Values**:
| Field | Value |
|-------|-------|
| Feedback ID | [FILL: UUID] |
| Description | [FILL: Should be "Test normal feedback"] |
| Rating (nota) | [FILL: Should be 9] |
| Urgency (urgencia) | [FILL: Should be "LOW"] |
| Created At (dataCriacao) | [FILL: ISO 8601 timestamp] |

### DynamoDB Verification

**Command Executed**:
```bash
docker compose exec localstack awslocal dynamodb scan \
  --table-name feedbacks \
  --region us-east-1 \
  --output json | jq '.Items[] | select(.urgencia.S == "LOW")'
```

**DynamoDB Response**:
```json
[FILL: Paste DynamoDB LOW items here]
```

**Verification Results**:
- [ ] **PASS**: Feedback found in DynamoDB table
- [ ] **PASS**: `nota` = 9
- [ ] **PASS**: `urgencia` = "LOW"

**Evidence**:
```
[PASTE ACTUAL QUERY RESULT]
```

### SNS No-Publish Verification

**Module Logs** (from receive-feedback Terminal 1):

Look for log entries indicating SNS was NOT called:
```
Received feedback: nota=9, urgencia=LOW
Saved to DynamoDB but NOT publishing to SNS (urgencia is not HIGH)
```

**Paste relevant log excerpt**:
```
[FILL: Paste log entries showing NO SNS publish occurred]
```

**Verification Results**:
- [ ] **PASS**: No SNS publish occurred for LOW urgency
- [ ] **PASS**: No email sent (check send-notification logs for no new entries)

**Evidence**:
```
[PASTE ACTUAL LOG RESULT]
```

### Overall Flow 2 Result

**Status**: [ ] PASS / [ ] FAIL / [ ] PARTIAL

**Summary**: [FILL: Brief description of test outcome]

---

## Flow 3: Generate Weekly Report

### Objective
Trigger weekly report generation, verify it queries DynamoDB for prior-week feedbacks, aggregates statistics, and sends HTML email to admin@feedback-fase-4.com.

### Test Input

```graphql
mutation {
  generateWeeklyReport {
    reportId
    totalFeedbacks
    averageRating
    highCount
    mediumCount
    lowCount
    generatedAt
  }
}
```

### Test Execution

**Start Time**: [FILL: HH:MM:SS UTC]

### Test Output

**Response JSON**:
```json
[FILL: Paste entire GraphQL response here]
```

**Extracted Values**:
| Field | Value |
|-------|-------|
| Report ID | [FILL: UUID or report identifier] |
| Total Feedbacks | [FILL: Should be 2 (from Flow 1 and 2)] |
| Average Rating | [FILL: Should be 6.5 ((4+9)/2)] |
| HIGH Count | [FILL: Should be 1] |
| MEDIUM Count | [FILL: Should be 0] |
| LOW Count | [FILL: Should be 1] |
| Generated At | [FILL: ISO 8601 timestamp] |

### DynamoDB Query Verification

**Module Logs** (from generate-report Terminal 3):

Look for log entries showing DynamoDB query with 7-day window:
```
INFO: Querying DynamoDB for feedbacks from 2024-10-19 to 2024-10-26
INFO: Found 2 matching feedbacks
INFO: Aggregating statistics...
INFO: Average rating: 6.5, HIGH: 1, MEDIUM: 0, LOW: 1
```

**Paste relevant log excerpt**:
```
[FILL: Paste log entries showing DynamoDB query and aggregation]
```

**Verification Results**:
- [ ] **PASS**: Date range is 7 days prior to today
- [ ] **PASS**: Found 2 feedbacks matching query
- [ ] **PASS**: Statistics calculated correctly
- [ ] **PASS**: Aggregation matches expected values

**Query Details**:
| Item | Value |
|------|-------|
| Query Start Date | [FILL: Should be 7 days ago] |
| Query End Date | [FILL: Should be today] |
| Feedbacks Found | [FILL: Should be 2] |
| Average Rating Calculated | [FILL: (4 + 9) / 2 = 6.5] |

**Evidence**:
```
[PASTE ACTUAL LOG RESULT]
```

### Report Email Verification

**Module Logs** (from generate-report Terminal 3):

Look for log entries showing email sent with HTML table:
```
INFO: Sending report email to admin@feedback-fase-4.com
INFO: Email contains HTML table with:
  - Report generation date: 2024-10-26T15:32:00Z
  - Date range: 2024-10-19 to 2024-10-26
  - Total feedbacks: 2
  - Average rating: 6.5
  - Urgency distribution table
```

**Paste relevant log excerpt**:
```
[FILL: Paste log entries showing email sent with HTML content]
```

**Email Details**:
| Item | Value |
|------|-------|
| From (sender) | [FILL: Should be noreply@feedback-fase-4.com] |
| To (recipient) | [FILL: Should be admin@feedback-fase-4.com] |
| Subject | [FILL: Should contain "Weekly Report" or similar] |
| HTML Content Includes | [FILL: Yes/No - date range, totals table, urgency distribution] |
| Execution Time | [FILL: milliseconds, should be < 1000] |

**Evidence** (HTML table structure):
```
[FILL: Paste actual HTML email content or table structure from logs]
```

### Overall Flow 3 Result

**Status**: [ ] PASS / [ ] FAIL / [ ] PARTIAL

**Summary**: [FILL: Brief description of test outcome]

---

## DynamoDB Final State

**Total Items in Table**:
```bash
docker compose exec localstack awslocal dynamodb scan --table-name feedbacks --region us-east-1 --output json | jq '.Items | length'
```

**Expected**: 2 items (one HIGH, one LOW)

**Actual**: [FILL: Number]

**Final Scan Output**:
```json
[FILL: Paste complete DynamoDB scan result here]
```

**Verification Results**:
- [ ] **PASS**: Total of 2 items in table
- [ ] **PASS**: 1 HIGH urgency item (nota=4)
- [ ] **PASS**: 1 LOW urgency item (nota=9)
- [ ] **PASS**: All timestamps are within test window

---

## SNS Statistics

**Command Executed**:
```bash
docker compose exec localstack awslocal sns get-topic-attributes \
  --topic-arn arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic \
  --region us-east-1 \
  --output json | jq '.Attributes | {SubscriptionsConfirmed, SubscriptionsPending}'
```

**Output**:
```json
[FILL: Paste SNS attributes]
```

**Verification Results**:
- [ ] **PASS**: Topic exists with subscriptions
- [ ] **PASS**: At least 1 confirmed subscription

---

## SES Statistics

**Command Executed**:
```bash
docker compose exec localstack awslocal ses list-verified-email-addresses \
  --region us-east-1 \
  --output json
```

**Output**:
```json
[FILL: Paste SES verified identities]
```

**Verification Results**:
- [ ] **PASS**: `noreply@feedback-fase-4.com` is verified
- [ ] **PASS**: `admin@feedback-fase-4.com` is verified

---

## CloudWatch Logs

**Log Groups Available**:
```bash
docker compose exec localstack awslocal logs describe-log-groups --region us-east-1 --output json | jq '.logGroups[].logGroupName'
```

**Output**:
```
[FILL: Paste log group names]
```

**Available Log Groups**:
- [ ] `/aws/lambda/ReceiveFeedback` (or similar)
- [ ] `/aws/lambda/SendNotification` (or similar)
- [ ] `/aws/lambda/GenerateReport` (or similar)

---

## Performance Metrics

### Execution Times

| Flow | Lambda | Execution Time (ms) | Target (ms) | Status |
|------|--------|-------------------|-------------|--------|
| 1 | ReceiveFeedback | [FILL] | < 1000 | [ ] PASS / [ ] FAIL |
| 1 | SendNotification | [FILL] | < 1000 | [ ] PASS / [ ] FAIL |
| 3 | GenerateReport | [FILL] | < 1000 | [ ] PASS / [ ] FAIL |

### Performance Analysis

**Slowest Operation**: [FILL: Which Lambda or operation took longest]

**Potential Bottleneck**: [FILL: Analysis of any slow operations]

**Recommendations**: [FILL: Suggestions for optimization if needed]

---

## Overall Test Results Summary

| Flow | Status | Issues | Evidence |
|------|--------|--------|----------|
| 1 - HIGH Urgency | [ ] PASS | [FILL] | [LINK: DynamoDB / SNS / SES logs] |
| 2 - LOW Urgency | [ ] PASS | [FILL] | [LINK: DynamoDB logs] |
| 3 - Report Generation | [ ] PASS | [FILL] | [LINK: Query logs / Email content] |

### Final Verdict

**All Flows Completed**: [ ] YES / [ ] NO

**All Expected Behaviors Observed**: [ ] YES / [ ] NO

**All Performance Targets Met**: [ ] YES / [ ] NO

**Overall Status**: [ ] PASS / [ ] FAIL / [ ] BLOCKED

---

## Issues Found

### Issue 1
**Description**: [FILL: What went wrong]

**Severity**: [ ] Critical / [ ] High / [ ] Medium / [ ] Low

**Module Affected**: [FILL: Which module]

**Root Cause Analysis**: [FILL: Why it happened]

**Resolution**: [FILL: How it was fixed or workaround]

**Evidence** (logs/screenshots):
```
[FILL: Paste relevant error logs]
```

---

## Notes and Observations

[FILL: Any additional notes about test execution, environment quirks, interesting findings, or suggestions for future test runs]

---

## Test Artifacts

**Log Files**:
- E2E Test Log: `/tmp/e2e-logs/test-execution-[TIMESTAMP].log`
- Verification Log: `/tmp/e2e-verify.log`
- Results CSV: `/tmp/e2e-results.csv`

**Commands to Collect Evidence**:

```bash
# Backup test logs
cp /tmp/e2e-logs/* ./docs/test-results/$(date +%Y%m%d_%H%M%S)/

# Export DynamoDB state
docker compose exec localstack awslocal dynamodb scan \
  --table-name feedbacks --region us-east-1 > ./docs/test-results/dynamodb-state.json

# Export module logs
# Copy output from Terminal 1, 2, 3 into separate text files
```

---

## Sign-off

| Item | Value |
|------|-------|
| Test Completed By | [FILL: Name] |
| Date Completed | [FILL: YYYY-MM-DD] |
| Approval | [ ] Approved / [ ] Approved with Issues / [ ] Rejected |
| Approved By | [FILL: Name if approved] |

---

**For Future Runs**: Copy this template, fill in the [FILL: ] sections with actual test data, and save as a dated snapshot for regression testing and documentation.
