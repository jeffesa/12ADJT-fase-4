# End-to-End Testing Guide

This guide provides step-by-step instructions for testing all three critical flows of the feedback platform with LocalStack.

## Prerequisites

Before starting, ensure:

1. LocalStack is running and healthy:
   ```bash
   ./run.sh health
   ```
   Expected output: `✅ LocalStack (edge:4566): UP`

2. All three modules are running in dev mode:
   - Terminal 1: `cd receive-feedback && mvn quarkus:dev`
   - Terminal 2: `cd send-notification && mvn quarkus:dev`
   - Terminal 3: `cd generate-report && mvn quarkus:dev`

3. Resources are created:
   ```bash
   ./run.sh resources
   ```
   Should show `feedbacks` table, `feedback-urgent-topic`, `feedback-report-topic`, and queues

4. Tools available:
   - `curl` (for HTTP requests)
   - `jq` (for JSON parsing)
   - `aws` CLI (recommended, but optional if using `awslocal` via docker)

## Setup: Verify Resources

Warm up services and confirm all endpoints are accessible:

```bash
# Check health
curl -v http://localhost:4566

# Expected response: 200 OK with metadata

# Test GraphQL endpoint for receive-feedback module
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ __typename }"}'

# Expected response: {"data":{"__typename":"Query"}}
```

If services aren't responding, check the module terminal outputs for errors and wait for "Listening on" messages.

## Flow 1: HIGH Urgency Feedback

**Objective**: Test that HIGH-urgency feedback (rating 0-5) triggers SNS publish → send-notification Lambda → SES email

**Expected Behavior**:
- Feedback saved to DynamoDB with `urgencia: HIGH`
- SNS message published to `feedback-urgent-topic`
- Send-notification Lambda triggered
- Email sent via SES to `admin@feedback-fase-4.com`
- CloudWatch logs show execution < 1 second
- No errors in logs

### Step 1: Submit HIGH Urgency Feedback

Using curl:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { receiveFeedback(input: {descricao: \"Test critical feedback\", nota: 4}) { id descricao nota urgencia dataCriacao } }"
  }'
```

**Expected Response**:

```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "descricao": "Test critical feedback",
      "nota": 4,
      "urgencia": "HIGH",
      "dataCriacao": "2024-10-26T15:30:45.123Z"
    }
  }
}
```

**Record the feedback ID** for later verification: `550e8400-e29b-41d4-a716-446655440000`

### Step 2: Verify Feedback in DynamoDB

```bash
# Using docker compose (from fase-4 root directory)
docker compose exec localstack awslocal dynamodb scan \
  --table-name feedbacks \
  --region us-east-1 \
  --output json | jq '.Items[] | {id: .feedbackId.S, nota: .nota.N, urgencia: .urgencia.S}'
```

**Expected Output**:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "nota": "4",
  "urgencia": "HIGH"
}
```

**Verification**: 
- ✅ Feedback found in table
- ✅ `nota` is 4
- ✅ `urgencia` is HIGH

### Step 3: Verify SNS Message Published

```bash
# Check SNS topic attributes
docker compose exec localstack awslocal sns get-topic-attributes \
  --topic-arn arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic \
  --region us-east-1 \
  --output json
```

**Expected Output** (sample):

```json
{
  "Attributes": {
    "TopicArn": "arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic",
    "DisplayName": "",
    "SubscriptionsPending": "0",
    "SubscriptionsConfirmed": "1",
    "SubscriptionsDeleted": "0",
    "DeliveryPolicy": null,
    "EffectiveDeliveryPolicy": "{\"http\":{\"disableSubscriptionOverrides\":false}}",
    "Owner": "000000000000",
    "Policy": null
  }
}
```

**Verification**:
- ✅ Topic exists
- ✅ At least 1 subscription (send-notification Lambda)

### Step 4: Verify Email Sent via SES

```bash
# Check SES sent emails
docker compose exec localstack awslocal ses get-account-sending-enabled \
  --region us-east-1 \
  --output json
```

Check module logs (Terminal 2, send-notification):

Look for log entries like:
```
INFO: Sending email notification for feedback 550e8400-e29b-41d4-a716-446655440000 to admin@feedback-fase-4.com
INFO: Email sent successfully for feedback 550e8400-e29b-41d4-a716-446655440000
```

**Verification**:
- ✅ Log shows email sent
- ✅ Recipient is `admin@feedback-fase-4.com`
- ✅ No SEVERE/ERROR logs

### Step 5: Check Performance

In the send-notification module terminal, look for execution timing:

```
Invocation: 550e8400-e29b-41d4-a716-446655440000 completed in 234ms
```

**Verification**:
- ✅ Execution time < 1000ms (1 second)

## Flow 2: LOW Urgency Feedback

