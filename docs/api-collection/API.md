# GraphQL API Documentation — Feedback Platform

**Base URL:** 
- **Local:** `http://localhost:4566/v1/graphql`
- **Production:** `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/v1/graphql`

**Authentication:** Bearer token via header `Authorization: Bearer <token>` (required on every route; token = secret `API_TOKEN`)

---

## GraphQL Schema

### Types

#### Feedback
```graphql
type Feedback {
  id: String!                    # UUID (auto-generated)
  description: String!           # Feedback description (1-1000 chars)
  rating: Int!                   # Rating (0-10)
  urgency: Urgency!              # Calculated from rating
  createdAt: String!             # ISO 8601 timestamp
}
```

#### Urgency (Enum)
```graphql
enum Urgency {
  HIGH                           # rating 0-5
  MEDIUM                         # rating 6-7
  LOW                            # rating 8-10
}
```

#### FeedbackInput
```graphql
input FeedbackInput {
  description: String!          # Required, min 1 char
  rating: Int!                   # Required, 0-10
}
```

---

## Mutations

### receiveFeedback

Creates a new feedback entry with description and rating. Calculates urgency automatically.

**Signature:**
```graphql
mutation receiveFeedback(
  input: FeedbackInput!
): Feedback!
```

**Request Example:**
```graphql
mutation ReceiveFeedback {
  receiveFeedback(input: {
    description: "Excelente produto, muito satisfeito!"
    rating: 9
  }) {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

**Success Response (HTTP 201):**
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "description": "Excelente produto, muito satisfeito!",
      "rating": 9,
      "urgency": "LOW",
      "createdAt": "2026-11-08T18:30:45.123456789"
    }
  }
}
```

**Validation Error Response (HTTP 400):**
```json
{
  "title": "Validation Error",
  "status": 400,
  "detail": "Rating must be between 0 and 10"
}
```

---

### Urgency Calculation Rules

| Rating | Urgency | Priority |
|--------|---------|----------|
| 0-5 | HIGH | 🔴 Critical — email notification sent |
| 6-7 | MEDIUM | 🟡 Normal — stored in DynamoDB |
| 8-10 | LOW | 🟢 Low priority — stored in DynamoDB |

---

## Validation Rules

### description
- **Type:** String
- **Required:** Yes
- **Min Length:** 1 character
- **Max Length:** 1000 characters
- **Constraint:** Must not be blank or null
- **Error:** "Feedback description must not be empty"

### rating
- **Type:** Integer
- **Required:** Yes
- **Min Value:** 0
- **Max Value:** 10
- **Constraint:** Rating between 0 and 10
- **Error (too low):** "Rating must be at least 0"
- **Error (too high):** "Rating must be at most 10"

---

## Error Responses

All errors follow RFC 7807 Problem Detail format:

### HTTP 400 — Bad Request

**Cause:** Validation failure

**Response:**
```json
{
  "title": "Validation Error",
  "status": 400,
  "detail": "Rating must be between 0 and 10"
}
```

**Common Error Messages:**
- "Feedback description must not be empty"
- "Rating must be at least 0"
- "Rating must be at most 10"

### HTTP 500 — Internal Server Error

**Cause:** Unexpected error in handler or Lambda runtime

**Response:**
```json
{
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred"
}
```

---

## Testing with Bruno/Postman

### 1. Import Collection

Download `fase4-feedback.postman_collection.json` from the repository:

```bash
git clone https://github.com/jeffesa/12ADJT-fase-4.git
cd fase-4/docs/api-collection
```

**Bruno:** File → Import → Select `fase4-feedback.postman_collection.json`  
**Postman:** Import → Select file

### 2. Set the tokens

Open **Collection > Variables** and set `prodToken` (value of the `API_TOKEN` secret, kept only on your machine). `localToken` already has the local default.

**Local (local-http):**
```
POST http://localhost:8080/v1/feedback
Content-Type: application/json
Authorization: Bearer <localToken>

{
  "description": "Test feedback",
  "rating": 8
}
```

