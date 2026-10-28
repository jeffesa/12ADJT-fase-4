# API Documentation

## Overview

This API provides a serverless platform for managing feedback through GraphQL and HTTP REST endpoints. The system automatically calculates feedback urgency, routes high-priority feedback for notification, and generates weekly reports.

The platform is built on AWS Lambda, DynamoDB, SNS, and SES using Quarkus with GraphQL support.

---

## Endpoints and Base URLs

### Local Development (LocalStack)

- **GraphQL Endpoint**: `http://localhost:8080/graphql`
- **HTTP REST Endpoint**: `http://localhost:8080/feedback`
- **Quarkus Dev Mode**: `mvn quarkus:dev` (runs on port 8080)

### Production (AWS)

- **GraphQL Endpoint**: `https://[API-GATEWAY-URL]/graphql`
- **HTTP REST Endpoint**: `https://[API-GATEWAY-URL]/feedback`
- **Region**: Configurable via `AWS_REGION` environment variable (default: `us-east-1`)

---

## Authentication

### API Key Requirement

All requests must include the `x-api-key` header with a valid API key.

```
x-api-key: your-api-key-value
```

**Response on missing or invalid API key**:
- Status: `401 Unauthorized` or `403 Forbidden`
- Implementation: Enforced by API Gateway in production; for local development with LocalStack, the key is validated against configured values.

**Obtaining an API Key**:
1. In production: Generated via AWS API Gateway console or CloudFormation templates.
2. For local development: Use any value (LocalStack does not enforce validation in dev mode).

---

## GraphQL Schema

### Types

#### `Feedback`
Represents a single feedback entry with automatically calculated urgency.

```graphql
type Feedback {
  id: UUID!
  descricao: String!
  nota: Int!
  urgencia: String!
  dataCriacao: DateTime!
}
```

**Fields**:
- `id` (UUID): Unique identifier (generated server-side).
- `descricao` (String): Description of the feedback. Must not be blank.
- `nota` (Int): Rating from 0 to 10 (inclusive). Determines urgency.
- `urgencia` (String): Urgency level calculated from rating: `HIGH`, `MEDIUM`, or `LOW`.
- `dataCriacao` (DateTime): ISO 8601 timestamp when feedback was created.

---

### Mutations

#### `receiveFeedback`

Receives and persists a new feedback entry. Calculates urgency automatically and publishes to SNS for high-urgency feedback.

```graphql
type Mutation {
  receiveFeedback(input: ReceiveFeedbackInput!): Feedback!
}

input ReceiveFeedbackInput {
  descricao: String!
  nota: Int!
}
```

**Input Fields**:
- `descricao` (String): Feedback description. Constraints: not blank, trimmed on storage.
- `nota` (Int): Rating 0–10. Constraints: `@Min(0)`, `@Max(10)`.

**Returns**: A `Feedback` object with generated ID, urgency, and timestamp.

**Error Responses**:
- `400 Bad Request`: Validation failed (description blank or missing, rating out of range).
- `401/403 Unauthorized/Forbidden`: Invalid or missing `x-api-key` header.
- `500 Internal Server Error`: Unexpected server error (e.g., DynamoDB unavailable).

**Example Response** (201 Created):
```json
{
  "receiveFeedback": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "descricao": "The application crashed on startup",
    "nota": 2,
    "urgencia": "HIGH",
    "dataCriacao": "2026-10-22T10:30:45.123456"
  }
}
```

---

### Queries

#### `listarAvaliacoes`

Lists all feedback entries stored in the system, ordered by creation date.

```graphql
type Query {
  listarAvaliacoes: [Feedback!]!
}
```

**Returns**: A list of `Feedback` objects. Empty list if no feedbacks exist.

**Error Responses**:
- `401/403 Unauthorized/Forbidden`: Invalid or missing `x-api-key` header.
- `500 Internal Server Error`: Unexpected server error (e.g., DynamoDB unavailable).

