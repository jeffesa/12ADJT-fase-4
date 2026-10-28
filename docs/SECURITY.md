# Security Documentation

## Cloud Model

This system is built on **AWS Serverless**, a cloud-native architecture that leverages managed AWS services to eliminate infrastructure management overhead. All compute, storage, and integration components are fully managed by AWS, enabling rapid scaling and high availability without operational burden.

The serverless model provides:
- **On-demand scaling**: Resources automatically scale to meet demand
- **Pay-per-use pricing**: Costs correlate directly with actual usage
- **Reduced operational overhead**: No server provisioning or patching required
- **Built-in resilience**: Managed services include redundancy and failover

---

## Security Components

### 1. API Key Authentication

The Feedback API requires an API Key for client authentication and is protected through:

- **API Gateway Usage Plan**: Enforces throttling and quota limits
  - **Burst Limit**: 100 requests per second
  - **Rate Limit**: 1,000 requests per second (sustained)
  - **Daily Quota**: 10,000 requests per day

- **Requirement**: All requests to the POST `/avaliacao` endpoint must include the API Key in the request headers (`X-API-Key` header)
- **Key Management**: API Keys are generated and managed through AWS API Gateway and must be treated as secrets (never committed to version control)

### 2. IAM Roles and Least-Privilege Access

Each Lambda function runs under a dedicated IAM role with permissions scoped to only the AWS resources and actions it requires. This follows the principle of least privilege, minimizing the blast radius if a function is compromised.

#### Role 1: ReceiveFeedback

**Purpose**: Receives feedback submissions via API Gateway, validates input, persists to DynamoDB, and publishes urgent feedback (urgency=HIGH) to SNS.

**Permissions**:
- `dynamodb:PutItem` on DynamoDB FeedbacksTable
- `sns:Publish` on SNS FeedbackUrgentTopic
- `logs:*` (via AWSLambdaBasicExecutionRole) for CloudWatch Logs

**Least-Privilege Rationale**:
- Write-only access to DynamoDB (no read/delete/update)
- SNS Publish limited to the specific urgent feedback topic
- No access to other AWS services or resources outside its scope

#### Role 2: SendNotification

**Purpose**: Consumes urgent feedback events from SNS and sends email notifications to administrators via AWS SES.

**Permissions**:
- `ses:SendEmail` and `ses:SendRawEmail` on all resources
- `logs:*` (via AWSLambdaBasicExecutionRole) for CloudWatch Logs

**Least-Privilege Rationale**:
- Email-only actions; no database or SNS access
- Cannot read, modify, or delete feedback data
- Scoped to email sending operations only

#### Role 3: GenerateReport

**Purpose**: Scheduled weekly (Monday 08:00 UTC) to read feedbacks from DynamoDB, aggregate statistics, and send reports via SES.

**Permissions**:
- `dynamodb:Scan` and `dynamodb:Query` on DynamoDB FeedbacksTable
- `ses:SendEmail` and `ses:SendRawEmail` on all resources
- `logs:*` (via AWSLambdaBasicExecutionRole) for CloudWatch Logs

**Least-Privilege Rationale**:
- Read-only access to DynamoDB (no write/delete/update)
- Can only scan or query, not modify table structure
- Email sending limited to report delivery
- Reserved concurrency set to 1 (prevents duplicate report generation)

### 3. CloudWatch Logging and Monitoring

Each Lambda function logs to a dedicated CloudWatch Log Group with:

- **Log Group**: `/aws/lambda/{environment}-{function-name}`
- **Retention**: 30 days (automatic cleanup of old logs)
- **Log Levels**: 
  - INFO: Handler entry/exit and major operations
  - FINE: Detailed operational information (request/response data)
  - SEVERE: Errors and exception details

**Monitoring Capabilities**:
- All Lambda invocations are logged in CloudWatch
- Errors are captured with full exception context
- Log Insights queries enable troubleshooting and audit trails

---

## API Gateway + Usage Plan Throttling

The Feedback API is protected from abuse and overload through AWS API Gateway Usage Plans:

### Rate Limiting
- **Burst Limit**: 100 requests per second (handles traffic spikes)
- **Rate Limit**: 1,000 requests per second (sustained rate)
- **Daily Quota**: 10,000 requests per day

### Enforcement
- Requests exceeding limits receive HTTP 429 (Too Many Requests)
- Clients must implement exponential backoff retry logic
- The API Key is linked to the Usage Plan; per-key metrics are tracked

### Configuration
```yaml
ApiUsagePlan:
  Throttle:
    BurstLimit: 100
    RateLimit: 1000
  Quota:
    Limit: 10000
    Period: DAY
```

---

## Environment Separation (dev/prod)

