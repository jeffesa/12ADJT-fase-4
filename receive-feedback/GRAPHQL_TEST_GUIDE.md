# GraphQL Query Testing Guide: listFeedbacks

## Section 1: Prerequisites

Before testing, ensure:
- The receive-feedback application is running (port 8080 by default)
- GraphQL endpoint is available at `http://localhost:8080/v1/graphql`
- DynamoDB (LocalStack or AWS) is accessible with configured credentials
- Bruno or Postman is installed for testing
- The build was successful: `mvn clean verify -pl receive-feedback` ✓

## Section 2: Create 3 Feedbacks via Mutation

Execute the following GraphQL mutation three times in sequence, once for each feedback:

### Mutation 1: Create HIGH urgency feedback (rating 4)

```graphql
mutation {
  receiveFeedback(input: {
    description: "Bad experience"
    rating: 4
  }) {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

Expected Response:
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "description": "Bad experience",
      "rating": 4,
      "urgency": "HIGH",
      "createdAt": "2024-10-26T16:37:57"
    }
  }
}
```

### Mutation 2: Create MEDIUM urgency feedback (rating 6)

```graphql
mutation {
  receiveFeedback(input: {
    description: "Okay service"
    rating: 6
  }) {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

Expected Response:
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "description": "Okay service",
      "rating": 6,
      "urgency": "MEDIUM",
      "createdAt": "2024-10-26T16:37:58"
    }
  }
}
```

### Mutation 3: Create LOW urgency feedback (rating 9)

```graphql
mutation {
  receiveFeedback(input: {
    description: "Excellent service"
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

Expected Response:
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "description": "Excellent service",
      "rating": 9,
      "urgency": "LOW",
      "createdAt": "2024-10-26T16:37:59"
    }
  }
}
```

## Section 3: List All Feedbacks via Query

Execute the following GraphQL query to retrieve all feedbacks:

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

Expected Response (array of 3 feedbacks):
```json
{
  "data": {
    "listFeedbacks": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "description": "Bad experience",
        "rating": 4,
        "urgency": "HIGH",
        "createdAt": "2024-10-26T16:37:57"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "description": "Okay service",
        "rating": 6,
        "urgency": "MEDIUM",
        "createdAt": "2024-10-26T16:37:58"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "description": "Excellent service",
        "rating": 9,
        "urgency": "LOW",
        "createdAt": "2024-10-26T16:37:59"
      }
    ]
  }
}
```

### Validation Checklist

After running the query, verify:
- [ ] Query returns 3 items
- [ ] First item has urgency: "HIGH" (rating 4)
- [ ] Second item has urgency: "MEDIUM" (rating 6)
- [ ] Third item has urgency: "LOW" (rating 9)
- [ ] All items have valid UUIDs in id field
- [ ] All items have non-null createdAt timestamps
- [ ] Descriptions match what was submitted

## Section 4: Testing with Bruno or Postman

### Import GraphQL Endpoint

1. Create a new request in Bruno/Postman
2. Set method to POST
3. Set URL to `http://localhost:8080/v1/graphql`
4. Add header: `Content-Type: application/json`

### Test Flow

1. **Run Mutation 1** (HIGH urgency): Send mutation query for "Bad experience" with rating 4
   - Verify response contains urgency: "HIGH"
2. **Run Mutation 2** (MEDIUM urgency): Send mutation query for "Okay service" with rating 6
   - Verify response contains urgency: "MEDIUM"
3. **Run Mutation 3** (LOW urgency): Send mutation query for "Excellent service" with rating 9
   - Verify response contains urgency: "LOW"
4. **Run Query**: Send query `listFeedbacks` to retrieve all feedbacks
   - Verify 3 items returned
   - Verify urgencies are correctly mapped (HIGH, MEDIUM, LOW)
   - Verify order is preserved

### Success Criteria

✓ All 3 mutations executed successfully
✓ Query returns all 3 feedbacks
✓ Urgencies correctly calculated from ratings (4→HIGH, 6→MEDIUM, 9→LOW)
✓ All output fields populated (id, description, rating, urgency, createdAt)

## Section 5: GraphQL Query Definition (for reference)

The `listFeedbacks` query is defined in `ReceiveFeedbackQueryResolver.java`:

```java
@Query
public List<FeedbackOutput> listFeedbacks() {
    // Calls receiveFeedbackUseCase.listAllFeedbacks()
    // Maps each Feedback to FeedbackOutput using InputOutputMapper
    // Returns list of all feedbacks from DynamoDB
}
```

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| "Query not found" | Resolver not deployed | Rebuild with `mvn clean compile` and restart app |
| Empty list returned | No feedbacks created | Run mutations in Section 2 first |
| Wrong urgency value | Rating range mismatch | Verify rating is 4 (HIGH), 6 (MEDIUM), or 9 (LOW) |
| 401/403 error | Auth required | Configure bearer token if enabled |
| Connection refused | App not running | Start application on port 8080 |
