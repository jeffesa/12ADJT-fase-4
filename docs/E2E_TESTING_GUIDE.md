# Guia de testes ponta a ponta (E2E)

Este guia traz instruções passo a passo para testar os três fluxos críticos da plataforma de feedback com o LocalStack.

**Métricas de desempenho**: os scripts de teste capturam e validam os tempos de execução. Todos os endpoints das Lambdas (receiveFeedback, generateWeeklyReport) precisam terminar em menos de 1000 ms (1 segundo). As medições reais são registradas e incluídas no CSV de resultados.

## Pré-requisitos

Antes de começar, confirme que:

1. O LocalStack está rodando e saudável:
   ```bash
   ./run.sh health
   ```
   Saída esperada: `✅ LocalStack (edge:4566): UP`

2. Os três módulos estão rodando em modo de desenvolvimento:
   - Terminal 1: `cd receive-feedback && mvn quarkus:dev`
   - Terminal 2: `cd send-notification && mvn quarkus:dev`
   - Terminal 3: `cd generate-report && mvn quarkus:dev`

3. Os recursos foram criados:
   ```bash
   ./run.sh resources
   ```
   Deve mostrar a tabela `feedbacks`, os tópicos `feedback-urgent-topic` e `feedback-report-topic`, e as filas

4. Ferramentas disponíveis:
   - `curl` (para requisições HTTP)
   - `jq` (para ler JSON)
   - CLI `aws` (recomendada, mas opcional se você usar o `awslocal` via Docker)

## Configuração: verificar os recursos

Aqueça os serviços e confirme que todos os endpoints estão acessíveis:

```bash
# Verificar a saúde
curl -v http://localhost:4566

# Resposta esperada: 200 OK com metadados

# Testar o endpoint GraphQL do módulo receive-feedback
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ __typename }"}'

# Resposta esperada: {"data":{"__typename":"Query"}}
```

Se os serviços não responderem, confira a saída dos terminais dos módulos e aguarde a mensagem de inicialização do Quarkus.

## Fluxo 1: feedback de urgência HIGH

**Objetivo**: testar se um feedback de urgência HIGH (nota de 0 a 5) dispara a publicação no SNS → a Lambda send-notification → o e-mail pelo SES

**Comportamento esperado**:
- O feedback é gravado no DynamoDB com `urgency: HIGH`
- A mensagem é publicada no `feedback-urgent-topic`
- A Lambda send-notification é disparada
- O e-mail é enviado pelo SES para `admin@feedback-fase-4.com`
- Os logs do CloudWatch mostram execução em menos de 1 segundo
- Nenhum erro nos logs

### Passo 1: enviar um feedback de urgência HIGH

Usando curl:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { receiveFeedback(input: {description: \"Test critical feedback\", rating: 4}) { id description rating urgency createdAt } }"
  }'
```

**Resposta esperada**:

```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "description": "Test critical feedback",
      "rating": 4,
      "urgency": "HIGH",
      "createdAt": "2024-10-26T15:30:45.123Z"
    }
  }
}
```

**Anote o ID do feedback** para a verificação seguinte: `550e8400-e29b-41d4-a716-446655440000`

### Passo 2: verificar o feedback no DynamoDB

```bash
# Usando docker compose (a partir da raiz do fase-4)
docker compose exec localstack awslocal dynamodb scan \
  --table-name feedbacks \
  --region us-east-1 \
  --output json | jq '.Items[] | {id: .id.S, rating: .rating.N, urgency: .urgency.S}'
```

**Saída esperada**:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "rating": "4",
  "urgency": "HIGH"
}
```

**Verificação**:
- ✅ Feedback encontrado na tabela
- ✅ `rating` é 4
- ✅ `urgency` é HIGH

### Passo 3: verificar a publicação da mensagem no SNS

```bash
# Consultar os atributos do tópico SNS
docker compose exec localstack awslocal sns get-topic-attributes \
  --topic-arn arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic \
  --region us-east-1 \
  --output json
```

**Saída esperada** (exemplo):

```json
{
  "Attributes": {
    "TopicArn": "arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic",
    "DisplayName": "",
    "SubscriptionsPending": "0",
    "SubscriptionsConfirmed": "1",
    "SubscriptionsDeleted": "0",
    "DeliveryPolicy": null,
    "EffectiveDeliveryPolicy": "{\"http\":{\"disableSubscriptionOverrides\":false}}",
    "Owner": "000000000000",
    "Policy": null
  }
}
```

