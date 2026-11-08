# TASK-023 Implementation Plan: Postman Collection for Fase-4 Feedback API

## Investigation Summary

### API Contract (Verified Against Source Code)

**Handler:** `/Users/jefferson/Documents/FIAP/fase-4/receive-feedback/src/main/java/com/fiap/feedback/infra/handler/ReceiveFeedbackHandler.java`

#### Request Contract
- **Method:** POST
- **Path:** `/avaliacao` (via API Gateway)
- **Content-Type:** `application/json`
- **Body schema (ReceiveFeedbackInput record):**
  ```
  {
    "descricao": String (must not be blank; @NotBlank),
    "nota": Integer (must be 0-10; @Min(0) @Max(10))
  }
  ```

#### Success Response
- **Status Code:** `201 Created` (NOT 200 — verified in handler line `response.setStatusCode(201)`)
- **Content-Type:** `application/json`
- **Response body shape (wrapped in GraphQL-like structure):**
  ```json
  {
    "receiveFeedback": {
      "id": "UUID",
      "descricao": "text",
      "nota": integer,
      "urgencia": "HIGH" | "MEDIUM" | "LOW",
      "dataCriacao": "ISO 8601 timestamp"
    }
  }
  ```

#### Error Response (Validation Failure)
- **Status Code:** `400 Bad Request`
- **RFC 7807 Problem Detail format (NOT GraphQL errors array):**
  ```json
  {
    "title": "Validation Error",
    "status": 400,
    "detail": "Description cannot be empty; Rating must be at least 0"
  }
  ```

#### Urgency Mapping (Verified from Urgency.java)
- **HIGH:** rating 0–5 (very negative)
- **MEDIUM:** rating 6–7 (moderate)
- **LOW:** rating 8–10 (positive)

### List Endpoint Investigation
**Finding:** No `listarAvaliacoes` or list handler exists in the deployed API.
- Search result: `ListFeedback` → no files found
- Handler directory scan: only `ReceiveFeedbackHandler.java`, `LambdaStartupHandler.java`, `HandlerFactory.java`
- Template.yaml: only `ReceiveFeedbackFunction` with `POST /avaliacao` configured
- **Decision:** Omit list operations from collection; document this as "not yet deployed" in README

### API Routing (Verified from template.yaml)
- **Local (LocalStack):** `http://localhost:4566/avaliacao`
  - SAM uses LocalStack on port 4566 for local testing
- **Production (AWS):** `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/avaliacao`
  - From template.yaml Outputs: `https://${FeedbackApi}.execute-api.${Region}.amazonaws.com/${Environment}/avaliacao`
  - Handler entry point: `io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest` (Quarkus Arc CDI bootstrap)
  - Configuration: `QUARKUS_LAMBDA_HANDLER=receiveFeedback` env var

### Key Contract Discrepancies from Brief
1. ❌ **Brief expected:** GraphQL `errors` array on validation failure
   - ✅ **Actual:** RFC 7807 Problem Detail with `title`, `status`, `detail` fields
2. ❌ **Brief expected:** HTTP 200 for success
   - ✅ **Actual:** HTTP 201 Created
3. ✅ **Urgency mapping:** LOW for nota 8 is correct (matches brief)

### Structural Model (fase-3-hospital.postman_collection.json)
Conventions to replicate:
- **Collection info block:** name, description with collection structure notes, schema v2.1.0
- **Collection-level pre-request event:** Auto-login using stored credentials (optional for REST, informative)
- **Environment selector requests:** "Selecionar ambiente local" and "Selecionar ambiente prod" that set collectionVariables
- **Folder structure:** Local folder + Prod folder, each containing operation subfolders
- **Per-request test scripts:** pm.test() assertions on status code, response structure, field values
- **collectionVariables:** Centralized in collection, no separate environment file
- **Variable naming:** `baseUrl`, `localUrl`, `prodUrl`, plus domain-specific vars

---

## Implementation Plan

- [ ] **1. Set up directory structure and collection skeleton**
      Create `/Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/fase4-feedback.postman_collection.json` with valid Postman v2.1.0 schema.
      Initialize collection object: `info`, `item` (empty array), `variable`, `event`.
      Files: `fase4-feedback.postman_collection.json`
      Verify: `jq '.info.schema' fase4-feedback.postman_collection.json` contains `https://schema.getpostman.com/json/collection/v2.1.0/collection.json`

