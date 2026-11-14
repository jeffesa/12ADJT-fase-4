#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# Tech Challenge Fase 4 — Plataforma de Feedback Serverless
# Script runner: build, testes, HTTP local (Quarkus Dev Mode) e AWS (somente leitura).
#
# Uso interativo:  ./run.sh
# Uso direto:      ./run.sh help   (lista os comandos)
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
cd "$SCRIPT_DIR" || exit 1

AWS_REGION="sa-east-1"
export AWS_PROFILE="${AWS_PROFILE:-fase4}"
export AWS_DEFAULT_REGION="$AWS_REGION"
PROD_STACK="feedback-api-prod"
DEV_STACK="feedback-api-dev"
HTTP_PORT=8080
MOCK_PORT=18081
LOCAL_URL="http://localhost:${HTTP_PORT}/avaliacao"
MODULES="receive-feedback send-notification generate-report"
MVN_FLAGS=(-B -Dquarkus.devservices.enabled=false)

# ─── JDK 17 ────────────────────────────────────────────────────

java_major() {
  local bin="$1/bin/java" v
  [ -x "$bin" ] || return 1
  v=$("$bin" -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -n1)
  case "$v" in
    1.*) printf '%s' "$v" | cut -d. -f2 ;;
    *)   printf '%s' "$v" | cut -d. -f1 ;;
  esac
}

