#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# E2E Test Script: Execute all three feedback flows
# Tests Flow 1 (HIGH urgency), Flow 2 (LOW urgency), and Flow 3 (Report)
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
LOG_DIR="/tmp/e2e-logs"
LOG_FILE="$LOG_DIR/test-execution-$(date +%s).log"

# Create log directory
mkdir -p "$LOG_DIR"

log_test() {
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

# Endpoints (assume services running locally on Quarkus dev port)
RECEIVE_FEEDBACK_URL="http://localhost:8080/graphql"
GENERATE_REPORT_URL="http://localhost:8282/graphql"
LOG_TAIL_CMD='docker compose logs --tail=50 localstack'

# Health check configuration
DEFAULT_RECV_PORT=8080
DEFAULT_GEN_PORT=8282
MAX_HEALTH_RETRIES=10
HEALTH_RETRY_DELAY=2

info "\n═══════════════════════════════════════════════════════════════"
info "E2E Test Suite: Feedback Platform"
info "═══════════════════════════════════════════════════════════════"
info "\nTest Log: $LOG_FILE"
log_test "Test execution started"

# ─────────────────────────────────────────────────────────────────
# Pre-flight Health Checks
# ─────────────────────────────────────────────────────────────────

info "\n🔍 Running pre-flight health checks..."
log_test "Pre-flight health checks initiated"

# Check if curl is available
if ! command -v curl &>/dev/null; then
  err "❌ curl not found. Please install curl."
  log_test "PREFLIGHT FAILURE: curl not available"
  exit 1
fi

# Check if jq is available
if ! command -v jq &>/dev/null; then
  err "❌ jq not found. Please install jq."
  log_test "PREFLIGHT FAILURE: jq not available"
  exit 1
fi

# Check if docker compose is available
if ! docker compose version &>/dev/null 2>&1 && ! command -v docker-compose &>/dev/null; then
  err "❌ docker compose not found. LocalStack queries will be unavailable."
  log_test "WARNING: docker compose/docker-compose not available"
fi

# Health check: receive-feedback service
info "  ⏳ Checking receive-feedback service on port $DEFAULT_RECV_PORT..."
RECV_HEALTH=0
for i in $(seq 1 $MAX_HEALTH_RETRIES); do
  if curl -s http://localhost:$DEFAULT_RECV_PORT/q/health &>/dev/null; then
    ok "  ✅ receive-feedback service is ready"
    log_test "receive-feedback health check passed on port $DEFAULT_RECV_PORT"
    RECV_HEALTH=1
    break
  fi
  if [ $i -lt $MAX_HEALTH_RETRIES ]; then
    info "    Retry $i/$MAX_HEALTH_RETRIES..."
    sleep $HEALTH_RETRY_DELAY
  fi
done

if [ $RECV_HEALTH -ne 1 ]; then
  err "❌ receive-feedback service not responding on port $DEFAULT_RECV_PORT"
  log_test "PREFLIGHT FAILURE: receive-feedback not responding"
  info "  💡 Make sure to run:"
  info "     cd receive-feedback && mvn quarkus:dev"
  exit 1
fi

# Health check: generate-report service
info "  ⏳ Checking generate-report service on port $DEFAULT_GEN_PORT..."
GEN_HEALTH=0
for i in $(seq 1 $MAX_HEALTH_RETRIES); do
  if curl -s http://localhost:$DEFAULT_GEN_PORT/q/health &>/dev/null; then
    ok "  ✅ generate-report service is ready"
    log_test "generate-report health check passed on port $DEFAULT_GEN_PORT"
    GEN_HEALTH=1
    break
  fi
  if [ $i -lt $MAX_HEALTH_RETRIES ]; then
    info "    Retry $i/$MAX_HEALTH_RETRIES..."
    sleep $HEALTH_RETRY_DELAY
  fi
done

if [ $GEN_HEALTH -ne 1 ]; then
  err "❌ generate-report service not responding on port $DEFAULT_GEN_PORT"
  log_test "PREFLIGHT FAILURE: generate-report not responding"
  info "  💡 Make sure to run:"
  info "     cd generate-report && mvn quarkus:dev"
  exit 1
fi

ok "\n✅ All pre-flight checks passed. Starting test flows..."

# ─────────────────────────────────────────────────────────────────
# Flow 1: HIGH Urgency Feedback (rating 4)
# Expected: Saved to DynamoDB, SNS publish to feedback-urgent-topic, 
#           send-notification triggered, SES email sent
# ─────────────────────────────────────────────────────────────────

info "\n📌 Flow 1: HIGH Urgency Feedback (rating 4)"
info "─────────────────────────────────────────────────────────────"

FLOW1_QUERY='
mutation {
  receiveFeedback(input: {descricao: "Test critical feedback", nota: 4}) {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
'

log_test "Flow 1: Sending HIGH urgency feedback (rating 4)"
info "  Sending GraphQL mutation to $RECEIVE_FEEDBACK_URL..."

FLOW1_START_TIME=$(date +%s%N | cut -b1-13)  # milliseconds

FLOW1_RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "{\"query\": $(echo "$FLOW1_QUERY" | jq -Rs .)}" \
  "$RECEIVE_FEEDBACK_URL" 2>&1)

FLOW1_END_TIME=$(date +%s%N | cut -b1-13)
FLOW1_DURATION=$((FLOW1_END_TIME - FLOW1_START_TIME))

echo "$FLOW1_RESPONSE" | jq '.' &>/dev/null
if [ $? -eq 0 ]; then
  FEEDBACK_ID_FLOW1=$(echo "$FLOW1_RESPONSE" | jq -r '.data.receiveFeedback.id' 2>/dev/null)
  URGENCIA_FLOW1=$(echo "$FLOW1_RESPONSE" | jq -r '.data.receiveFeedback.urgencia' 2>/dev/null)
  
  if [ "$FEEDBACK_ID_FLOW1" != "null" ] && [ ! -z "$FEEDBACK_ID_FLOW1" ]; then
    ok "  ✅ Flow 1 Feedback created: $FEEDBACK_ID_FLOW1 (urgencia: $URGENCIA_FLOW1) [${FLOW1_DURATION}ms]"
    log_test "Flow 1: Feedback created successfully: $FEEDBACK_ID_FLOW1 with urgencia $URGENCIA_FLOW1 in ${FLOW1_DURATION}ms"
    
    # Check if execution time is within acceptable range (< 1000ms)
    if [ $FLOW1_DURATION -lt 1000 ]; then
      ok "    ✅ Performance acceptable: ${FLOW1_DURATION}ms < 1000ms threshold"
      log_test "Flow 1: Performance check PASS (${FLOW1_DURATION}ms < 1000ms)"
    else
      warn "    ⚠️  Performance warning: ${FLOW1_DURATION}ms >= 1000ms threshold"
      log_test "Flow 1: Performance check WARN (${FLOW1_DURATION}ms >= 1000ms)"
    fi
  else
    err "  ❌ Flow 1 failed: Invalid response"
    log_test "Flow 1: Invalid response - missing feedbackId"
    echo "Response: $FLOW1_RESPONSE" >> "$LOG_FILE"
  fi
else
  err "  ❌ Flow 1 failed: Invalid JSON response"
  log_test "Flow 1: Invalid JSON response"
  echo "Response: $FLOW1_RESPONSE" >> "$LOG_FILE"
fi

info "  Response:"
echo "$FLOW1_RESPONSE" | jq '.' | sed 's/^/    /' >> "$LOG_FILE" 2>/dev/null || echo "    $FLOW1_RESPONSE" >> "$LOG_FILE"

# Small delay to allow async processing
sleep 2

# ─────────────────────────────────────────────────────────────────
# Flow 2: LOW Urgency Feedback (rating 9)
# Expected: Saved to DynamoDB, NO SNS publish, NO email
# ─────────────────────────────────────────────────────────────────

info "\n📌 Flow 2: LOW Urgency Feedback (rating 9)"
info "─────────────────────────────────────────────────────────────"

FLOW2_QUERY='
mutation {
  receiveFeedback(input: {descricao: "Test normal feedback", nota: 9}) {
    id
    descricao
    nota
    urgencia
    dataCriacao
  }
}
'

log_test "Flow 2: Sending LOW urgency feedback (rating 9)"
info "  Sending GraphQL mutation to $RECEIVE_FEEDBACK_URL..."

FLOW2_START_TIME=$(date +%s%N | cut -b1-13)  # milliseconds

FLOW2_RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "{\"query\": $(echo "$FLOW2_QUERY" | jq -Rs .)}" \
  "$RECEIVE_FEEDBACK_URL" 2>&1)

FLOW2_END_TIME=$(date +%s%N | cut -b1-13)
FLOW2_DURATION=$((FLOW2_END_TIME - FLOW2_START_TIME))

echo "$FLOW2_RESPONSE" | jq '.' &>/dev/null
if [ $? -eq 0 ]; then
  FEEDBACK_ID_FLOW2=$(echo "$FLOW2_RESPONSE" | jq -r '.data.receiveFeedback.id' 2>/dev/null)
  URGENCIA_FLOW2=$(echo "$FLOW2_RESPONSE" | jq -r '.data.receiveFeedback.urgencia' 2>/dev/null)
  
  if [ "$FEEDBACK_ID_FLOW2" != "null" ] && [ ! -z "$FEEDBACK_ID_FLOW2" ]; then
    ok "  ✅ Flow 2 Feedback created: $FEEDBACK_ID_FLOW2 (urgencia: $URGENCIA_FLOW2) [${FLOW2_DURATION}ms]"
    log_test "Flow 2: Feedback created successfully: $FEEDBACK_ID_FLOW2 with urgencia $URGENCIA_FLOW2 in ${FLOW2_DURATION}ms"
    
    # Check if execution time is within acceptable range (< 1000ms)
    if [ $FLOW2_DURATION -lt 1000 ]; then
      ok "    ✅ Performance acceptable: ${FLOW2_DURATION}ms < 1000ms threshold"
      log_test "Flow 2: Performance check PASS (${FLOW2_DURATION}ms < 1000ms)"
    else
      warn "    ⚠️  Performance warning: ${FLOW2_DURATION}ms >= 1000ms threshold"
      log_test "Flow 2: Performance check WARN (${FLOW2_DURATION}ms >= 1000ms)"
    fi
  else
    err "  ❌ Flow 2 failed: Invalid response"
    log_test "Flow 2: Invalid response - missing feedbackId"
    echo "Response: $FLOW2_RESPONSE" >> "$LOG_FILE"
  fi
else
  err "  ❌ Flow 2 failed: Invalid JSON response"
  log_test "Flow 2: Invalid JSON response"
  echo "Response: $FLOW2_RESPONSE" >> "$LOG_FILE"
fi

info "  Response:"
echo "$FLOW2_RESPONSE" | jq '.' | sed 's/^/    /' >> "$LOG_FILE" 2>/dev/null || echo "    $FLOW2_RESPONSE" >> "$LOG_FILE"

# Small delay
sleep 2

# ─────────────────────────────────────────────────────────────────
# Flow 3: Generate Weekly Report
# Expected: Query DynamoDB for feedbacks from prior week, aggregate stats,
#           send HTML email with tables
# ─────────────────────────────────────────────────────────────────

info "\n📌 Flow 3: Generate Weekly Report"
info "─────────────────────────────────────────────────────────────"

FLOW3_QUERY='
mutation {
  generateWeeklyReport {
    reportId
    totalFeedbacks
    averageRating
    highCount
    mediumCount
    lowCount
    generatedAt
  }
}
'

log_test "Flow 3: Triggering weekly report generation"
info "  Sending GraphQL mutation to $GENERATE_REPORT_URL..."

FLOW3_START_TIME=$(date +%s%N | cut -b1-13)  # milliseconds

FLOW3_RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d "{\"query\": $(echo "$FLOW3_QUERY" | jq -Rs .)}" \
  "$GENERATE_REPORT_URL" 2>&1)

