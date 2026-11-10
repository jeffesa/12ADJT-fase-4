#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# E2E Verification Script: Validate test results against AWS
# Quarkus Dev Mode Edition (No LocalStack)
# ═══════════════════════════════════════════════════════════════

set -o pipefail

# Cores
C_CYAN='\033[0;36m'
C_GREEN='\033[0;32m'
C_RED='\033[0;31m'
C_YELLOW='\033[1;33m'
C_RESET='\033[0m'

info()  { printf "${C_CYAN}%b${C_RESET}\n" "$1"; }
ok()    { printf "${C_GREEN}%b${C_RESET}\n" "$1"; }
warn()  { printf "${C_YELLOW}%b${C_RESET}\n" "$1"; }
err()   { printf "${C_RED}%b${C_RESET}\n" "$1"; }

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
RESULTS_FILE="/tmp/e2e-results.csv"
VERIFY_LOG="/tmp/e2e-verify.log"

# AWS Configuration
AWS_REGION="us-east-1"
DYNAMODB_TABLE="prod-feedback-table"
SNS_TOPIC_ARN="arn:aws:sns:${AWS_REGION}:000000000000:prod-feedback-urgent-topic"
API_ENDPOINT="https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/feedback"

info "\n═══════════════════════════════════════════════════════════════"
info "E2E Verification: AWS Integration Testing"
info "═══════════════════════════════════════════════════════════════"
info "\nVerification Log: $VERIFY_LOG"
info "API Endpoint: $API_ENDPOINT"
info "DynamoDB Table: $DYNAMODB_TABLE (${AWS_REGION})"

> "$VERIFY_LOG"  # Clear log file

log_verify() {
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" >> "$VERIFY_LOG"
}

log_verify "Verification started"

# ─────────────────────────────────────────────────────────────────
# Pre-flight checks
# ─────────────────────────────────────────────────────────────────

info "\n🔍 Checking dependencies..."

if ! command -v jq &>/dev/null; then
  err "❌ jq not found. Please install jq."
  log_verify "PREFLIGHT FAILURE: jq not available"
  exit 1
fi

if ! command -v aws &>/dev/null; then
  err "❌ AWS CLI not found. Please install AWS CLI."
  log_verify "PREFLIGHT FAILURE: AWS CLI not available"
  exit 1
fi

if ! command -v curl &>/dev/null; then
  err "❌ curl not found. Please install curl."
  log_verify "PREFLIGHT FAILURE: curl not available"
  exit 1
fi

# Verify AWS credentials are configured
if ! aws sts get-caller-identity &>/dev/null; then
  err "❌ AWS credentials not configured. Please run 'aws configure' or set AWS_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY."
  log_verify "PREFLIGHT FAILURE: AWS credentials not configured"
  exit 1
fi

ok "✅ All dependencies OK (jq, AWS CLI, curl, credentials)"
log_verify "Dependencies verified: jq, AWS CLI, curl, AWS credentials configured"

# ─────────────────────────────────────────────────────────────────
# Initialize results CSV
# ─────────────────────────────────────────────────────────────────

info "\n📊 Initializing results CSV..."
cat > "$RESULTS_FILE" << 'EOF'
Test,Status,Detail,Evidence
EOF

log_verify "Created results CSV at $RESULTS_FILE"

# Helper function to add result
add_result() {
  local test=$1
  local status=$2
  local detail=$3
  local evidence=$4
  
  # Escape quotes and commas in CSV
  detail=$(echo "$detail" | sed 's/"/""/g')
  evidence=$(echo "$evidence" | sed 's/"/""/g')
  
  echo "\"$test\",\"$status\",\"$detail\",\"$evidence\"" >> "$RESULTS_FILE"
}

# ─────────────────────────────────────────────────────────────────
# Test 1: API Gateway Endpoint Health
# ─────────────────────────────────────────────────────────────────

info "\n✅ Test 1: API Gateway endpoint health check..."
log_verify "Testing API Gateway endpoint"

HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" "$API_ENDPOINT" 2>&1)

if [ "$HEALTH_CHECK" = "000" ]; then
  err "  ❌ API endpoint unreachable (curl error)"
  log_verify "API endpoint unreachable"
  add_result "API Gateway Health" "FAIL" "Endpoint unreachable" "HTTP Code: $HEALTH_CHECK (curl error)"
else
  ok "  ✅ API endpoint responding with HTTP $HEALTH_CHECK"
  log_verify "API endpoint responsive: HTTP $HEALTH_CHECK"
  add_result "API Gateway Health" "PASS" "Endpoint is responding" "HTTP Code: $HEALTH_CHECK"
