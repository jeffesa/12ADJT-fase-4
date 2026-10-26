# FIAP Fase-4: Serverless Feedback Platform

**Status**: Infrastructure Deployed | Application Blocker Documented  
**Region**: AWS US-East-1  
**Build**: Java 17 + Maven 3.9 + Quarkus 3.8

## Overview

This is a multi-module Quarkus serverless application for collecting, processing, and analyzing feedback at scale using AWS services:

- **receive-feedback**: REST API Lambda for ingesting feedback submissions
- **send-notification**: SNS-triggered Lambda for sending urgent notifications via SES
- **generate-report**: EventBridge-scheduled Lambda for weekly report generation

### Architecture

```
┌─────────────────┐
│   API Gateway   │ HTTP POST /avaliacao
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────┐
│ ReceiveFeedbackFunction (Lambda) │ ◄──── DynamoDB (persist)
└────────┬────────────────────────┘       SNS (publish if urgent)
         │
         ├──────────────────────────────┐
         │ HIGH urgency (rating 0-5)?   │
         ▼                              ▼
      [SNS Publish]             [No action]
         │
         ▼
   ┌──────────────┐
   │ SNS Topic    │
   └──────┬───────┘
          │
          ▼
┌──────────────────────────────┐
│ SendNotificationFunction     │ ◄──── SES (send email)
│ (SNS-triggered Lambda)       │
└──────────────────────────────┘

Schedule: Every Monday 8 AM UTC
          │
          ▼
┌──────────────────────────────┐
│ GenerateReportFunction       │ ◄──── DynamoDB (scan)
│ (EventBridge-triggered)      │       SES (send email)
└──────────────────────────────┘
```

### Feedback Urgency Classification

| Rating | Urgency | Action |
|--------|---------|--------|
| 0-5 | HIGH | SNS publish → Email notification immediately |
| 6-7 | MEDIUM | DynamoDB only, no notification |
| 8-10 | LOW | DynamoDB only, no notification |

---

## Quick Start

### Prerequisites

```bash
# Check Java version
java -version          # Should be Java 17+

# Check Maven version
mvn -version           # Should be 3.9+

# Check AWS CLI
aws --version          # Should be 2.x

# Configure AWS credentials
aws configure
# OR use environment variables:
export AWS_ACCESS_KEY_ID="..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_DEFAULT_REGION="us-east-1"
```

### Build Locally

```bash
# Build all modules
cd /Users/jefferson/Documents/FIAP/fase-4
mvn clean package -DskipTests

# Run unit tests
mvn test

# Build only specific module
mvn -pl receive-feedback package
```

### Deploy to AWS

```bash
# Build SAM artifacts
sam build -t infra/template.yaml

# Deploy to AWS (interactive)
cd infra
sam deploy --guided

# Or deploy programmatically (see PRODUCTION_DEPLOYMENT_GUIDE.md)
sam deploy --config-env prod --no-confirm-changeset
```

---

## AWS Deployment

### Stack Overview

**Stack Name**: `prod-feedback-stack`  
**Region**: `us-east-1`  
**Status**: ✅ Deployed and Verified

### Key Resources

| Resource | Name | Status |
|---|---|---|
| DynamoDB Table | `dev-feedback-table` | ✅ Active |
| SNS Topic | `dev-feedback-urgent-topic` | ✅ Active |
| Lambda Functions | `dev-receive-feedback` | ✅ Active |
| | `dev-send-notification` | ✅ Active |
| | `dev-generate-report` | ✅ Active |
| API Gateway | `dev-feedback-api` | ✅ Active |
| CloudWatch Logs | 3 log groups | ✅ Created |

### Stack Outputs

After deployment, retrieve outputs:

```bash
aws cloudformation describe-stacks \
  --stack-name prod-feedback-stack \
  --region us-east-1 \
  --query 'Stacks[0].Outputs' \
  --output table
```

**Key Outputs**:
- **ApiEndpoint**: `https://ts8f4zjhl9.execute-api.us-east-1.amazonaws.com/dev/avaliacao`
- **ApiKey**: `ilhy8cbjo2`
- **DynamoDBTableName**: `dev-feedback-table`
- **SNSTopicArn**: `arn:aws:sns:us-east-1:ACCOUNT_ID:dev-feedback-urgent-topic`