- [ ] **2. Define collection-level variables (collectionVariables)**
      Add all shared variables that test requests will reference:
      - `localUrl` = `http://localhost:4566` (LocalStack API Gateway base)
      - `prodUrl` = `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod` (production)
      - `baseUrl` = empty (set by environment selector)
      - `feedbackId` = empty (captured from first successful create response)
      - `timestamp` = empty (set dynamically in pre-request)
      - `apiKey` = empty (optional; no API Key required per template.yaml, but included for future use)
      Files: `fase4-feedback.postman_collection.json` → update `variable` array
      Verify: `jq '.variable | length' fase4-feedback.postman_collection.json` returns ≥ 5

- [ ] **3. Create collection-level pre-request event**
      Write JavaScript to set timestamp variable before each request (for dynamic test data).
      Content: `pm.collectionVariables.set('timestamp', Date.now());`
      Purpose: Ensure unique feedback descriptions per test run to avoid duplicates in DynamoDB.
      Files: `fase4-feedback.postman_collection.json` → `event[0]` (type: `prerequest`)
      Verify: Can view script in Postman UI; logs timestamp on request

- [ ] **4. Create "Selecionar ambiente local" environment selector request**
      Folder: `item[0]` = `"name": "local"`
      Request: `"name": "Selecionar ambiente local"`
      Method: GET to `{{localUrl}}/health` or similar health endpoint (use DynamoDB ListTables as dummy)
      Test script: Sets `baseUrl = localUrl`, clears `feedbackId`, asserts HTTP 200
      Purpose: All requests in local folder will use `{{baseUrl}}` which now points to LocalStack
      Files: `fase4-feedback.postman_collection.json` (nested in local folder)
      Verify: Running this request in Postman sets `baseUrl` collectionVariable to `http://localhost:4566`

- [ ] **5. Create "Selecionar ambiente prod" environment selector request**
      Folder: `item[1]` = `"name": "prod"`
      Request: `"name": "Selecionar ambiente prod"`
      Method: GET (dummy, just to switch variables; or OPTIONS /avaliacao)
      Test script: Sets `baseUrl = prodUrl`, clears `feedbackId`
      Purpose: Switch collection to prod endpoints
      Files: `fase4-feedback.postman_collection.json` (nested in prod folder)
      Verify: Running this request sets `baseUrl` to production URL

- [ ] **6. Create operation group: criarAvaliacao (Mutation analog)**
      Under each environment folder (local + prod), create subfolder "Criar Avaliação" containing test cases:
      - **Case 6a: ✅ 201 Success — nota HIGH (0-5)**
        - Name: `"Criar Avaliação — nota 1 (urgência HIGH) ✅ 201"`
        - Method: POST
        - URL: `{{baseUrl}}/avaliacao`
        - Body: `{ "descricao": "Feedback crítico - teste {{timestamp}}", "nota": 1 }`
        - Pre-request: Set timestamp to ensure unique description
        - Test assertions:
          - Status 201
          - `receiveFeedback.urgencia === "HIGH"`
          - `receiveFeedback.nota === 1`
          - `receiveFeedback.id` matches UUID format
          - `receiveFeedback.dataCriacao` is valid ISO 8601
          - Save `feedbackId` from response for later queries
      - **Case 6b: ✅ 201 Success — nota MEDIUM (6-7)**
        - Name: `"Criar Avaliação — nota 6 (urgência MEDIUM) ✅ 201"`
        - Body: `{ "descricao": "Feedback moderado - teste {{timestamp}}", "nota": 6 }`
        - Test assertions: Status 201, `urgencia === "MEDIUM"`, `nota === 6`
      - **Case 6c: ✅ 201 Success — nota LOW (8-10)**
        - Name: `"Criar Avaliação — nota 9 (urgência LOW) ✅ 201"`
        - Body: `{ "descricao": "Feedback positivo - teste {{timestamp}}", "nota": 9 }`
        - Test assertions: Status 201, `urgencia === "LOW"`, `nota === 9`
      - **Case 6d: ❌ 400 Bad Request — nota inválida (< 0)**
        - Name: `"Criar Avaliação — nota -1 (inválida) ❌ 400"`
        - Body: `{ "descricao": "Feedback inválido", "nota": -1 }`
        - Test assertions: Status 400, `title === "Validation Error"`, detail contains "must be at least 0"
      - **Case 6e: ❌ 400 Bad Request — nota inválida (> 10)**
        - Name: `"Criar Avaliação — nota 11 (inválida) ❌ 400"`
        - Body: `{ "descricao": "Feedback inválido", "nota": 11 }`
        - Test assertions: Status 400, detail contains "must be at most 10"
      - **Case 6f: ❌ 400 Bad Request — descricao vazia**
        - Name: `"Criar Avaliação — descrição vazia ❌ 400"`
        - Body: `{ "descricao": "", "nota": 5 }`
        - Test assertions: Status 400, detail contains "cannot be empty"
      - **Case 6g: ❌ 400 Bad Request — descricao com espaços em branco apenas**
        - Name: `"Criar Avaliação — descrição somente espaços ❌ 400"`
        - Body: `{ "descricao": "   ", "nota": 5 }`
        - Test assertions: Status 400
      - **Case 6h: ❌ 400 Bad Request — missing nota field**
        - Name: `"Criar Avaliação — nota ausente ❌ 400"`
        - Body: `{ "descricao": "Feedback sem nota" }`
        - Test assertions: Status 400
      - **Case 6i: ❌ 400 Bad Request — missing descricao field**
        - Name: `"Criar Avaliação — descrição ausente ❌ 400"`
        - Body: `{ "nota": 5 }`
        - Test assertions: Status 400
      - **Case 6j: ❌ 400 Bad Request — nota como string**
        - Name: `"Criar Avaliação — nota como string ❌ 400"`
        - Body: `{ "descricao": "Feedback", "nota": "cinco" }`
        - Test assertions: Status 400
      - **Case 6k: ✅ 201 Success — nota no boundary (0)**
        - Name: `"Criar Avaliação — nota 0 (boundary) ✅ 201"`
        - Body: `{ "descricao": "Feedback no mínimo {{timestamp}}", "nota": 0 }`
        - Test assertions: Status 201, `urgencia === "HIGH"`
      - **Case 6l: ✅ 201 Success — nota no boundary (10)**
        - Name: `"Criar Avaliação — nota 10 (boundary) ✅ 201"`
        - Body: `{ "descricao": "Feedback no máximo {{timestamp}}", "nota": 10 }`
        - Test assertions: Status 201, `urgencia === "LOW"`

      Files: `fase4-feedback.postman_collection.json` (nested under local/prod → "Criar Avaliação" folder)
      Verify: All 12 requests present; each has method POST, URL includes `{{baseUrl}}`, test scripts defined