**Verificação**:
- ✅ O tópico existe
- ✅ Há pelo menos 1 assinatura (a Lambda send-notification)

### Passo 4: verificar o envio do e-mail pelo SES

```bash
# Consultar o status de envio do SES
docker compose exec localstack awslocal ses get-account-sending-enabled \
  --region us-east-1 \
  --output json
```

Confira os logs do módulo (Terminal 2, send-notification):

Procure entradas como:
```
INFO: Enviando notificação por e-mail para o feedback 550e8400-e29b-41d4-a716-446655440000 para admin@feedback-fase-4.com
INFO: E-mail enviado com sucesso para o feedback 550e8400-e29b-41d4-a716-446655440000
```

**Verificação**:
- ✅ O log mostra o e-mail enviado
- ✅ O destinatário é `admin@feedback-fase-4.com`
- ✅ Nenhum log SEVERE ou ERROR

### Passo 5: conferir o desempenho

No terminal do módulo send-notification, procure o tempo de execução:

```
Invocação: 550e8400-e29b-41d4-a716-446655440000 concluída em 234ms
```

**Verificação**:
- ✅ Tempo de execução abaixo de 1000 ms (1 segundo)

## Fluxo 2: feedback de urgência LOW

**Objetivo**: testar se um feedback de urgência LOW (nota de 8 a 10) NÃO dispara o SNS nem o e-mail

**Comportamento esperado**:
- O feedback é gravado no DynamoDB com `urgency: LOW`
- NENHUMA mensagem é publicada no SNS
- A Lambda send-notification NÃO é disparada
- NENHUM e-mail é enviado
- Nenhum erro nos logs

### Passo 1: enviar um feedback de urgência LOW

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { receiveFeedback(input: {description: \"Test normal feedback\", rating: 9}) { id description rating urgency createdAt } }"
  }'
```

**Resposta esperada**:

```json
{
  "data": {
    "receiveFeedback": {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "description": "Test normal feedback",
      "rating": 9,
      "urgency": "LOW",
      "createdAt": "2024-10-26T15:31:45.123Z"
    }
  }
}
```

### Passo 2: verificar o feedback no DynamoDB (e que não houve e-mail)

```bash
docker compose exec localstack awslocal dynamodb scan \
  --table-name feedbacks \
  --region us-east-1 \
  --output json | jq '.Items[] | select(.urgency.S == "LOW") | {id: .id.S, rating: .rating.N, urgency: .urgency.S}'
```

**Saída esperada**:

```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "rating": "9",
  "urgency": "LOW"
}
```

### Passo 3: verificar que NÃO houve publicação no SNS

Confira os logs do módulo send-notification (Terminal 2):

**Esperado**: nenhuma nova entrada de log para o feedback de urgência LOW (nenhum e-mail enviado)

Se aparecerem linhas como:
```
INFO: Enviando notificação por e-mail para o feedback 660e8400-...
```

isso indica que um feedback LOW disparou o e-mail por engano. É um bug.

### Passo 4: verificar o estado do módulo

No módulo receive-feedback (Terminal 1), você deve ver:

```
Feedback criado: 660e8400-e29b-41d4-a716-446655440001
(gravado no DynamoDB, sem publicação no SNS, porque a urgência não é HIGH)
```

**Verificação**:
- ✅ Feedback gravado no DynamoDB
- ✅ Nenhuma publicação no SNS
- ✅ Nenhum e-mail enviado
- ✅ Nenhum erro

## Fluxo 3: gerar o relatório semanal

**Objetivo**: testar se a Lambda generate-report consulta os feedbacks da semana anterior, agrega as estatísticas e envia um e-mail HTML

**Comportamento esperado**:
- Consulta no DynamoDB os feedbacks dos 7 dias anteriores
- Agrega: total, nota média e contagem por urgência
- Envia um e-mail HTML para `admin@feedback-fase-4.com` com uma tabela formatada
- O relatório inclui: período, distribuição de urgência e estatísticas de nota
- Os logs do CloudWatch mostram execução em menos de 1 segundo
- Nenhum erro

### Passo 1: disparar a geração do relatório semanal

```bash
curl -X POST http://localhost:8282/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { generateWeeklyReport { reportId totalFeedbacks averageRating highCount mediumCount lowCount generatedAt } }"
  }'
