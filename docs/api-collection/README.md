# FIAP Fase-4 Feedback API — Postman Collection

## Overview

This Postman collection covers the Feedback API endpoints for FIAP Fase-4, with comprehensive test cases for creating feedback and validating urgency calculations based on ratings. The collection includes both local (LocalStack) and production (AWS API Gateway) environments, allowing easy switching between testing scenarios.

### API Coverage
- **POST `/v1/feedback`** — Create feedback with automatic urgency classification
  - Success cases: 3 urgency tiers (HIGH, MEDIUM, LOW) with boundary value testing
  - Error cases: 9 validation and constraint violation scenarios
- **GET `/v1/feedback`** — List all feedbacks
- **POST `/v1/graphql`** — GraphQL query `listFeedbacks`

### Architecture
- **REST-based**, with one GraphQL query (`listFeedbacks`)
- **Request contract:** JSON with `description` (string) and `rating` (integer 0-10)
- **Response format:** RFC 7807 Problem Detail for errors; flat JSON object (no wrapper) for the v1 success response
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
3. All requests will hit `{{localUrl}}/v1/feedback` = `http://localhost:8080/v1/feedback`

### To test PRODUCTION (AWS):
1. Expand folder **"prod"**
2. Run any request under **"Criar Avaliação"**
3. All requests will hit `{{prodUrl}}/v1/feedback` = `https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod/v1/feedback`

### Test Cases in "Criar Avaliação"
| Request | Rating | Urgency | Note |
|---------|--------|---------|------|
| ✅ 201 — rating 1 (urgência HIGH) | 1 | HIGH | Captures feedback ID for future list queries |
| ✅ 201 — rating 6 (urgência MEDIUM) | 6 | MEDIUM | Mid-range feedback |
| ✅ 201 — rating 9 (urgência LOW) | 9 | LOW | High satisfaction |
| ✅ 201 — rating 0 (boundary, urgência HIGH) | 0 | HIGH | Boundary: minimum valid rating |
| ✅ 201 — rating 10 (boundary, urgência LOW) | 10 | LOW | Boundary: maximum valid rating |

### Test Cases in "Listar Avaliações"
| Request | Purpose | Response |
|---------|---------|----------|
| ✅ 200 — GET all feedbacks (prod) / ✅ 200 — GET /v1/feedback com token (local) | List all feedbacks stored in DynamoDB | HTTP 200 with `feedbacks` array and `total` |
| listFeedbacks - Query all feedbacks | Same data through GraphQL | HTTP 200 with `data.listFeedbacks` array |

**Test Assertions (listFeedbacks):**
- ✅ Status code 200
- ✅ Response has `data.listFeedbacks` field
- ✅ Each feedback item has: `id`, `description`, `rating`, `urgency`, `createdAt`
- ✅ `id` is a valid UUID
- ✅ `urgency` is one of: HIGH, MEDIUM, LOW
- ✅ `createdAt` is ISO 8601 format
- ✅ Array is returned (may be empty)

#### Error Cases (HTTP 400 — RFC 7807 Problem Detail)
| Request | Issue | Error Field |
|---------|-------|------------|
| ❌ 400 — rating -1 (inválida) | Rating below minimum | `detail` contains "must be at least 0" |
| ❌ 400 — rating 11 (inválida) | Rating above maximum | `detail` contains "must be at most 10" |
| ❌ 400 — descrição vazia | Empty string | `detail` contains "cannot be empty" |
| ❌ 400 — descrição com espaços apenas | Whitespace only | `detail` contains blank-check violation |
| ❌ 400 — rating ausente | Missing field | HTTP 400 — field required |
| ❌ 400 — descrição ausente | Missing field | HTTP 400 — field required |
| ❌ 400 — rating como string | Type mismatch | HTTP 400 — number expected |

---

## Urgency Mapping

The API calculates urgency based on the rating:

| Rating Range | Urgency | Business Meaning |
|--------------|---------|------------------|
| 0–5 | **HIGH** | Negative feedback requiring immediate attention |
| 6–7 | **MEDIUM** | Moderate feedback — standard review |
| 8–10 | **LOW** | Positive feedback — no action needed |

All tests verify this mapping by asserting the `urgency` field matches the expected tier.

---

## Expected Test Results

### All Success Cases Pass
- HTTP 201 Created
- Response is a flat object (no `receiveFeedback` wrapper)
- Fields: `id` (UUID), `description` (echoed), `rating` (echoed), `urgency` (calculated), `createdAt` (ISO 8601)
- Timestamp in description is unique (no duplicates in DynamoDB)

### All Error Cases Return HTTP 400
- Response format: RFC 7807 Problem Detail
- Fields: `title` (e.g., "Validation Error"), `status` (400), `detail` (specific violation)
- **Not** a GraphQL `errors` array

