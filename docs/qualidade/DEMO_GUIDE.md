# Demo Video Guide: Serverless Feedback Platform

**Duration**: ~10-12 minutes  
**Format**: Screen recording with narration  
**Target Audience**: Technical stakeholders, architects, developers

---

## Executive Summary

This demo showcases a production-grade serverless feedback platform built on AWS Lambda, DynamoDB, SNS, EventBridge, and SES. The demo focuses on the end-to-end data flow, demonstrating how feedback urgency automatically triggers notifications and how the platform scales without managing infrastructure.

---

## Demo Flow Overview

**Total Duration: 12 minutes**

| Segment | Duration | What to Show |
|---------|----------|--------------|
| **1. Architecture & Setup** | 1.5 min | Architecture diagram, local environment ready |
| **2. Create HIGH-Urgency Feedback** | 2.5 min | GraphQL mutation, email notification, DynamoDB |
| **3. Create LOW-Urgency Feedback** | 1.5 min | GraphQL mutation, verify NO email sent |
| **4. Query All Feedbacks** | 1 min | GraphQL query, list all with filters |
| **5. CloudWatch Logs & Metrics** | 2.5 min | Lambda execution logs, performance metrics |
| **6. AWS Console Walkthrough** | 2.5 min | IAM policies, event flow, architecture explanation |
| **7. Summary & Scalability** | 1 min | Key takeaways |

---

## Segment 1: Architecture & Setup (1.5 minutes)

**Narration Points:**
- "This is a serverless feedback platform running entirely on AWS."
- "The platform has three main components: feedback ingestion, notification delivery, and report generation."
- "All infrastructure is defined as code using AWS SAM and deployed to LocalStack for demo purposes."

**Screen Actions:**
1. Open a terminal showing the worktree directory structure:
   ```bash
   cd /Users/jefferson/Documents/FIAP/fase-4/.worktrees/final-docs
   tree -L 2
   ```

2. Show the architecture diagram from README.md:
   - Open README.md in an editor
   - Show the ASCII architecture diagram that illustrates:
     - API Gateway (GraphQL endpoint)
     - Lambda 1 (Receive Feedback) → DynamoDB
     - SNS Topic (HIGH urgency)
     - Lambda 2 (Send Notification) → SES
     - EventBridge → Lambda 3 (Generate Report)

3. Verify LocalStack is running:
   ```bash
   ./run.sh health
   ```
   - Expected: `✅ LocalStack (edge:4566): UP`

**Narration Close:**
- "Everything you see is deployed locally using LocalStack, which emulates AWS services."
- "In production, this would scale to thousands of requests per second without any infrastructure management."

---

## Segment 2: Create HIGH-Urgency Feedback (2.5 minutes)

**Narration Points:**
- "Let's create a high-urgency feedback by giving it a low rating (0-5)."
- "This will automatically be marked as HIGH urgency and trigger an email notification."
- "The GraphQL API validates the input and stores it in DynamoDB."

**Screen Actions:**

1. Open a terminal and create a HIGH-urgency feedback using GraphQL:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{
       "query": "mutation { receiveFeedback(input: {descricao: \"Critical issue: System crashes on login\", nota: 2}) { id descricao nota urgencia dataCriacao } }"
     }'
   ```

2. Show the response:
   - Highlight the returned fields:
     - `id`: unique identifier (UUID)
     - `descricao`: feedback description
     - `nota`: rating (2 in this case)
     - `urgencia`: "HIGH" (automatically calculated)
     - `dataCriacao`: timestamp

3. Record the feedback ID for later verification (copy and paste to a notepad).

4. Immediately switch to the email client/inbox:
   - Open browser or email client showing received emails
   - Show that an email has been received at `admin@feedback-fase-4.com`
   - Subject: "Critical Feedback Alert - Rating: 2"
   - Body should contain the feedback description and timestamp

5. Switch to AWS Console → DynamoDB (or LocalStack UI):
   - Navigate to the `feedbacks` table
   - Show the feedback just created:
     - Query by ID or scan the table
     - Highlight fields: `id`, `nota`, `urgencia`, `descricao`, `dataCriacao`
     - Point out: "Notice the urgencia is HIGH because the rating is 2"

**Narration Close:**
- "Within seconds, the feedback was stored in DynamoDB and an email notification was sent."
- "The entire flow is automated through our serverless pipeline."

---

## Segment 3: Create LOW-Urgency Feedback (1.5 minutes)

**Narration Points:**
- "Now let's create a positive feedback with a high rating (8-10)."
- "This will be marked as LOW urgency and NO email will be sent."
- "This demonstrates how we filter notifications based on urgency."

**Screen Actions:**

1. Create a LOW-urgency feedback using GraphQL:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{
       "query": "mutation { receiveFeedback(input: {descricao: \"Great service! Very satisfied with the experience.\", nota: 9}) { id descricao nota urgencia dataCriacao } }"
     }'
   ```