# Localiza um JDK 17 e exporta JAVA_HOME/PATH. Os testes (Mockito) falham em JDKs mais novos.
setup_java() {
  local candidate=""
  if [ -n "$JAVA_HOME" ] && [ "$(java_major "$JAVA_HOME")" = "17" ]; then
    candidate="$JAVA_HOME"
  elif [ -x /usr/libexec/java_home ]; then
    candidate=$(/usr/libexec/java_home -v 17 2>/dev/null)
    [ "$(java_major "$candidate")" = "17" ] || candidate=""
  fi
  if [ -z "$candidate" ]; then
    local d
    for d in /usr/lib/jvm/*17* /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; do
      if [ "$(java_major "$d")" = "17" ]; then candidate="$d"; break; fi
    done
  fi
  if [ -z "$candidate" ]; then
    err "❌ JDK 17 não encontrado. Instale um JDK 17 (ex.: brew install openjdk@17) ou aponte JAVA_HOME para ele."
    return 1
  fi
  export JAVA_HOME="$candidate"
  export PATH="$JAVA_HOME/bin:$PATH"
  ok "  ✅ Java 17: $("$JAVA_HOME/bin/java" -version 2>&1 | head -n1) ($JAVA_HOME)"
}

# ─── Validação de dependências ─────────────────────────────────
# Uso: require java mvn aws sam curl profile

require() {
  local missing=0 dep
  for dep in "$@"; do
    case "$dep" in
      java)
        setup_java || missing=1 ;;
      mvn)
        if command -v mvn &>/dev/null; then ok "  ✅ Maven: $(mvn -v 2>/dev/null | head -n1)"
        else err "❌ Maven não encontrado (comando mvn). Instale o Maven 3.9+."; missing=1; fi ;;
      aws)
        if command -v aws &>/dev/null; then ok "  ✅ AWS CLI: $(aws --version 2>&1 | head -n1)"
        else err "❌ AWS CLI não encontrado (comando aws). Instale o AWS CLI v2."; missing=1; fi ;;
      sam)
        if command -v sam &>/dev/null; then ok "  ✅ SAM CLI: $(sam --version 2>&1 | head -n1)"
        else err "❌ SAM CLI não encontrado (comando sam). Instale o AWS SAM CLI."; missing=1; fi ;;
      curl)
        if command -v curl &>/dev/null; then ok "  ✅ curl: $(curl --version | head -n1 | cut -d' ' -f1-2)"
        else err "❌ curl não encontrado."; missing=1; fi ;;
      profile)
        if ! command -v aws &>/dev/null; then
          err "❌ Não foi possível checar o profile AWS '$AWS_PROFILE' sem o AWS CLI."; missing=1
        elif aws configure list-profiles 2>/dev/null | grep -qx "$AWS_PROFILE"; then
          ok "  ✅ Profile AWS '$AWS_PROFILE' configurado"
        else
          err "❌ Profile AWS '$AWS_PROFILE' não configurado. Rode: aws configure --profile $AWS_PROFILE"; missing=1
        fi ;;
    esac
  done
  return $missing
}

check_dependencies() {
  info "\n🔍 Validando dependências..."
  if require java mvn aws sam curl profile; then
    ok "\n✅ Todas as dependências OK"
  else
    err "\n❌ Dependências faltando (veja acima)"
    return 1
  fi
}

# ─── Build e testes ────────────────────────────────────────────

compile_modules() {
  require java mvn || return 1
  info "\n🔨 Compilando todos os módulos (mvn clean package -DskipTests)..."
  if mvn "${MVN_FLAGS[@]}" clean package -DskipTests; then
    ok "✅ Compilação OK"
  else
    err "❌ Compilação falhou"; return 1
  fi
}

run_tests() {
  local module="$1"
  require java mvn || return 1
  if [ -n "$module" ]; then
    case " $MODULES " in
      *" $module "*) ;;
      *) err "❌ Módulo inválido: '$module'. Use: ${MODULES// / | }"; return 1 ;;
    esac
    info "\n🧪 Rodando testes de $module (mvn clean verify -pl $module)..."
    if mvn "${MVN_FLAGS[@]}" clean verify -pl "$module"; then ok "✅ Testes de $module passaram"
    else err "❌ Testes de $module falharam"; return 1; fi
  else
    info "\n🧪 Rodando testes de todos os módulos (mvn clean verify)..."
    if mvn "${MVN_FLAGS[@]}" clean verify; then ok "✅ Testes passaram"
    else err "❌ Testes falharam"; return 1; fi
  fi
}

# ─── HTTP local (local-http) ───────────────────────────────────

port_in_use() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN 2>/dev/null | tail -n +2
}

local_up() {
  require java mvn || return 1
  local busy=0 port holder
  for port in "$HTTP_PORT" "$MOCK_PORT"; do
    holder=$(port_in_use "$port")
    if [ -n "$holder" ]; then
      err "❌ Porta $port ocupada. Processo(s) (COMMAND PID USER ...):"
      printf '%s\n' "$holder" | awk '{printf "     %s (PID %s, usuário %s)\n", $1, $2, $3}'
      busy=1
    fi
  done
  if [ "$busy" = 1 ]; then
    err "   Nada foi encerrado. Libere as portas $HTTP_PORT e $MOCK_PORT e tente de novo."
    return 1
  fi

  if [ -z "$GRAPHQL_API_KEY" ]; then
    export GRAPHQL_API_KEY="minha-chave-local"
    warn "⚠️  GRAPHQL_API_KEY não definida: usando 'minha-chave-local' (valor só para uso local)."
  fi

  info "\n📦 Instalando receive-feedback (mvn install -pl .,receive-feedback)..."
  if ! mvn "${MVN_FLAGS[@]}" -q -DskipTests install -pl .,receive-feedback; then
    err "❌ Instalação do receive-feedback falhou"; return 1
  fi

  info "\n🚀 Subindo local-http (AWS_PROFILE=$AWS_PROFILE, região $AWS_REGION)"
  info "   Rotas em http://localhost:${HTTP_PORT}:"
  info "     POST /avaliacao   cria feedback"
  info "     GET  /avaliacao   lista feedbacks"
  info "     POST /graphql     consulta GraphQL (header X-API-Key = GRAPHQL_API_KEY)"
  info "   Mock Event Server do Lambda na porta ${MOCK_PORT}."
  warn "⚠️  Grava na tabela dev-feedback-table (sa-east-1), nunca em prod."
  info "   Bloqueia o terminal. Use Ctrl+C para parar.\n"
  # O handler ':' faz o script ignorar o Ctrl+C sem deixar o filho ignorando também.
  trap ':' INT
  ( cd "$SCRIPT_DIR/local-http" && AWS_REGION="$AWS_REGION" exec mvn -B quarkus:dev -Ddebug=false -Dquarkus.console.enabled=false )
  local rc=$?
  trap - INT
  info "\n🛑 local-http encerrado (código $rc)"
  return 0
}

local_check() {
  require curl || return 1
  info "\n🩺 Checando módulo local: GET $LOCAL_URL"
  local code
  code=$(curl -s -o /dev/null -m 10 -w '%{http_code}' "$LOCAL_URL" 2>/dev/null)
  if [ "$code" = "200" ]; then
    ok "  ✅ Módulo local no ar (HTTP 200)"
  else
    err "  ❌ Módulo local fora do ar ou com erro (HTTP ${code:-000}). Suba com: ./run.sh local-up (na pasta local-http; rodar na raiz não expõe as rotas)"
    return 1
  fi
}

# ─── SAM e AWS (somente leitura) ───────────────────────────────

validate_sam() {
  require sam || return 1
  info "\n🏗️  Validando template SAM (sam validate --lint)..."
  if (cd "$SCRIPT_DIR/infra" && sam validate --lint); then
    ok "✅ Template SAM válido"
  else
    err "❌ Template SAM inválido"; return 1
  fi
}

check_prod() {
  require aws curl profile || return 1
  info "\n🌐 Verificando produção (somente leitura, stack $PROD_STACK)..."
  local endpoint code
  endpoint=$(aws cloudformation describe-stacks --stack-name "$PROD_STACK" --region "$AWS_REGION" \
    --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue | [0]" --output text 2>/dev/null)
  if [ -z "$endpoint" ] || [ "$endpoint" = "None" ]; then
    err "  ❌ Não foi possível ler o output ApiEndpoint da stack $PROD_STACK (profile $AWS_PROFILE)"
    return 1
  fi
  info "   Endpoint: $endpoint"
  code=$(curl -s -o /dev/null -m 15 -w '%{http_code}' "$endpoint" 2>/dev/null)
  if [ "$code" = "200" ]; then
    ok "  ✅ GET $endpoint → HTTP 200"
  else
    err "  ❌ GET $endpoint → HTTP ${code:-000} (esperado 200)"
    return 1
  fi
}

list_aws_resources() {
  require aws profile || return 1
  local failed=0 out
  info "\n📊 Recursos AWS ($AWS_REGION, profile $AWS_PROFILE, somente leitura)..."

  info "\n  🧱 Stacks CloudFormation:"
  local stack
  for stack in "$PROD_STACK" "$DEV_STACK"; do
    if out=$(aws cloudformation describe-stacks --stack-name "$stack" --region "$AWS_REGION" \
        --query 'Stacks[0].StackStatus' --output text 2>&1); then
      ok "     ✅ $stack: $out"
    else
      err "     ❌ $stack: falha ao consultar ($(printf '%s' "$out" | tail -n1))"; failed=1
    fi
  done

  info "\n  λ Funções Lambda (prod-*, dev-*):"
  if out=$(aws lambda list-functions --region "$AWS_REGION" \
      --query "Functions[?starts_with(FunctionName,'prod-') || starts_with(FunctionName,'dev-')].FunctionName" \
      --output text 2>&1); then
    if [ -n "$out" ]; then printf '%s\n' "$out" | tr '\t' '\n' | sed 's/^/     ✅ /'
    else warn "     ⚠️  Nenhuma função prod-*/dev-* encontrada"; fi
  else
    err "     ❌ Falha ao listar funções ($(printf '%s' "$out" | tail -n1))"; failed=1
  fi

  info "\n  📦 Tabelas DynamoDB:"
  local table
  for table in dev-feedback-table prod-feedback-table; do
    if out=$(aws dynamodb describe-table --table-name "$table" --region "$AWS_REGION" \
        --query 'Table.TableStatus' --output text 2>&1); then
      ok "     ✅ $table: $out"
    else
      err "     ❌ $table: falha ao consultar ($(printf '%s' "$out" | tail -n1))"; failed=1
    fi
  done

  info "\n  📨 Tópicos SNS:"
  if out=$(aws sns list-topics --region "$AWS_REGION" --query 'Topics[].TopicArn' --output text 2>&1); then
    if [ -n "$out" ]; then printf '%s\n' "$out" | tr '\t' '\n' | sed 's/^/     ✅ /'
    else warn "     ⚠️  Nenhum tópico SNS encontrado"; fi
  else
    err "     ❌ Falha ao listar tópicos ($(printf '%s' "$out" | tail -n1))"; failed=1
  fi

  if [ "$failed" = 0 ]; then ok "\n✅ Listagem concluída"
  else err "\n❌ Alguma consulta AWS falhou"; return 1; fi
}

