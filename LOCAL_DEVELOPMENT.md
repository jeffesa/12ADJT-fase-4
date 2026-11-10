# Local Development with Quarkus Dev Mode

This guide explains how to set up local development for Fase-4 using Quarkus Dev Mode with hot reload, and how to test against AWS for integration testing.

## Quick Start

### Prerequisites

- **Java 17+** (verify: `java -version`)
- **Maven 3.8+** (verify: `mvn -version`)
- **AWS CLI** (optional, for AWS integration testing)

### 1. Start Quarkus Dev Mode (3 terminals)

Each module runs on a separate port with hot reload enabled.

**Terminal 1 — receive-feedback (port 8080):**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/receive-feedback
mvn quarkus:dev
```

**Terminal 2 — send-notification (port 8081):**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/send-notification
mvn quarkus:dev
```

**Terminal 3 — generate-report (port 8082):**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/generate-report
mvn quarkus:dev
```

### 2. Verify Modules Are Running

Once all three are running, check their health:

```bash
# Should return 200/404/405 (endpoints exist)
curl -v http://localhost:8080
curl -v http://localhost:8081
curl -v http://localhost:8082
```

Or use the convenience script:

```bash
./run.sh health
```

### 3. Integration Testing on AWS

All integration tests point to the live AWS deployment:

```
https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/
```

Use Postman or curl to test:

```bash
curl -X POST https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/feedback \
  -H "Content-Type: application/json" \
  -d '{"descricao": "Test feedback", "nota": 5}'
```

## Hot Reload Workflow

Quarkus Dev Mode provides instant hot reload of code changes:

1. Edit a Java source file (e.g., `ReceiveFeedbackUseCase.java`)
2. **Save the file** — Maven detects the change
3. **Automatic compilation & reload** — Within 1-2 seconds
4. **Test immediately** — No restart needed

### Example: Adding a Log Statement

```java
// In receive-feedback/src/main/java/com/fiap/feedback/application/usecase/ReceiveFeedbackUseCase.java
@Override
public Feedback receiveFeedback(String description, Integer rating) {
    LOGGER.info("Received feedback: " + description); // Add this line
    // ... rest of code
}
```

Save the file → Hot reload triggered → Log appears in terminal output.

## Stopping Modules

To stop any module, press **Ctrl+C** in its terminal.

```bash
# Terminal 1
^C

# Terminal 2
^C

# Terminal 3
^C
```

To verify they've stopped:

```bash
curl http://localhost:8080
# Should fail: Connection refused
```

## Troubleshooting

### 1. Port Already in Use

If you see `Address already in use`:

**Find and kill the process:**
```bash
# macOS/Linux
lsof -i :8080  # Find what's using port 8080
kill -9 <PID>

# Or kill all Java processes (use with caution)
pkill -f "java.*quarkus:dev"
```

**Or use different ports:**
```bash
cd receive-feedback
mvn quarkus:dev -Dquarkus.http.port=9080
```

### 2. Memory Issues

If Maven runs out of memory during compilation:

```bash
export MAVEN_OPTS="-Xmx2g"
mvn quarkus:dev
```

### 3. Classpath or Missing Dependency

If you see `ClassNotFoundException` or `NoClassDefFoundError`:

```bash
# Clean and rebuild
cd receive-feedback
mvn clean compile
mvn quarkus:dev
```

### 4. Module Won't Start (Logs Show Errors)

Common causes:

- **Missing AWS credentials**: Set `AWS_REGION` and `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
  ```bash
  export AWS_REGION=us-east-1
  export AWS_ACCESS_KEY_ID=test
  export AWS_SECRET_ACCESS_KEY=test
  mvn quarkus:dev
  ```

- **DynamoDB table not found**: Expected when running locally without AWS—use `%dev` profile config
  - Check `src/main/resources/application.properties` for dev config
  - Errors are safe; service is designed for AWS

