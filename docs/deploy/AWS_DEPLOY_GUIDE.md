# AWS Deployment Guide

This guide walks you through deploying the FIAP Fase-4 Serverless Feedback Platform to AWS using AWS SAM (Serverless Application Model).

## Prerequisites

Before you begin, ensure you have the following installed and configured:

### 1. AWS Account and Credentials

- An active AWS account with appropriate permissions to create Lambda, API Gateway, DynamoDB, SNS, IAM roles, and CloudWatch resources.
- AWS credentials configured locally via one of these methods:
  - AWS CLI: `aws configure` (stores credentials in `~/.aws/credentials`)
  - Environment variables: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_DEFAULT_REGION`
  - AWS Single Sign-On (SSO): `aws sso login`

### 2. AWS SAM CLI

Install the AWS Serverless Application Model CLI:

```bash
# macOS (using Homebrew)
brew tap aws/tap
brew install aws-sam-cli

# Linux
pip install aws-sam-cli

# Windows
choco install aws-sam-cli
```

Verify installation:
```bash
sam --version
```

### 3. Java 17 and Maven

The Lambda functions are built with Java 17 and Maven.

```bash
# Install Java 17
# macOS (Homebrew)
brew install openjdk@17

# Verify Java version
java -version  # Should output Java 17.x.x

# Verify Maven
mvn -version
```

### 4. Docker (for SAM Local Testing)

Optional but recommended for local testing:

```bash
docker --version
```

## Project Structure

The project is organized as follows:

```
.
├── infra/
│   └── template.yaml              # SAM CloudFormation template
├── receive-feedback/
│   ├── src/
│   ├── pom.xml
│   └── target/function.zip        # Built Lambda deployment package
├── send-notification/
│   ├── src/
│   ├── pom.xml
│   └── target/function.zip        # Built Lambda deployment package
├── generate-report/
│   ├── src/
│   ├── pom.xml
│   └── target/function.zip        # Built Lambda deployment package
├── pom.xml                        # Parent POM for multi-module build
└── docs/
    └── deploy/
        └── AWS_DEPLOY_GUIDE.md    # This file
```

## Building the Lambda Functions

Before deploying, build all three Lambda functions using Maven:

```bash
# From the project root directory
mvn clean verify

# This will:
# 1. Compile all three modules (receive-feedback, send-notification, generate-report)
# 2. Run unit tests
# 3. Generate JaCoCo coverage reports
# 4. Package each function as a ZIP file in target/function.zip
```

Verify the built artifacts:

```bash
ls -la receive-feedback/target/function.zip
ls -la send-notification/target/function.zip
ls -la generate-report/target/function.zip
```

## Deploying to AWS

### Step 1: Prepare Your Environment Parameters

Before deploying, decide on your environment settings:

- **Environment**: `dev` or `prod` (default: `dev`)
- **Admin Email**: An email address to receive notifications and reports (must be verified in SES for production)
- **Region**: AWS region for deployment (default: `us-east-1`)

### Step 2: Run SAM Deploy (Guided)

For first-time deployment, use the guided mode:

```bash
cd infra

sam deploy --guided \
  --stack-name fiap-feedback-platform-dev \
  --parameter-overrides \
      Environment=dev \
      AdminEmail=admin@example.com \
      Region=us-east-1
```

The `--guided` flag will prompt you interactively for:

1. **Stack Name**: Name your CloudFormation stack (e.g., `fiap-feedback-platform-dev`)
2. **AWS Region**: Confirm the region (default: `us-east-1`)
3. **Parameter Prompts**:
   - `Environment`: Enter `dev` or `prod`
   - `AdminEmail`: Enter a verified email address
   - `Region`: Confirm AWS region
4. **Capabilities**: When prompted, confirm that SAM creates IAM roles by entering `y`
5. **Save Configuration**: When prompted, enter `y` to save settings to `samconfig.toml`

Example output:
```
Creating CloudFormation changeset for stack 'fiap-feedback-platform-dev'...
⏳ Waiting for changeset to be created...
✅ Changeset created successfully!
⏳ Deploying changeset...
2024-10-27 15:30:45 - CREATE_IN_PROGRESS
...
2024-10-27 15:35:10 - CREATE_COMPLETE
Stack creation succeeded. Outputs:
  - ApiEndpoint: https://abc123.execute-api.us-east-1.amazonaws.com/dev/avaliacao
  - ApiKey: xyz789
```

### Step 3: Retrieve Deployment Outputs

After successful deployment, retrieve the output values:

```bash
aws cloudformation describe-stacks \
  --stack-name fiap-feedback-platform-dev \
  --query 'Stacks[0].Outputs'