- [ ] **7. Document "Listar Avaliações" as not-yet-deployed**
      Create folder "Listar Avaliações (Coming Soon)" under each environment.
      Add placeholder request with description: "This endpoint will list all feedback. Not yet deployed in TASK-023."
      Files: `fase4-feedback.postman_collection.json`
      Verify: Folder visible in collection; request marked with note

- [ ] **8. Create README.md for collection**
      Path: `/Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/README.md`
      Content:
      - **Title:** "FIAP Fase-4 Feedback API — Postman Collection"
      - **Overview:** What the collection covers (create feedback with urgency validation)
      - **Import Instructions:**
        - In Postman: File → Import → select `fase4-feedback.postman_collection.json`
        - In Bruno: Open Bruno → Add Collection → select JSON file
      - **Execution Order:**
        1. Run "Selecionar ambiente local" (or prod)
        2. Run "Criar Avaliação" test cases in sequence
      - **Expected Results:**
        - Success cases return 201 with correct urgency
        - Error cases return 400 with RFC 7807 error details
        - All timestamps unique (no duplicate descriptions)
      - **Variables Explanation:**
        - `baseUrl`: Set by environment selector; points to local or prod
        - `localUrl`: LocalStack on localhost:4566
        - `prodUrl`: AWS API Gateway endpoint
        - `feedbackId`: Captured from first successful response (for future list queries)
        - `timestamp`: Auto-generated per request for unique test data
      - **API Contract Summary:**
        - Endpoint: POST `/avaliacao`
        - Success: HTTP 201 with `receiveFeedback` wrapper
        - Error: HTTP 400 with RFC 7807 format (title, status, detail)
        - Urgency: HIGH (0-5), MEDIUM (6-7), LOW (8-10)
      - **Known Limitations:**
        - List/query endpoint not yet deployed (TASK planned for future sprint)
        - No API Key required (removed from template; optional for future)
      - **Testing in Local Environment:**
        - Ensure LocalStack running: `docker-compose up -d`
        - Ensure app built: `mvn clean package`
        - Run environment selector, then test cases
        - Check CloudWatch logs for Lambda execution
      - **Testing in Production:**
        - Requires active AWS deployment
        - Ensure e-mail in SES is verified (sandbox mode)
        - Monitor CloudWatch logs
      Files: `/Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/README.md`
      Verify: Markdown renders correctly; all sections present; no broken links

- [ ] **9. Validate JSON schema and structure**
      Run: `npx ajv validate -s https://schema.getpostman.com/json/collection/v2.1.0/collection.json -d fase4-feedback.postman_collection.json`
      Or manual check: Open `fase4-feedback.postman_collection.json` in jq and verify:
      - `info.name` is non-empty string
      - `info.schema` is exactly `https://schema.getpostman.com/json/collection/v2.1.0/collection.json`
      - `item` array has ≥ 2 elements (local + prod folders)
      - Each folder `item` contains "Criar Avaliação" and "Listar Avaliações (Coming Soon)"
      - Each request has `method`, `url`, `body` (if applicable)
      - Each test request has `event` with `listen: "test"` and `script`
      Files: `fase4-feedback.postman_collection.json`
      Verify: AJV passes without validation errors; structure matches Postman v2.1.0 spec