- **GraphQL schema not loading**: Restart the module
  ```bash
  Ctrl+C
  mvn quarkus:dev
  ```

## Integration Testing Against AWS

### Using Postman

1. Open `docs/api-collection/fase4-feedback.postman_collection.json` in Postman
2. In the collection, set the `prodUrl` variable to:
   ```
   https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod
   ```
3. Run the `prod` folder requests

### Using curl

```bash
# Create feedback
curl -X POST https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "descricao": "Testing integration",
    "nota": 7
  }'

# Expected response (201 Created):
# {
#   "receiveFeedback": {
#     "id": "uuid-here",
#     "descricao": "Testing integration",
#     "nota": 7,
#     "urgencia": "MEDIUM",
#     "dataCriacao": "2024-11-09T..."
#   }
# }
```

### Batch Testing with Newman

```bash
# Install Newman (one-time)
npm install -g newman

# Run the Postman collection against AWS
newman run docs/api-collection/fase4-feedback.postman_collection.json \
  --folder prod \
  -r htmlextra \
  --reporter-htmlextra-export results.html

# View results
open results.html
```

## Running Tests

### Unit & Integration Tests

```bash
# All modules
mvn clean verify

# Specific module
mvn clean verify -pl receive-feedback

# With coverage report
open target/site/jacoco/index.html
```

### E2E Tests

```bash
# Full end-to-end test suite
./run.sh e2e-test

# Verify results
./run.sh e2e-verify
```

## Local Development Strategy

| Scenario | Tool | Command |
|----------|------|---------|
| **Edit code & test locally** | Quarkus Dev Mode | `mvn quarkus:dev` in 3 terminals |
| **Run unit tests** | Maven/JUnit | `mvn test` or `mvn test -pl receive-feedback` |
| **Check integration with AWS** | Postman/curl | Test against `prod` endpoint |
| **Full E2E test** | bash script | `./run.sh e2e-test` |
| **Code coverage** | JaCoCo | `mvn verify && open target/site/jacoco/index.html` |

## Architecture Notes

### Why Quarkus Dev Mode, Not LocalStack?

1. **Reliability**: LocalStack 3.0 crashes after ~5 operations; Quarkus dev mode is stable
2. **No Mocking**: Tests run against real AWS, which validates the actual deployment
3. **Simplicity**: No Docker setup needed; just `mvn quarkus:dev`
4. **Hot Reload**: Instant feedback loop for development
5. **Academic Standard**: For a FIAP project, using AWS directly is more authentic and demonstrates cloud-native design

### Local vs. Production

- **Local (Dev Mode)**: Use for rapid iteration, debugging, hot reload
- **Production (AWS)**: Use for integration testing, E2E tests, final verification
- **Tests**: Run locally; verify against AWS before commit

## Module Overview

### receive-feedback (port 8080)

- **Handler**: REST endpoint for POST /feedback (create)
- **Role**: Validates feedback, calculates urgency, stores in DynamoDB, publishes to SNS
- **Dependencies**: DynamoDB, SNS
- **Test**: `curl -X POST http://localhost:8080/...`

### send-notification (port 8081)

- **Handler**: Consumes SNS events from receive-feedback
- **Role**: Sends email notifications for HIGH urgency feedback
- **Dependencies**: SNS (consume), SES (send email)
- **Test**: Via SNS event (integration test)

### generate-report (port 8082)

- **Handler**: Consumes EventBridge events
- **Role**: Aggregates metrics and generates summary reports
- **Dependencies**: EventBridge, DynamoDB
- **Test**: Via EventBridge (integration test)

## Next Steps

- Edit source code in an IDE (e.g., VS Code, IntelliJ)
- Save changes → watch hot reload in terminal
- Test against `http://localhost:8080` (local) or AWS endpoint (integration)
- Run `mvn test` before committing
- Deploy to AWS via SAM when ready

For more details on building and deploying, see the main [README.md](README.md).