**Production (AWS):**
```
POST https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/v1/feedback
Content-Type: application/json
Authorization: Bearer <token>

{
  "description": "Test feedback",
  "rating": 8
}
```

### 3. Execute Requests

The collection contains 20 pre-built requests per environment (`local` and `prod`), including 401 cases for missing and invalid tokens:

- ✅ Success cases (rating 1, 5, 6, 7, 8, 10)
- ❌ Error cases (rating -1, 11, empty description, null description)

Each request includes:
- Pre-request script: Auto-generates timestamp
- Test script: Validates response status and structure
- Pre-configured variables: localUrl, prodUrl, localToken, prodToken, feedbackId, timestamp

### 4. View Results

**Test Results Tab:** Shows pass/fail for each assertion  
**Response Body:** Shows JSON response from API  
**Test Stats:** Summary of passed/failed tests

---

## Urgency and Notifications

### HIGH Urgency Feedback (rating 0-5)

When a feedback with HIGH urgency is created:

1. **DynamoDB:** Feedback stored in `prod-feedback-table`
2. **SNS:** Message published to `prod-feedback-urgent-topic`
3. **Lambda 2:** SendNotificationHandler triggered by SNS
4. **Email:** SES sends notification email to admin

**Email Format:**
```
To: admin@company.com
Subject: ⚠️ Feedback CRÍTICO Recebido

Conteúdo:
- ID: 550e8400-e29b-41d4-a716-446655440000
- Feedback: "Produto não funciona corretamente"
- Rating: 2/10
- Urgência: HIGH
- Data: 2026-11-08 18:30:45
```

### MEDIUM/LOW Urgency Feedback (rating 6-10)

- **DynamoDB:** Feedback stored in `prod-feedback-table`
- **SNS:** No message published
- **Lambda 2:** Not triggered
- **Email:** No notification sent

---

## Weekly Report Generation

**Trigger:** EventBridge cron rule (Monday 8 AM UTC)

**Lambda 3 (GenerateReport):**
- Queries DynamoDB for feedbacks from past week
- Aggregates statistics (total, average rating, by urgency, by day)
- Generates HTML report
- Sends report via email to admin

**Report Format:**
```
To: admin@company.com
Subject: Relatório de Feedback — Semana de 4-10 de Novembro

Conteúdo:
- Total de Avaliações: 15
- Média Geral: 7.2/10
- Urgências:
  - HIGH (0-5): 3 feedbacks
  - MEDIUM (6-7): 5 feedbacks
  - LOW (8-10): 7 feedbacks
- Por Dia: [tabela com contagem diária]
```

---

## Environment Variables

### Local (docker-compose)

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export TABLE_NAME=dev-feedback-table
export FEEDBACK_TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:dev-feedback-urgent-topic
```

### Production (AWS)

```bash
export AWS_REGION=us-east-1
export TABLE_NAME=prod-feedback-table
export FEEDBACK_TOPIC_ARN=arn:aws:sns:us-east-1:422648464291:prod-feedback-urgent-topic
export ADMIN_EMAIL=admin@company.com
```

---

## GraphiQL Playground

**URL:** 
- Local: `http://localhost:4566/graphiql`
- Prod: Not available (Bearer token required)

**How to Use GraphiQL:**

1. Navigate to URL in browser
2. Left panel: Write GraphQL query/mutation
3. Middle panel: Click "Play" button to execute
4. Right panel: View response JSON

**Example Query:**
```graphql
mutation {
  receiveFeedback(input: {
    description: "Test"
    rating: 8
  }) {
    id
    urgency
  }
}
```

---

## Useful Links

- **Repository:** https://github.com/jeffesa/12ADJT-fase-4
- **Postman Collection:** `docs/api-collection/fase4-feedback.postman_collection.json`
- **SonarCloud Dashboard:** https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4
- **AWS Endpoint:** https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/v1/graphql
- **Quarkus GraphQL:** https://quarkus.io/guides/smallrye-graphql
- **RFC 7807 Problem Detail:** https://tools.ietf.org/html/rfc7807

---

**Last Updated:** November 8, 2026  
**API Version:** v1  
**Status:** Production Ready ✅