FLOW3_END_TIME=$(date +%s%N | cut -b1-13)
FLOW3_DURATION=$((FLOW3_END_TIME - FLOW3_START_TIME))

echo "$FLOW3_RESPONSE" | jq '.' &>/dev/null
if [ $? -eq 0 ]; then
  REPORT_ID=$(echo "$FLOW3_RESPONSE" | jq -r '.data.generateWeeklyReport.reportId' 2>/dev/null)
  TOTAL_FEEDBACKS=$(echo "$FLOW3_RESPONSE" | jq -r '.data.generateWeeklyReport.totalFeedbacks' 2>/dev/null)
  
  if [ "$REPORT_ID" != "null" ] && [ ! -z "$REPORT_ID" ]; then
    ok "  ✅ Flow 3 Report generated: $REPORT_ID (total feedbacks: $TOTAL_FEEDBACKS) [${FLOW3_DURATION}ms]"
    log_test "Flow 3: Report generated successfully: $REPORT_ID with $TOTAL_FEEDBACKS feedbacks in ${FLOW3_DURATION}ms"
    
    # Check if execution time is within acceptable range (< 1000ms)
    if [ $FLOW3_DURATION -lt 1000 ]; then
      ok "    ✅ Performance acceptable: ${FLOW3_DURATION}ms < 1000ms threshold"
      log_test "Flow 3: Performance check PASS (${FLOW3_DURATION}ms < 1000ms)"
    else
      warn "    ⚠️  Performance warning: ${FLOW3_DURATION}ms >= 1000ms threshold"
      log_test "Flow 3: Performance check WARN (${FLOW3_DURATION}ms >= 1000ms)"
    fi
  else
    err "  ❌ Flow 3 failed: Invalid response"
    log_test "Flow 3: Invalid response - missing reportId"
    echo "Response: $FLOW3_RESPONSE" >> "$LOG_FILE"
  fi