- [ ] **10. Import into Postman/Bruno and run all requests**
      - Import `fase4-feedback.postman_collection.json` into Postman
      - Run "Selecionar ambiente local" → asserts pass
      - Run one success case (nota 1, urgencia HIGH) → asserts pass
      - Run one error case (nota -1) → asserts pass
      - Confirm `baseUrl` variable is set to localUrl
      - Run "Selecionar ambiente prod" → asserts pass
      - Confirm `baseUrl` variable is set to prodUrl
      Files: `fase4-feedback.postman_collection.json`
      Verify: All requests execute without syntax errors; test assertions pass in local environment

- [ ] **11. Final review and documentation of test data**
      Document in collection comments (via Postman folder/request descriptions):
      - Business rule: Urgency calculated from rating, not sent by client
      - Validation: Rating must be 0-10, description must not be blank
      - Response structure: HTTP 201 with wrapped `receiveFeedback` object
      - Error format: RFC 7807 with title, status, detail
      Files: `fase4-feedback.postman_collection.json` (descriptions), `README.md`
      Verify: Open collection in Postman; view folder/request descriptions; all business rules documented

---

## Contract Corrections Summary

### Verified Facts vs. Brief Assumptions
1. **HTTP Status Code:**
   - Brief assumption: 200 OK
   - Actual (from source): **201 Created** ✅ (line: `response.setStatusCode(201);`)

2. **Error Response Format:**
   - Brief assumption: GraphQL-style `{ "errors": [...] }` array
   - Actual (from source): **RFC 7807 Problem Detail** `{ "title", "status", "detail" }` ✅

3. **Urgency Mapping:**
   - Brief assumption: LOW for nota 8-10
   - Actual (from source): **HIGH (0-5), MEDIUM (6-7), LOW (8-10)** ✅ Matches brief exactly

4. **List Endpoint:**
   - Brief assumption: `listarAvaliacoes` query exists
   - Actual: **Not deployed in current API** ❌ (no handler found)
   - Decision: Omit from collection; note as "coming soon"

5. **Request/Response Wrapper:**
   - Brief assumption: Plain JSON `{ "id", "descricao", "nota", "urgencia", "dataCriacao" }`
   - Actual (from source): **Wrapped in `receiveFeedback` key** `{ "receiveFeedback": { ... } }` ✅

### Collection Design Decisions
- **No GraphQL endpoint in collection:** SAM template configures REST API Gateway with POST `/avaliacao`, not GraphQL. GraphQL mentioned in brief may refer to future task (TASK-022 documents "Documentação da API GraphQL").
- **REST-only test cases:** All requests use REST method/URL; no GraphQL mutation syntax.
- **No API Key testing:** Template.yaml removed API Key/Usage Plan; all tests do not include `x-api-key` header.
- **Folder structure:** Mirrors fase-3 pattern with local/prod folders for easy environment switching.
- **Comprehensive error coverage:** 12 test cases including boundary values, missing fields, type mismatches, and business rule violations.

---

## Files to Create

1. `/Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/fase4-feedback.postman_collection.json` — Main collection file
2. `/Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/README.md` — Usage and reference guide

## Verification Commands

After implementation:

```bash
# Validate JSON structure
jq '.' /Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/fase4-feedback.postman_collection.json > /dev/null && echo "✅ JSON valid"

# Count test requests
jq '[.. | .request? | select(.method == "POST")] | length' \
  /Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/fase4-feedback.postman_collection.json

# Verify collection info
jq '.info | {name, schema}' /Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/fase4-feedback.postman_collection.json

# Check collectionVariables count
jq '.variable | length' /Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/fase4-feedback.postman_collection.json
```

---

## Notes for Implementation Agent

- Follow the exact folder structure and naming from fase-3 model (local → prod subfolders).
- Each test case name should include the business rule being tested (e.g., "nota HIGH", "descricao vazia").
- Use `{{timestamp}}` in descriptions to ensure uniqueness across test runs.
- Test assertions must match the REAL API contract verified from source (not the brief's assumptions).
- The response envelope `{ "receiveFeedback": { ... } }` must be reflected in all success test assertions.
- RFC 7807 error format must be used for all 400 error assertions.
- Verify imports work in both Postman and Bruno (both support v2.1.0 schema).

