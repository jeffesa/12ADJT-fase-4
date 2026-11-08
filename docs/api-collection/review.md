# Review: FIAP Fase-4 Feedback API Postman Collection

## Summary

The Postman collection and documentation for FIAP Fase-4 Feedback API have been created to specification. The collection follows the structural model from fase-3 (local/prod folder separation, environment selector pattern) and correctly implements all 12 test cases across both environments, totaling 24 requests. The JSON schema is valid Postman v2.1.0 and imports cleanly. The collection accurately reflects the actual API contract verified against source code: REST-based POST `/avaliacao`, HTTP 201 for success with `receiveFeedback` wrapper, HTTP 400 with RFC 7807 Problem Detail for errors, and urgency calculated from 0-10 rating scale (HIGH 0-5, MEDIUM 6-7, LOW 8-10). The README provides comprehensive execution guidance, API contract documentation, and troubleshooting steps.

**Watch for:** The prod endpoint URL in the collection (x250lwug12.execute-api.us-east-1.amazonaws.com) must be verified as the current live deployment; if that endpoint has changed in AWS, tests against prod will fail with 404.

**Verdict**: APPROVED

---

## High-level view

The collection structure mirrors the fase-3 hospital system model exactly: top-level folders for "local" and "prod" environments, each containing an environment selector request that sets the `baseUrl` variable, followed by a "Criar Avaliação" subfolder with all test cases. This design lets users run the same test suite against both LocalStack and production without modifying individual requests.

The request contract correctly targets the actual REST API (not GraphQL) with POST body containing `descricao` and `nota` fields. Request bodies are identical across local/prod; only the baseUrl changes via the environment selector. The collection captures all 12 planned test cases, including three success scenarios (HIGH/MEDIUM/LOW urgency), boundary values (nota 0 and 10), and nine failure scenarios covering validation errors, missing fields, and type mismatches.

Success test scripts assert HTTP 201 and validate the response structure: `receiveFeedback` wrapper containing `id` (UUID), `descricao`, `nota`, `urgencia` (calculated, not sent by client), and `dataCriacao` (ISO 8601). Error test scripts assert HTTP 400 and verify RFC 7807 format with `title`, `status`, and `detail` fields — not GraphQL `errors` arrays.

The collection variables are properly embedded (localUrl, prodUrl, baseUrl, feedbackId, timestamp) and the collection-level pre-request event auto-generates timestamps to ensure unique test data per run. The README explains import, execution workflow, expected results, and troubleshooting; it correctly documents that the list endpoint is not yet deployed.

---

<details>
<summary>Issues (1)</summary>

1. **Prod endpoint URL must be verified live** — The collection hardcodes prodUrl as `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod`. Before declaring this collection ready for prod testing, confirm this endpoint is the current live deployment. If the Lambda function or API Gateway was redeployed with a different endpoint, all prod requests will fail with 404.

</details>

---

<details>
<summary>Details</summary>

### Postman v2.1.0 Schema Validity

The collection info block declares schema `https://schema.getpostman.com/json/collection/v2.1.0/collection.json` and passes JSON parsing. The structure conforms to the schema: `info` (with name/description), `item` array (folders and requests), `variable` array (collection variables), `event` array (collection-level scripts). No schema violations detected.

### Folder Structure and Environment Switching

Both local and prod folders contain identical substructure: an environment selector request at the top level, followed by a "Criar Avaliação" folder containing all test cases. The "Selecionar ambiente local" request sets `baseUrl` to `http://localhost:4566` via a test script; "Selecionar ambiente prod" sets it to the AWS API Gateway URL. This pattern mirrors fase-3 exactly and ensures users run a single test suite against either environment without request duplication or modification.

### Request Contract Accuracy

All POST requests target `{{baseUrl}}/avaliacao` with headers `Content-Type: application/json`. Request bodies are plain JSON with `descricao` (string) and `nota` (integer), matching the actual API contract verified from ReceiveFeedbackHandler.java. The bodies do not include `urgencia` or `id` fields — these are calculated server-side and returned in the response only. Timestamp interpolation (`{{timestamp}}`) is used consistently across success cases to ensure unique descriptions and avoid DynamoDB duplicate key violations on repeated test runs.

### Test Case Coverage (12 Total × 2 Environments = 24 Requests)

**Success cases (HTTP 201):**
- nota 1 → urgencia HIGH (boundary: 0-5 range)
- nota 6 → urgencia MEDIUM (boundary: 6-7 range)
- nota 9 → urgencia LOW (boundary: 8-10 range)
- nota 0 → urgencia HIGH (boundary: minimum valid)
- nota 10 → urgencia LOW (boundary: maximum valid)

