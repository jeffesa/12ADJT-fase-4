# FIAP Fase 4 - Bruno Collection

## Overview

Bruno collection para testar a Fase-4 Feedback Platform API.

**Architecture:** Lambda-only (AWS API Gateway)
- ✅ Endpoints funcionam em produção (AWS)
- ❌ Endpoints NÃO funcionam localmente (Lambda-only, sem REST expostos)

## Environments

### prod
- **URL:** `https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod`
- **Status:** ✅ Ativo e testado
- Teste com: `bruno run [request] --env prod`

### local
- **URL:** `http://localhost:8080`
- **Status:** ⚠️ App sobe, mas sem endpoints REST
- Nota: Arquitetura Lambda-only, sem REST expostos localmente
- Para testar local, use testes unitários/integração

## Requests

### 1. POST /feedback - HIGH urgência
- **Path:** `POST_feedback_HIGH.bru`
- **Data:** `{"descricao": "...", "nota": 1}`
- **Expected:** HTTP 201, `urgencia: "HIGH"`
- **Test:** `curl -X POST https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod/feedback -H "Content-Type: application/json" -d '{"descricao":"test","nota":1}'`

### 2. POST /feedback - MEDIUM urgência
- **Path:** `POST_feedback_MEDIUM.bru`
- **Data:** `{"descricao": "...", "nota": 6}`
- **Expected:** HTTP 201, `urgencia: "MEDIUM"`

### 3. POST /feedback - LOW urgência
- **Path:** `POST_feedback_LOW.bru`
- **Data:** `{"descricao": "...", "nota": 9}`
- **Expected:** HTTP 201, `urgencia: "LOW"`

### 4. POST /feedback - Invalid (nota -1)
- **Path:** `POST_feedback_invalid.bru`
- **Data:** `{"descricao": "...", "nota": -1}`
- **Expected:** HTTP 400, validation error

### 5. GET /feedback - List all
- **Path:** `GET_feedback_list.bru`
- **Expected:** HTTP 200, array of feedbacks with total count

## Urgency Logic

- **0-5:** HIGH
- **6-7:** MEDIUM
- **8-10:** LOW

## Using Bruno

1. Open Bruno (GUI)
2. Import this collection: `docs/api-collection/bruno/`
3. Select environment: `prod` or `local`
4. Run requests with test assertions

## Tested Endpoints

| Endpoint | Method | Status | Evidence |
|---|---|---|---|
| `/feedback` | POST (HIGH) | ✅ 201 | UUID generated, urgencia HIGH |
| `/feedback` | POST (MEDIUM) | ✅ 201 | UUID generated, urgencia MEDIUM |
| `/feedback` | POST (LOW) | ✅ 201 | UUID generated, urgencia LOW |
| `/feedback` | POST (invalid) | ✅ 400 | Validation error |
| `/feedback` | GET | ✅ 200 | List with 15+ items |

## Local Development

**Note:** The app is Lambda-only architecture. For local testing:

1. Compile: `mvn clean package -DskipTests`
2. Start: `java -jar receive-feedback/target/receive-feedback-1.0.0-runner.jar`
3. App starts but has NO HTTP REST endpoints (by design)
4. Use unit/integration tests instead of HTTP tests

## Notes

- All tests verified with `curl` manually
- Bruno collection ready for UI testing
- Production endpoint fully operational
- Local environment for compilation/startup only