**Objective**: Test that LOW-urgency feedback (rating 8-10) does NOT trigger SNS or email

**Expected Behavior**:
- Feedback saved to DynamoDB with `urgencia: LOW`
- NO SNS message published
- NO send-notification Lambda triggered
- NO email sent
- No errors in logs

### Step 1: Submit LOW Urgency Feedback

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { receiveFeedback(input: {descricao: \"Test normal feedback\", nota: 9}) { id descricao nota urgencia dataCriacao } }"
  }'
```

**Expected Response**:

```json
{
  "data": {
    "receiveFeedback": {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "descricao": "Test normal feedback",
      "nota": 9,
      "urgencia": "LOW",
      "dataCriacao": "2024-10-26T15:31:45.123Z"
    }
  }
}
```

### Step 2: Verify Feedback in DynamoDB (but no email)

```bash
docker compose exec localstack awslocal dynamodb scan \
  --table-name feedbacks \
  --region us-east-1 \
  --output json | jq '.Items[] | select(.urgencia.S == "LOW") | {id: .feedbackId.S, nota: .nota.N, urgencia: .urgencia.S}'
```

**Expected Output**:

```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "nota": "9",
  "urgencia": "LOW"
}
```

### Step 3: Verify NO SNS Publish

Check the send-notification module logs (Terminal 2):

**Expected**: No new log entries for the LOW urgency feedback (no email sent)

If you see lines like:
```
INFO: Sending email notification for feedback 660e8400-...
```

That indicates LOW urgency feedback incorrectly triggered email. This is a bug.

### Step 4: Check Module State

In the receive-feedback module (Terminal 1), you should see:

```
Received feedback: nota=9, urgencia=LOW
Saved to DynamoDB but NOT publishing to SNS (urgencia is not HIGH)
```

**Verification**:
- ✅ Feedback saved to DynamoDB
- ✅ NO SNS publish occurred
- ✅ NO email sent
- ✅ No errors

## Flow 3: Generate Weekly Report

**Objective**: Test that generate-report Lambda queries prior-week feedbacks, aggregates stats, and sends HTML email

**Expected Behavior**:
- Query DynamoDB for feedbacks from prior 7 days
- Aggregate: total count, average rating, per-urgency counts
- Send HTML email to `admin@feedback-fase-4.com` with formatted table
- Report includes: date range, urgency distribution, rating statistics
- CloudWatch logs show execution < 1 second
- No errors

### Step 1: Trigger Weekly Report Generation

```bash
curl -X POST http://localhost:8282/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { generateWeeklyReport { reportId totalFeedbacks averageRating highCount mediumCount lowCount generatedAt } }"
  }'
```

**Expected Response** (example):

```json
{
  "data": {
    "generateWeeklyReport": {
      "reportId": "report-2024-10-26-001",
      "totalFeedbacks": 2,
      "averageRating": 6.5,
      "highCount": 1,
      "mediumCount": 0,
      "lowCount": 1,
      "generatedAt": "2024-10-26T15:32:00.000Z"
    }
  }
}
```

**Expected Metrics**:
- `totalFeedbacks`: 2 (one HIGH from Flow 1, one LOW from Flow 2)
- `averageRating`: 6.5 ((4 + 9) / 2)
- `highCount`: 1
- `lowCount`: 1
- `mediumCount`: 0

### Step 2: Verify DynamoDB Query

The generate-report module should query DynamoDB with a date range filter (prior 7 days):

In Terminal 3 (generate-report), look for:

```
INFO: Querying DynamoDB for feedbacks from 2024-10-19 to 2024-10-26
INFO: Found 2 matching feedbacks
INFO: Aggregating statistics...
INFO: Average rating: 6.5, HIGH: 1, MEDIUM: 0, LOW: 1
```

**Verification**:
- ✅ Query date range is 7 days back from today
- ✅ Found 2 feedbacks (exact match from Flows 1 & 2)
- ✅ Statistics calculated correctly

### Step 3: Verify Report Email Sent

In Terminal 3 (generate-report), look for:

```
INFO: Sending report email to admin@feedback-fase-4.com
INFO: Email contains HTML table with:
  - Report generation date: 2024-10-26T15:32:00Z
  - Date range: 2024-10-19 to 2024-10-26
  - Total feedbacks: 2
  - Average rating: 6.5
  - Urgency distribution table
