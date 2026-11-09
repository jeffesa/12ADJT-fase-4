# FIAP Fase-4 Feedback API — Postman Collection

## Overview

This Postman collection covers the Feedback API endpoints for FIAP Fase-4, with comprehensive test cases for creating feedback and validating urgency calculations based on ratings. The collection includes both local (LocalStack) and production (AWS API Gateway) environments, allowing easy switching between testing scenarios.

### API Coverage
- **POST `/avaliacao`** — Create feedback with automatic urgency classification
  - Success cases: 3 urgency tiers (HIGH, MEDIUM, LOW) with boundary value testing
  - Error cases: 9 validation and constraint violation scenarios

### Architecture
- **REST-based** (not GraphQL)
- **Request contract:** JSON with `descricao` (string) and `nota` (integer 0-10)
- **Response format:** RFC 7807 Problem Detail for errors; wrapped in `receiveFeedback` object for success
- **Status codes:** 201 Created (success), 400 Bad Request (validation errors)

---

## Import Instructions

### In Postman
1. Open Postman
2. Click **File** → **Import**
3. Select `/Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/fase4-feedback.postman_collection.json`
4. Collection appears in the left sidebar

### In Bruno
1. Open Bruno
2. Click **+ New** → **Collection**
3. Import the JSON file
4. Collection loads with all variables and requests

---

## Collection Variables

All variables are embedded in the collection (no separate environment file needed):

| Variable | Default Value | Purpose |
|----------|---------------|---------|
| `localUrl` | `http://localhost:4566` | LocalStack API Gateway base URL |
| `prodUrl` | `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod` | AWS API Gateway base URL |
| `feedbackId` | *(empty)* | Captured from successful create response |
| `timestamp` | *(auto-generated per request)* | Ensures unique feedback descriptions |

---

## Execution Workflow

The collection has two folders: **local** and **prod**. Each contains the same test cases but pointing to different endpoints.

### To test LOCAL (LocalStack):
1. Expand folder **"local"**
2. Run any request under **"Criar Avaliação"**
3. All requests will hit `{{localUrl}}/avaliacao` = `http://localhost:4566/avaliacao`

### To test PRODUCTION (AWS):
1. Expand folder **"prod"**
2. Run any request under **"Criar Avaliação"**
3. All requests will hit `{{prodUrl}}/avaliacao` = `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/avaliacao`

### Test Cases in "Criar Avaliação"
| Request | Rating | Urgency | Note |
|---------|--------|---------|------|
| nota 1 | 1 | HIGH | Captures feedback ID for future list queries |
| nota 6 | 6 | MEDIUM | Mid-range feedback |
| nota 9 | 9 | LOW | High satisfaction |
| nota 0 | 0 | HIGH | Boundary: minimum valid rating |
| nota 10 | 10 | LOW | Boundary: maximum valid rating |

### Test Cases in "Listar Avaliações"
| Request | Purpose | Response |
|---------|---------|----------|
| listarAvaliacoes | Query all feedbacks stored in DynamoDB | HTTP 200 with `data.listarAvaliacoes` array |

**Test Assertions:**
- ✅ Status code 200
- ✅ Response has `data.listarAvaliacoes` field
- ✅ Each feedback item has: `id`, `descricao`, `nota`, `urgencia`, `dataCriacao`
- ✅ `id` is a valid UUID
- ✅ `urgencia` is one of: HIGH, MEDIUM, LOW
- ✅ `dataCriacao` is ISO 8601 format
- ✅ Array is returned (may be empty)

#### Error Cases (HTTP 400 — RFC 7807 Problem Detail)
| Request | Issue | Error Field |
|---------|-------|------------|
| nota -1 | Rating below minimum | `detail` contains "must be at least 0" |
| nota 11 | Rating above maximum | `detail` contains "must be at most 10" |
| descrição vazia | Empty string | `detail` contains "cannot be empty" |
| descrição com espaços | Whitespace only | `detail` contains blank-check violation |
| nota ausente | Missing field | HTTP 400 — field required |
| descrição ausente | Missing field | HTTP 400 — field required |
| nota como string | Type mismatch | HTTP 400 — number expected |

---

## Urgency Mapping

The API calculates urgency based on the rating:

| Rating Range | Urgency | Business Meaning |
|--------------|---------|------------------|
| 0–5 | **HIGH** | Negative feedback requiring immediate attention |
| 6–7 | **MEDIUM** | Moderate feedback — standard review |
| 8–10 | **LOW** | Positive feedback — no action needed |

All tests verify this mapping by asserting the `urgencia` field matches the expected tier.

---

## Expected Test Results

### All Success Cases Pass
- HTTP 201 Created
- Response contains `receiveFeedback` wrapper object
- Fields: `id` (UUID), `descricao` (echoed), `nota` (echoed), `urgencia` (calculated), `dataCriacao` (ISO 8601)
- Timestamp in description is unique (no duplicates in DynamoDB)

### All Error Cases Return HTTP 400
- Response format: RFC 7807 Problem Detail
- Fields: `title` (e.g., "Validation Error"), `status` (400), `detail` (specific violation)
- **Not** a GraphQL `errors` array

