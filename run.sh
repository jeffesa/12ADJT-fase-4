#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# Tech Challenge Fase 4 — Plataforma de Feedback Serverless
# Script runner: facilita execução do projeto multi-Lambda.
#
# Funciona em: macOS, Linux, Windows (Git Bash)
# Uso: ./run.sh ou ./run.sh [comando]
# ═══════════════════════════════════════════════════════════════

set -o pipefail

# Cores (funcionam em macOS, Linux, Git Bash)
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
cd "$SCRIPT_DIR"

# ─── Validação de pré-requisitos ───────────────────────────────

check_maven() {
  if command -v mvn &>/dev/null; then
    return 0
  fi
  err "❌ Maven não encontrado."
  return 1
}

# ─── Ações ─────────────────────────────────────────────────────

quarkus_dev_receive_feedback() {
  check_maven || return 1
  info "\n🚀 Iniciando receive-feedback (porta 8080)..."
  cd "$SCRIPT_DIR/receive-feedback"
  mvn quarkus:dev
}

quarkus_dev_send_notification() {
  check_maven || return 1
  info "\n🚀 Iniciando send-notification (porta 8081)..."
  cd "$SCRIPT_DIR/send-notification"
  mvn quarkus:dev
}

quarkus_dev_generate_report() {
  check_maven || return 1
  info "\n🚀 Iniciando generate-report (porta 8082)..."
  cd "$SCRIPT_DIR/generate-report"
  mvn quarkus:dev
}

run_tests() {
  check_maven || return 1
  info "\n🧪 Rodando testes (mvn clean verify)..."
  mvn clean verify || { err "❌ Testes falharam"; return 1; }
  ok "✅ Testes passou"
}

compile() {
  check_maven || return 1
  info "\n🔨 Compilando (mvn clean compile)..."
  mvn clean compile -DskipTests || { err "❌ Compilação falhou"; return 1; }
  ok "✅ Compilação OK"
}

health_check() {
  if ! command -v curl &>/dev/null; then
    err "❌ curl não encontrado"
    return 1
  fi
  
  info "\n🩺 Health check (Quarkus Dev Mode)..."
  
  local endpoints=("http://localhost:8080" "http://localhost:8081" "http://localhost:8082")
  local names=("receive-feedback" "send-notification" "generate-report")
  
  local all_up=true
  for i in 0 1 2; do
    local code=$(curl -s -o /dev/null -w "%{http_code}" "${endpoints[$i]}" 2>/dev/null)
    if [ "$code" = "200" ] || [ "$code" = "404" ] || [ "$code" = "405" ]; then
      ok "  ✅ ${names[$i]}: UP (HTTP $code)"
    else
      err "  ❌ ${names[$i]}: DOWN"
      all_up=false
    fi
  done
  
  if [ "$all_up" = true ]; then
    ok "\n✅ Todos UP"
  else
    return 1
  fi
}

list_aws_resources() {
  if ! command -v aws &>/dev/null; then
    warn "⚠️  AWS CLI não encontrado"
    return 0
  fi
  
  info "\n📊 Recursos AWS (us-east-1)..."
  info "\n  📦 DynamoDB Tables:"
  aws dynamodb list-tables --region us-east-1 --query 'TableNames[]' --output text 2>/dev/null | tr '\t' '\n' | sed 's/^/     • /'
  
  info "\n  📨 SNS Topics:"
  aws sns list-topics --region us-east-1 --query 'Topics[].TopicArn' --output text 2>/dev/null | tr '\t' '\n' | sed 's/^/     • /'
}

validate_sam() {
  if ! command -v sam &>/dev/null; then
    err "❌ SAM CLI não encontrado"
    return 1
  fi
  info "\n🏗️  Validando SAM template..."
  sam validate --template infra/template.yaml --lint || { err "❌ Template inválido"; return 1; }
  ok "✅ SAM válido"
}

test_aws() {
  info "\n🌐 AWS Integration Testing"
  info ""
  info "   Endpoint: https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/feedback"
  info ""
  info "   Exemplos curl:"
  info ""
  info "   1. HIGH urgency (nota 2):"
  info '      curl -X POST https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/feedback \'
  info '        -H "Content-Type: application/json" \'
  info '        -d '"'"'{"descricao":"Test","nota":2}'"'"
  info ""
  info "   2. LOW urgency (nota 9):"
  info '      curl -X POST https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/feedback \'
  info '        -H "Content-Type: application/json" \'
  info '        -d '"'"'{"descricao":"Test","nota":9}'"'"
  info ""
  info "   3. Verificar DynamoDB:"
  info "      aws dynamodb scan --table-name prod-feedback-table --region us-east-1"
}

e2e_test() {
  if ! command -v curl &>/dev/null; then
    err "❌ curl não encontrado"
    return 1
  fi
  if [ ! -f "./e2e-test.sh" ]; then
    err "❌ e2e-test.sh não encontrado"
    return 1
  fi
  info "\n🧪 E2E Test..."
  bash ./e2e-test.sh
}

e2e_verify() {
  if [ ! -f "./e2e-verify.sh" ]; then
    err "❌ e2e-verify.sh não encontrado"
    return 1
  fi
  info "\n✅ E2E Verify..."
  bash ./e2e-verify.sh
}

show_help() {
  echo ""
  echo "Tech Challenge Fase 4 — Feedback Serverless"
  echo ""
  echo "Uso:"
  echo "  ./run.sh quarkus-receive      Inicia receive-feedback (porta 8080)"
  echo "  ./run.sh quarkus-notify       Inicia send-notification (porta 8081)"
  echo "  ./run.sh quarkus-report       Inicia generate-report (porta 8082)"
  echo "  ./run.sh tests                Rodar testes (mvn clean verify)"
  echo "  ./run.sh compile              Compilar (mvn clean compile)"
  echo "  ./run.sh health               Health check dos 3 serviços"
  echo "  ./run.sh aws-list             Listar recursos na AWS"
  echo "  ./run.sh sam-validate         Validar SAM template"
  echo "  ./run.sh aws-test             Info: exemplos curl para testar na AWS"
  echo "  ./run.sh e2e-test             E2E test (rodar testes em AWS)"
  echo "  ./run.sh e2e-verify           E2E verify (validar resultados)"
  echo ""
  echo "Exemplo — Começar a desenvolver:"
  echo "  Terminal 1: ./run.sh quarkus-receive"
  echo "  Terminal 2: ./run.sh quarkus-notify"
  echo "  Terminal 3: ./run.sh quarkus-report"
  echo ""
}

# ─── Entry point ───────────────────────────────────────────────

if [ -z "$1" ]; then
  show_help
else
  case "$1" in
    quarkus-receive)  quarkus_dev_receive_feedback ;;
    quarkus-notify)   quarkus_dev_send_notification ;;
    quarkus-report)   quarkus_dev_generate_report ;;
    tests|test)       run_tests ;;
    compile|build)    compile ;;
    health|check)     health_check ;;
    aws-list)         list_aws_resources ;;
    sam-validate)     validate_sam ;;
    aws-test)         test_aws ;;
    e2e-test)         e2e_test ;;
    e2e-verify)       e2e_verify ;;
    *)                show_help ;;
  esac
fi
