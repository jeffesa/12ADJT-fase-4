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
3. Select `docs/api-collection/fase4-feedback.postman_collection.json` (only the collection; there are no environment files)
4. Collection appears in the left sidebar

### In Bruno
1. Open Bruno
2. Click **+ New** → **Collection**
3. Import the JSON file
4. Collection loads with the `local` and `prod` folders
5. Open **Collection > Variables** and set `prodToken` (see below)

---

## Collection Variables

All variables are embedded in the collection (no separate environment file needed). Every request sends `Authorization: Bearer <token>` using `localToken` (folder `local`) or `prodToken` (folder `prod`), except the requests named "sem token", which test the 401 response.

| Variable | Default Value | Purpose |
|----------|---------------|---------|
| `localUrl` | `http://localhost:8080` | local-http base URL |
| `prodUrl` | `https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod` | AWS API Gateway base URL |
| `localToken` | `minha-chave-local` | Local token (not a secret; matches the `./run.sh local-up` default) |
| `prodToken` | *(empty)* | Production token: paste the value of the `API_TOKEN` secret on your machine only; never commit it |
| `feedbackId` | *(empty)* | Captured from successful create response |
| `timestamp` | *(auto-generated per request)* | Ensures unique feedback descriptions |

---

## Execution Workflow

The collection has two folders: **local** and **prod**. Each contains the same test cases but pointing to different endpoints.

### To test LOCAL (local-http):
1. Expand folder **"local"**
2. Run any request under **"Criar Avaliação"**
3. All requests will hit `{{localUrl}}/avaliacao` = `http://localhost:8080/avaliacao`

### To test PRODUCTION (AWS):
1. Expand folder **"prod"**
2. Run any request under **"Criar Avaliação"**
3. All requests will hit `{{prodUrl}}/avaliacao` = `https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod/avaliacao`

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
1. AWS credentials with profile `fase4` (the local module writes to `dev-feedback-table`)
2. Start the module: in the `local-http` folder, run `./run.sh local-up`
3. Routes available at `http://localhost:8080` (`/avaliacao` and `/graphql`)

### Steps
1. Keep the default `localToken` (or export `API_TOKEN` before `./run.sh local-up` and set the same value in `localToken`)
2. Run "✅ 201 — nota 1 (urgência HIGH)" (a note from 0 to 5 triggers SNS/email)
3. Run the remaining test cases in any order, including the 401 requests ("sem token" e "token inválido")

---

## Running in Production (AWS)

### Prerequisites
1. AWS Lambda function deployed and accessible
2. API Gateway endpoint active: `https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod/avaliacao`
3. DynamoDB table `prod-feedback-table` available in sa-east-1
4. SES verified (if email notifications enabled)
5. `prodToken` set to the value of the `API_TOKEN` secret

### Steps
1. Set `prodToken` (Collection > Variables)
2. Run test cases (same structure as local). Requests with a note from 0 to 5 create real records and send SNS/email in production
3. Monitor AWS CloudWatch Logs for Lambda execution
4. Verify DynamoDB items created in the console

### Known Limitations
- **Bearer token required** on every route (`Authorization: Bearer <token>`); to create it, see the secret `API_TOKEN` in `docs/deploy/AWS_DEPLOY_GUIDE.md`
- **Email notifications** — Currently in development; test with SES sandbox mode

---

## API Contract Summary

### Endpoint
```
POST /avaliacao
Host: {{localUrl}} or {{prodUrl}}
Authorization: Bearer <token>
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

### Error Response (401 Unauthorized)
```json
{
  "title": "Unauthorized",
  "status": 401,
  "detail": "Missing bearer token | Invalid bearer token"
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

#### 401 Unauthorized
- **Cause:** `localToken`/`prodToken` empty or different from the token configured in the API (`API_TOKEN`)
- **Fix:** Set the variable in Collection > Variables; locally, use the same value passed to `./run.sh local-up`

#### 404/405 on {{localUrl}}/avaliacao
- **Cause:** local-http not running, or started from the wrong folder (the `receive-feedback` module has no HTTP routes)
- **Fix:** Run `./run.sh local-up` in the `local-http` folder and keep it running

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

1. **Check variables:** Collection > Variables shows `localUrl`, `prodUrl`, `localToken`, `prodToken`, `feedbackId` and `timestamp`
2. **Check folders:** only `local` and `prod`, with 20 requests each
3. **Run one 401 case:** "🔒 401 — GET /avaliacao sem token" → HTTP 401
4. **Run one success case:** "✅ 200 — GET /avaliacao com token" (local) → HTTP 200

---

## Known Issues & Future Work

### Not Yet Implemented
- **Listar Avaliações endpoint:** Query to retrieve feedback by ID or filter
- **Email notifications:** Infrastructure present but not yet wired in Quarkus handler

### Deprecated (from Fase-3)
- **GraphQL endpoint:** Collection no longer tests GraphQL mutations; API is REST-only
- **Legacy GraphQL key header:** no longer accepted; all routes use `Authorization: Bearer <token>`

---

## Support & Feedback

For issues with the collection:
1. Verify the API contract in `/Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/plan.md`
2. Check source code in `/Users/jefferson/Documents/FIAP/fase-4/receive-feedback/src/main/java/com/fiap/feedback/infra/handler/ReceiveFeedbackHandler.java`
3. Review Lambda logs in AWS CloudWatch for actual error details
4. Confirm local-http is running if testing locally: `./run.sh local-check`