2. Show the response:
   - Highlight: `urgencia: "LOW"` (automatically calculated from rating 9)

3. Switch back to the email client:
   - Show that NO new email was received for this feedback
   - Point out: "No notification was sent because LOW urgency feedback doesn't trigger emails."

4. Switch to DynamoDB:
   - Scan the feedbacks table again
   - Show both feedbacks now exist:
     - First: rating 2, urgencia HIGH
     - Second: rating 9, urgencia LOW

5. Optional: Create one MEDIUM-urgency feedback (rating 6-7) to show all three states:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{
       "query": "mutation { receiveFeedback(input: {descricao: \"Average experience, room for improvement.\", nota: 6}) { id descricao nota urgencia dataCriacao } }"
     }'
   ```

**Narration Close:**
- "The same GraphQL mutation handles all urgency levels automatically."
- "The platform intelligently routes notifications based on business rules, not manual configuration."

---

## Segment 4: Query All Feedbacks (1 minute)

**Narration Points:**
- "Let's verify that all feedback we created is queryable through GraphQL."
- "The query endpoint provides a single source of truth for all feedback data."

**Screen Actions:**

1. Execute the GraphQL query to list all feedbacks:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{
       "query": "{ listarAvaliacoes { id descricao nota urgencia dataCriacao } }"
     }'
   ```

2. Show the response containing all three feedbacks (HIGH, MEDIUM, LOW).

