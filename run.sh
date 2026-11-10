#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# Tech Challenge Fase 4 — Plataforma de Feedback Serverless
# Script runner: facilita execução do projeto multi-Lambda.
#
# Uso interativo:  ./run.sh
# Uso direto:      ./run.sh [quarkus|stop|tests|compile|health|resources|sam-validate|aws-test|e2e-test|e2e-verify]
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

check_java() {
  if command -v java &>/dev/null; then
    return 0
  fi
  err "❌ Java não encontrado. Necessário para Quarkus Dev Mode."
  echo "   Instale Java 17+: https://www.java.com/download"
  return 1
}

check_maven() {
  if command -v mvn &>/dev/null; then
    return 0
  fi
  err "❌ Maven não encontrado. Necessário para compilar e testar."
  echo "   Instale Maven 3.8+: https://maven.apache.org/download.cgi"
  return 1
}

# ─── Ações ─────────────────────────────────────────────────────

run_quarkus_dev() {
  check_maven || return 1
  
  info "\n🚀 Iniciando Quarkus Dev Mode para os 3 módulos..."
  info ""
  info "   Abra 3 terminais separados e execute em cada um:"
  info ""
  info "   📍 TERMINAL 1 (receive-feedback) — porta 8080:"
  info "      cd $SCRIPT_DIR/receive-feedback"
  info "      mvn quarkus:dev"
  info ""
  info "   📍 TERMINAL 2 (send-notification) — porta 8081:"
  info "      cd $SCRIPT_DIR/send-notification"
  info "      mvn quarkus:dev"
  info ""
  info "   📍 TERMINAL 3 (generate-report) — porta 8082:"
  info "      cd $SCRIPT_DIR/generate-report"
  info "      mvn quarkus:dev"
  info ""
  info "✅ Quando todos os módulos estiverem rodando:"
  info "   • Local endpoints: http://localhost:8080 (receive-feedback)"
  info "   •                 http://localhost:8081 (send-notification)"
  info "   •                 http://localhost:8082 (generate-report)"
  info ""
  info "💡 Para testes de integração (recomendado):"
  info "   Use os endpoints em produção na AWS (veja opção 5)"
  info ""
  info "🔄 Hot Reload: Edite o código Java, salve, e recompila automático!"
  info ""
  info "📖 Para mais detalhes, veja: LOCAL_DEVELOPMENT.md"
}

stop_quarkus_dev() {
  warn "\n⏹️  Parando Quarkus Dev Mode"
  warn ""
  warn "   Em CADA terminal onde você executou 'mvn quarkus:dev':"
  warn "   • Pressione Ctrl+C"
  warn "   • Isso matará o processo Maven"
  warn "   • O servidor fechará a porta"
  warn ""
  warn "✅ Todos os serviços locais estarão parados."
}

run_tests() {
  check_maven || return 1
  info "\n🧪 Rodando testes (mvn clean verify)..."
  info "   • JUnit tests"
  info "   • JaCoCo coverage (≥80% gate)"
  info "   • SonarCloud analysis"
  info ""
  mvn clean verify || { err "❌ Testes falharam"; return 1; }
  ok "✅ Todos os testes passaram com sucesso"
  ok "📊 Relatório de cobertura: target/site/jacoco/index.html"
}

compile_modules() {
  check_maven || return 1
  info "\n🔨 Compilando módulos (mvn clean compile)..."
  mvn clean compile -DskipTests || { err "❌ Compilação falhou"; return 1; }
  ok "✅ Todos os módulos compilados com sucesso"
}

health_check() {
  info "\n🩺 Verificando saúde dos serviços (Quarkus Dev Mode)..."
  info ""
  
  if ! command -v curl &>/dev/null; then
    err "❌ curl não encontrado (necessário para health check)"
    return 1
  fi
  
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
      ok "  ✅ ${port_names[$i]}: UP (HTTP $code)"
    else
      err "  ❌ ${port_names[$i]}: DOWN (HTTP $code or no response)"
      all_up=false
    fi
  done
  
  echo ""
  if [ "$all_up" = true ]; then
    ok "✅ Todos os 3 serviços estão UP e respondendo"
  else
    warn "⚠️  Alguns serviços estão DOWN"
    info "   Execute 'mvn quarkus:dev' em 3 terminais separados"
    return 1
  fi
}