```

**Verification**:
- ✅ Report email sent
- ✅ Recipient is `admin@feedback-fase-4.com`
- ✅ Email contains HTML tables with aggregation data
- ✅ Date range is correct (7-day window)

### Step 4: Check Performance

In Terminal 3, look for timing:

```
Report generation completed in 456ms
```

**Verification**:
- ✅ Execution time < 1000ms

## Verification: All Flows Complete

Run the comprehensive verification script:

```bash
./run.sh e2e-verify
```

This produces a CSV file at `/tmp/e2e-results.csv` with:

```
Flow,Status,Detail,ExecutionTime(ms),Evidence
"Flow 1 & 2 - DynamoDB","PASS","Feedbacks persisted correctly","0","Items: 2, HIGH: 1, LOW: 1"
"Flow 1 - SNS","PASS","SNS topic is accessible and configured","0","Topic ARN: arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic"
"Flow 1 - SES","PASS","SES identities configured (noreply, admin @feedback-fase-4.com)","0","Identities: noreply@feedback-fase-4.com,admin@feedback-fase-4.com"
"CloudWatch Logs","PASS","Log groups accessible","0","Groups: 3"
"Data Persistence","PASS","DynamoDB data persists across queries","0","Consistent item count: 2"
```

**All rows should show Status: PASS**

## Troubleshooting Tests

### Test Hangs on GraphQL Request

**Problem**: curl request times out or hangs

**Cause**: Module not responding (still starting up or crashed)

**Solution**:
1. Check module terminal for "Listening on port 8080" or similar
2. Wait 30+ seconds for Quarkus to fully start
3. Restart the module:
   - Press `Ctrl+C` in the module terminal
   - Run `mvn quarkus:dev` again
   - Wait for full startup

**Quick test**:
```bash
curl -v http://localhost:8080/q/health
# Should return 200 OK
```

### GraphQL Query Error

**Problem**: Returns GraphQL error, not data

**Example Error**:
```json
{
  "errors": [
    {
      "message": "Cannot query field 'receiveFeedback' on type 'Mutation'"
    }
  ]
}
```

**Cause**: Schema not updated or field name incorrect

**Solution**:
1. Verify field names are exactly: `descricao`, `nota`, not English names
2. Verify urgencia is calculated correctly (0-5=HIGH, 6-7=MEDIUM, 8-10=LOW)
3. Restart the module to reload schema

### Feedback Not Saved to DynamoDB

**Problem**: GraphQL returns success but DynamoDB scan shows 0 items

**Cause**: Module not connected to LocalStack or saving failed silently

**Solution**:
1. Check module logs for connection errors
2. Verify DynamoDB endpoint in `application.properties`
3. Verify table exists: `./run.sh resources`
4. Check module logs for DynamoDB exceptions

### SNS Not Triggered for HIGH Urgency

**Problem**: Flow 1 feedback created but send-notification not invoked

**Cause**: SNS subscription not set up or urgency calculation wrong

**Solution**:
1. Verify SNS topic exists: `./run.sh resources`
2. Check urgency calculation: rating 4 should = HIGH
3. Check send-notification module logs for subscription errors
4. Verify SNS event source mapping in send-notification config

### Performance > 1 Second

**Problem**: Lambda execution times are slow (> 1000ms)

**Cause**: DynamoDB throttling, network latency, or inefficient code

**Solution**:
1. Check LocalStack resource usage: `docker stats`
2. Check module logs for DynamoDB timeout errors
3. Try restarting LocalStack: `./run.sh clean && ./run.sh docker`
4. Profile code for inefficient queries

## Manual Testing with AWS CLI

### Query DynamoDB Directly

```bash
aws dynamodb scan \
  --table-name feedbacks \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --output json | jq '.'
```

### List SNS Messages

```bash
aws sns list-topics \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --output json | jq '.Topics'
```

### Check SES Identities

```bash
aws ses list-verified-email-addresses \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --output json
```

## Testing Tips

1. **Test in sequence**: Always test Flow 1 before Flow 2, to have valid test data
2. **Wait for async processing**: After Flow 1 submission, wait 2-3 seconds before checking SNS/SES
3. **Check timestamps**: Verify `dataCriacao` is recent (within seconds of your test time)
4. **Reset state**: Use `./run.sh clean` between test cycles to get a fresh database
5. **Monitor all terminals**: Keep Terminals 1, 2, 3 visible to catch errors immediately
6. **Save responses**: Copy GraphQL responses to a file for your test results documentation

## Next Steps

After completing all three flows successfully:

1. **Document results**: Record test outputs in [E2E_TEST_RESULTS.md](./E2E_TEST_RESULTS.md)
2. **Performance tuning**: If execution times > 1s, optimize queries or increase resources
3. **Load testing**: Run multiple feedbacks to test under load
4. **Integration testing**: Test error cases (invalid input, network failures)
5. **Deploy to AWS**: Use the same Lambda functions in production with real AWS credentials

## Related Documentation

- [LOCAL_SETUP.md](./LOCAL_SETUP.md) - Setup instructions
- [E2E_TEST_RESULTS.md](./E2E_TEST_RESULTS.md) - Test results template
- [../LOCALSTACK_SETUP.md](../LOCALSTACK_SETUP.md) - LocalStack configuration details