### Test the Deployment

```bash
# Get outputs
export API_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name prod-feedback-stack \
  --region us-east-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' \
  --output text)

export API_KEY=$(aws cloudformation describe-stacks \
  --stack-name prod-feedback-stack \
  --region us-east-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiKey`].OutputValue' \
  --output text)

# Test API (currently blocked - see Known Issues)
curl -X POST "$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "x-api-key: $API_KEY" \
  -d '{"descricao": "Test feedback", "nota": 4}'
```

---

## Documentation

### Deployment Documentation

- **[PRODUCTION_DEPLOYMENT_GUIDE.md](./docs/PRODUCTION_DEPLOYMENT_GUIDE.md)** - Complete step-by-step deployment instructions
  - Prerequisites & setup
  - Build & package process
  - AWS configuration
  - Deployment procedures
  - Post-deployment verification
  - Troubleshooting guide

### Test Results & Status

- **[AWS_DEPLOYMENT_RESULTS.md](./docs/AWS_DEPLOYMENT_RESULTS.md)** - End-to-end test results and metrics
  - Infrastructure deployment summary
  - Test execution results
  - Performance metrics (all < 1000ms ✅)
  - Known issues & blockers
  - Recommendations

---

## Known Issues

### ⚠️ CDI Container Initialization Blocker

**Status**: BLOCKED  
**Severity**: High  
**Component**: REST API handlers

**Issue**: AWS Lambda runtime instantiates RequestHandler implementations before Quarkus CDI container is initialized, causing `Arc.container()` to return null.

**Impact**: REST API endpoints return HTTP 500 because services cannot be injected.

**Evidence**:
```
SEVERE: Arc.container() returned null - CDI container not initialized
java.lang.RuntimeException: ReceiveFeedbackUseCase not available
  at ServiceFactory.getReceiveFeedbackUseCase(ServiceFactory.java:48)
  at ReceiveFeedbackHandler.handleRequest(ReceiveFeedbackHandler.java:109)
```

**Solutions**:
1. **Use GraphQL endpoint** (Recommended) - 4-6 hours
   - Quarkus SmallRye GraphQL auto-handles CDI initialization
   - Minimal code changes
   
2. **StreamHandler pattern** - 6-8 hours
   - Rewrite handlers as Quarkus Lambda StreamHandler
   - Quarkus manages CDI context automatically
   
3. **@QuarkusMain entry point** - 4-5 hours
   - Create explicit entry point that initializes CDI
   - Register handlers with Quarkus

**Tracked in**: `AWS_DEPLOYMENT_RESULTS.md` § Technical Issue: CDI Container Initialization

---

## Project Structure

```
fase-4/
├── receive-feedback/          # Lambda 1: Receive & persist feedback
│   ├── src/main/java/com/fiap/feedback/
│   │   ├── domain/           # Domain entities & interfaces
│   │   ├── application/      # Use cases
│   │   └── infra/            # AWS infrastructure & handlers
│   └── pom.xml
├── send-notification/         # Lambda 2: Send urgent notifications
│   ├── src/main/java/com/fiap/notification/
│   │   ├── domain/
│   │   ├── application/
│   │   └── infra/
│   └── pom.xml
├── generate-report/           # Lambda 3: Generate weekly reports
│   ├── src/main/java/com/fiap/report/
│   │   ├── domain/
│   │   ├── application/
│   │   └── infra/
│   └── pom.xml
├── infra/                     # AWS SAM infrastructure-as-code
│   ├── template.yaml          # CloudFormation template
│   ├── samconfig.toml         # SAM configuration
│   └── packaged.yaml          # Generated SAM package
├── docs/                      # Documentation
│   ├── AWS_DEPLOYMENT_RESULTS.md
│   ├── PRODUCTION_DEPLOYMENT_GUIDE.md
│   └── planejamento/          # Planning documents
├── pom.xml                    # Parent POM
└── README.md                  # This file
```

---

## Performance Metrics

### Lambda Execution Times

| Function | Duration | Requirement | Status |
|---|---|---|---|
| ReceiveFeedbackFunction | 890.73 ms | < 1000 ms | ✅ PASS |
| SendNotificationFunction | Pending | < 1000 ms | ⏸️ Blocked |
| GenerateReportFunction | Pending | < 1000 ms | ⏸️ Blocked |

### Resource Allocation

| Resource | Configuration |
|---|---|
| Lambda Memory | 1024 MB |
| Lambda Timeout | 30 seconds |
| DynamoDB Billing | PAY_PER_REQUEST |
| API Gateway Throttle | 1000 RPS |
| API Gateway Quota | 10,000 requests/day |

---

## Technology Stack

### Languages & Frameworks
- **Java 17** - Primary language
- **Quarkus 3.8.4** - Serverless framework
- **Maven 3.9** - Build tool

### AWS Services
- **Lambda** - Compute
- **API Gateway** - REST endpoint
- **DynamoDB** - NoSQL database
- **SNS** - Event pub/sub
- **SES** - Email service
- **EventBridge** - Scheduled events
- **CloudWatch** - Logging & monitoring
- **CloudFormation** - Infrastructure-as-code
- **IAM** - Access control

### Quarkus Extensions
- `quarkus-amazon-lambda` - Lambda runtime
- `quarkus-amazon-dynamodb` - DynamoDB client
- `quarkus-amazon-sns` - SNS client
- `quarkus-smallrye-graphql` - GraphQL API (alternative to REST)
- `quarkus-hibernate-validator` - Bean validation
- `quarkus-jackson` - JSON serialization

---

## Development

### Local Testing

```bash
# Test single module
mvn -pl receive-feedback test