list_resources() {
  info "\n📊 Recursos AWS (Produção em us-east-1)..."
  info ""
  
  if ! command -v aws &>/dev/null; then
    warn "⚠️  AWS CLI não encontrado. Instale para listar recursos."
    return 0
  fi
  
  info "  📦 DynamoDB Tables:"
  aws dynamodb list-tables --region us-east-1 --query 'TableNames[]' --output text 2>/dev/null | tr '\t' '\n' | sed 's/^/     • /' || info "     (erro ao listar)"
  
  info ""
  info "  📨 SNS Topics:"
  aws sns list-topics --region us-east-1 --query 'Topics[].TopicArn' --output text 2>/dev/null | tr '\t' '\n' | sed 's/^/     • /' || info "     (erro ao listar)"
  
  info ""
  info "💡 Estes recursos estão em PRODUÇÃO (AWS). Use com cuidado!"
}

validate_sam() {
  if ! command -v sam &>/dev/null; then
    err "❌ SAM CLI não encontrado."
    echo ""
    echo "   Instale: https://aws.amazon.com/serverless/sam/get-started/"
    return 1
  fi
  info "\n🏗️  Validando SAM template (infra/template.yaml)..."
  info ""
  sam validate --template infra/template.yaml --lint || { err "❌ Template inválido"; return 1; }
  ok "✅ SAM template válido"
}

test_aws_integration() {
  info "\n🌐 Testing Integration on AWS (Production)"
  info ""
  info "   📍 API Endpoint:"
  info "      https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/feedback"
  info ""
  info "   📝 Exemplos de uso com curl:"
  info ""
  info "   1️⃣  Criar feedback (HIGH urgency - nota 2):"
  info '      curl -X POST https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/feedback \'
  info '        -H "Content-Type: application/json" \'
  info '        -d '"'"'{"descricao":"Test","nota":2}'"'"
  info ""
  info "   2️⃣  Criar feedback (LOW urgency - nota 9):"
  info '      curl -X POST https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/feedback \'
  info '        -H "Content-Type: application/json" \'
  info '        -d '"'"'{"descricao":"Test","nota":9}'"'"
  info ""
  info "   3️⃣  Verificar dados no DynamoDB:"
  info "      aws dynamodb scan --table-name prod-feedback-table --region us-east-1"
  info ""
  info "   📦 Para testes automatizados com Postman:"
  info "      Veja: docs/api-collection/"
  info ""
  info "   ⚠️  Nota: Estes testes usam dados REAIS em produção!"
}

e2e_test() {
  info "\n🧪 E2E Test: Testing all three Lambda functions on AWS..."
  info ""
  
  if ! command -v curl &>/dev/null; then
    err "❌ curl não encontrado (necessário para E2E tests)"
    return 1
  fi
  
  if [ ! -f "./e2e-test.sh" ]; then
    err "❌ e2e-test.sh não encontrado"
    return 1
  fi
  
  info "   Executando: bash e2e-test.sh"
  info ""
  bash ./e2e-test.sh
  return $?
}

e2e_verify() {
  info "\n✅ E2E Verify: Validating test results from AWS..."
  info ""
  
  if [ ! -f "./e2e-verify.sh" ]; then
    err "❌ e2e-verify.sh não encontrado"
    return 1
  fi
  
  info "   Executando: bash e2e-verify.sh"
  info ""
  bash ./e2e-verify.sh
  return $?
}

# ─── Menu interativo ───────────────────────────────────────────

show_menu() {
  echo ""
  printf "${C_CYAN}╔════════════════════════════════════════════════════╗${C_RESET}\n"
  printf "${C_CYAN}║   Tech Challenge Fase 4 — Feedback Serverless      ║${C_RESET}\n"
  printf "${C_CYAN}║   Quarkus Dev Mode (Local) + AWS (Production)      ║${C_RESET}\n"
  printf "${C_CYAN}╚════════════════════════════════════════════════════╝${C_RESET}\n"
  echo ""
  echo "  🚀 Quarkus Dev Mode (Local Development)"
  echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  1) Iniciar Quarkus Dev Mode (3 terminais, hot reload)"
  echo "  2) Parar Quarkus Dev Mode"
  echo ""
  echo "  🔨 Build & Testing"
  echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  3) Rodar testes (mvn clean verify)"
  echo "  4) Compilar módulos (mvn clean compile)"
  echo ""
  echo "  ☁️  AWS Integration & Production"
  echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  5) Info: Testing no AWS (instruções + exemplos curl)"
  echo "  6) Health check (verifica se serviços locais estão UP)"
  echo "  7) Listar recursos na AWS (DynamoDB, SNS)"
  echo "  8) Validar SAM template (sam validate --lint)"
  echo ""
  echo "  🧪 End-to-End Tests (AWS)"
  echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  11) E2E Test: Executar todos os testes"
  echo "  12) E2E Verify: Validar resultados"
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