The infrastructure supports isolated development and production environments controlled by the `Environment` parameter:

### Resource Naming
All resources are prefixed with the environment name to ensure complete isolation:
- DynamoDB Table: `{environment}-feedback-table`
- SNS Topic: `{environment}-feedback-urgent-topic`
- Lambda Functions: `{environment}-{function-name}`
- Log Groups: `/aws/lambda/{environment}-{function-name}`
- API Gateway: `{environment}-feedback-api`

### Separate Deployments
- **Development (dev)**: Used for testing, experimentation, and integration validation
- **Production (prod)**: Live environment serving real users

### Separate API Keys and Usage Plans
Each environment has its own API Key and Usage Plan, preventing dev traffic from affecting production quotas or throttling.

### Email Verification (SES)
- In production, the `AdminEmail` parameter must reference an email address verified in Amazon SES
- In development, any email address can be used for testing

### Deployment Parameters
```yaml
Parameters:
  Environment:
    AllowedValues:
      - dev
      - prod
  AdminEmail:
    Description: Email for notifications (must be verified in SES for prod)
  Region:
    Default: us-east-1
```

---

## Data Security and Access Control

### DynamoDB Encryption
- All data at rest in DynamoDB is encrypted by default using AWS managed keys
- Point-in-time recovery is disabled in this configuration (can be enabled if needed for disaster recovery)

### Network Security
- All Lambda-to-AWS service communication occurs over private AWS networks
- API Gateway enforces HTTPS for all incoming requests (TLS 1.2+)
- No data is transmitted over unencrypted channels

### Access Audit Trail
- CloudWatch Logs capture all function invocations and errors
- SNS message delivery is logged
- DynamoDB access is implicit in Lambda logs (via CloudWatch)

---

## Security Best Practices Implemented

1. **Least-Privilege IAM Roles**: Each Lambda has minimal required permissions
2. **API Key Authentication**: All API requests require an API Key
3. **Rate Limiting**: Usage Plans prevent abuse and resource exhaustion
4. **Encryption**: Data at rest (DynamoDB) and in transit (HTTPS) are encrypted
5. **Audit Logging**: CloudWatch Logs capture all operations for compliance and debugging
6. **Environment Isolation**: Separate stacks for dev and prod prevent cross-environment interference
7. **Reserved Concurrency**: Critical functions (GenerateReport) have reserved slots to prevent starvation
8. **Error Handling**: Errors are logged without exposing internal details to clients

---

## Deployment and Configuration

### Prerequisites
1. AWS Account with appropriate permissions
2. AWS SAM CLI installed
3. SES verified email address (for production deployments)
4. S3 bucket for storing packaged Lambda code

### Deployment Steps
```bash
# Build all Lambda functions
sam build

# Package (substitute S3 bucket name)
sam package --s3-bucket <your-s3-bucket> --output-template-file packaged.yaml

# Deploy to dev environment
sam deploy --template-file packaged.yaml \
  --parameter-overrides Environment=dev AdminEmail=dev@example.com \
  --stack-name fiap-fase4-dev

# Deploy to prod environment
sam deploy --template-file packaged.yaml \
  --parameter-overrides Environment=prod AdminEmail=admin@example.com \
  --stack-name fiap-fase4-prod
```

### Environment-Specific Configuration
- Update `AdminEmail` parameter per environment
- Ensure SES sender email is verified in production
- Adjust throttle limits as needed based on usage patterns

---

## Incident Response

### Monitoring and Alerting
- CloudWatch Logs are retained for 30 days
- Logs should be monitored for ERROR and SEVERE level messages
- Consider enabling CloudWatch Alarms for:
  - Lambda error rates
  - API Gateway 5xx errors
  - DynamoDB throttling

### Log Analysis
```bash
# Example: Query recent errors in CloudWatch Logs Insights
fields @timestamp, @message, @logStream
| filter @message like /ERROR|SEVERE/
| stats count() by @logStream
```

### Recovery
- Lambda functions are stateless and automatically recover from transient failures
- SNS retry policy (default 3 retries) ensures urgent notifications are eventually delivered
- Scheduled reports can be manually triggered by invoking GenerateReportFunction via the AWS Lambda console

---

## Compliance Considerations

- **Data Retention**: DynamoDB and CloudWatch Logs are retained according to configuration
- **Audit Trail**: All operations are logged in CloudWatch for compliance audits
- **Encryption**: Data at rest and in transit meets standard security requirements
- **Access Control**: IAM roles enforce least-privilege access
- **Environment Separation**: Production and development workloads are isolated

---

## Contact and Support

For security concerns or vulnerabilities, please contact the development team immediately. Do not disclose security issues publicly.