3. Format the output using `jq` for clarity:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{"query": "{ listarAvaliacoes { id descricao nota urgencia dataCriacao } }"}' | jq '.data.listarAvaliacoes[]'
   ```

**Narration Close:**
- "All feedback is persisted and queryable in real-time."
- "The DynamoDB table is configured with on-demand billing, so you pay only for what you use."

---

## Segment 5: CloudWatch Logs & Metrics (2.5 minutes)

**Narration Points:**
- "Let's examine the CloudWatch logs to understand what happened under the hood."
- "Every Lambda execution is logged, and we can verify performance metrics."
- "All three Lambdas completed in under 1 second."

**Screen Actions:**

1. Open AWS Console (or LocalStack CloudWatch UI):
   - Navigate to CloudWatch → Log Groups
   - Show log groups for each function:
     - `/aws/lambda/dev-receive-feedback`
     - `/aws/lambda/dev-send-notification`
     - `/aws/lambda/dev-generate-report`

2. Click on the receive-feedback log group:
   - Show log stream for the HIGH-urgency feedback creation
   - Highlight key events:
     - "Received GraphQL mutation receiveFeedback"
     - "Validating input: descricao, nota (0-10)"
     - "Calculating urgency from rating 2 → HIGH"
     - "Persisting feedback to DynamoDB"
     - "Publishing message to SNS topic: feedback-urgent-topic"
     - "Duration: 247ms"

3. Click on the send-notification log group:
   - Show log stream showing SNS trigger
   - Highlight key events:
     - "Triggered by SNS message from feedback-urgent-topic"
     - "Extracting feedback details from event"
     - "Sending email via SES to admin@feedback-fase-4.com"
     - "Email subject: Critical Feedback Alert"
     - "Duration: 412ms"

4. Switch to Metrics tab:
   - Show Lambda invocation count (3 invocations)
   - Show error count (0)
   - Show duration histogram (all under 500ms)

**Narration Close:**
- "The logs show exactly what happened in each step of the pipeline."
- "Performance is excellent: all operations completed in under 500ms."
- "This is the benefit of serverless: automatic logging, monitoring, and scaling."

---

## Segment 6: AWS Console Walkthrough (2.5 minutes)

**Narration Points:**
- "Let's zoom out and understand the architecture from an AWS perspective."
- "We'll review the IAM security model, event flow, and why this design scales."

**Screen Actions:**

1. **Open the AWS Console (CloudFormation stack)**:
   - Show the deployed stack: `fase-4-stack`
   - Expand resources to show:
     - DynamoDB table: `dev-feedback-table`
     - SNS topic: `dev-feedback-urgent-topic`
     - Lambda functions (3 listed)
     - IAM roles (3 listed)
     - CloudWatch log groups (3 listed)

2. **Show IAM Roles**:
   - Click on `ReceiveFeedbackRole`:
     - Inline policy: `DynamoDBWritePolicy` → allows `PutItem` on feedbacks table
     - Inline policy: `SNSPublishPolicy` → allows `Publish` to feedback-urgent-topic
     - Managed policy: `AWSLambdaBasicExecutionRole` → allows logging
     - **Principle of least privilege**: only permissions needed

   - Click on `SendNotificationRole`:
     - Inline policy: `SESEmailPolicy` → allows `SendEmail` and `SendRawEmail`
     - No access to DynamoDB (doesn't need it)
     - Managed policy: `AWSLambdaBasicExecutionRole` → allows logging
     - **Security**: isolated responsibility

   - Click on `GenerateReportRole`:
     - Inline policy: `DynamoDBScanPolicy` → allows `Scan` and `Query` on feedbacks table
     - Inline policy: `SESEmailPolicy` → allows sending emails
     - Managed policy: `AWSLambdaBasicExecutionRole` → allows logging
     - **Separation of concerns**: this Lambda only reads, doesn't write

3. **Show SNS Topic**:
   - Topic ARN: `arn:aws:sns:us-east-1:000000000000:dev-feedback-urgent-topic`
   - Subscriptions: 1 (the send-notification Lambda)
   - **Design pattern**: SNS acts as a message bus for decoupling

4. **Show DynamoDB Table**:
   - Billing mode: On-demand (pay per request)
   - TTL: enabled on `dataCriacao` field (90 days auto-delete)
   - Partition key: `id` (UUID)
   - **Scalability**: on-demand scaling, automatic cleanup

5. **Show EventBridge Rule** (if visible in SAM template):
   - Name: `feedback-report-rule`
   - Pattern: matches events from SNS topic
   - Target: invoke `generate-report` Lambda
   - **Event-driven architecture**: asynchronous, loosely coupled

**Narration Close:**
- "The entire architecture is defined in code using AWS SAM (Serverless Application Model)."
- "Security is built in: each Lambda has minimal required permissions."
- "Scaling is automatic: Lambda handles burst traffic, DynamoDB scales on-demand, no capacity planning needed."

---

## Segment 7: Summary & Scalability (1 minute)

**Narration Points:**
- "Let's recap what we've demonstrated."
- "This architecture handles real-world requirements at scale."

**Screen Actions:**

1. Return to terminal and show the run.sh help:
   ```bash
   ./run.sh help
   ```

2. Show available commands to highlight platform capabilities:
   - `./run.sh health` → Infrastructure monitoring
   - `./run.sh resources` → Resource creation and listing
   - `./run.sh logs` → Unified log viewing
   - `./run.sh test` → Automated testing

**Key Talking Points:**

- **Reliability**: 3-layer redundancy (DynamoDB, SNS, SES all have built-in replication)
- **Scalability**: Designed for 1000s of requests/sec; pay only for what you use
- **Security**: IAM policies enforce least privilege; no direct database access from APIs
- **Maintainability**: Infrastructure as code (SAM); services handle patching and updates
- **Monitoring**: CloudWatch provides comprehensive observability out of the box
- **Cost**: No servers to run 24/7; on-demand pricing scales with actual usage

**Final Narration:**

"This serverless feedback platform demonstrates how modern AWS services can be combined to build secure, scalable, and maintainable applications. The three feedback flows (HIGH → notification, MEDIUM → queued, LOW → stored) showcase event-driven architecture in action. In production, this design would support millions of feedback entries with zero infrastructure management overhead."

---

## Technical Notes for Demo Day

### Pre-Demo Checklist

- [ ] LocalStack is running and healthy (`./run.sh health`)
- [ ] All three modules are running in dev mode (receive-feedback, send-notification, generate-report)
- [ ] Resources are created (`./run.sh resources`)
- [ ] Email client is open and configured to show `admin@feedback-fase-4.com` inbox
- [ ] AWS Console or LocalStack dashboard is accessible
- [ ] Terminal is clear and ready for commands
- [ ] Recording software (OBS, QuickTime, etc.) is set up

### Recording Tips

- **Zoom**: Set terminal font size to 14pt for readability
- **Pacing**: Speak clearly and pause after each action for clarity
- **Timing**: Use silence strategically (e.g., wait for email to arrive)
- **Annotations**: Use screen highlighting tools to point out key elements
- **Mistakes**: If an error occurs, pause and explain the root cause or retry
- **Video Quality**: Record at 1080p, 30 FPS minimum

### Commands Cheat Sheet

**Check Health:**
```bash
./run.sh health
```

**Create Feedback (HIGH):**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { receiveFeedback(input: {descricao: \"Critical issue: System crashes on login\", nota: 2}) { id descricao nota urgencia dataCriacao } }"}'
```

