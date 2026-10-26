#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# Tech Challenge Fase 4 — Plataforma de Feedback Serverless
# Script runner: facilita execução do projeto multi-Lambda.
#
# Uso interativo:  ./run.sh
# Uso direto:      ./run.sh [docker|stop|tests|compile|clean|logs|health|resources|sam-validate]
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
cd "$SCRIPT_DIR"

# ─── Validação de pré-requisitos ───────────────────────────────

check_docker() {
  if docker ps &>/dev/null; then
    return 0
  fi
  # macOS com Colima
  if command -v colima &>/dev/null; then
    warn "⚠️  Docker não está rodando. Tentando iniciar Colima..."
    colima start 2>/dev/null || true
    sleep 3
  fi
  if docker ps &>/dev/null; then
    return 0
  fi
  err "❌ Docker não está rodando."
  echo "   macOS: abra Docker Desktop ou execute 'colima start'"
  echo "   Linux: sudo systemctl start docker"
  return 1
}

check_docker_compose() {
  if docker compose version &>/dev/null 2>&1; then
    return 0
  elif command -v docker-compose &>/dev/null; then
    return 0
  else
    err "❌ Docker Compose não encontrado."
    return 1
  fi
}

check_java() {
  if command -v java &>/dev/null; then
    return 0
  fi
  warn "⚠️  Java não encontrado (necessário para testes)."
  return 0
}

check_maven() {
  if command -v mvn &>/dev/null; then
    return 0
  fi
  warn "⚠️  Maven não encontrado (necessário para compilar/testar)."
  return 0
}

compose() {
  if docker compose version &>/dev/null 2>&1; then
    docker compose "$@"
  elif command -v docker-compose &>/dev/null; then
    docker-compose "$@"
  else
    err "❌ docker compose/docker-compose não encontrados"
    return 1
  fi
}

# ─── Ações ─────────────────────────────────────────────────────

run_docker() {
  info "\n🐳 Subindo LocalStack via Docker Compose..."
  check_docker || return 1
  check_docker_compose || return 1
  
  compose down --remove-orphans 2>/dev/null || true
  compose up --build -d || { err "❌ Falha ao subir containers"; return 1; }
  
  ok "✅ Containers iniciados. Aguardando healthcheck..."
  RETRY=0
  until compose exec -T localstack awslocal ec2 describe-regions &>/dev/null 2>&1; do
    RETRY=$((RETRY + 1))
    if [ $RETRY -ge 60 ]; then
      err "❌ LocalStack não respondeu após 60s"
      return 1
    fi
    echo -n "."
    sleep 1
  done
  ok "\n✅ LocalStack pronto!"
  
  info "\n📍 Endpoints LocalStack:"
  info "   Edge:      http://localhost:4566"
  info "   DynamoDB:  http://localhost:4569"
  info "   SNS:       http://localhost:4575"
  info "   SQS:       http://localhost:4576"
  
  info "\n📚 Próximos passos:"
  info "   Abra 3 terminais separados e execute:"
  info "   Terminal 1: cd receive-feedback && mvn quarkus:dev"
  info "   Terminal 2: cd send-notification && mvn quarkus:dev"
  info "   Terminal 3: cd generate-report && mvn quarkus:dev"
}

stop_docker() {
  info "\n🛑 Parando LocalStack..."
  check_docker || return 1
  compose down
  ok "✅ Containers parados"
}

run_tests() {
  check_java || return 1
  check_maven || return 1
  info "\n🧪 Rodando testes (mvn clean verify)..."
  mvn clean verify || { err "❌ Testes falharam"; return 1; }
  ok "✅ Testes passaram com sucesso"
}

compile_modules() {
  check_maven || return 1
  info "\n🔨 Compilando módulos (mvn clean compile)..."
  mvn clean compile -DskipTests || { err "❌ Compilação falhou"; return 1; }
  ok "✅ Módulos compilados com sucesso"
}

clean() {
  info "\n🗑️  Limpando volumes e dados..."
  check_docker || return 1
  compose down -v
  ok "✅ Volumes removidos. Execute './run.sh docker' para subir novamente"
}

show_logs() {
  info "\n📋 Mostrando logs do LocalStack (Ctrl+C para sair)..."
  check_docker || return 1
  compose logs -f localstack
}

