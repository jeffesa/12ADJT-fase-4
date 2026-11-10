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

run_quarkus_dev() {
  check_maven || return 1
  
  info "\n🚀 Iniciando Quarkus Dev Mode para os 3 módulos..."
  info "   Abra 3 terminais separados e execute em cada um:"
  info ""
  info "   Terminal 1:"
  info "     cd /Users/jefferson/Documents/FIAP/fase-4/receive-feedback"
  info "     mvn quarkus:dev"
  info ""
  info "   Terminal 2:"
  info "     cd /Users/jefferson/Documents/FIAP/fase-4/send-notification"
  info "     mvn quarkus:dev"
  info ""
  info "   Terminal 3:"
  info "     cd /Users/jefferson/Documents/FIAP/fase-4/generate-report"
  info "     mvn quarkus:dev"
  info ""
  info "📍 Local Endpoints (uma vez que os módulos estiverem running):"
  info "   receive-feedback: http://localhost:8080"
  info "   send-notification: http://localhost:8081"
  info "   generate-report: http://localhost:8082"
  info ""
  info "💡 Para testes de integração, use os endpoints em AWS:"
  info "   https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/"
}

stop_quarkus_dev() {
  warn "\n⏹️  Para parar Quarkus Dev Mode:"
  warn "   Em cada terminal onde você executou 'mvn quarkus:dev', pressione Ctrl+C"
  warn ""
  warn "   Isso matará os processos Maven e fechará os servidores locais."
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



health_check() {
  info "\n🩺 Verificando saúde dos serviços (Quarkus Dev Mode)..."
  
  local dev_endpoints=(
    "http://localhost:8080"
    "http://localhost:8081"
    "http://localhost:8082"
  )
  
  local port_names=("receive-feedback" "send-notification" "generate-report")
  
  local all_up=true
  for i in "${!dev_endpoints[@]}"; do
    local code=$(curl -s -o /dev/null -w "%{http_code}" "${dev_endpoints[$i]}" 2>/dev/null)
    if [ "$code" = "200" ] || [ "$code" = "404" ] || [ "$code" = "405" ]; then
      ok "  ✅ ${port_names[$i]} (${dev_endpoints[$i]}): UP"
    else
      err "  ❌ ${port_names[$i]} (${dev_endpoints[$i]}): DOWN (HTTP $code)"
      all_up=false
    fi
  done
  
  if [ "$all_up" = true ]; then
    ok "\n✅ Todos os serviços estão UP"
  else
    warn "\n⚠️  Alguns serviços estão DOWN. Execute 'mvn quarkus:dev' em terminais separados."
    return 1
  fi
}

list_resources() {
  info "\n📊 Verificando recursos na AWS (producción)..."
  
  if ! command -v aws &>/dev/null; then
    warn "⚠️  AWS CLI não encontrado. Instale para verificar recursos em produção."
    return 0
  fi
  
  info "\n  📦 DynamoDB Tables:"
  aws dynamodb list-tables --region sa-east-1 --query 'TableNames[]' --output text 2>/dev/null | tr '\t' '\n' | sed 's/^/     - /' || info "     (não conseguiu listar)"
  
  info "\n  📨 SNS Topics:"
  aws sns list-topics --region sa-east-1 --query 'Topics[].TopicArn' --output text 2>/dev/null | tr '\t' '\n' | sed 's/^/     - /' || info "     (não conseguiu listar)"
  
  info "\n💡 Nota: Recursos locais estão em memória durante dev mode. Para persistência, use AWS."
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

test_aws_integration() {
  info "\n🌐 Testing Integration on AWS..."
  info ""
  info "   Use Postman or curl to test against the live AWS deployment:"
  info "   Base URL: https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/"
  info ""
  info "   Example (create feedback):"
  info "   curl -X POST https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/feedback \\"
  info "     -H 'Content-Type: application/json' \\"
  info "     -d '{\"descricao\":\"Test\",\"nota\":5}'"
  info ""
  info "   See docs/api-collection/ for Postman collection."
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
  echo "  ╔ Quarkus Dev Mode (Local Development) ╗"
  echo "  1) Iniciar Quarkus Dev Mode (instruções)"
  echo "  2) Parar Quarkus Dev Mode (instruções)"
  echo ""
  echo "  ╔ Build & Test ╗"
  echo "  3) Rodar testes (mvn clean verify)"
  echo "  4) Compilar módulos (mvn clean compile)"
  echo ""
  echo "  ╔ AWS Integration Testing ╗"
  echo "  5) Testar no AWS (instruções)"
  echo "  6) Health check dos serviços (local)"
  echo "  7) Listar recursos na AWS"
  echo "  8) Validar SAM template (sam validate)"
  echo ""
  echo "  ╔ Testes End-to-End ╗"
  echo "  11) E2E Test: Run all three feedback flows"
  echo "  12) E2E Verify: Check results and collect evidence"
  echo ""
  echo "  0) Sair"
  echo ""
  read -r -p "Escolha uma opção: " option
  case $option in
    1) run_quarkus_dev && show_menu ;;
    2) stop_quarkus_dev && show_menu ;;
    3) run_tests && show_menu ;;
    4) compile_modules && show_menu ;;
    5) test_aws_integration && show_menu ;;
    6) health_check && show_menu ;;
    7) list_resources && show_menu ;;
    8) validate_sam && show_menu ;;
    11) e2e_test && show_menu ;;
    12) e2e_verify && show_menu ;;
    0) echo "👋 Até mais!" && exit 0 ;;
    *) err "❌ Opção inválida." && show_menu ;;
  esac
}

# ─── Entry point ───────────────────────────────────────────────

if [ -n "$1" ]; then
  case $1 in
    quarkus|dev)     run_quarkus_dev ;;
    stop)            stop_quarkus_dev ;;
    tests|test)      run_tests ;;
    compile|build)   compile_modules ;;
    health)          health_check ;;
    resources)       list_resources ;;
    sam-validate)    validate_sam ;;
    aws-test)        test_aws_integration ;;
    e2e-test)        e2e_test ;;
    e2e-verify)      e2e_verify ;;
    *)               echo "Uso: ./run.sh [quarkus|stop|tests|compile|health|resources|sam-validate|aws-test|e2e-test|e2e-verify]" ;;
  esac
else
  show_menu
fi
