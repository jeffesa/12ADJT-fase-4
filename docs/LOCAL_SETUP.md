# Local Development Setup Guide

This guide covers setting up the feedback-platform serverless application for local development and testing using LocalStack to simulate AWS services.

## Overview

LocalStack is a fully functional local AWS cloud stack that allows you to develop and test serverless applications without connecting to AWS. This platform uses:

- **DynamoDB**: Stores feedback records with primary key (feedbackId, createdAt)
- **SNS**: Publishes notifications for high-urgency feedback to `feedback-urgent-topic`
- **SQS**: Queues for asynchronous message processing
- **SES**: Simulates email sending (mocked in LocalStack)
- **Lambda**: Simulated via Quarkus dev mode with event triggering
- **CloudWatch**: Logs for monitoring and debugging

## Prerequisites

Ensure your system has the following installed:

### Required Software

- **Docker**: Version 20.10 or later
  - macOS: Install [Docker Desktop](https://www.docker.com/products/docker-desktop)
  - Linux: `sudo apt-get install docker.io docker-compose`
  - Windows: Install [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)

- **Docker Compose**: Version 1.29 or later
  - Usually bundled with Docker Desktop
  - Verify: `docker compose version`

- **Java 17**: For building and running the modules
  - Download from [Amazon Corretto](https://aws.amazon.com/corretto/) or [Adoptium](https://adoptium.net/)
  - Verify: `java -version`

- **Maven 3.8+**: For project compilation and testing
  - Download from [Apache Maven](https://maven.apache.org/download.cgi)
  - Verify: `mvn --version`

- **Git**: For version control
  - macOS: `brew install git`
  - Linux: `sudo apt-get install git`
  - Windows: [Git for Windows](https://gitforwindows.org/)

### Recommended Tools

- **curl**: For testing GraphQL endpoints manually
  - Usually pre-installed on macOS/Linux
  - Windows: Install [Git Bash](https://gitforwindows.org/) or [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install)

- **jq**: For parsing JSON in shell scripts
  - macOS: `brew install jq`
  - Linux: `sudo apt-get install jq`
  - Windows: `choco install jq` or use [pre-built binaries](https://stedolan.github.io/jq/download/)

- **AWS CLI v2**: For direct interaction with LocalStack
  - Install from [AWS CLI Documentation](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
  - Verify: `aws --version`

## Quick Start (5 minutes)

### Step 1: Clone and Navigate to Repository

```bash
cd /Users/jefferson/Documents/FIAP/fase-4
```

### Step 2: Start LocalStack

```bash
./run.sh docker
```

This command:
- Starts the LocalStack container with all AWS services
- Automatically runs the initialization script (`infra/localstack-init.sh`) to create DynamoDB tables, SNS topics, and SQS queues
- Outputs available endpoints after services are ready

Expected output:
```
✅ LocalStack pronto!

📍 Endpoints LocalStack:
   Edge:      http://localhost:4566
   DynamoDB:  http://localhost:4569
   SNS:       http://localhost:4575
   SQS:       http://localhost:4576
```

### Step 3: Verify LocalStack Health

In a second terminal:

```bash
./run.sh health
```

Expected output:
```
✅ LocalStack (edge:4566): UP
```

### Step 4: Start the Three Modules

Open three separate terminals and run each command in its own terminal:

**Terminal 1 - Receive Feedback Module:**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/receive-feedback
mvn quarkus:dev
```

**Terminal 2 - Send Notification Module:**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/send-notification
mvn quarkus:dev
```

**Terminal 3 - Generate Report Module:**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/generate-report
mvn quarkus:dev
```

Each module will:
- Compile and start a Quarkus development server
- Display the listening port (usually 8080, 8081, 8282)
- Monitor file changes for live reload
- Connect to LocalStack via configured endpoints

### Step 5: Run E2E Tests

In the original terminal (or a new one):

```bash
./run.sh e2e-setup
```

This verifies all resources are ready:
- Checks DynamoDB table existence
- Verifies SNS topics
- Confirms SQS queues
- Tests SES email identities

Then run the test flows:

```bash
./run.sh e2e-test
```

Finally, verify results:

```bash
./run.sh e2e-verify
```

## LocalStack Configuration

### Endpoints and Services

All AWS services are available at the edge endpoint with port 4566:

| Service | Port | Endpoint |
|---------|------|----------|
| Edge (All Services) | 4566 | http://localhost:4566 |
| DynamoDB | 4569 | http://localhost:4569 |
| SNS | 4575 | http://localhost:4575 |
| SQS | 4576 | http://localhost:4576 |

### Environment Variables

The following environment variables are configured automatically:

```bash
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_ENDPOINT_URL_DYNAMODB=http://localhost:4569
AWS_ENDPOINT_URL_SNS=http://localhost:4575
AWS_ENDPOINT_URL_SQS=http://localhost:4576
AWS_ENDPOINT_URL_SES=http://localhost:4566
```

These are set in:
- `docker-compose.yml` (LocalStack container environment)
- Quarkus `application.properties` (per module)
- `.env` files (if present)

### AWS Credentials

Test credentials (valid only with LocalStack):

```
Access Key ID: test
Secret Access Key: test
Region: us-east-1
```

**Security Note**: These credentials are only valid locally and have no real AWS permissions. Never use these in production.

## Resources Created by LocalStack Initialization

When LocalStack starts, the initialization script (`infra/localstack-init.sh`) automatically creates:

### DynamoDB Table: `feedbacks`

**Primary Key:**
- Partition Key (HASH): `feedbackId` (String)
- Sort Key (RANGE): `createdAt` (String)

**Attributes:**
- `feedbackId`: UUID of the feedback (auto-generated)
- `descricao`: Feedback description text
- `nota`: Numeric rating (0-10)
- `urgencia`: Urgency level (HIGH, MEDIUM, LOW)
- `createdAt`: ISO 8601 timestamp

**Billing Mode**: Pay-per-request (no provisioned capacity needed)

### SNS Topics

- `feedback-urgent-topic`: Receives messages when HIGH-urgency feedback is submitted
- `feedback-report-topic`: Receives scheduled report messages

### SQS Queues

- `notification-queue`: Processes notifications (subscribed to `feedback-urgent-topic`)
- `report-queue`: Processes report generation requests

## Quarkus Development Mode

Each module runs in Quarkus dev mode, which provides:

- **Live Reload**: Changes to Java code are automatically compiled and deployed
- **GraphQL Hot Reload**: Schema changes take effect without restart
- **Dev UI**: Accessible at `http://localhost:8080/q/dev`
- **Logging**: Console output shows all requests and errors

### Quarkus Module Ports

By default:
- **receive-feedback**: http://localhost:8080/graphql
- **send-notification**: http://localhost:8081/graphql (or background Lambda)
- **generate-report**: http://localhost:8282/graphql

Check the module's console output for the exact port.

### Module Dependencies

The modules are designed to work together via:

1. **Receive Feedback** → persists to DynamoDB, publishes to SNS if HIGH urgency
2. **SNS Event** → triggers Send Notification Lambda
3. **Send Notification** → sends email via SES
4. **Generate Report** → queries DynamoDB, sends summary email

### Application Properties

Each module has an `application.properties` file in `src/main/resources`:

```properties
# LocalStack endpoints
quarkus.dynamodb.endpoint-override=http://localhost:4569
quarkus.sns.endpoint-override=http://localhost:4575
quarkus.sqs.endpoint-override=http://localhost:4576
quarkus.ses.endpoint-override=http://localhost:4566

# AWS credentials
aws.region=us-east-1
aws.credentials.static.provider.access-key-id=test
aws.credentials.static.provider.secret-access-key=test

# Email configuration (SES)
notification.sender-email=noreply@feedback-fase-4.com
notification.admin-email=admin@feedback-fase-4.com
notification.ses-region=us-east-1
```

## Docker Compose Setup

The `docker-compose.yml` file orchestrates all services:

### LocalStack Service

- **Image**: `localstack/localstack:3.0` (latest stable)
- **Ports**: 4566 (edge), 4569 (DynamoDB), 4575 (SNS), 4576 (SQS)
- **Volumes**: LocalStack state persists in `localstack-data` volume
- **Init Script**: Runs `infra/localstack-init.sh` on startup
- **Network**: `fase4-network` (bridge network for inter-service communication)

### Network

- **Driver**: Bridge network (`fase4-network`)
- **Purpose**: Allows Quarkus modules (running on host) to communicate with LocalStack container

## Common Issues and Troubleshooting

### Port Conflicts

If you get "Port already in use" errors:

**Problem**: Another service is using the LocalStack ports (4566, 4569, 4575, 4576)

**Solution 1**: Stop the conflicting service
```bash
# Find what's using port 4566
lsof -i :4566

# Kill the process (macOS/Linux)
kill -9 <PID>
```

**Solution 2**: Use different ports in `docker-compose.yml`
```yaml
ports:
  - "4566:4566"  # Change first number to different port
```

**Solution 3**: Clean up and restart
```bash
./run.sh clean
./run.sh docker
```

### Docker Not Running

**Problem**: "Cannot connect to Docker daemon"

**Solution - macOS (Docker Desktop)**:
```bash
# Start Docker Desktop via GUI or CLI
open /Applications/Docker.app

# Or with Colima
colima start
```

**Solution - Linux**:
```bash
sudo systemctl start docker
sudo usermod -aG docker $USER
# Log out and back in for group changes to take effect
```

### Modules Won't Connect to LocalStack

**Problem**: "Connection refused" when modules try to reach LocalStack

**Cause**: Modules might be using wrong endpoint or LocalStack isn't ready

**Solution**:
1. Verify LocalStack is running: `./run.sh health`
2. Check endpoints in module's `application.properties`
3. Ensure modules are using `http://localhost:4566` (not IP)
4. Wait longer for LocalStack startup (sometimes takes 30+ seconds)

**Debug**:
```bash
# Check if LocalStack is responding
curl -v http://localhost:4566

# Check module logs for connection errors
# Look in Terminal 1, 2, or 3 where you ran mvn quarkus:dev
```

### LocalStack Services Not Initialized

**Problem**: DynamoDB table, SNS topics not found

**Cause**: Initialization script didn't run or failed

**Solution**:
```bash
# Check LocalStack logs
./run.sh logs

# Look for lines like:
# "LocalStack Init: Recursos criados com sucesso"
# Or error lines starting with "ERR"

# Manually run init script if needed
docker compose exec localstack /docker-entrypoint-initaws.d/init-aws.sh
```

### Tests Timeout

**Problem**: E2E test script hangs or times out

**Cause**: Modules not responding, GraphQL endpoint not available

**Solution**:
1. Verify modules are running: Check Terminals 1, 2, 3
2. Test endpoint manually:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ __typename }"}'
```
3. Check module logs for startup errors
4. Restart the module that's not responding

## Cleanup

To remove LocalStack state and reset for testing:

```bash
# Stop all containers and remove volumes
./run.sh clean

# This removes all DynamoDB data, SNS messages, etc.
# Useful before running a fresh test cycle
```

## Verify Setup is Complete

Run this command to verify everything is working:

```bash
./run.sh health && ./run.sh resources
```

Expected output:

```
🩺 Verificando saúde dos serviços...
  ✅ LocalStack (edge:4566): UP

📊 Listando recursos no LocalStack...

  📦 DynamoDB Tables:
     - feedbacks

  📨 SNS Topics:
     - arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic
     - arn:aws:sns:us-east-1:000000000000:feedback-report-topic

  📬 SQS Queues:
     - http://localhost:4576/000000000000/notification-queue
     - http://localhost:4576/000000000000/report-queue
```

If all resources are present and all endpoints are UP, your local setup is complete!

## Next Steps

After completing this setup:

1. **Run E2E Tests**: Follow the steps in [E2E_TESTING_GUIDE.md](./E2E_TESTING_GUIDE.md)
2. **Review Test Results**: Check [E2E_TEST_RESULTS.md](./E2E_TEST_RESULTS.md)
3. **Develop Features**: The modules are now ready for development with hot reload

## References

- [LocalStack Documentation](https://docs.localstack.cloud/)
- [Quarkus AWS Services](https://quarkus.io/guides/amazon-services)
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [DynamoDB Local Development](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html)

## Support

For issues or questions:

1. Check the troubleshooting section above
2. Review LocalStack logs: `./run.sh logs`
3. Check module logs in their respective terminals
4. Review generated test logs in `/tmp/e2e-logs/`