health_check() {
  info "\n🩺 Verificando saúde dos serviços..."
  
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:4566" 2>/dev/null)
  if [ "$code" = "200" ] || [ "$code" = "404" ]; then
    ok "  ✅ LocalStack (edge:4566): UP"
  else
    err "  ❌ LocalStack (edge:4566): DOWN (HTTP $code)"
    warn "  Dica: Execute './run.sh docker' para iniciar"
    return 1
  fi
}

list_resources() {
  info "\n📊 Listando recursos no LocalStack..."
  
  if ! compose exec -T localstack awslocal ec2 describe-regions &>/dev/null 2>&1; then
    err "❌ LocalStack não está pronto. Execute './run.sh docker' primeiro."
    return 1
  fi
  
  info "\n  📦 DynamoDB Tables:"
  if compose exec -T localstack awslocal dynamodb list-tables 2>/dev/null | grep -q '"TableNames"'; then
    compose exec -T localstack awslocal dynamodb list-tables --query 'TableNames[]' --output text 2>/dev/null | tr '\t' '\n' | sed 's/^/     - /'
  else
    info "     (nenhuma)"
  fi
  
  info "\n  📨 SNS Topics:"
  if compose exec -T localstack awslocal sns list-topics 2>/dev/null | grep -q '"Topics"'; then
    compose exec -T localstack awslocal sns list-topics --query 'Topics[].TopicArn' --output text 2>/dev/null | tr '\t' '\n' | sed 's/^/     - /'
  else
    info "     (nenhum)"
  fi
  
  info "\n  📬 SQS Queues:"
  if compose exec -T localstack awslocal sqs list-queues 2>/dev/null | grep -q '"QueueUrls"'; then
    compose exec -T localstack awslocal sqs list-queues --query 'QueueUrls[]' --output text 2>/dev/null | tr '\t' '\n' | sed 's/^/     - /'
  else
    info "     (nenhuma)"
  fi
}

validate_sam() {
  if ! command -v sam &>/dev/null; then
    err "❌ SAM CLI não encontrado."
    echo "   Instale: https://aws.amazon.com/serverless/sam/get-started/"
    return 1
  fi
  info "\n🏗️  Validando SAM template (infra/template.yaml)..."
  sam validate --template infra/template.yaml || { err "❌ Template inválido"; return 1; }
  ok "✅ Template SAM válido"
}

e2e_setup() {
  info "\n🚀 E2E Setup: Starting LocalStack and verifying resources..."
  
  check_docker || return 1
  check_docker_compose || return 1
  
  # Start LocalStack
  run_docker || return 1
  
  # Wait for services to be ready
  info "\n⏳ Waiting for DynamoDB, SNS, SES to be accessible..."
  RETRY=0
  until compose exec -T localstack awslocal dynamodb list-tables &>/dev/null 2>&1; do
    RETRY=$((RETRY + 1))
    if [ $RETRY -ge 60 ]; then
      err "❌ AWS services not ready after 60s"
      return 1
    fi
    echo -n "."
    sleep 1
  done
  
  ok "\n✅ AWS services are ready"
  
  # Verify SES domain (mock in LocalStack)
  info "\n📧 Verifying SES email identities..."
  for email in "noreply@feedback-fase-4.com" "admin@feedback-fase-4.com"; do
    compose exec -T localstack awslocal ses verify-email-identity --email-address "$email" &>/dev/null 2>&1
    ok "  ✅ Verified: $email"
  done
  
  # Verify resources were created
  info "\n📊 Verifying resources created by initialization..."
  
  # Check DynamoDB table
  if compose exec -T localstack awslocal dynamodb describe-table --table-name feedbacks &>/dev/null 2>&1; then
    ok "  ✅ DynamoDB table 'feedbacks' exists (hash: feedbackId, range: createdAt)"
  else
    err "  ❌ DynamoDB table 'feedbacks' not found"
    return 1
  fi
  
  # Check SNS topics
  for topic in "feedback-urgent-topic" "feedback-report-topic"; do
    if compose exec -T localstack awslocal sns list-topics 2>/dev/null | grep -q "$topic"; then
      ok "  ✅ SNS topic '$topic' exists"
    else
      err "  ❌ SNS topic '$topic' not found"
      return 1
    fi
  done
  
  # Check SQS queues
  for queue in "notification-queue" "report-queue"; do
    if compose exec -T localstack awslocal sqs list-queues 2>/dev/null | grep -q "$queue"; then
      ok "  ✅ SQS queue '$queue' exists"
    else
      err "  ❌ SQS queue '$queue' not found"
      return 1
    fi
  done
  
  ok "\n✅ E2E Setup completed successfully!"
  info "\n📝 Next steps:"
  info "   Terminal 1: cd receive-feedback && mvn quarkus:dev"
  info "   Terminal 2: cd send-notification && mvn quarkus:dev"
  info "   Terminal 3: cd generate-report && mvn quarkus:dev"
  info "   Then run: ./run.sh e2e-test"
}