```

**Resposta esperada** (exemplo):

```json
{
  "data": {
    "generateWeeklyReport": {
      "reportId": "report-2024-10-26-001",
      "totalFeedbacks": 2,
      "averageRating": 6.5,
      "highCount": 1,
      "mediumCount": 0,
      "lowCount": 1,
      "generatedAt": "2024-10-26T15:32:00.000Z"
    }
  }
}
```

**Métricas esperadas**:
- `totalFeedbacks`: 2 (um HIGH do Fluxo 1 e um LOW do Fluxo 2)
- `averageRating`: 6.5 ((4 + 9) / 2)
- `highCount`: 1
- `lowCount`: 1
- `mediumCount`: 0

### Passo 2: verificar a consulta no DynamoDB

O módulo generate-report deve consultar o DynamoDB com um filtro de intervalo de datas (7 dias anteriores). No Terminal 3 (generate-report), procure:

```
INFO: Gerando relatório para o período 2024-10-19 a 2024-10-26
FINE: Encontrados 2 feedbacks para o período
FINE: Estatísticas agregadas: total=2, média=6.5, HIGH=1, MEDIUM=0, LOW=1
```

**Verificação**:
- ✅ O intervalo da consulta vai 7 dias para trás a partir de hoje
- ✅ Foram encontrados 2 feedbacks (exatamente os dos Fluxos 1 e 2)
- ✅ As estatísticas foram calculadas corretamente

### Passo 3: verificar o envio do e-mail do relatório

No Terminal 3 (generate-report), procure:

```
INFO: Enviando relatório semanal <id> para admin@feedback-fase-4.com
O e-mail contém tabelas HTML com:
  - Data de geração do relatório: 2024-10-26T15:32:00Z
  - Período: 2024-10-19 a 2024-10-26
  - Total de feedbacks: 2
  - Nota média: 6.5
  - Tabela de distribuição por urgência