**Example Response** (200 OK):
```json
{
  "listarAvaliacoes": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "descricao": "The application crashed on startup",
      "nota": 2,
      "urgencia": "HIGH",
      "dataCriacao": "2026-10-22T10:30:45.123456"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "descricao": "Good performance overall",
      "nota": 8,
      "urgencia": "LOW",
      "dataCriacao": "2026-10-22T11:15:22.654321"
    }
  ]
}
```

---

## Urgency Calculation

Urgency is automatically calculated when feedback is received based on the rating:

| Rating Range | Urgency Level | Notes |
|---|---|---|
| 0–5 | HIGH | Critical/very negative feedback requires immediate attention |
| 6–7 | MEDIUM | Moderate feedback with room for improvement |
| 8–10 | LOW | Positive feedback with minor concerns (if any) |

**Implementation**: The `Urgency.fromRating(int rating)` method in the domain layer performs this calculation. HIGH urgency feedback is automatically published to an SNS topic for notification processing.

---

## Worked Examples

### Example 1: Create a HIGH Urgency Feedback (via GraphQL)

**Request**:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "x-api-key: test-key" \
  -d '{
    "query": "mutation { receiveFeedback(input: { descricao: \"App crashes on login\", nota: 3 }) { id descricao nota urgencia dataCriacao } }"
  }'
```

**GraphQL Body** (raw):
```graphql
mutation {
  receiveFeedback(
    input: {
      descricao: "App crashes on login"
      nota: 3
    }
  ) {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
```

**Response** (201 Created):
```json
{
  "data": {
    "receiveFeedback": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "descricao": "App crashes on login",
      "nota": 3,
      "urgencia": "HIGH",
      "dataCriacao": "2026-10-22T10:30:00"
    }
  }
}
```

---

### Example 2: Create a MEDIUM Urgency Feedback (via GraphQL)

**Request**:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "x-api-key: test-key" \
  -d '{
    "query": "mutation { receiveFeedback(input: { descricao: \"UI could be more intuitive\", nota: 6 }) { id descricao nota urgencia dataCriacao } }"
  }'
```

**GraphQL Body** (raw):
```graphql
mutation {
  receiveFeedback(
    input: {
      descricao: "UI could be more intuitive"
      nota: 6
    }
  ) {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
```

**Response** (201 Created):
```json
{
  "data": {
    "receiveFeedback": {
      "id": "123e4567-e89b-12d3-a456-426614174001",
      "descricao": "UI could be more intuitive",
      "nota": 6,
      "urgencia": "MEDIUM",
      "dataCriacao": "2026-10-22T10:31:00"
    }
  }
}
```

---

### Example 3: Create a LOW Urgency Feedback (via GraphQL)

**Request**:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "x-api-key: test-key" \
  -d '{
    "query": "mutation { receiveFeedback(input: { descricao: \"Excellent support team\", nota: 9 }) { id descricao nota urgencia dataCriacao } }"
  }'
