# Fase-4: Serverless Feedback Platform

A production-grade serverless feedback platform built on AWS Lambda, DynamoDB, SNS, and EventBridge. This platform demonstrates Clean Architecture principles with a modern Java stack, providing high throughput and low latency feedback collection and processing.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       API GATEWAY                               │
│              (GraphQL endpoint with API Key auth)               │
└────────┬────────────────────────────────────────────────────────┘
         │
         │ GraphQL Mutation/Query
         │
    ┌────▼──────────────────────────────────────────────────────┐
    │    LAMBDA 1: Receive Feedback                             │
    │  - Validates input (description, rating 0-10)            │
    │  - Persists feedback to DynamoDB                         │
    │  - Publishes HIGH urgency to SNS topic                  │
    └────┬──────────────────────────┬──────────────────────────┘
         │                          │
         │                   ┌──────▼──────┐
         │                   │ SNS TOPIC   │
         │                   │ (High Urgency)
         │                   └──────┬──────┘
         │                          │
    ┌────▼─────────────────┐   ┌───▼──────────────────────────┐
    │  DYNAMODB            │   │ LAMBDA 2: Send Notification  │
    │  (Feedbacks Table)   │   │  - Consumes HIGH priority   │
    │  - Stores feedback   │   │  - Sends email via SES      │
    │  - Query: list all   │   │  - Publishes to EventBridge │
    │  - TTL: 90 days      │   └───┬──────────────────────────┘
    └────┬──────────────────┘       │
         │                          │
         │                   ┌──────▼──────────────┐
         │                   │  EVENTBRIDGE        │
         │                   │ (Event routing)     │
         │                   └──────┬──────────────┘
         │                          │
         │                   ┌──────▼──────────────────────────┐
         │                   │ LAMBDA 3: Generate Report       │
         │                   │  - Consumes events              │
         │                   │  - Aggregates metrics           │
         │                   │  - Sends summary reports        │
         │                   └─────────────────────────────────┘
         │
         └──────────────────────────────────────┐
                                                │
                                    ┌──────────▼────┐
                                    │  GraphQL Query │
                                    │  listarAvaliacoes
                                    └────────────────┘
```

## Badges

[![Build Status](https://github.com/jeffesa/12ADJT-fase-4/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/jeffesa/12ADJT-fase-4/actions)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jeffesa_12ADJT-fase-4&metric=coverage)](https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=jeffesa_12ADJT-fase-4&metric=alert_status)](https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/jeffesa/12ADJT-fase-4.git
cd fase-4
```

### 2. Build the Project

```bash
mvn clean package -DskipTests
```

### 3. Run Tests

```bash
mvn test
```

Tests execute automatically during the build phase. Coverage reports are generated in `target/site/jacoco/jacoco.html`.

## Local Setup

### Prerequisites

- Java 17 or later
- Maven 3.8+
- Docker and Docker Compose
- AWS CLI (optional, for manual LocalStack testing)

### Start LocalStack

LocalStack emulates AWS services locally (DynamoDB, SNS, Lambda, API Gateway, EventBridge, SES).

```bash
docker-compose up -d
```

This command:
- Starts LocalStack container with all required services
- Creates DynamoDB table `dev-feedback-table`
- Sets up SNS topic `dev-feedback-urgent-topic`
- Configures SQS queues for event processing
- Initializes EventBridge rules

**Health check:**

```bash
docker-compose ps
```

When LocalStack is ready, you should see the localstack container with status "Up".

### Build and Run Locally

After LocalStack is running:

```bash
mvn clean package -DskipTests
```

Then, test individual Lambdas with SAM CLI:

```bash
# Test receiveFeedback mutation
sam local invoke ReceiveFeedbackFunction --event infra/events/sample-feedback.json

# Test listarAvaliacoes query
sam local invoke ReceiveFeedbackFunction --event infra/events/sample-query.json
```

For interactive local development with a mock API Gateway:

```bash
sam local start-api --port 3000
```

GraphQL endpoint becomes available at `http://localhost:3000/graphql`.

## AWS Deploy

### Prerequisites for Deployment

- AWS credentials configured (run `aws configure` or use environment variables)
- S3 bucket for SAM artifacts (adjust `s3_bucket` in `samconfig.toml`)
- For production: SES verified email address for notifications

### Configure Deployment Parameters

Edit `infra/samconfig.toml`:

```toml
[default]
[default.deploy.parameters]
stack_name = "feedback-api-dev"
s3_bucket = "sam-artifacts-us-east-1-dev"

[default.deploy.parameters.Parameter]
Environment = "dev"
AdminEmail = "your-email@example.com"
Region = "us-east-1"
```