INFO: Geração e envio do relatório concluídos com sucesso
```

**Verificação**:
- ✅ E-mail do relatório enviado
- ✅ O destinatário é `admin@feedback-fase-4.com`
- ✅ O e-mail contém tabelas HTML com os dados agregados
- ✅ O intervalo de datas está correto (janela de 7 dias)

### Passo 4: conferir o desempenho

No Terminal 3, procure o tempo:

```
Geração do relatório concluída em 456 ms
```

**Verificação**:
- ✅ Tempo de execução abaixo de 1000 ms

## Verificação: todos os fluxos concluídos

Rode o script de verificação completo:

```bash
./run.sh e2e-verify
```

Ele gera um arquivo CSV em `/tmp/e2e-results.csv` com:

```
Flow,Status,Detail,ExecutionTime(ms),Evidence
"Flow 1 & 2 - DynamoDB","PASS","Feedbacks persisted correctly","0","Items: 2, HIGH: 1, LOW: 1"
"Flow 1 - SNS","PASS","SNS topic is accessible and configured","0","Topic ARN: arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic"
"Flow 1 - SES","PASS","SES identities configured (noreply, admin @feedback-fase-4.com)","0","Identities: noreply@feedback-fase-4.com,admin@feedback-fase-4.com"
"CloudWatch Logs","PASS","Log groups accessible","0","Groups: 3"
"Data Persistence","PASS","DynamoDB data persists across queries","0","Consistent item count: 2"
```

**Todas as linhas devem mostrar Status: PASS**

## Solução de problemas nos testes

### O teste trava na requisição GraphQL

**Problema**: a requisição curl expira ou fica travada

**Causa**: o módulo não responde (ainda está iniciando ou travou)

**Solução**:
1. Veja no terminal do módulo se aparece a mensagem de inicialização do Quarkus na porta 8080
2. Aguarde mais de 30 segundos para o Quarkus iniciar por completo
3. Reinicie o módulo:
   - Pressione `Ctrl+C` no terminal do módulo
   - Rode `mvn quarkus:dev` de novo
   - Aguarde a inicialização completa

**Teste rápido**:
```bash
curl -v http://localhost:8080/q/health
# Deve retornar 200 OK
```

### Erro de consulta GraphQL

**Problema**: retorna erro GraphQL, e não dados

**Exemplo de erro**:
```json
{
  "errors": [
    {
      "message": "Cannot query field 'receiveFeedback' on type 'Mutation'"
    }
  ]
}
```

**Causa**: o schema não foi atualizado ou o nome do campo está errado

**Solução**:
1. Confirme que os nomes dos campos são exatamente: `description`, `rating`, `urgency`, `createdAt`
2. Confirme que a urgência é calculada corretamente (0-5=HIGH, 6-7=MEDIUM, 8-10=LOW)
3. Reinicie o módulo para recarregar o schema

### Feedback não gravado no DynamoDB

**Problema**: o GraphQL retorna sucesso, mas o scan do DynamoDB mostra 0 itens

**Causa**: o módulo não está conectado ao LocalStack, ou a gravação falhou sem aviso

**Solução**:
1. Confira os logs do módulo quanto a erros de conexão
2. Confirme o endpoint do DynamoDB em `application.properties`
3. Confirme que a tabela existe: `./run.sh resources`
4. Confira os logs do módulo quanto a exceções do DynamoDB

### SNS não disparado para urgência HIGH

**Problema**: o feedback do Fluxo 1 foi criado, mas o send-notification não foi invocado

**Causa**: a assinatura do SNS não foi configurada, ou o cálculo da urgência está errado

**Solução**:
1. Confirme que o tópico SNS existe: `./run.sh resources`
2. Confira o cálculo da urgência: nota 4 deve resultar em HIGH
3. Confira os logs do módulo send-notification quanto a erros de assinatura
4. Confirme o event source mapping do SNS na configuração do send-notification

### Desempenho acima de 1 segundo

**Problema**: os tempos de execução das Lambdas estão altos (acima de 1000 ms)

**Causa**: throttling do DynamoDB, latência de rede ou código ineficiente

**Solução**:
1. Verifique o uso de recursos do LocalStack: `docker stats`
2. Confira os logs do módulo quanto a timeouts do DynamoDB
3. Tente reiniciar o LocalStack: `./run.sh clean && ./run.sh docker`
4. Faça o profiling do código para achar consultas ineficientes

## Testes manuais com a CLI da AWS

### Consultar o DynamoDB diretamente

```bash
aws dynamodb scan \
  --table-name feedbacks \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --output json | jq '.'
```

### Listar os tópicos do SNS

```bash
aws sns list-topics \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --output json | jq '.Topics'
```

### Verificar as identidades do SES

```bash
aws ses list-verified-email-addresses \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --output json
```

## Dicas para os testes

1. **Teste em sequência**: sempre rode o Fluxo 1 antes do Fluxo 2, para ter dados de teste válidos
2. **Aguarde o processamento assíncrono**: depois do envio do Fluxo 1, espere 2 a 3 segundos antes de consultar o SNS e o SES
3. **Confira os timestamps**: verifique se o `createdAt` é recente (de poucos segundos atrás do seu teste)
4. **Zere o estado**: use `./run.sh clean` entre os ciclos de teste para ter um banco novo
5. **Acompanhe todos os terminais**: mantenha os Terminais 1, 2 e 3 visíveis para ver os erros na hora
6. **Salve as respostas**: copie as respostas do GraphQL para um arquivo, para documentar os resultados dos testes

## Próximos passos

Depois de concluir os três fluxos com sucesso:

1. **Documente os resultados**: registre a saída dos testes em [E2E_TEST_RESULTS.md](./E2E_TEST_RESULTS.md)
2. **Ajuste de desempenho**: se os tempos passarem de 1 s, otimize as consultas ou aumente os recursos
3. **Teste de carga**: rode vários feedbacks para testar sob carga
4. **Teste de integração**: teste os casos de erro (entrada inválida, falhas de rede)
5. **Deploy na AWS**: use as mesmas funções Lambda em produção, com credenciais reais da AWS

## Documentação relacionada

- [LOCAL_SETUP.md](./LOCAL_SETUP.md) - instruções de configuração
- [E2E_TEST_RESULTS.md](./E2E_TEST_RESULTS.md) - modelo de resultados dos testes
- [../LOCALSTACK_SETUP.md](../LOCALSTACK_SETUP.md) - detalhes da configuração do LocalStack