```

**GraphQL Body** (raw):
```graphql
mutation {
  receiveFeedback(
    input: {
      descricao: "Excellent support team"
      nota: 9
    }
  ) {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
```

**Response** (201 Created):
```json
{
  "data": {
    "receiveFeedback": {
      "id": "123e4567-e89b-12d3-a456-426614174002",
      "descricao": "Excellent support team",
      "nota": 9,
      "urgencia": "LOW",
      "dataCriacao": "2026-10-22T10:32:00"
    }
  }
}
```

---

### Example 4: List All Feedbacks (via GraphQL)

**Request**:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "x-api-key: test-key" \
  -d '{
    "query": "query { listarAvaliacoes { id descricao nota urgencia dataCriacao } }"
  }'
```

**GraphQL Body** (raw):
```graphql
query {
  listarAvaliacoes {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
```

**Response** (200 OK):
```json
{
  "data": {
    "listarAvaliacoes": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "descricao": "App crashes on login",
        "nota": 3,
        "urgencia": "HIGH",
        "dataCriacao": "2026-10-22T10:30:00"
      },
      {
        "id": "123e4567-e89b-12d3-a456-426614174001",
        "descricao": "UI could be more intuitive",
        "nota": 6,
        "urgencia": "MEDIUM",
        "dataCriacao": "2026-10-22T10:31:00"
      },
      {
        "id": "123e4567-e89b-12d3-a456-426614174002",
        "descricao": "Excellent support team",
        "nota": 9,
        "urgencia": "LOW",
        "dataCriacao": "2026-10-22T10:32:00"
      }
    ]
  }
}
```

---

### Example 5: Create Feedback via HTTP REST (Lambda Handler)

The same feedback can be sent via HTTP POST to the Lambda function through API Gateway.

**Request**:
```bash
curl -X POST http://localhost:8080/feedback \
  -H "Content-Type: application/json" \
  -H "x-api-key: test-key" \
  -d '{
    "descricao": "Fast response times",
    "nota": 8
  }'
```

**Response** (201 Created):
```json
{
  "receiveFeedback": {
    "id": "123e4567-e89b-12d3-a456-426614174003",
    "descricao": "Fast response times",
    "nota": 8,
    "urgencia": "LOW",
    "dataCriacao": "2026-10-22T10:33:00"
  }
}
```

---

## Error Handling

### Validation Errors

**Scenario**: Invalid input (missing field, out-of-range value).

**HTTP Status**: `400 Bad Request`

**Response Format** (RFC 7807 Problem Detail):
```json
{
  "title": "Validation Error",
  "status": 400,
  "detail": "Rating must be between 0 and 10, received: 15"
}
```

**Common Validation Errors**:
- Missing `descricao`: "Description cannot be empty"
- Missing or null `nota`: "Rating is required"
- `nota < 0`: "Rating must be at least 0"
- `nota > 10`: "Rating must be at most 10"
- `descricao` is blank: "Description cannot be empty"

---

### Missing or Invalid API Key

**Scenario**: Request without `x-api-key` header or with an invalid key.

**HTTP Status**: `401 Unauthorized` or `403 Forbidden`

**Response** (API Gateway):
```json
{
  "message": "Unauthorized"
}
```

**Solution**:
1. Add the `x-api-key` header to all requests.
2. Verify the key is valid with your administrator.
3. Ensure the header is spelled correctly (case-sensitive: `x-api-key`).

---

### Malformed GraphQL

**Scenario**: Invalid GraphQL syntax or query structure.

**HTTP Status**: `400 Bad Request`

**Response**:
```json
{
  "errors": [
    {
      "message": "Syntax error in query",
      "locations": [{"line": 1, "column": 10}]
    }
  ]
}
```

**Common Issues**:
- Missing closing braces or quotes in the query.
- Typo in mutation/query name (e.g., `recieveFeedback` instead of `receiveFeedback`).
- Using a field that does not exist in the type (e.g., `rating` instead of `nota`).

---

### DynamoDB Unavailable

**Scenario**: Database is offline or unreachable.

**HTTP Status**: `500 Internal Server Error`

**Response**:
```json
{
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred. Please try again later."
}
```

**Troubleshooting**:
1. Check LocalStack is running: `docker ps | grep localstack`
2. Verify DynamoDB table exists: `aws --endpoint-url=http://localhost:4566 dynamodb list-tables`
3. Check application logs for detailed error: `mvn quarkus:dev` output.

---

### SNS Unavailable (HIGH Urgency Publishing)

**Scenario**: SNS topic is offline or the Lambda lacks permission to publish.

**Impact**: Feedback is still persisted to DynamoDB, but notifications will not be sent.

**HTTP Status**: Request succeeds (`201`), but notification delivery fails asynchronously.

**Troubleshooting**:
1. Verify SNS topic exists: `aws --endpoint-url=http://localhost:4566 sns list-topics`
2. Check feedback topic ARN is correctly set: `echo $FEEDBACK_TOPIC_ARN`
3. Check Lambda IAM role has `sns:Publish` permission.

---

## Data Models

### Feedback Storage (DynamoDB)

Feedback is stored in a DynamoDB table named `feedbacks` with the following schema:

| Attribute | Type | Description |
|---|---|---|
| `id` | String (PK) | Unique identifier (UUID format) |
| `description` | String | Feedback text |
| `rating` | Number | Rating 0–10 |
| `urgency` | String | HIGH, MEDIUM, or LOW |
| `createdAt` | String | ISO 8601 timestamp |
| `notificationSent` | Boolean | Whether notification was sent (default: false) |

---

### SNS Message Format (HIGH Urgency)

When feedback with urgency HIGH is received, it is published to SNS with the following JSON structure:

```json
{
  "feedbackId": "123e4567-e89b-12d3-a456-426614174000",
  "description": "App crashes on login",
  "rating": 3,
  "urgency": "HIGH",
  "createdAt": "2026-10-22T10:30:00"
}
```

This message triggers the `send-notification` Lambda for email delivery via SES.

---

### Weekly Report (Email)

The `generate-report` Lambda runs every Monday at 08:00 UTC and generates a JSON report of the previous week's feedback:

```json
{
  "reportPeriod": {
    "startDate": "2026-10-19",
    "endDate": "2026-10-23"
  },
  "summary": {
    "totalFeedback": 42,
    "highUrgency": 5,
    "mediumUrgency": 12,
    "lowUrgency": 25,
    "averageRating": 7.1
  },
  "feedbacks": [
    {
      "id": "...",
      "description": "...",
      "rating": "...",
      "urgency": "...",
      "createdAt": "..."
    }
  ]
}
```

---

## Local Development Setup

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker (for LocalStack)

### Start LocalStack

```bash
docker-compose up -d
```

This starts:
- LocalStack (DynamoDB, SNS, SES on port 4566)
- Quarkus in dev mode (GraphQL on port 8080)

### Initialize DynamoDB Table

```bash
./run.sh
```

Or manually:

```bash
aws --endpoint-url=http://localhost:4566 dynamodb create-table \
  --table-name feedbacks \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### Run Quarkus in Dev Mode

```bash
mvn quarkus:dev
```

GraphQL UI is available at: `http://localhost:8080/q/graphql-ui`

### Test with curl

Create feedback:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "x-api-key: test" \
  -d '{"query": "mutation { receiveFeedback(input: { descricao: \"Test\", nota: 5 }) { id urgencia } }"}'
```

---

## Production Deployment (AWS)

### Prerequisites

- AWS account with appropriate permissions
- SAM CLI or CloudFormation for infrastructure deployment
- API Gateway configured with API key validation

### Environment Variables

Set these variables in your Lambda environment:

- `AWS_REGION`: AWS region (default: `us-east-1`)
- `FEEDBACK_TOPIC_ARN`: SNS topic ARN for HIGH urgency feedback
- `ADMIN_EMAIL`: Email address for report delivery
- `DYNAMODB_TABLE_NAME`: Table name (default: `feedbacks`)

### Deploy

```bash
sam deploy --guided
```

### Verify

```bash
curl -X POST https://[API-GATEWAY-URL]/graphql \
  -H "Content-Type: application/json" \
  -H "x-api-key: [YOUR-API-KEY]" \
  -d '{"query": "query { listarAvaliacoes { id } }"}'
```

---

## Rate Limiting and Quotas

- **API Gateway**: Default 10,000 requests per second per account.
- **DynamoDB**: On-demand billing (provisioned throughput not required).
- **SNS**: Standard throughput per AWS quotas.
- **SES**: Default sandbox mode (100 emails/day). Request production access for higher limits.

---

## Support and Troubleshooting

### Common Issues

1. **"x-api-key header missing"**: Add header to all requests.
2. **"Rating must be between 0 and 10"**: Ensure `nota` is an integer in range.
3. **"DynamoDB is unavailable"**: Start LocalStack and verify table exists.
4. **"GraphQL endpoint returns 404"**: Verify Quarkus is running and the path is `/graphql`.

### Enable Debug Logging

Set in `application.properties`:

```properties
quarkus.log.level=DEBUG
quarkus.log.category."com.fiap".level=DEBUG
```

Then check logs:

```bash
mvn quarkus:dev 2>&1 | grep -i "com.fiap"
```

---

## Summary

This API provides a robust, serverless feedback management platform with automatic urgency calculation, notification routing, and reporting. All operations are RESTful and GraphQL-enabled, with strong validation and error handling. The system is designed for scalability on AWS and easy local development with LocalStack.

For questions or issues, refer to the project README or contact the development team.