```

Important outputs:
- **ApiEndpoint**: The HTTP endpoint for submitting feedback
- **ApiKey**: Required API key for authentication
- **DynamoDBTableName**: The name of the DynamoDB table
- **SNSTopicArn**: The SNS topic for urgent feedback notifications

Save these values for testing and monitoring.

## Deploying to Production

When deploying to production (`prod`), follow these best practices:

### 1. Email Verification

In production, verify your admin email address in Amazon SES:

```bash
aws ses verify-email-identity \
  --email-address admin@example.com \
  --region us-east-1
```

Check your email for the verification link.

### 2. Deploy with Production Parameters

```bash
cd infra

sam deploy \
  --stack-name fiap-feedback-platform-prod \
  --parameter-overrides \
      Environment=prod \
      AdminEmail=admin@example.com \
      Region=us-east-1 \
  --capabilities CAPABILITY_IAM
```

### 3. Verify Production Deployment

```bash
aws cloudformation describe-stacks \
  --stack-name fiap-feedback-platform-prod \
  --query 'Stacks[0].StackStatus'
```

Expected output: `CREATE_COMPLETE` or `UPDATE_COMPLETE`

## Environment Configuration

### Development Environment (`dev`)

- **Resource Names**: Prefixed with `dev-`
- **Lambda Memory**: 1024 MB
- **Lambda Timeout**: 30 seconds
- **DynamoDB**: On-demand billing
- **CloudWatch Logs Retention**: 30 days
- **API Gateway Throttle**:
  - Burst Limit: 100 requests
  - Rate Limit: 1,000 requests/second
  - Quota: 10,000 requests/day

### Production Environment (`prod`)

- **Resource Names**: Prefixed with `prod-`
- **Lambda Memory**: 1024 MB
- **Lambda Timeout**: 30 seconds
- **DynamoDB**: On-demand billing
- **CloudWatch Logs Retention**: 30 days
- **API Gateway Throttle**:
  - Burst Limit: 100 requests
  - Rate Limit: 1,000 requests/second
  - Quota: 10,000 requests/day
- **Email Verification**: Required for SES sendEmail permissions
- **Reserved Concurrency**: Limited to 10 for receive-feedback and send-notification, 1 for generate-report

### API Key Management

An API key is automatically created for each environment:

#### Retrieve the API Key

```bash
aws apigateway get-api-keys \
  --query 'items[?name==`dev-feedback-api-key`].id' \
  --output text
```

Then get the actual key value:

```bash
KEY_ID=<output-from-above>
aws apigateway get-api-key \
  --id $KEY_ID \
  --include-value
```

#### Using the API Key

Include the API key in requests as a header:

```bash
curl -X POST https://<api-endpoint>/avaliacao \
  -H "x-api-key: <your-api-key>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "rating": 5,
    "urgency": "HIGH",
    "comment": "Excellent service"
  }'
```

#### Rotating the API Key

To disable an old key and create a new one:

```bash
# Disable the old key
aws apigateway update-api-key \
  --id <old-key-id> \
  --patch-operations op=replace,path=/enabled,value=false

# Create a new API key
aws apigateway create-api-key \
  --name dev-feedback-api-key-new \
  --enabled
```

### API Gateway Usage Plan

The Usage Plan enforces rate limiting and quotas:

- **Throttle**: Prevents excessive burst requests
- **Quota**: Limits total daily requests

View the usage plan details:

```bash
aws apigateway get-usage-plans \
  --query 'items[?name==`dev-feedback-usage-plan`]'
```

## The Three Lambda Functions

### 1. ReceiveFeedbackFunction

**Trigger**: HTTP POST request to `/avaliacao` via API Gateway  
**Handler**: `com.fiap.feedback.infra.handler.ReceiveFeedbackHandler`  
**Permissions**: Write to DynamoDB, Publish to SNS

**Flow**:
1. Receives feedback data (userId, rating, urgency, comment)
2. Writes feedback to DynamoDB table
3. If urgency is HIGH, publishes a message to the SNS topic
4. Returns success response

**Example Request**:
```bash
curl -X POST https://<api-endpoint>/avaliacao \
  -H "x-api-key: <your-api-key>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "rating": 5,
    "urgency": "HIGH",
    "comment": "Great experience!"
  }'
