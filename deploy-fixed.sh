#!/bin/bash

echo "⏳ Aguardando deleção da stack fase-4-feedback-prod..."

for i in {1..20}; do
  STATUS=$(aws cloudformation describe-stacks \
    --stack-name fase-4-feedback-prod \
    --region us-east-1 \
    --profile pessoal 2>&1)
  
  if echo "$STATUS" | grep -q "does not exist"; then
    echo "✅ Stack foi deletada. Criando nova stack..."
    
    aws cloudformation create-stack \
      --stack-name fase-4-feedback-prod \
      --template-body file:///Users/jefferson/Documents/FIAP/fase-4/packaged.yaml \
      --parameters ParameterKey=AdminEmail,ParameterValue=jeffesa@gmail.com \
      --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
      --disable-validation \
      --disable-rollback \
      --region us-east-1 \
      --profile pessoal
    
    echo "✅ Nova stack criada! Iniciando monitoramento..."
    bash /Users/jefferson/Documents/FIAP/fase-4/monitor-stack.sh
    exit 0
  fi
  
  echo "⏳ Tentativa $i: Stack ainda em deleção..."
  sleep 5
done

echo "❌ Timeout aguardando deleção da stack"
exit 1