# ─── Menu ──────────────────────────────────────────────────────

show_menu() {
  local option module
  while true; do
    echo ""
    printf "${C_CYAN}╔════════════════════════════════════════════════════╗${C_RESET}\n"
    printf "${C_CYAN}║   Tech Challenge Fase 4 — Feedback Serverless      ║${C_RESET}\n"
    printf "${C_CYAN}║   Quarkus Dev Mode + AWS (sa-east-1)               ║${C_RESET}\n"
    printf "${C_CYAN}╚════════════════════════════════════════════════════╝${C_RESET}\n"
    echo ""
    echo "  🔨 Build & Testes"
    echo "   1) Validar dependências"
    echo "   2) Compilar todos os módulos"
    echo "   3) Rodar testes de todos os módulos"
    echo "   4) Rodar testes de um módulo"
    echo ""
    echo "  🚀 HTTP local (porta $HTTP_PORT, grava em dev-feedback-table)"
    echo "   5) Instalar receive-feedback e subir local-http"
    echo "   6) Checar se o módulo local está no ar"
    echo ""
    echo "  ☁️  AWS (somente leitura)"
    echo "   7) Validar template SAM"
    echo "   8) Verificar produção (GET)"
    echo "   9) Listar recursos AWS"
    echo ""
    echo "   0) Sair"
    echo ""
    if ! read -r -p "Escolha uma opção: " option; then echo ""; exit 0; fi

    case $option in
      1) check_dependencies ;;
      2) compile_modules ;;
      3) run_tests ;;
      4)
        read -r -p "Módulo (${MODULES// / | }): " module
        run_tests "$module" ;;
      5) local_up ;;
      6) local_check ;;
      7) validate_sam ;;
      8) check_prod ;;
      9) list_aws_resources ;;
      0) echo "👋 Até mais!"; exit 0 ;;
      *) err "❌ Opção inválida." ;;
    esac

    echo ""
    read -r -p "Pressione ENTER para continuar..." _ || { echo ""; exit 0; }
  done
}

