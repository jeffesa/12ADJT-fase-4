#!/bin/bash

while true; do
  STATUS=$(aws cloudformation describe-stacks \
    --stack-name fase-4-feedback-prod \
    --region us-east-1 \
    --profile pessoal \
    --query 'Stacks[0].StackStatus' \
    --output text 2>&1)
  
  TIMESTAMP=$(date '+%H:%M:%S')
  
  if [ "$STATUS" = "CREATE_COMPLETE" ]; then
    echo "[$TIMESTAMP] ✅ CREATE_COMPLETE - Stack foi criada com sucesso!"
    break
  elif echo "$STATUS" | grep -q "FAILED\|ROLLBACK"; then
    echo "[$TIMESTAMP] ❌ $STATUS - Stack creation FALHOU"
    break
  else
    echo "[$TIMESTAMP] ⏳ $STATUS - Em progresso..."
    sleep 30
  fi
done