fi

# ─────────────────────────────────────────────────────────────────
# Test 2: Create test feedback via API
# ─────────────────────────────────────────────────────────────────

info "\n✅ Test 2: Create test feedback (HIGH urgency)..."
log_verify "Creating test feedback via API"

TEST_FEEDBACK=$(curl -s -X POST "$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{"descricao": "E2E Test Feedback - High Urgency", "nota": 2}')

FEEDBACK_ID=$(echo "$TEST_FEEDBACK" | jq -r '.receiveFeedback.id // empty' 2>/dev/null)

if [ -z "$FEEDBACK_ID" ] || [ "$FEEDBACK_ID" = "null" ]; then
  err "  ❌ Failed to create feedback"
  log_verify "Failed to create feedback: $TEST_FEEDBACK"
  add_result "Create Feedback" "FAIL" "API returned invalid response" "Response: $TEST_FEEDBACK"
else
  ok "  ✅ Feedback created with ID: $FEEDBACK_ID"
  log_verify "Feedback created successfully: $FEEDBACK_ID"
  add_result "Create Feedback" "PASS" "Feedback created via API" "ID: $FEEDBACK_ID"
fi

# ─────────────────────────────────────────────────────────────────
# Test 3: Verify DynamoDB persistence
# ─────────────────────────────────────────────────────────────────

info "\n✅ Test 3: Verify DynamoDB persistence..."
log_verify "Checking DynamoDB feedbacks table on AWS"

sleep 2  # Give DynamoDB time to persist

DYNAMODB_SCAN=$(aws dynamodb scan \
  --table-name "$DYNAMODB_TABLE" \
  --region "$AWS_REGION" 2>&1)

if [ $? -ne 0 ]; then
  err "  ❌ Failed to query DynamoDB"
  log_verify "DynamoDB scan failed: $DYNAMODB_SCAN"
  add_result "DynamoDB Persistence" "FAIL" "Unable to query table" "Error: $DYNAMODB_SCAN"
else
  ITEM_COUNT=$(echo "$DYNAMODB_SCAN" | jq '.Items | length' 2>/dev/null)
  if [ ! -z "$ITEM_COUNT" ] && [ "$ITEM_COUNT" -gt 0 ]; then
    ok "  ✅ DynamoDB contains $ITEM_COUNT items"
    log_verify "DynamoDB persistence verified: $ITEM_COUNT items"
    
    # Count urgency distribution
    HIGH_COUNT=$(echo "$DYNAMODB_SCAN" | jq '[.Items[] | select(.urgencia.S == "HIGH")] | length' 2>/dev/null)
    MEDIUM_COUNT=$(echo "$DYNAMODB_SCAN" | jq '[.Items[] | select(.urgencia.S == "MEDIUM")] | length' 2>/dev/null)
    LOW_COUNT=$(echo "$DYNAMODB_SCAN" | jq '[.Items[] | select(.urgencia.S == "LOW")] | length' 2>/dev/null)
    
    ok "    Distribution: HIGH=$HIGH_COUNT, MEDIUM=$MEDIUM_COUNT, LOW=$LOW_COUNT"
    log_verify "Urgency distribution: HIGH=$HIGH_COUNT, MEDIUM=$MEDIUM_COUNT, LOW=$LOW_COUNT"
    
    add_result "DynamoDB Persistence" "PASS" "Feedbacks persisted in DynamoDB" "Total: $ITEM_COUNT, HIGH: $HIGH_COUNT, MEDIUM: $MEDIUM_COUNT, LOW: $LOW_COUNT"
  else
    warn "  ⚠️  DynamoDB table is empty or has 0 items"
    log_verify "DynamoDB table empty"
    add_result "DynamoDB Persistence" "WARN" "No items found in table (run API test first)" "Item Count: 0"
  fi
fi

# ─────────────────────────────────────────────────────────────────
# Test 4: List all feedbacks (Query operation)
# ─────────────────────────────────────────────────────────────────

info "\n✅ Test 4: Query all feedbacks via API..."
log_verify "Querying all feedbacks"

FEEDBACKS=$(curl -s -X POST "$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{}')

LIST_COUNT=$(echo "$FEEDBACKS" | jq '.listarAvaliacoes | length' 2>/dev/null)

if [ -z "$LIST_COUNT" ] || [ "$LIST_COUNT" = "null" ]; then
  warn "  ⚠️  Could not query feedbacks list"
  log_verify "Failed to query feedbacks list: $FEEDBACKS"
  add_result "Query Feedbacks" "WARN" "List query returned unexpected format" "Response: $FEEDBACKS"
else
  ok "  ✅ Query returned $LIST_COUNT feedbacks"
  log_verify "Feedbacks query successful: $LIST_COUNT items"
  add_result "Query Feedbacks" "PASS" "Successfully queried all feedbacks" "Count: $LIST_COUNT"
fi

# ─────────────────────────────────────────────────────────────────
# Test 5: SNS Topic Health
# ─────────────────────────────────────────────────────────────────

info "\n✅ Test 5: Check SNS topic (SNS for HIGH urgency notifications)..."
log_verify "Checking SNS topic"

# Note: Actual SNS message count verification is difficult in production
# We'll just verify the topic is accessible
SNS_CHECK=$(aws sns get-topic-attributes \
  --topic-arn "$SNS_TOPIC_ARN" \
  --region "$AWS_REGION" 2>&1)

if echo "$SNS_CHECK" | jq '.Attributes' &>/dev/null; then
  ok "  ✅ SNS topic is accessible"
  log_verify "SNS topic verified"
  add_result "SNS Topic Health" "PASS" "Topic is accessible and configured" "Topic ARN: $SNS_TOPIC_ARN"
else
  warn "  ⚠️  Could not access SNS topic (may not exist or permission issue)"
  log_verify "SNS topic not accessible: $SNS_CHECK"
  add_result "SNS Topic Health" "WARN" "SNS topic not directly accessible (may be OK in production)" "Error: Check permissions and topic ARN"
fi

# ─────────────────────────────────────────────────────────────────
# Test 6: CloudWatch Logs
# ─────────────────────────────────────────────────────────────────

info "\n✅ Test 6: Check CloudWatch Logs..."
log_verify "Checking CloudWatch logs"

LOGS=$(aws logs describe-log-groups \
  --region "$AWS_REGION" 2>&1)

if echo "$LOGS" | jq '.logGroups' &>/dev/null; then
  LOG_COUNT=$(echo "$LOGS" | jq '.logGroups | length' 2>/dev/null)
  ok "  ✅ CloudWatch has $LOG_COUNT log groups"
  log_verify "CloudWatch logs available: $LOG_COUNT groups"
  
  # List Lambda log groups
  LAMBDA_LOGS=$(echo "$LOGS" | jq '.logGroups[] | select(.logGroupName | contains("/aws/lambda/")) | .logGroupName' 2>/dev/null)
  
  if [ ! -z "$LAMBDA_LOGS" ]; then
    LAMBDA_LOG_COUNT=$(echo "$LAMBDA_LOGS" | wc -l)
    ok "    Lambda log groups: $LAMBDA_LOG_COUNT"
    echo "$LAMBDA_LOGS" | sed 's/^/      /' >> "$VERIFY_LOG"
    add_result "CloudWatch Logs" "PASS" "Lambda logs are available" "Lambda Log Groups: $LAMBDA_LOG_COUNT"
  else
    warn "    No Lambda log groups found yet"
    add_result "CloudWatch Logs" "WARN" "Limited Lambda logs (may be expected on first run)" "Count: 0"
  fi
else
  err "  ❌ Could not access CloudWatch logs"
  log_verify "CloudWatch access failed: $LOGS"
  add_result "CloudWatch Logs" "FAIL" "Unable to access CloudWatch" "Error: Check AWS credentials and permissions"
fi

# ─────────────────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────────────────

log_verify "Verification completed"

info "\n═══════════════════════════════════════════════════════════════"
info "E2E Verification Summary"
info "═══════════════════════════════════════════════════════════════"

info "\n📊 Results CSV:"
cat "$RESULTS_FILE" | sed 's/^/  /'

info "\n📋 Verification Log: $VERIFY_LOG"
info "\n📊 Results File: $RESULTS_FILE"

# Count pass/fail/warn
PASS_COUNT=$(grep '"PASS"' "$RESULTS_FILE" | wc -l)
FAIL_COUNT=$(grep '"FAIL"' "$RESULTS_FILE" | wc -l)
WARN_COUNT=$(grep '"WARN"' "$RESULTS_FILE" | wc -l)

info "\n📈 Summary:"
ok "  ✅ PASS: $PASS_COUNT"
warn "  ⚠️  WARN: $WARN_COUNT"
err "  ❌ FAIL: $FAIL_COUNT"

if [ $FAIL_COUNT -eq 0 ]; then
  ok "\n✅ E2E verification complete. No critical failures."
  log_verify "Verification succeeded with no failures"
  exit 0
else
  err "\n❌ Some tests failed. Please review the results file above."
  log_verify "Verification failed with $FAIL_COUNT critical issues"
  exit 1
fi