show_help() {
  echo ""
  echo "Tech Challenge Fase 4 — Feedback Serverless"
  echo ""
  echo "Uso:"
  echo "  ./run.sh                     Menu interativo"
  echo "  ./run.sh deps                Validar dependências (JDK 17, Maven, AWS CLI, SAM CLI, curl, profile $AWS_PROFILE)"
  echo "  ./run.sh compile             Compilar todos os módulos"
  echo "  ./run.sh tests [módulo]      Testes de todos os módulos ou de um (${MODULES// / | })"
  echo "  ./run.sh local-up            Instalar receive-feedback e subir local-http (portas $HTTP_PORT e $MOCK_PORT)"
  echo "  ./run.sh local-check         Checar módulo local: GET $LOCAL_URL = 200"
  echo "  ./run.sh sam-validate        Validar template SAM (sam validate --lint)"
  echo "  ./run.sh prod-check          Verificar produção com GET (somente leitura)"
  echo "  ./run.sh aws-list            Listar recursos AWS (somente leitura)"
  echo "  ./run.sh help                Esta ajuda"
  echo ""
}

# ─── Entry point ───────────────────────────────────────────────

if [ -z "$1" ]; then
  show_menu
else
  case "$1" in
    deps)             check_dependencies ;;
    compile|build)    compile_modules ;;
    tests|test)       run_tests "$2" ;;
    local-up)         local_up ;;
    local-check)      local_check ;;
    sam-validate)     validate_sam ;;
    prod-check)       check_prod ;;
    aws-list)         list_aws_resources ;;
    help|-h|--help)   show_help ;;
    *)                err "❌ Comando desconhecido: $1"; show_help; exit 1 ;;
  esac
fi
