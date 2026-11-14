#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# Tech Challenge Fase 4 — Plataforma de Feedback Serverless
# Script runner: facilita execução do projeto multi-Lambda.
#
# Uso interativo:  ./run.sh
# Uso direto:      ./run.sh [quarkus-receive|quarkus-notify|quarkus-report|tests|compile|health|aws-list|sam-validate|aws-test|e2e-test|e2e-verify]
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

AWS_REGION="sa-east-1"

# ─── Validação de pré-requisitos ───────────────────────────────

check_java() {
  if ! command -v java &>/dev/null; then
    err "❌ Java não encontrado. Instale o JDK 17 ou superior e configure JAVA_HOME."
    return 1
  fi
  local version major
  version=$(java -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -n1)
  major=$(printf '%s' "$version" | sed -n 's/^\([0-9][0-9]*\).*/\1/p')
  if [ -z "$major" ] || [ "$major" -lt 17 ]; then
    err "❌ Java 17 ou superior necessário (encontrado: ${version:-desconhecido})."
    return 1
  fi
  return 0
}

check_deps() {
  local missing=0
  if ! command -v mvn &>/dev/null; then
    err "❌ Maven não encontrado. Instale o Maven (comando mvn) para compilar e rodar os módulos."
    missing=1
  fi
  check_java || missing=1
  return $missing
}

# ─── Ações ─────────────────────────────────────────────────────

quarkus_dev_receive_feedback() {
  check_deps || return 1
  info "\n🚀 Iniciando receive-feedback (porta 8080)..."
  info "    Bloqueia terminal. Use Ctrl+C para parar.\n"
  cd "$SCRIPT_DIR/receive-feedback"
  exec mvn quarkus:dev -Ddebug=false -Dquarkus.console.legacy=true -Dquarkus.http.port=8080
}

quarkus_dev_send_notification() {
  check_deps || return 1
  info "\n🚀 Iniciando send-notification (porta 8081)..."
  info "    Bloqueia terminal. Use Ctrl+C para parar.\n"
  cd "$SCRIPT_DIR/send-notification"
  exec mvn quarkus:dev -Ddebug=false -Dquarkus.console.legacy=true -Dquarkus.http.port=8081
}

quarkus_dev_generate_report() {
  check_deps || return 1
  info "\n🚀 Iniciando generate-report (porta 8082)..."
  info "    Bloqueia terminal. Use Ctrl+C para parar.\n"
  cd "$SCRIPT_DIR/generate-report"
  exec mvn quarkus:dev -Ddebug=false -Dquarkus.console.legacy=true -Dquarkus.http.port=8082
}

run_tests() {
  check_deps || return 1
  info "\n🧪 Rodando testes (mvn clean verify)..."
  mvn clean verify || { err "❌ Testes falharam"; return 1; }
  ok "✅ Testes passou"
}

compile_modules() {
  check_deps || return 1
  info "\n🔨 Compilando (mvn clean compile)..."
  mvn clean compile -DskipTests || { err "❌ Compilação falhou"; return 1; }
  ok "✅ Compilação OK"
}

