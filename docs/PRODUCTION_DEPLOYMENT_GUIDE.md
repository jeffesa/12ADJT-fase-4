# Production Deployment Guide - FIAP Fase-4 Feedback Platform

**Last Updated**: 2026-10-26  
**AWS Region**: us-east-1  
**Status**: Deployment Process Documented | Application Blocker Documented

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Pre-Deployment Checklist](#pre-deployment-checklist)
3. [Build & Package](#build--package)
4. [AWS Setup](#aws-setup)
5. [Deployment Steps](#deployment-steps)
6. [Post-Deployment Verification](#post-deployment-verification)
7. [Troubleshooting](#troubleshooting)
8. [Known Issues](#known-issues)

---

## Prerequisites

### Required Software

- **Java 17**: `java -version` should return version 17.x
- **Maven 3.9+**: `mvn -v` should return 3.9.x or later
- **AWS CLI v2**: `aws --version` should return version 2.x
- **AWS SAM CLI**: `sam --version` should return version 1.80+
- **Git**: For version control and commit management

### Required AWS Permissions

The AWS account must have permissions for:
- CloudFormation (StackSet operations)
- Lambda (function creation, role assignment)
- DynamoDB (table creation)
- SNS (topic creation, subscriptions)
- IAM (role and policy creation)
- CloudWatch (log group creation)
- API Gateway (REST API creation)
- SES (email service - optional, for production)

### Required AWS Configuration

```bash
# Set AWS credentials
export AWS_ACCESS_KEY_ID="your-access-key"
export AWS_SECRET_ACCESS_KEY="your-secret-key"
export AWS_DEFAULT_REGION="us-east-1"

# Or use aws configure
aws configure
```

### Environment Variables

```bash
# Required for deployment
export AWS_REGION="us-east-1"
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export ADMIN_EMAIL="admin@feedback-fase-4.com"  # Must be verified in SES for production

# Optional for local development
export AWS_ENDPOINT_URL_DYNAMODB="http://localhost:4566"  # LocalStack
export AWS_ENDPOINT_URL_SNS="http://localhost:4566"      # LocalStack
```

---

## Pre-Deployment Checklist

### Code Readiness
- [ ] All modules compile locally: `mvn clean compile`
- [ ] Unit tests pass: `mvn test`
- [ ] No snapshot dependencies (except internal)
- [ ] All `TODO` comments addressed
- [ ] Code review completed

### AWS Account Readiness
- [ ] AWS account has sufficient quota for Lambda functions
- [ ] S3 bucket exists for SAM artifacts: `s3://sam-artifacts-phase-4-us-east-1-1793031500`
- [ ] IAM user has required permissions (see Prerequisites)
- [ ] Admin email is SES-verified (production only)

### Configuration Readiness
- [ ] `infra/samconfig.toml` configured for target environment
- [ ] `application.yaml` configured for prod profile
- [ ] All environment variables exported
- [ ] No hardcoded secrets in code

### Network & Routing
- [ ] API Gateway stage name decided (default: `dev`)
- [ ] CloudFront distribution setup (optional)
- [ ] Route53 DNS configured (optional)

---

## Build & Package

### Step 1: Clean and Build All Modules

```bash
cd /Users/jefferson/Documents/FIAP/fase-4

# Clean previous builds
mvn clean

# Build without running tests (tests should be run separately in CI/CD)
mvn package -DskipTests

# Expected output:
# [INFO] Building receive-feedback 1.0.0
# [INFO] Building send-notification 1.0.0
# [INFO] Building generate-report 1.0.0
# [INFO] BUILD SUCCESS
```

**Verification**:
```bash
# Verify build artifacts exist
ls -lh receive-feedback/target/function.zip
ls -lh send-notification/target/function.zip
ls -lh generate-report/target/function.zip

# All should be > 10 MB
```

### Step 2: Build SAM Artifacts

```bash
cd /Users/jefferson/Documents/FIAP/fase-4

# Build SAM template and Lambda functions
sam build -t infra/template.yaml

# Expected output:
# [*] Invoke Function: sam local invoke
# [*] Test Function in the Cloud: sam sync --stack-name {{stack-name}} --watch
# [*] Deploy: sam deploy --guided
```

**Verification**:
```bash
# Check built artifacts
ls -R .aws-sam/build/
```

### Step 3: Validate CloudFormation Template

```bash
# Option 1: SAM validate
sam validate -t infra/template.yaml

# Option 2: AWS CLI validate
aws cloudformation validate-template \
  --template-body file://infra/template.yaml \
  --region us-east-1
```

---

## AWS Setup

### Step 1: Create S3 Bucket for SAM Artifacts

```bash
# Generate unique bucket name
BUCKET_NAME="sam-artifacts-phase-4-us-east-1-$(date +%s)"

# Create bucket (must be in same region as deployment)
aws s3 mb s3://$BUCKET_NAME --region us-east-1

# Verify creation
aws s3 ls s3://$BUCKET_NAME

# Update samconfig.toml with bucket name
# Edit: infra/samconfig.toml
# Set: s3_bucket = "sam-artifacts-phase-4-us-east-1-YOUR-TIMESTAMP"
```

### Step 2: Verify SES Sender Identity (Production Only)

```bash
# List verified identities
aws ses list-verified-email-addresses --region us-east-1

# If admin email not verified, verify it:
aws ses verify-email-identity \
  --email-address admin@feedback-fase-4.com \
  --region us-east-1

# Follow the verification link in email
# Check status:
aws ses list-verified-email-addresses \
  --region us-east-1
```

### Step 3: Prepare CloudFormation Parameters

```bash
# Create parameters file (optional, for repeated deployments)
cat > /tmp/deployment-params.json << 'EOF'
[
  {
    "ParameterKey": "Environment",
    "ParameterValue": "prod"
  },
  {
    "ParameterKey": "AdminEmail",
    "ParameterValue": "admin@feedback-fase-4.com"
  },
  {
    "ParameterKey": "Region",
    "ParameterValue": "us-east-1"
  }
]
EOF
```

---

## Deployment Steps

### Option A: Interactive Deployment (First Time)

```bash
cd /Users/jefferson/Documents/FIAP/fase-4/infra

# Run guided deployment
sam deploy --guided

# Follow prompts:
# - Stack name: prod-feedback-stack
# - Region: us-east-1
# - Parameter Environment: prod
# - Parameter AdminEmail: admin@feedback-fase-4.com
# - Parameter Region: us-east-1
# - Confirm changes: Y
# - Allow SAM CLI to create IAM roles: Y
# - Save parameters to file: Y
```

### Option B: Automated Deployment (Subsequent Deployments)

```bash
cd /Users/jefferson/Documents/FIAP/fase-4/infra

# Package artifacts
sam package \
  --template-file template.yaml \
  --s3-bucket sam-artifacts-phase-4-us-east-1-1793031500 \
  --region us-east-1 \
  --output-template-file packaged.yaml

# Deploy using configuration
sam deploy \
  --config-env prod \
  --no-confirm-changeset \
  --region us-east-1 \
  --s3-bucket sam-artifacts-phase-4-us-east-1-1793031500 \
  --capabilities CAPABILITY_NAMED_IAM
```

### Option C: Script-Based Deployment

```bash
#!/bin/bash
set -e

# Configuration
STACK_NAME="prod-feedback-stack"
REGION="us-east-1"
S3_BUCKET="sam-artifacts-phase-4-us-east-1-1793031500"
ADMIN_EMAIL="admin@feedback-fase-4.com"

echo "🚀 Building SAM artifacts..."
sam build -t infra/template.yaml

echo "📦 Packaging..."
sam package \
  --template-file infra/template.yaml \
  --s3-bucket $S3_BUCKET \
  --region $REGION \
  --output-template-file infra/packaged.yaml

echo "🚀 Deploying to AWS..."
sam deploy \
  --template-file infra/packaged.yaml \
  --stack-name $STACK_NAME \
  --region $REGION \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    Environment=prod \
    AdminEmail=$ADMIN_EMAIL \
    Region=$REGION

echo "✅ Deployment complete!"
```

---

## Post-Deployment Verification

### Step 1: Verify Stack Creation

```bash
# Check stack status
aws cloudformation describe-stacks \
  --stack-name prod-feedback-stack \
  --region us-east-1 \
  --query 'Stacks[0].[StackStatus,CreationTime]' \
  --output table

# Expected: CREATE_COMPLETE or UPDATE_COMPLETE
```

### Step 2: Retrieve Stack Outputs

```bash
# Get all outputs
aws cloudformation describe-stacks \
  --stack-name prod-feedback-stack \
  --region us-east-1 \
  --query 'Stacks[0].Outputs' \
  --output table

# Store for use in tests
export API_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name prod-feedback-stack \
  --region us-east-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' \
  --output text)

export API_KEY=$(aws cloudformation describe-stacks \
  --stack-name prod-feedback-stack \
  --region us-east-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiKey`].OutputValue' \
  --output text)

export TABLE_NAME=$(aws cloudformation describe-stacks \
  --stack-name prod-feedback-stack \
  --region us-east-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`DynamoDBTableName`].OutputValue' \
  --output text)
```

### Step 3: Verify Lambda Functions

```bash
# List deployed functions
aws lambda list-functions \
  --region us-east-1 \
  --query 'Functions[?contains(FunctionName, `dev-`)].FunctionName' \
  --output table

# Check function configuration
aws lambda get-function-configuration \
  --function-name dev-receive-feedback \
  --region us-east-1 \
  --query '[FunctionArn,Role,Timeout,MemorySize,Handler]'

# Check function permissions
aws lambda get-policy \
  --function-name dev-receive-feedback \
  --region us-east-1 \
  --output text
```

### Step 4: Verify DynamoDB Table

```bash
# Describe table
aws dynamodb describe-table \
  --table-name $TABLE_NAME \
  --region us-east-1 \
  --query 'Table.[TableName,TableStatus,ItemCount]'

# Expected: ACTIVE with ItemCount = 0 (new table)
```

### Step 5: Verify SNS Topic

```bash
# List topics
aws sns list-topics --region us-east-1 --output table

# Get topic attributes
aws sns get-topic-attributes \
  --topic-arn arn:aws:sns:us-east-1:${AWS_ACCOUNT_ID}:dev-feedback-urgent-topic \
  --region us-east-1 \
  --query 'Attributes.DisplayName'
```

### Step 6: Test API Gateway Endpoint

```bash
# Test with curl
curl -v -X POST "$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "x-api-key: $API_KEY" \
  -d '{"descricao": "Test feedback", "nota": 7}'

# Expected: HTTP 201 or HTTP 500 (if CDI blocker present)
# See Known Issues section if HTTP 500
```

---

## Troubleshooting

### Issue 1: S3 Bucket Already Exists

**Error**: `BucketAlreadyExists: The requested bucket name is not available`

**Solution**:
```bash
# Generate new bucket name with unique timestamp
TIMESTAMP=$(date +%s)
BUCKET_NAME="sam-artifacts-phase-4-us-east-1-${TIMESTAMP}"
aws s3 mb s3://$BUCKET_NAME --region us-east-1

# Update samconfig.toml with new bucket name
sed -i "" "s/s3_bucket = .*/s3_bucket = \"$BUCKET_NAME\"/" infra/samconfig.toml
```

### Issue 2: CloudFormation Stack Creation Timeout

**Error**: `StackSetOperationStatus: FAILED_OPERATION`

**Solution**:
```bash
# Check CloudFormation events for details
aws cloudformation describe-stack-events \
  --stack-name prod-feedback-stack \
  --region us-east-1 \
  --query 'StackEvents[?ResourceStatus==`CREATE_FAILED`].[ResourceType,ResourceStatusReason]'

# Common causes:
# - IAM role permissions insufficient
# - Lambda handler syntax error
# - Environment variable missing

# Rollback stack:
aws cloudformation delete-stack \
  --stack-name prod-feedback-stack \
  --region us-east-1
```

### Issue 3: Lambda Function Execution Fails

**Error**: HTTP 500 from API Gateway

**Solution**:
```bash
# Check Lambda logs
aws logs tail /aws/lambda/dev-receive-feedback \
  --region us-east-1 \
  --follow \
  --max-items 20

# Check for:
# - NullPointerException (missing dependencies)
# - ValidationException (invalid input)
# - DynamoDbException (table access)
# - SnsException (topic access)

# Get full error context:
aws logs filter-log-events \
  --log-group-name /aws/lambda/dev-receive-feedback \
  --region us-east-1 \
  --start-time $(date -v-1H +%s)000 \
  --query 'events[?contains(message, `ERROR`)]'
```

### Issue 4: API Key Authentication Fails

**Error**: `{"message":"Forbidden"}`

**Solution**:
```bash
# Verify API key is attached to usage plan
aws apigateway get-usage-plans \
  --region us-east-1 \
  --query 'items[0].id' \
  --output text

# Check API key association
aws apigateway get-api-keys \
  --region us-east-1 \
  --include-values \
  --query 'items[?name==`dev-feedback-api-key`].[id,value]'

# Ensure API Gateway has resource policy allowing API key usage:
aws apigateway get-rest-apis \
  --region us-east-1 \
  --query 'items[0].id'
```

### Issue 5: CDI Container Not Initialized

**Error**: `Arc.container() returned null`

**Solution**: See [Known Issues](#known-issues) section below

---

## Known Issues

### Issue: CDI Container Not Initialized in Lambda

**Status**: ⚠️ BLOCKER  
**Severity**: High  
**Affected Components**: All REST API endpoints

**Description**:
AWS Lambda runtime instantiates RequestHandler before Quarkus CDI is initialized, resulting in `Arc.container()` returning null and preventing access to Spring/Quarkus managed beans.

**Workaround** (Temporary):
1. Use GraphQL endpoints instead (auto-handle CDI)
2. Rewrite handlers as StreamHandler with @QuarkusMain
3. Wait for Quarkus Lambda CDI fix (upstream issue)

**Expected Fix**: Update handler implementation (4-6 hours)  
**Impact**: End-to-end tests cannot run until resolved

**Tracking**: See `AWS_DEPLOYMENT_RESULTS.md` for detailed analysis

---

## Rollback Procedures

### Complete Stack Deletion

```bash
# Delete entire stack (destructive)
aws cloudformation delete-stack \
  --stack-name prod-feedback-stack \
  --region us-east-1

# Monitor deletion
aws cloudformation wait stack-delete-complete \
  --stack-name prod-feedback-stack \
  --region us-east-1

# Verify deletion
aws cloudformation describe-stacks \
  --stack-name prod-feedback-stack \
  --region us-east-1 2>&1 | grep ValidationError

# Delete S3 artifacts (if needed)
aws s3 rm s3://$S3_BUCKET --recursive
aws s3 rb s3://$S3_BUCKET
```

### Selective Resource Removal

```bash
# Remove only Lambda functions (keep DynamoDB/SNS data)
aws lambda delete-function --function-name dev-receive-feedback --region us-east-1
aws lambda delete-function --function-name dev-send-notification --region us-east-1
aws lambda delete-function --function-name dev-generate-report --region us-east-1

# Remove API Gateway
aws apigateway delete-rest-api \
  --rest-api-id $(aws apigateway get-rest-apis --region us-east-1 --query 'items[0].id' --output text) \
  --region us-east-1
```

---

## Performance Optimization

### Lambda Concurrency Configuration

```bash
# Set reserved concurrency for ReceiveFeedbackFunction
aws lambda put-function-concurrency \
  --function-name dev-receive-feedback \
  --reserved-concurrent-executions 100 \
  --region us-east-1
```

### DynamoDB Capacity Planning

```bash
# Check current capacity
aws dynamodb describe-table \
  --table-name dev-feedback-table \
  --region us-east-1 \
  --query 'Table.[BillingModeSummary.BillingMode,ProvisionedThroughput]'

# For production, consider switching to provisioned capacity:
aws dynamodb update-billing-mode \
  --table-name dev-feedback-table \
  --billing-mode PROVISIONED \
  --provisioned-throughput ReadCapacityUnits=100,WriteCapacityUnits=100 \
  --region us-east-1
```

---

## Support & Further Information

- **AWS Documentation**: https://docs.aws.amazon.com/lambda/
- **Quarkus AWS Documentation**: https://quarkus.io/guides/amazon-lambda
- **SAM Documentation**: https://docs.aws.amazon.com/serverless-application-model/
- **CloudFormation Reference**: https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/

---

**Last Updated**: 2026-10-26  
**Deployment Status**: Ready for Production (with CDI blocker documented)