### Example Success Response
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Feedback crítico - teste 1699523045123",
  "rating": 1,
  "urgency": "HIGH",
  "createdAt": "2024-11-08T18:50:45.123Z"
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

## GraphQL Query: listFeedbacks

Retrieve all feedbacks stored in DynamoDB.

### Request
```graphql
query {
  listFeedbacks {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

### Success Response (200 OK)
```json
{
  "data": {
    "listFeedbacks": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "description": "Feedback teste 1",
        "rating": 1,
        "urgency": "HIGH",
        "createdAt": "2024-11-08T18:50:45.123Z"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "description": "Feedback teste 2",
        "rating": 8,
        "urgency": "LOW",
        "createdAt": "2024-11-08T19:22:30.456Z"
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
3. Routes available at `http://localhost:8080` (`/v1/feedback` and `/v1/graphql`)

### Steps
1. Keep the default `localToken` (or export `API_TOKEN` before `./run.sh local-up` and set the same value in `localToken`)
2. Run "✅ 201 — rating 1 (urgência HIGH)" (a rating from 0 to 5 triggers SNS/email)
3. Run the remaining test cases in any order, including the 401 requests ("sem token" e "token inválido")

---

## Running in Production (AWS)

### Prerequisites
1. AWS Lambda function deployed and accessible
2. API Gateway endpoint active: `https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod/v1/feedback`
3. DynamoDB table `prod-feedback-table` available in sa-east-1
4. SES verified (if email notifications enabled)
5. `prodToken` set to the value of the `API_TOKEN` secret

### Steps
1. Set `prodToken` (Collection > Variables)
2. Run test cases (same structure as local). Requests with a rating from 0 to 5 create real records and send SNS/email in production
3. Monitor AWS CloudWatch Logs for Lambda execution
4. Verify DynamoDB items created in the console

### Known Limitations
- **Bearer token required** on every route (`Authorization: Bearer <token>`); to create it, see the secret `API_TOKEN` in `docs/deploy/AWS_DEPLOY_GUIDE.md`
- **Email notifications** — Currently in development; test with SES sandbox mode

---

## API Contract Summary

### Endpoint
```
POST /v1/feedback
Host: {{localUrl}} or {{prodUrl}}
Authorization: Bearer <token>
Content-Type: application/json
```

### Request Body
```json
{
  "description": "string (required, non-blank)",
  "rating": "integer (required, 0-10 inclusive)"
}
```

### Success Response (201 Created)
```json
{
  "id": "UUID",
  "description": "string (echoed from request)",
  "rating": "integer (echoed from request)",
  "urgency": "HIGH|MEDIUM|LOW (calculated)",
  "createdAt": "ISO 8601 timestamp"
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

#### 404/405 on {{localUrl}}/v1/feedback
- **Cause:** local-http not running, or started from the wrong folder (the `receive-feedback` module has no HTTP routes)
- **Fix:** Run `./run.sh local-up` in the `local-http` folder and keep it running

### 500 Internal Server Error
- **Cause:** Lambda handler initialization failed
- **Fix:** Check CloudWatch logs for Arc CDI bootstrap errors; confirm Quarkus handler configured

### Test Assertion Failures
- **Cause:** Response structure doesn't match (for example, expecting a `receiveFeedback` wrapper, which v1 no longer returns)
- **Fix:** Verify the v1 response is a flat object with English fields; check Lambda logs

### Duplicate Description Error (if persisting to external store)
- **Cause:** Same test run multiple times with fixed descriptions
- **Fix:** Use `{{timestamp}}` in all descriptions; it auto-generates per request

---

## Import Verification

After importing, verify the collection loads correctly:

1. **Check variables:** Collection > Variables shows `localUrl`, `prodUrl`, `localToken`, `prodToken`, `feedbackId` and `timestamp`
2. **Check folders:** only `local` and `prod`, with 20 requests each
3. **Run one 401 case:** "🔒 401 — GET /v1/feedback sem token" → HTTP 401
4. **Run one success case:** "✅ 200 — GET /v1/feedback com token" (local) → HTTP 200

---

## Known Issues & Future Work

### Not Yet Implemented
- **Listar Avaliações endpoint:** Query to retrieve feedback by ID or filter
- **Email notifications:** Infrastructure present but not yet wired in Quarkus handler

### Deprecated (from Fase-3)
- **GraphQL mutations:** Collection no longer tests GraphQL mutations; it only runs the `listFeedbacks` query
- **Legacy GraphQL key header:** no longer accepted; all routes use `Authorization: Bearer <token>`

---

## Support & Feedback

For issues with the collection:
1. Verify the API contract in `/Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/plan.md`
2. Check source code in `/Users/jefferson/Documents/FIAP/fase-4/receive-feedback/src/main/java/com/fiap/feedback/infra/handler/ReceiveFeedbackHandler.java`
3. Review Lambda logs in AWS CloudWatch for actual error details
4. Confirm local-http is running if testing locally: `./run.sh local-check`