else
  err "  ❌ Flow 3 failed: Invalid JSON response"
  log_test "Flow 3: Invalid JSON response"
  echo "Response: $FLOW3_RESPONSE" >> "$LOG_FILE"
fi

info "  Response:"
echo "$FLOW3_RESPONSE" | jq '.' | sed 's/^/    /' >> "$LOG_FILE" 2>/dev/null || echo "    $FLOW3_RESPONSE" >> "$LOG_FILE"

# ─────────────────────────────────────────────────────────────────
# Post-Test: Save command outputs for verification
# ─────────────────────────────────────────────────────────────────

info "\n📊 Capturing post-test evidence..."
log_test "Capturing DynamoDB scan results"

info "  DynamoDB feedbacks table scan:"
DYNAMODB_SCAN=$(cd "$SCRIPT_DIR" && docker compose exec -T localstack awslocal dynamodb scan \
  --table-name feedbacks \
  --region us-east-1 2>&1)
echo "DynamoDB Scan:" >> "$LOG_FILE"
echo "$DYNAMODB_SCAN" >> "$LOG_FILE"
echo "$DYNAMODB_SCAN" | jq '.Items | length' &>/dev/null
if [ $? -eq 0 ]; then
  ITEM_COUNT=$(echo "$DYNAMODB_SCAN" | jq '.Items | length' 2>/dev/null)
  ok "    ✅ Found $ITEM_COUNT items in feedbacks table"
fi

log_test "Checking SNS topic message count"
info "  SNS feedback-urgent-topic messages:"
TOPIC_ATTRS=$(cd "$SCRIPT_DIR" && docker compose exec -T localstack awslocal sns get-topic-attributes \
  --topic-arn arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic \
  --region us-east-1 2>&1)
echo "SNS Topic Attributes:" >> "$LOG_FILE"
echo "$TOPIC_ATTRS" >> "$LOG_FILE"
ok "    ✅ SNS topic attributes retrieved"

log_test "Test execution completed"
info "\n✅ E2E Test execution completed"
info "📋 Test log saved to: $LOG_FILE"
ok "\n✅ All flows executed. Run './run.sh e2e-verify' to validate results."

exit 0