check_module_health() {
  local name=$1 port=$2
  local response code body
  response=$(curl -s -m 5 -w '\n%{http_code}' "http://localhost:${port}/q/health" 2>/dev/null)
  code=${response##*$'\n'}
  body=${response%$'\n'*}
  if [ "$code" = "200" ] && [[ "$body" == *'"UP"'* ]]; then
    ok "  ✅ ${name}: UP (HTTP $code, porta $port)"
    return 0
  fi
  err "  ❌ ${name}: DOWN (HTTP $code, porta $port). Verifique se o módulo está rodando e tem quarkus-smallrye-health."
  return 1
}

health_check() {
  if ! command -v curl &>/dev/null; then
    err "❌ curl não encontrado"
    return 1
  fi

  info "\n🩺 Health check (Quarkus /q/health, módulos locais)..."

  local ports=(8080 8081 8082)
  local names=("receive-feedback" "send-notification" "generate-report")

  local all_up=true
  for i in 0 1 2; do
    check_module_health "${names[$i]}" "${ports[$i]}" || all_up=false
  done

  if [ "$all_up" = true ]; then
    ok "\n✅ Todos UP"
  else
    err "\n❌ Nem todos os módulos estão UP"
    return 1
  fi
}

print_aws_items() {
  local raw=$1 pattern=$2
  local items
  items=$(printf '%s\n' "$raw" | tr '\t' '\n' | grep -E "$pattern")
  if [ -z "$items" ]; then
    warn "     ⚠️  Nenhum item encontrado"
  else
    printf '%s\n' "$items" | sed 's/^/     • /'
  fi
}

list_aws_resources() {
  if ! command -v aws &>/dev/null; then
    err "❌ AWS CLI não encontrado. Instale o AWS CLI para listar os recursos."
    return 1
  fi

  local out
  info "\n📊 Recursos AWS (região $AWS_REGION)..."

  info "\n  📦 DynamoDB Tables (prod-* e dev-*):"
  if out=$(aws dynamodb list-tables --region "$AWS_REGION" --query 'TableNames[]' --output text 2>/dev/null); then
    print_aws_items "$out" '^(prod|dev)-'
  else
    err "     ❌ Falha ao listar tabelas (verifique AWS_PROFILE e credenciais)"
  fi

  info "\n  📨 SNS Topics:"
  if out=$(aws sns list-topics --region "$AWS_REGION" --query 'Topics[].TopicArn' --output text 2>/dev/null); then
    print_aws_items "$out" '.'
  else
    err "     ❌ Falha ao listar tópicos (verifique AWS_PROFILE e credenciais)"
  fi

  info "\n  ⚡ Lambda Functions (prod-*):"
  if out=$(aws lambda list-functions --region "$AWS_REGION" --query 'Functions[].FunctionName' --output text 2>/dev/null); then
    print_aws_items "$out" '^prod-'
  else
    err "     ❌ Falha ao listar funções (verifique AWS_PROFILE e credenciais)"
  fi
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
  info "      aws dynamodb scan --table-name prod-feedback-table --region sa-east-1"
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

show_menu() {
  while true; do
    echo ""
    printf "${C_CYAN}╔════════════════════════════════════════════════════╗${C_RESET}\n"
    printf "${C_CYAN}║   Tech Challenge Fase 4 — Feedback Serverless      ║${C_RESET}\n"
    printf "${C_CYAN}║   Quarkus Dev Mode · Build · Health · AWS          ║${C_RESET}\n"
    printf "${C_CYAN}╚════════════════════════════════════════════════════╝${C_RESET}\n"
    echo ""
    echo "  🚀 Quarkus Dev Mode (Local Development)"
    echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  1) Iniciar receive-feedback (porta 8080)"
    echo "  2) Iniciar send-notification (porta 8081)"
    echo "  3) Iniciar generate-report (porta 8082)"
    echo ""
    echo "  🔨 Build & Testing"
    echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  4) Rodar testes (mvn clean verify)"
    echo "  5) Compilar módulos (mvn clean compile)"
    echo ""
    echo "  ☁️  Health & AWS (região $AWS_REGION)"
    echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  6) Health check (/q/health dos 3 módulos locais)"
    echo "  7) Listar recursos AWS (DynamoDB, SNS, Lambda)"
    echo "  8) Validar SAM template"
    echo "  9) Info: Testing no AWS (exemplos curl)"
    echo ""
    echo "  🧪 End-to-End Tests"
    echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  10) E2E Test"
    echo "  11) E2E Verify"
    echo ""
    echo "  0) Sair"
    echo ""
    read -r -p "Escolha uma opção: " option

    case $option in
      1) quarkus_dev_receive_feedback ;;
      2) quarkus_dev_send_notification ;;
      3) quarkus_dev_generate_report ;;
      4) run_tests ;;
      5) compile_modules ;;
      6) health_check ;;
      7) list_aws_resources ;;
      8) validate_sam ;;
      9) test_aws ;;
      10) e2e_test ;;
      11) e2e_verify ;;
      0) echo "👋 Até mais!" && exit 0 ;;
      *) err "❌ Opção inválida." ;;
    esac

    echo ""
    read -r -p "Pressione ENTER para continuar..." dummy
  done
}

show_help() {
  echo ""
  echo "Tech Challenge Fase 4 — Feedback Serverless"
  echo ""
  echo "Uso:"
  echo "  ./run.sh                    Menu interativo"
  echo "  ./run.sh quarkus-receive    Inicia receive-feedback (porta 8080)"
  echo "  ./run.sh quarkus-notify     Inicia send-notification (porta 8081)"
  echo "  ./run.sh quarkus-report     Inicia generate-report (porta 8082)"
  echo "  ./run.sh tests              Rodar testes"
  echo "  ./run.sh compile            Compilar"
  echo "  ./run.sh health             Health check (/q/health)"
  echo "  ./run.sh aws-list           Listar recursos AWS ($AWS_REGION)"
  echo "  ./run.sh sam-validate       Validar SAM"
  echo "  ./run.sh aws-test           Info: exemplos curl"
  echo "  ./run.sh e2e-test           E2E test"
  echo "  ./run.sh e2e-verify         E2E verify"
  echo ""
}

# ─── Entry point ───────────────────────────────────────────────

if [ -z "$1" ]; then
  show_menu
else
  case "$1" in
    quarkus-receive)  quarkus_dev_receive_feedback ;;
    quarkus-notify)   quarkus_dev_send_notification ;;
    quarkus-report)   quarkus_dev_generate_report ;;
    tests|test)       run_tests ;;
    compile|build)    compile_modules ;;
    health|check)     health_check ;;
    aws-list)         list_aws_resources ;;
    sam-validate)     validate_sam ;;
    aws-test)         test_aws ;;
    e2e-test)         e2e_test ;;
    e2e-verify)       e2e_verify ;;
    *)                show_help ;;
  esac
fi