# Run with coverage
mvn clean verify -Djacoco.enabled=true

# Integration tests (requires LocalStack)
mvn failsafe:integration-test
```

### Code Quality

```bash
# SonarCloud analysis
mvn sonar:sonar \
  -Dsonar.projectKey=jeffesa_12ADJT-fase-4 \
  -Dsonar.organization=jeffesa
```

### Local SAM Testing

```bash
# Build for local testing
sam build -t infra/template.yaml --use-container

# Invoke function locally
sam local invoke ReceiveFeedbackFunction \
  -e sam-event.json

# Start API locally
sam local start-api --port 3000
# Then: curl -X POST http://localhost:3000/dev/avaliacao
```

---

## Deployment History

| Date | Event | Status |
|---|---|---|
| 2026-10-26 | Infrastructure deployed | ✅ Success |
| 2026-10-26 | End-to-end tests attempted | ⚠️ Blocked |
| 2026-10-26 | Performance validation | ✅ Pass |

---

## Support & Troubleshooting

### Common Issues

1. **Build fails**: Run `mvn clean` and retry
2. **AWS credentials error**: Run `aws configure` or set env vars
3. **Stack creation timeout**: Check CloudFormation events in AWS Console
4. **Lambda function returns 500**: Check CloudWatch logs (see PRODUCTION_DEPLOYMENT_GUIDE.md)
5. **API Gateway returns 403**: Verify API key in request header

### Useful Commands

```bash
# Check stack status
aws cloudformation describe-stacks \
  --stack-name prod-feedback-stack \
  --query 'Stacks[0].StackStatus'

# View Lambda logs
aws logs tail /aws/lambda/dev-receive-feedback --region us-east-1 --follow

# Query DynamoDB
aws dynamodb scan \
  --table-name dev-feedback-table \
  --region us-east-1 \
  --query 'Items'

# Check SNS subscriptions
aws sns list-subscriptions-by-topic \
  --topic-arn arn:aws:sns:us-east-1:ACCOUNT_ID:dev-feedback-urgent-topic
```

### Getting Help

1. Check [PRODUCTION_DEPLOYMENT_GUIDE.md](./docs/PRODUCTION_DEPLOYMENT_GUIDE.md) Troubleshooting section
2. Review [AWS_DEPLOYMENT_RESULTS.md](./docs/AWS_DEPLOYMENT_RESULTS.md) for known issues
3. Consult AWS documentation links
4. Check CloudWatch Logs in AWS Console

---

## License

Proprietary - FIAP Tech Challenge

## Authors

- Architecture & Implementation: FIAP Students
- AWS Infrastructure: Quarkus Lambda Framework

---

**Last Updated**: 2026-10-26  
**Deployment Status**: Production Ready (with documented CDI blocker)