**Create Feedback (LOW):**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { receiveFeedback(input: {descricao: \"Great service! Very satisfied with the experience.\", nota: 9}) { id descricao nota urgencia dataCriacao } }"}'
```

**Query All Feedbacks:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ listarAvaliacoes { id descricao nota urgencia dataCriacao } }"}'
```

**View DynamoDB Table:**
```bash
docker compose exec localstack awslocal dynamodb scan \
  --table-name feedbacks \
  --region us-east-1 \
  --output json | jq '.Items[] | {id: .feedbackId.S, nota: .nota.N, urgencia: .urgencia.S}'
```

**View CloudWatch Logs:**
```bash
./run.sh logs
```

---

## Expected Demo Results

### HIGH-Urgency Flow
- ✅ GraphQL mutation accepted with rating 2
- ✅ Urgencia field returns "HIGH"
- ✅ Feedback stored in DynamoDB within 300ms
- ✅ SNS message published to feedback-urgent-topic
- ✅ Send-notification Lambda triggered
- ✅ Email received in `admin@feedback-fase-4.com` inbox within 500ms
- ✅ CloudWatch logs show all steps with timestamps

### LOW-Urgency Flow
- ✅ GraphQL mutation accepted with rating 9
- ✅ Urgencia field returns "LOW"
- ✅ Feedback stored in DynamoDB within 300ms
- ✅ No SNS message published (LOW urgency doesn't trigger notifications)
- ✅ No email sent
- ✅ Feedback still queryable via GraphQL

### Query Flow
- ✅ GraphQL query returns all created feedbacks
- ✅ All fields are present and correctly formatted
- ✅ Query completes within 100ms

### Observability
- ✅ CloudWatch logs show all Lambda executions
- ✅ Execution times are under 500ms per function
- ✅ Zero errors in logs
- ✅ Metrics dashboard shows correct invocation count and error rate (0)

---

## Appendix: Architecture Principles Explained

### Urgency Calculation
```
Rating 0-5  → HIGH    (critical feedback requires immediate attention)
Rating 6-7  → MEDIUM  (moderate feedback, queued for review)
Rating 8-10 → LOW     (positive feedback, logged for analytics)
```

### Event-Driven Pipeline
1. **Trigger**: GraphQL mutation invokes receive-feedback Lambda
2. **Validation**: Input validated (description, rating 0-10)
3. **Persistence**: Feedback stored in DynamoDB with generated UUID
4. **Routing**: If HIGH urgency, message published to SNS topic
5. **Notification**: Send-notification Lambda triggered by SNS
6. **Delivery**: Email sent via SES to admin
7. **Analytics**: EventBridge captures all events for reporting Lambda

### Serverless Benefits
- **No servers to manage**: AWS handles infrastructure
- **Automatic scaling**: Handles 1 req/sec or 10,000 req/sec
- **Pay-per-use**: Only charged for execution time and data storage
- **Built-in security**: IAM roles, encryption in transit/at rest
- **Integrated monitoring**: CloudWatch logs and metrics included
- **Rapid development**: Focus on business logic, not infrastructure

---

## Video Script Template

**[INTRO - 30 seconds]**

"Welcome to the FIAP Fase-4 Serverless Feedback Platform demo. In the next 12 minutes, you'll see how AWS Lambda, DynamoDB, SNS, and SES work together to create a scalable, secure feedback collection system. We'll create feedback with different urgency levels, watch notifications trigger in real-time, and explore the CloudWatch logs that power our observability."

**[MAIN DEMO - 10 minutes]**
*Follow segments 1-6 as outlined above*

**[CLOSING - 1 minute]**

"This architecture demonstrates best practices in serverless design: automated urgency calculation, event-driven communication, security through IAM roles, and complete observability. The entire infrastructure is defined as code using AWS SAM, making it reproducible and maintainable. Thank you for watching."

---

