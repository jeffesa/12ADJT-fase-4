# LocalStack Development Environment Setup

## Overview

This directory contains the Docker Compose configuration and initialization scripts for running **fase-4** locally using [LocalStack](https://www.localstack.cloud/) — a fully-featured AWS cloud environment emulator.

## Files

- **`docker-compose.yml`**: Orchestrates LocalStack container with all necessary AWS services (DynamoDB, SNS, SQS, Lambda, API Gateway, EventBridge, SES)
- **`infra/localstack-init.sh`**: Auto-executed initialization script that creates DynamoDB tables, SNS topics, and SQS queues
- **`run.sh`**: Main orchestration script that validates dependencies, starts LocalStack, and prepares the dev environment
- **`.dockerignore`**: Optimizes Docker image builds by excluding unnecessary files

## Quick Start

### 1. Start LocalStack

```bash
./run.sh
```

This script will:
- Validate Docker, Docker Compose, and Maven installations
- Start LocalStack container (background)
- Initialize DynamoDB tables, SNS topics, and SQS queues
- Compile the Quarkus modules

### 2. Run Each Module (in separate terminals)

After `./run.sh` completes, start each Lambda function in dev mode:

```bash
# Terminal 1
cd receive-feedback
mvn quarkus:dev

# Terminal 2
cd send-notification
mvn quarkus:dev

# Terminal 3
cd generate-report
mvn quarkus:dev
```

### 3. Stop Environment

```bash
docker-compose down
```

## LocalStack Endpoints

All AWS services are accessible at the edge service endpoint:

- **Edge Service**: `http://localhost:4566` (all APIs)
- **DynamoDB**: `http://localhost:4569`
- **SNS**: `http://localhost:4575`
- **SQS**: `http://localhost:4576`

## Configuration

### Application Properties

Each module's `application.properties` contains LocalStack-specific configuration under the `%dev` profile:

```properties
# DynamoDB (LocalStack)
%dev.quarkus.dynamodb.endpoint-override=http://localhost:4566
%dev.quarkus.dynamodb.aws.region=us-east-1
%dev.quarkus.dynamodb.aws.credentials.type=static
%dev.quarkus.dynamodb.aws.credentials.static-provider.access-key-id=test
%dev.quarkus.dynamodb.aws.credentials.static-provider.secret-access-key=test

# SNS (LocalStack)
%dev.quarkus.sns.endpoint-override=http://localhost:4566
%dev.quarkus.sns.aws.region=us-east-1
```

When running locally, set `quarkus.profile=dev` to use these overrides.

### Default Credentials

LocalStack uses test credentials for local development:

- **Access Key ID**: `test`
- **Secret Access Key**: `test`
- **Region**: `us-east-1`

## Debugging

### View LocalStack Logs

```bash
docker-compose logs -f localstack
```

### List Created Resources

```bash
# List DynamoDB tables
docker-compose exec localstack awslocal dynamodb list-tables

# List SNS topics
docker-compose exec localstack awslocal sns list-topics

# List SQS queues
docker-compose exec localstack awslocal sqs list-queues
```

### Test AWS CLI Commands

All `aws` commands can be replaced with `awslocal` when inside the LocalStack container:

```bash
docker-compose exec localstack awslocal dynamodb scan --table-name feedbacks
docker-compose exec localstack awslocal sns publish --topic-arn arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic --message "Test"
```

## Troubleshooting

### LocalStack Container Won't Start

Check Docker daemon is running:

```bash
docker ps
```

If Docker isn't running, start Docker Desktop (macOS) or Docker daemon (Linux).

### Port Conflicts

If ports 4566, 4569, 4575, 4576 are already in use:

1. Find and stop the process using the port:
   ```bash
   lsof -i :4566
   ```
2. Or modify `docker-compose.yml` port mappings

### Resources Not Initialized

If DynamoDB tables or SNS topics aren't created:

1. Check LocalStack is healthy:
   ```bash
   docker-compose ps
   ```
2. View initialization logs:
   ```bash
   docker-compose logs localstack | grep "Init:"
   ```
3. Manually run init script:
   ```bash
   docker-compose exec localstack bash /docker-entrypoint-initaws.d/init-aws.sh
   ```

## Production Deployment

When deploying to AWS (via SAM), the `%prod` profile uses IAM role credentials and does not require LocalStack:

```properties
# prod: AWS IAM Role (no explicit credentials needed)
%prod.quarkus.dynamodb.aws.region=${AWS_REGION:us-east-1}
%prod.quarkus.sns.aws.region=${AWS_REGION:us-east-1}
```

See `docs/planejamento/BACKLOG.md` → TASK-019 for production deployment.

## References

- [LocalStack Documentation](https://docs.localstack.cloud/)
- [AWS Services Emulated](https://docs.localstack.cloud/user-guide/aws-feature-coverage/)
- [Quarkus AWS Services](https://quarkiverse.github.io/quarkiverse-docs/quarkus-amazon-services/)
