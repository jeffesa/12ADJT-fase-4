#!/bin/bash
# ==========================================
# run.sh — Orquestrador de desenvolvimento local
# Inicia LocalStack, cria recursos AWS (DynamoDB, SNS, SQS),
# e compila/executa os módulos Quarkus em dev mode
# ==========================================

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}FASE-4: Dev Environment Setup${NC}"
echo -e "${GREEN}========================================${NC}"

# ==========================================
# 1. Validar dependências
# ==========================================
echo -e "\n${YELLOW}[1/4] Validando dependências...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker não encontrado. Instale Docker Desktop.${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}✗ Docker Compose não encontrado.${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven não encontrado. Use JAVA_HOME=/path/to/jdk mvn${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker, Docker Compose, Maven encontrados${NC}"

# ==========================================
# 2. Iniciar LocalStack
# ==========================================
echo -e "\n${YELLOW}[2/4] Iniciando LocalStack com docker-compose...${NC}"

docker-compose down --remove-orphans 2>/dev/null || true
docker-compose up -d

echo -e "${GREEN}✓ LocalStack iniciado${NC}"

# Aguardar LocalStack ficar pronto
echo -e "\n${YELLOW}Aguardando LocalStack estar pronto (max 60s)...${NC}"
RETRY_COUNT=0
MAX_RETRIES=60
until docker-compose exec -T localstack awslocal ec2 describe-regions &>/dev/null; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo -e "${RED}✗ LocalStack não respondeu após 60s${NC}"
        docker-compose logs localstack
        exit 1
    fi
    echo -n "."
    sleep 1
done
echo -e " ${GREEN}✓ LocalStack pronto${NC}"

# ==========================================
# 3. Compilar módulos
# ==========================================
echo -e "\n${YELLOW}[3/4] Compilando módulos Quarkus (Maven)...${NC}"

mvn clean compile -DskipTests -q
echo -e "${GREEN}✓ Módulos compilados com sucesso${NC}"

# ==========================================
# 4. Informações finais
# ==========================================
echo -e "\n${YELLOW}[4/4] Ambiente pronto para desenvolvimento${NC}"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}LOCALSTACK ENDPOINTS:${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "  Edge Service (todos os APIs):    ${YELLOW}http://localhost:4566${NC}"
echo -e "  DynamoDB:                        ${YELLOW}http://localhost:4569${NC}"
echo -e "  SNS:                             ${YELLOW}http://localhost:4575${NC}"
echo -e "  SQS:                             ${YELLOW}http://localhost:4576${NC}"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}PRÓXIMOS PASSOS:${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "  1. Abra um novo terminal"
echo -e "  2. Execute cada módulo em dev mode:"
echo -e "     ${YELLOW}cd receive-feedback && mvn quarkus:dev${NC}"
echo -e "     ${YELLOW}cd send-notification && mvn quarkus:dev${NC}"
echo -e "     ${YELLOW}cd generate-report && mvn quarkus:dev${NC}"
echo -e "  3. LocalStack permanecerá rodando em background"
echo -e "  4. Para parar: ${YELLOW}docker-compose down${NC}"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}UTILITIES AWS LOCAL:${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "  Listar tabelas DynamoDB:"
echo -e "    ${YELLOW}docker-compose exec localstack awslocal dynamodb list-tables${NC}"
echo -e "  Listar tópicos SNS:"
echo -e "    ${YELLOW}docker-compose exec localstack awslocal sns list-topics${NC}"
echo -e "  Listar filas SQS:"
echo -e "    ${YELLOW}docker-compose exec localstack awslocal sqs list-queues${NC}"

echo -e "\n"