### Deploy to AWS

**Dev environment:**

```bash
sam build -t infra/template.yaml
sam deploy --config-env default
```

**Prod environment (with secrets from GitHub Actions):**

During CI/CD, the pipeline reads `ADMIN_EMAIL` from GitHub Secrets and deploys:

```bash
sam deploy \
  --template-file infra/.aws-sam/build/template.yaml \
  --stack-name feedback-api-prod \
  --parameter-overrides Environment=prod AdminEmail=$ADMIN_EMAIL \
  --s3-bucket sam-artifacts-us-east-1 \
  --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM
```

### Outputs

After deployment, CloudFormation outputs include:

```yaml
APIEndpoint: https://iq0ce3my5m.execute-api.us-east-1.amazonaws.com/v1/avaliacao
GraphQLSchema: https://iq0ce3my5m.execute-api.us-east-1.amazonaws.com/v1/avaliacao/schema.graphql
```

## GraphQL API

The platform exposes a **GraphQL-only API** (no REST endpoints). All operations use the GraphQL mutation/query pattern.

### Endpoint

```
https://iq0ce3my5m.execute-api.us-east-1.amazonaws.com/v1/avaliacao
```

### Authentication

All requests require an API Key header. The key is stored in GitHub Secrets and injected at deployment time.

```
X-API-Key: $API_KEY
```

In local testing or examples, replace `$API_KEY` with the actual key from your environment.

### Mutation: criarAvaliacao

Create a new feedback entry with description and rating.

**GraphQL Mutation:**

```graphql
mutation CriarFeedback($descricao: String!, $nota: Int!) {
  receiveFeedback(input: { descricao: $descricao, nota: $nota }) {
    id
    descricao
    nota
    createdAt
    urgency
  }
}
```

**Variables:**

```json
{
  "descricao": "The application is user-friendly and responsive.",
  "nota": 9
}
```

**Example curl request:**

```bash
curl -X POST https://iq0ce3my5m.execute-api.us-east-1.amazonaws.com/v1/avaliacao \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "query": "mutation CriarFeedback($descricao: String!, $nota: Int!) { receiveFeedback(input: { descricao: $descricao, nota: $nota }) { id descricao nota createdAt urgency } }",
    "variables": {
      "descricao": "The application is user-friendly and responsive.",
      "nota": 9
    }
  }'
```

**Response (success):**

```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "descricao": "The application is user-friendly and responsive.",
      "nota": 9,
      "createdAt": "2024-10-26T15:30:45Z",
      "urgency": "MEDIUM"
    }
  }
}
```

**Response (validation error):**

```json
{
  "errors": [
    {
      "message": "Validation constraint violation",
      "extensions": {
        "constraintViolations": [
          {
            "fieldName": "nota",
            "message": "Rating must be between 0 and 10"
          }
        ]
      }
    }
  ]
}
```

---

### Query: listarAvaliacoes

Retrieve all stored feedback entries.

**GraphQL Query:**

```graphql
query ListarAvaliacoes {
  listarAvaliacoes {
    id
    descricao
    nota
    createdAt
    urgency
  }
}
```

**Example curl request:**

```bash
curl -X POST https://iq0ce3my5m.execute-api.us-east-1.amazonaws.com/v1/avaliacao \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "query": "query ListarAvaliacoes { listarAvaliacoes { id descricao nota createdAt urgency } }"
  }'
```

**Response (success):**

```json
{
  "data": {
    "listarAvaliacoes": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "descricao": "The application is user-friendly and responsive.",
        "nota": 9,
        "createdAt": "2024-10-26T15:30:45Z",
        "urgency": "MEDIUM"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "descricao": "Performance needs improvement during peak hours.",
        "nota": 5,
        "createdAt": "2024-10-26T14:15:20Z",
        "urgency": "HIGH"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "descricao": "Great support team!",
        "nota": 10,
        "createdAt": "2024-10-26T13:45:00Z",
        "urgency": "LOW"
      }
    ]
  }
}
```

### Testing with Bruno or Postman

Create a new request:

1. **Method:** POST
2. **URL:** `https://iq0ce3my5m.execute-api.us-east-1.amazonaws.com/v1/avaliacao`
3. **Headers:**
   - `Content-Type: application/json`
   - `X-API-Key: $API_KEY`
4. **Body:** Use the JSON examples above

### GraphQL Schema Introspection

Fetch the full schema:

```bash
curl -X POST https://iq0ce3my5m.execute-api.us-east-1.amazonaws.com/v1/avaliacao \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{ "query": "{ __schema { types { name } } }" }'
```