### Example Success Response
```json
{
  "receiveFeedback": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "descricao": "Feedback crítico - teste 1699523045123",
    "nota": 1,
    "urgencia": "HIGH",
    "dataCriacao": "2024-11-08T18:50:45.123Z"
  }
}
```

### Example Error Response
```json
{
  "title": "Validation Error",
  "status": 400,
  "detail": "Rating must be at least 0"
}
```

---

## GraphQL Query: listarAvaliacoes

Retrieve all feedbacks stored in DynamoDB.

### Request
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

### Success Response (200 OK)
```json
{
  "data": {
    "listarAvaliacoes": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "descricao": "Feedback teste 1",
        "nota": 1,
        "urgencia": "HIGH",
        "dataCriacao": "2024-11-08T18:50:45.123Z"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "descricao": "Feedback teste 2",
        "nota": 8,
        "urgencia": "LOW",
        "dataCriacao": "2024-11-08T19:22:30.456Z"
      }
    ]
  }
}
```

---

## Running in Local Environment

### Prerequisites
1. LocalStack running: `docker-compose up -d`
2. Application built and deployed to LocalStack: `mvn clean package`
3. Lambda function accessible at `http://localhost:4566/avaliacao`

### Steps
1. Run "Selecionar ambiente local"
2. Run "Criar Avaliação — nota 1 (urgência HIGH) ✅ 201"
3. Verify timestamp in description is captured
4. Run remaining test cases in any order
5. Check CloudWatch logs in LocalStack for Lambda execution details

---

## Running in Production (AWS)

### Prerequisites
1. AWS Lambda function deployed and accessible
2. API Gateway endpoint active: `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/avaliacao`
3. DynamoDB table `feedback` available in us-east-1
4. SES verified (if email notifications enabled)

### Steps
1. Run "Selecionar ambiente prod"
2. Run test cases (same structure as local)
3. Monitor AWS CloudWatch Logs for Lambda execution
4. Verify DynamoDB items created in the console

### Known Limitations
- **No API Key required** (removed from template for public access)
- **List endpoint not yet deployed** — `listarAvaliacoes` query is a future task
- **Email notifications** — Currently in development; test with SES sandbox mode

---

## API Contract Summary

### Endpoint
```
POST /avaliacao
Host: {{baseUrl}}
Content-Type: application/json
```

### Request Body
```json
{
  "descricao": "string (required, non-blank)",
  "nota": "integer (required, 0-10 inclusive)"
}
```

### Success Response (201 Created)
```json
{
  "receiveFeedback": {
    "id": "UUID",
    "descricao": "string (echoed from request)",
    "nota": "integer (echoed from request)",
    "urgencia": "HIGH|MEDIUM|LOW (calculated)",
    "dataCriacao": "ISO 8601 timestamp"
  }
}
```

### Error Response (400 Bad Request)
```json
{
  "title": "Validation Error|Constraint Violation|etc",
  "status": 400,
  "detail": "Specific error message explaining the violation"
}
```

---

## Troubleshooting

### Troubleshooting

#### 404 Not Found on {{localUrl}}/avaliacao
- **Cause:** LocalStack/Lambda not running or deployed
- **Fix:** Verify `docker-compose up -d` and Lambda function deployed

### 500 Internal Server Error
- **Cause:** Lambda handler initialization failed
- **Fix:** Check CloudWatch logs for Arc CDI bootstrap errors; confirm Quarkus handler configured

### Test Assertion Failures
- **Cause:** Response structure doesn't match (e.g., missing `receiveFeedback` wrapper)
- **Fix:** Verify API updated to REST response format (not GraphQL); check Lambda logs

### Duplicate Description Error (if persisting to external store)
- **Cause:** Same test run multiple times with fixed descriptions
- **Fix:** Use `{{timestamp}}` in all descriptions; it auto-generates per request

---

## Import Verification

After importing, verify the collection loads correctly:

1. **Check variables:** Collection dropdown → "Manage variables" shows all 5 variables
2. **Check requests:** Local folder should have 2 items (selector + Criar Avaliação)
3. **Run environment selector:** Click "Selecionar ambiente local" → test passes, `baseUrl` now set
4. **Run one success case:** Click "Criar Avaliação — nota 1 (urgência HIGH)" → HTTP 201, sees `feedbackId`

---

## Known Issues & Future Work

### Not Yet Implemented
- **Listar Avaliações endpoint:** Query to retrieve feedback by ID or filter
- **API Key authentication:** Currently not enforced (template removed x-api-key)
- **Email notifications:** Infrastructure present but not yet wired in Quarkus handler

### Deprecated (from Fase-3)
- **GraphQL endpoint:** Collection no longer tests GraphQL mutations; API is REST-only
- **x-api-key header:** Removed from all requests (no API Key required in current template)

---

## Support & Feedback

For issues with the collection:
1. Verify the API contract in `/Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/plan.md`
2. Check source code in `/Users/jefferson/Documents/FIAP/fase-4/receive-feedback/src/main/java/com/fiap/feedback/infra/handler/ReceiveFeedbackHandler.java`
3. Review Lambda logs in AWS CloudWatch for actual error details
4. Confirm LocalStack is running if testing locally: `docker ps | grep localstack`
