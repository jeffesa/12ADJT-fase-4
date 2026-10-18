#!/bin/bash
# ==========================================
# LocalStack Initialization Script
# Cria tabelas DynamoDB, tópicos SNS, filas SQS necessários para desenvolvimento local
# Executado automaticamente pelo LocalStack ao iniciar (via /docker-entrypoint-initaws.d/)
# ==========================================

set -e

echo "=== LocalStack Init: Aguardando LocalStack estar pronto ==="
sleep 10

# Usando awslocal CLI (pre-instalado no container LocalStack)
# Alternativa: aws --endpoint-url=http://localhost:4566

echo "=== Criando DynamoDB Table: feedbacks ==="
awslocal dynamodb create-table \
  --table-name feedbacks \
  --attribute-definitions \
    AttributeName=feedbackId,AttributeType=S \
    AttributeName=createdAt,AttributeType=S \
  --key-schema \
    AttributeName=feedbackId,KeyType=HASH \
    AttributeName=createdAt,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1 || echo "Table feedbacks may already exist"

echo "=== Criando SNS Topic: feedback-urgent-topic ==="
awslocal sns create-topic \
  --name feedback-urgent-topic \
  --region us-east-1 || echo "Topic feedback-urgent-topic may already exist"

echo "=== Criando SNS Topic: feedback-report-topic ==="
awslocal sns create-topic \
  --name feedback-report-topic \
  --region us-east-1 || echo "Topic feedback-report-topic may already exist"

echo "=== Criando SQS Queue: notification-queue ==="
awslocal sqs create-queue \
  --queue-name notification-queue \
  --region us-east-1 || echo "Queue notification-queue may already exist"

echo "=== Criando SQS Queue: report-queue ==="
awslocal sqs create-queue \
  --queue-name report-queue \
  --region us-east-1 || echo "Queue report-queue may already exist"

echo "=== LocalStack Init: Recursos criados com sucesso ==="