```

**Monitoring**:
- CloudWatch Log Group: `/aws/lambda/dev-receive-feedback`
- Errors: Check for DynamoDB write failures or SNS publish failures

### 2. SendNotificationFunction

**Trigger**: SNS message from FeedbackUrgentTopic  
**Handler**: `com.fiap.notification.infra.handler.SendNotificationHandler`  
**Permissions**: Send emails via SES

**Flow**:
1. Receives SNS message with HIGH urgency feedback
2. Sends email notification to admin email address via SES
3. Logs completion

**Monitoring**:
- CloudWatch Log Group: `/aws/lambda/dev-send-notification`
- SES Sandbox: In development, you must verify sender and recipient emails
- Production: Verify admin email in SES to send without sandbox restrictions

### 3. GenerateReportFunction

**Trigger**: EventBridge scheduled rule (Monday 8 AM UTC)  
**Handler**: `com.fiap.report.infra.handler.GenerateReportHandler`  
**Permissions**: Scan DynamoDB, Send emails via SES

**Flow**:
1. Triggered by EventBridge cron schedule
2. Scans DynamoDB table for all feedback
3. Generates weekly report (summary, statistics)
4. Sends report email to admin via SES

**Schedule**: `cron(0 8 ? * MON *)` = Every Monday at 8:00 AM UTC

**Monitoring**:
- CloudWatch Log Group: `/aws/lambda/dev-generate-report`
- Manual Trigger: Use AWS Lambda console or CLI:
  ```bash
  aws lambda invoke \
    --function-name dev-generate-report \
    --region us-east-1 \
    response.json
  cat response.json
  ```

## CloudWatch Monitoring

### Viewing Logs

Each Lambda function has its own log group. View logs using the AWS CLI or console:

```bash
# View recent logs for ReceiveFeedbackFunction
aws logs tail /aws/lambda/dev-receive-feedback --follow

# View logs for a specific time period
aws logs filter-log-events \
  --log-group-name /aws/lambda/dev-receive-feedback \
  --start-time $(date -d '1 hour ago' +%s)000 \
  --query 'events[].message' \
  --output text
```

### Creating Alarms

Monitor function health with CloudWatch alarms:

```bash
# Alarm for high error rate
aws cloudwatch put-metric-alarm \
  --alarm-name dev-receive-feedback-errors \
  --alarm-description "Alert if errors exceed 10 per minute" \
  --metric-name Errors \
  --namespace AWS/Lambda \
  --statistic Sum \
  --period 60 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --dimensions Name=FunctionName,Value=dev-receive-feedback

# Alarm for function duration
aws cloudwatch put-metric-alarm \
  --alarm-name dev-receive-feedback-duration \
  --alarm-description "Alert if avg duration exceeds 5 seconds" \
  --metric-name Duration \
  --namespace AWS/Lambda \
  --statistic Average \
  --period 60 \
  --threshold 5000 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --dimensions Name=FunctionName,Value=dev-receive-feedback
```

### CloudWatch Dashboards

Create a custom dashboard for monitoring:

```bash
aws cloudwatch put-dashboard \
  --dashboard-name FIAPFeedbackPlatform \
  --dashboard-body file://dashboard-config.json
```

Dashboard should include:
- Lambda invocation counts and errors
- DynamoDB throttle events and consumed capacity
- SNS publish count and delivery failures
- SES send attempts and bounce/complaint rates
- API Gateway request count and 4xx/5xx errors

## Troubleshooting

### Deployment Failures

#### Error: "User is not authorized to perform: iam:PassRole"

**Cause**: Your AWS credentials don't have IAM permissions.  
**Solution**: Ask your AWS administrator to grant `iam:PassRole` and `iam:CreateRole` permissions.

#### Error: "Parameter validation failed: Invalid email address"

**Cause**: The AdminEmail parameter is not a valid email format.  
**Solution**: Use a properly formatted email (e.g., `admin@company.com`).

#### Error: "CloudFormation stack creation timeout"

**Cause**: Deployment took too long (usually Lambda build or VPC issues).  
**Solution**: 
```bash
# Rollback the failed stack
aws cloudformation delete-stack --stack-name fiap-feedback-platform-dev

# Check for VPC or network issues, then retry
```

### Runtime Failures

#### Lambda Function Returns 500 Error

**Diagnosis**:
```bash
# Check CloudWatch logs
aws logs tail /aws/lambda/dev-receive-feedback --follow

# Look for errors related to:
# - DynamoDB table not found (TABLE_NAME env var)
# - SNS topic not found (FEEDBACK_TOPIC_ARN env var)
# - Missing IAM permissions
```

**Solution**:
- Verify Lambda environment variables match your stack outputs
- Confirm IAM role policies grant necessary permissions
- Check DynamoDB table exists and is accessible

#### Email Not Sent (SES Sandbox Restriction)

**Symptom**: Emails are not delivered in development.  
**Cause**: SES starts in sandbox mode. Only verified emails can send/receive.

**Solution for Development**:
```bash
# Verify sender email
aws ses verify-email-identity \
  --email-address sender@example.com \
  --region us-east-1

# Verify recipient email
aws ses verify-email-identity \
  --email-address admin@example.com \
  --region us-east-1
