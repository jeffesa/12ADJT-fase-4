#!/bin/bash

echo "⏳ Aguardando deleção da stack..."
for i in {1..15}; do
  STATUS=$(aws cloudformation describe-stacks \
    --stack-name fase-4-feedback-prod \
    --region us-east-1 \
    --profile pessoal 2>&1)
  
  if echo "$STATUS" | grep -q "does not exist"; then
    echo "✅ Stack deletada. Criando nova com template corrigido..."
    
    aws cloudformation create-stack \
      --stack-name fase-4-feedback-prod \
      --template-body file:///Users/jefferson/Documents/FIAP/fase-4/packaged.yaml \
      --parameters ParameterKey=AdminEmail,ParameterValue=jeffesa@gmail.com \
      --capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
      --disable-validation \
      --region us-east-1 \
      --profile pessoal
    
    echo "✅ Stack criada! Monitorando..."
    bash /Users/jefferson/Documents/FIAP/fase-4/monitor-stack.sh
    exit 0
  fi
  
  sleep 5
done

echo "❌ Timeout"
exit 1