## Project Structure

```
fase-4/
├── README.md                      # This file
├── pom.xml                        # Parent Maven POM (multi-module)
├── sonar-project.properties       # SonarCloud configuration
│
├── receive-feedback/              # Lambda 1: Receives and persists feedback
│   ├── pom.xml
│   ├── GRAPHQL_TEST_GUIDE.md
│   └── src/
│       ├── main/java/com/fiap/feedback/
│       │   ├── domain/            # Clean Architecture: Domain layer
│       │   │   ├── entity/        # Business entities (Feedback, Urgency)
│       │   │   └── gateway/       # Gateway interfaces
│       │   ├── application/       # Application layer
│       │   │   ├── usecase/       # Use cases (ReceiveFeedbackUseCase)
│       │   │   └── dto/           # Application DTOs
│       │   └── infra/             # Infrastructure layer
│       │       ├── graphql/       # GraphQL resolvers
│       │       ├── persistence/   # DynamoDB implementation
│       │       ├── messaging/     # SNS implementation
│       │       └── dto/           # Input/Output DTOs
│       └── test/java/             # Unit tests (23 test files, 146 tests)
│
├── send-notification/             # Lambda 2: Sends notifications for HIGH urgency
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fiap/notification/
│       │   ├── domain/
│       │   ├── application/
│       │   └── infra/
│       └── test/java/
│
├── generate-report/               # Lambda 3: Generates aggregated reports
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fiap/report/
│       │   ├── domain/
│       │   ├── application/
│       │   └── infra/
│       └── test/java/
│
├── infra/                         # AWS infrastructure (SAM)
│   ├── template.yaml              # SAM template (Lambdas, DynamoDB, SNS, EventBridge)
│   ├── samconfig.toml             # SAM deployment configuration
│   └── localstack-init.sh         # LocalStack initialization script
│
├── docs/                          # Documentation
│   ├── planejamento/              # Planning and backlog
│   │   ├── BACKLOG.md
│   │   └── HANDOFF_SESSION.md
│   ├── qualidade/                 # Quality and testing documentation
│   ├── deploy/                    # Deployment guides
│   ├── api-collection/            # GraphQL/REST request collections
│   └── ci-cd/                     # CI/CD pipeline documentation
│
├── docker-compose.yml             # LocalStack (local AWS emulation)
├── e2e-test.sh                    # End-to-end testing script
├── e2e-verify.sh                  # E2E verification script
└── .github/
    └── workflows/
        ├── ci-cd.yml              # Build, test, and SonarCloud analysis
        ├── branch-naming.yml      # Branch naming validation
        └── auto-pr-to-main.yml    # Auto PR to main after develop merge
```

## Technologies

| Technology | Version | Justification |
|---|---|---|
| **Java** | 17 | LTS version; strong performance and feature stability for enterprise workloads. |
| **Quarkus** | 3.8.x | Optimized for AWS Lambda; reduced memory footprint, faster startup times, native compilation support. |
| **Maven** | 3.8+ | Industry standard build tool; excellent multi-module project support. |
| **DynamoDB** | AWS managed | Serverless, pay-per-request pricing, scales automatically, global replication support. |
| **SNS** | AWS managed | Event-driven notifications; decouples feedback processing from email delivery. |
| **EventBridge** | AWS managed | Flexible event routing; enables complex workflows between services. |
| **SES** | AWS managed | Cost-effective email delivery; integrates with SNS topics. |
| **Lambda** | Java 17 runtime | Serverless compute; charges only for execution time; native integration with AWS services. |
| **GraphQL** | SmallRye 2.x | Type-safe API design; single endpoint; strong query language; ideal for serverless backends. |
| **LocalStack** | 3.0 | Local AWS emulation; enables offline development and testing. |
| **JaCoCo** | 0.8.11 | Code coverage measurement; integrates with SonarCloud. |
| **SonarCloud** | Latest | Continuous code quality; coverage gate enforcement; OWASP security scanning. |

## Tests and Coverage

### Running Tests

**Run all tests:**

```bash
mvn test
```

**Run tests for a specific module:**

```bash
mvn test -pl receive-feedback
mvn test -pl send-notification
mvn test -pl generate-report
```

**Run tests with coverage report:**

```bash
mvn clean verify
```

Coverage reports are generated in `target/site/jacoco/jacoco.html` for each module.

### Coverage Targets

- **Overall threshold:** ≥80% (enforced by SonarCloud quality gate)
- **Current status:** 146 unit tests, 0 failures
- **Coverage breakdown:**
  - Domain layer: High coverage (business logic)
  - Application layer: High coverage (use cases)
  - Infrastructure layer: Selective coverage (excludes config, DTOs, exception handlers)

