# GraphQL Query Testing Guide: listarAvaliacoes

## Section 1: Prerequisites

Before testing, ensure:
- The receive-feedback application is running (port 8080 by default)
- GraphQL endpoint is available at `http://localhost:8080/graphql`
- DynamoDB (LocalStack or AWS) is accessible with configured credentials
- Bruno or Postman is installed for testing
- The build was successful: `mvn clean verify -pl receive-feedback` ✓

## Section 2: Create 3 Feedbacks via Mutation

Execute the following GraphQL mutation three times in sequence, once for each feedback:

### Mutation 1: Create HIGH urgency feedback (rating 4)

```graphql
mutation {
  receiveFeedback(input: {
    descricao: "Bad experience"
    nota: 4
  }) {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
```

Expected Response:
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "descricao": "Bad experience",
      "nota": 4,
      "urgencia": "HIGH",
      "dataCriacao": "2024-10-26T16:37:57"
    }
  }
}
```

### Mutation 2: Create MEDIUM urgency feedback (rating 6)

```graphql
mutation {
  receiveFeedback(input: {
    descricao: "Okay service"
    nota: 6
  }) {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
```

Expected Response:
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "descricao": "Okay service",
      "nota": 6,
      "urgencia": "MEDIUM",
      "dataCriacao": "2024-10-26T16:37:58"
    }
  }
}
```

### Mutation 3: Create LOW urgency feedback (rating 9)

```graphql
mutation {
  receiveFeedback(input: {
    descricao: "Excellent service"
    nota: 9
  }) {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
```

Expected Response:
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "descricao": "Excellent service",
      "nota": 9,
      "urgencia": "LOW",
      "dataCriacao": "2024-10-26T16:37:59"
    }
  }
}
```

## Section 3: List All Feedbacks via Query

Execute the following GraphQL query to retrieve all feedbacks:

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

Expected Response (array of 3 feedbacks):
```json
{
  "data": {
    "listarAvaliacoes": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "descricao": "Bad experience",
        "nota": 4,
        "urgencia": "HIGH",
        "dataCriacao": "2024-10-26T16:37:57"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "descricao": "Okay service",
        "nota": 6,
        "urgencia": "MEDIUM",
        "dataCriacao": "2024-10-26T16:37:58"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "descricao": "Excellent service",
        "nota": 9,
        "urgencia": "LOW",
        "dataCriacao": "2024-10-26T16:37:59"
      }
    ]
  }
}
```

### Validation Checklist

After running the query, verify:
- [ ] Query returns 3 items
- [ ] First item has urgencia: "HIGH" (rating 4)
- [ ] Second item has urgencia: "MEDIUM" (rating 6)
- [ ] Third item has urgencia: "LOW" (rating 9)
- [ ] All items have valid UUIDs in id field
- [ ] All items have non-null dataCriacao timestamps
- [ ] Descriptions match what was submitted

## Section 4: Testing with Bruno or Postman

### Import GraphQL Endpoint

1. Create a new request in Bruno/Postman
2. Set method to POST
3. Set URL to `http://localhost:8080/graphql`
4. Add header: `Content-Type: application/json`

### Test Flow

1. **Run Mutation 1** (HIGH urgency): Send mutation query for "Bad experience" with rating 4
   - Verify response contains urgencia: "HIGH"
2. **Run Mutation 2** (MEDIUM urgency): Send mutation query for "Okay service" with rating 6
   - Verify response contains urgencia: "MEDIUM"
3. **Run Mutation 3** (LOW urgency): Send mutation query for "Excellent service" with rating 9
   - Verify response contains urgencia: "LOW"
4. **Run Query**: Send query `listarAvaliacoes` to retrieve all feedbacks
   - Verify 3 items returned
   - Verify urgencies are correctly mapped (HIGH, MEDIUM, LOW)
   - Verify order is preserved

### Success Criteria

✓ All 3 mutations executed successfully
✓ Query returns all 3 feedbacks
✓ Urgencies correctly calculated from ratings (4→HIGH, 6→MEDIUM, 9→LOW)
✓ All output fields populated (id, descricao, nota, urgencia, dataCriacao)

## Section 5: GraphQL Query Definition (for reference)

The `listarAvaliacoes` query is defined in `ReceiveFeedbackQueryResolver.java`:

```java
@Query
public List<ReceiveFeedbackOutput> listarAvaliacoes() {
    // Implementation: calls receiveFeedbackUseCase.listarTodos()
    // Maps each Feedback to ReceiveFeedbackOutput using InputOutputMapper
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