All success test scripts assert HTTP 201, extract `receiveFeedback` wrapper, validate UUID format for `id`, verify urgency matches expected tier, and confirm `dataCriacao` is ISO 8601.

**Error cases (HTTP 400):**
- nota -1: Below minimum (0)
- nota 11: Above maximum (10)
- descricao empty string: Fails @NotBlank validation
- descricao whitespace-only: Fails blank check after trim
- nota field missing: Required field not present
- descricao field missing: Required field not present
- nota as string ("cinco"): Type mismatch, not an integer
- Plus the three core urgency cases listed above

Error test scripts all assert HTTP 400 and verify RFC 7807 structure: `title`, `status: 400`, `detail` present. They do not check for GraphQL `errors` arrays, which is correct given the actual API returns REST error format.

### Response Wrapper Structure

Success responses are wrapped in `{ "receiveFeedback": { ... } }` — test scripts correctly unpack this structure and assert on nested properties like `feedback.nota`, `feedback.urgencia`, etc. This matches the handler source code which wraps the response: `new ReceiveFeedbackOutput(new FeedbackDTO(...))`.

### Error Response Format Validation

Error test scripts check for RFC 7807 Problem Detail: `title` and `detail` properties are asserted to exist, and `status` is asserted to equal 400. This is the correct format for the actual API (confirmed in plan.md, verified from handler source). No checks for GraphQL-style `errors` or `message` arrays — good, because the API does not use those.

### Collection Variables

Five variables defined at the collection level:
- `localUrl`: `http://localhost:4566` (LocalStack API Gateway)
- `prodUrl`: `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod` (AWS API Gateway)
- `baseUrl`: Empty, set by environment selector request
- `feedbackId`: Empty, captured from first successful POST response for future list queries (not yet deployed)
- `timestamp`: Empty, auto-generated by collection-level pre-request script

This matches the plan's variable list and the fase-3 model.

### Collection-Level Pre-Request Script

A single event block (type `prerequest`) contains JavaScript that calls `pm.collectionVariables.set('timestamp', Date.now())` before each request. This ensures every request with `{{timestamp}}` in the body gets a fresh timestamp value, preventing duplicate descriptions and DynamoDB write collisions on repeated test runs.

### README Documentation

The README covers:
- Import instructions for Postman and Bruno
- Collection variables table with descriptions
- Execution workflow: environment selector first, then test cases
- Success case table with rating → urgency mappings
- Error case table with expected HTTP 400 responses and RFC 7807 format
- Urgency mapping table (HIGH 0-5, MEDIUM 6-7, LOW 8-10)
- Expected test results: 201 for success, 400 for errors, RFC 7807 format
- Local and production environment setup prerequisites
- Troubleshooting section covering common failures
- API contract summary with request/response examples

The README correctly notes that the list endpoint is not yet deployed and documents the known limitation. It also mentions that API Key authentication was removed from the template and is not required in the current deployment.

### Identical Local/Prod Structure

Both environment folders contain the exact same test requests with identical bodies and test scripts. Only the environment selector differs (setting baseUrl to local vs prod). This means users can run "Selecionar ambiente local", execute all 12 test cases, then run "Selecionar ambiente prod" and execute the same 12 cases against production without any request changes.

### Verification Against the Plan

The implementation satisfies all 11 tasks from the plan:
1. ✅ Directory structure created with valid v2.1.0 schema
2. ✅ Collection variables defined (localUrl, prodUrl, baseUrl, feedbackId, timestamp)
3. ✅ Collection-level pre-request script sets timestamp
4. ✅ "Selecionar ambiente local" request sets baseUrl to localUrl
5. ✅ "Selecionar ambiente prod" request sets baseUrl to prodUrl
6. ✅ 12 operation test cases created (POST requests with correct bodies, test scripts)
7. ✅ List endpoint noted as coming soon (not yet deployed)
8. ✅ README.md created with all required sections
9. ✅ JSON schema valid (Postman v2.1.0)
10. ✅ Structure mirrors fase-3 model exactly
11. ✅ All test assertions match actual API contract (201 + receiveFeedback wrapper, 400 + RFC 7807)

</details>

---

## File Map

<details>
<summary>Files Changed</summary>

- **fase4-feedback.postman_collection.json** — Postman Collection v2.1.0 with 24 POST requests (12 test cases × 2 environments), collection variables, environment selectors, and RFC 7807 error validation scripts. All requests target `/avaliacao` with plain JSON bodies matching the actual API contract.

- **README.md** — Comprehensive guide covering import (Postman/Bruno), execution workflow, collection variables, test case descriptions, expected results, urgency mapping, local/prod prerequisites, troubleshooting, and API contract examples.

</details>