e2e_test() {
  info "\n🧪 E2E Test: Running all three feedback flows..."
  
  if ! command -v curl &>/dev/null; then
    err "❌ curl is required for E2E tests"
    return 1
  fi
  
  # Run the e2e test script
  if [ -f "./e2e-test.sh" ]; then
    bash ./e2e-test.sh
    return $?
  else
    err "❌ e2e-test.sh not found"
    return 1
  fi
}

e2e_verify() {
  info "\n✅ E2E Verify: Checking test results and collecting evidence..."
  
  # Run the e2e verification script
  if [ -f "./e2e-verify.sh" ]; then
    bash ./e2e-verify.sh
    return $?
  else
    err "❌ e2e-verify.sh not found"
    return 1
  fi
}

# ─── Menu interativo ───────────────────────────────────────────

show_menu() {
  echo ""
  printf "${C_CYAN}╔════════════════════════════════════════════════════╗${C_RESET}\n"
  printf "${C_CYAN}║   Tech Challenge Fase 4 — Feedback Serverless      ║${C_RESET}\n"
  printf "${C_CYAN}║   Menu de Desenvolvimento Local                    ║${C_RESET}\n"
  printf "${C_CYAN}╚════════════════════════════════════════════════════╝${C_RESET}\n"
  echo ""
  echo "  1) Subir LocalStack (Docker Compose up --build)"
  echo "  2) Parar LocalStack (Docker Compose down)"
  echo "  3) Rodar testes (mvn clean verify)"
  echo "  4) Compilar módulos (mvn clean compile)"
  echo "  5) Limpar volumes (Docker Compose down -v)"
  echo "  6) Ver logs (Docker Compose logs -f)"
  echo "  7) Health check dos serviços"
  echo "  8) Listar recursos (DynamoDB, SNS, SQS)"
  echo "  9) Validar SAM template (sam validate)"
  echo ""
  echo "  11) E2E Setup: Start LocalStack and verify resources"
  echo "  12) E2E Test: Run all three feedback flows"
  echo "  13) E2E Verify: Check results and collect evidence"
  echo ""
  echo "  0) Sair"
  echo ""
  read -r -p "Escolha uma opção: " option
  case $option in
    1) run_docker && show_menu ;;
    2) stop_docker && show_menu ;;
    3) run_tests && show_menu ;;
    4) compile_modules && show_menu ;;
    5) clean && show_menu ;;
    6) show_logs ;;
    7) health_check && show_menu ;;
    8) list_resources && show_menu ;;
    9) validate_sam && show_menu ;;
    11) e2e_setup && show_menu ;;
    12) e2e_test && show_menu ;;
    13) e2e_verify && show_menu ;;
    0) echo "👋 Até mais!" && exit 0 ;;
    *) err "❌ Opção inválida." && show_menu ;;
  esac
}

# ─── Entry point ───────────────────────────────────────────────

if [ -n "$1" ]; then
  case $1 in
    docker|up)      run_docker ;;
    stop|down)      stop_docker ;;
    tests|test)     run_tests ;;
    compile|build)  compile_modules ;;
    clean)          clean ;;
    logs)           show_logs ;;
    health)         health_check ;;
    resources)      list_resources ;;
    sam-validate)   validate_sam ;;
    e2e-setup)      e2e_setup ;;
    e2e-test)       e2e_test ;;
    e2e-verify)     e2e_verify ;;
    *)              echo "Uso: ./run.sh [docker|stop|tests|compile|clean|logs|health|resources|sam-validate|e2e-setup|e2e-test|e2e-verify]" ;;
  esac
else
  show_menu
fi