### Excluded from Coverage

- `**/config/**` — Configuration classes (environment setup)
- `**/dto/**` — Data Transfer Objects (simple mappers)
- `**/*Handler.java` — Lambda request handlers (runtime-only)
- `**/exception/**` — Custom exceptions (trivial implementations)

### Viewing Coverage Reports

After running tests:

```bash
# Open HTML report in browser
open target/site/jacoco/jacoco.html

# Or view per-module reports
open receive-feedback/target/site/jacoco/jacoco.html
open send-notification/target/site/jacoco/jacoco.html
open generate-report/target/site/jacoco/jacoco.html
```

## CI/CD and SonarCloud

### CI/CD Pipeline

Triggered on:
- Push to `main` or `develop` branch
- Pull request to `develop` branch
- Manual workflow dispatch

**Pipeline steps:**

1. **Checkout code** — Clone repository with full history (required for SonarCloud)
2. **Setup Java 17** — Maven build runtime
3. **Build and Test** — Execute `mvn clean verify`
4. **Generate coverage** — JaCoCo creates XML reports
5. **SonarCloud Scan** — Analyzes code quality and coverage
6. **Quality gate check** — Fails if coverage < 80% or critical issues detected

### SonarCloud Configuration

- **Organization:** jeffesa
- **Project Key:** jeffesa_12ADJT-fase-4
- **URL:** https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4

**Quality gate rules:**

- Coverage: ≥80%
- Critical issues: 0
- Blocker issues: 0

### Accessing SonarCloud

```bash
# View project dashboard
open https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4

# View code quality metrics
open https://sonarcloud.io/code?id=jeffesa_12ADJT-fase-4

# View security issues
open https://sonarcloud.io/security?id=jeffesa_12ADJT-fase-4
```

## FAQ

### How do I test the GraphQL API locally?

Use the `sam local start-api` command to run the GraphQL endpoint on your machine, then send requests via curl or Postman.

### What happens when I submit HIGH urgency feedback?

The `receive-feedback` Lambda persists the feedback to DynamoDB and publishes a message to the SNS topic. The `send-notification` Lambda subscribes to that topic, receives the event, and sends an email notification.

### Can I run the project without AWS credentials?

Yes. LocalStack emulates AWS services in Docker, so you can develop locally without AWS account access. Use the `docker-compose up -d` command to start LocalStack.

### How is code coverage measured?

JaCoCo instruments your code during test execution and generates XML reports. SonarCloud parses these reports and enforces a minimum coverage threshold (≥80%).

### What is the DynamoDB table structure?

The `dev-feedback-table` (or `prod-feedback-table`) has:
- **Partition Key:** `id` (String, UUID)
- **Attributes:** `descricao`, `nota`, `createdAt`, `urgency`, `ttl`
- **TTL:** 90 days (auto-deletion of old feedback)

### How do I deploy to production?

Update `infra/samconfig.toml` with production values, set environment variables (e.g., `ADMIN_EMAIL`), and run:

```bash
sam deploy --config-env prod
```

The CI/CD pipeline automates this process on push to `main`.

### Can I use REST endpoints instead of GraphQL?

No. This platform implements GraphQL only. All operations (mutation and query) use the GraphQL protocol via a single endpoint.

### How do I monitor Lambda performance?

View CloudWatch Logs and X-Ray traces in the AWS Console, or configure CloudWatch dashboards to visualize:
- Lambda invocation count
- Error rate and duration
- DynamoDB read/write capacity consumed
- SNS message count

## Useful Links

- **Repository:** https://github.com/jeffesa/12ADJT-fase-4
- **SonarCloud Dashboard:** https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4
- **AWS API Endpoint:** https://iq0ce3my5m.execute-api.us-east-1.amazonaws.com/v1/avaliacao
- **Quarkus Documentation:** https://quarkus.io
- **AWS Lambda Developer Guide:** https://docs.aws.amazon.com/lambda
- **GraphQL Official:** https://graphql.org
- **LocalStack Documentation:** https://docs.localstack.cloud
- **AWS SAM:** https://aws.amazon.com/serverless/sam/
- **DynamoDB Developer Guide:** https://docs.aws.amazon.com/dynamodb
- **SNS Developer Guide:** https://docs.aws.amazon.com/sns
- **EventBridge Documentation:** https://docs.aws.amazon.com/eventbridge

---

**Last updated:** October 26, 2024  
**Maintainer:** FIAP Tech Challenge Fase 4 Team  
**License:** MIT