```

**Solution for Production**: Request production access from SES console.

#### DynamoDB Throttling

**Symptom**: Lambda logs show throttling errors.  
**Cause**: On-demand billing is active, but request rate exceeds provisioned capacity.

**Solution**:
```bash
# Check DynamoDB table metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/DynamoDB \
  --metric-name UserErrors \
  --dimensions Name=TableName,Value=dev-feedback-table \
  --start-time $(date -d '1 hour ago' -u +%Y-%m-%dT%H:%M:%SZ) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%SZ) \
  --period 60 \
  --statistics Sum
```

On-demand billing automatically scales, so throttling is rare. If persistent, enable autoscaling on indexes.

## Updating the Stack

To update an existing deployment:

1. Make changes to the SAM template or Lambda code
2. Rebuild the functions:
   ```bash
   mvn clean verify
   ```
3. Deploy the update:
   ```bash
   cd infra
   sam deploy \
     --stack-name fiap-feedback-platform-dev \
     --capabilities CAPABILITY_IAM
   ```
4. Verify the update:
   ```bash
   aws cloudformation describe-stacks \
     --stack-name fiap-feedback-platform-dev \
     --query 'Stacks[0].StackStatus'
   ```

Expected status: `UPDATE_COMPLETE`

## Deleting the Stack

To remove all AWS resources created by this deployment:

```bash
# Delete the CloudFormation stack
aws cloudformation delete-stack \
  --stack-name fiap-feedback-platform-dev

# Wait for deletion to complete
aws cloudformation wait stack-delete-complete \
  --stack-name fiap-feedback-platform-dev

# Verify deletion
aws cloudformation describe-stacks \
  --stack-name fiap-feedback-platform-dev 2>&1 | grep -i "does not exist"
```

Alternatively, use SAM:

```bash
cd infra
sam delete --stack-name fiap-feedback-platform-dev
```

This will prompt for confirmation and remove:
- CloudFormation stack
- Lambda functions
- DynamoDB table
- SNS topics
- API Gateway
- IAM roles
- CloudWatch log groups

**Warning**: This is irreversible. Ensure you have backups of any critical data in DynamoDB before deleting.

## Local Testing with SAM CLI

To test Lambda functions locally before deploying:

```bash
cd infra

# Build functions locally
sam build

# Start local API Gateway and Lambda environment
sam local start-api

# In another terminal, test the API
curl -X POST http://localhost:3000/avaliacao \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "local-user",
    "rating": 4,
    "urgency": "MEDIUM",
    "comment": "Testing locally"
  }'
```

**Note**: Local testing requires Docker to run Lambda container images.

## Continuous Integration / Continuous Deployment

The project includes a CI/CD pipeline (`.github/workflows/ci-cd.yml`) that:

1. Builds all modules on every push to `main` or `develop`
2. Runs unit tests and generates coverage reports
3. Performs SonarCloud code analysis
4. Validates builds before deployment

To integrate automatic deployment:

1. Add deployment step to the workflow
2. Store AWS credentials as GitHub Secrets
3. Update the workflow to deploy after successful tests

Example deployment step:
```yaml
- name: Deploy to AWS
  if: github.ref == 'refs/heads/main'
  env:
    AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
    AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
  run: |
    cd infra
    sam deploy \
      --stack-name fiap-feedback-platform-prod \
      --parameter-overrides Environment=prod AdminEmail=${{ secrets.ADMIN_EMAIL }} \
      --no-confirm-changeset \
      --capabilities CAPABILITY_IAM
```

## Security Best Practices

1. **API Key Management**:
   - Rotate API keys regularly
   - Use different keys for dev/prod environments
   - Store keys securely (use AWS Secrets Manager or environment variables)

2. **IAM Least Privilege**:
   - Each Lambda has its own role with minimal permissions
   - DynamoDB policies are restricted to specific operations
   - SES policies are scoped to SendEmail actions

3. **Email Verification**:
   - Always verify admin email addresses in SES
   - For production, request SES production access
   - Use separate email addresses for dev and prod

4. **Encryption**:
   - DynamoDB encryption at rest is enabled by default
   - API Gateway uses HTTPS only
   - Consider enabling encryption for CloudWatch logs

5. **Monitoring and Alerts**:
   - Set up CloudWatch alarms for errors and throttling
   - Review logs regularly for suspicious activity
   - Use CloudTrail to audit API calls

## Additional Resources

- [AWS SAM Developer Guide](https://docs.aws.amazon.com/serverless-application-model/)
- [Lambda Function Best Practices](https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html)
- [API Gateway API Keys and Usage Plans](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-use-lambda-authorizer.html)
- [DynamoDB Developer Guide](https://docs.aws.amazon.com/dynamodb/)
- [CloudWatch Logs Insights Query Syntax](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CWL_QuerySyntax.html)
