# Documentação da API

## Visão geral

Esta API oferece uma plataforma serverless para gerenciar feedbacks por meio de endpoints GraphQL e HTTP REST. O sistema calcula automaticamente a urgência do feedback, encaminha os feedbacks de alta prioridade para notificação e gera relatórios semanais.

A plataforma é construída sobre AWS Lambda, DynamoDB, SNS e SES, usando Quarkus com suporte a GraphQL.

---

## Endpoints e URLs base

### Desenvolvimento local (LocalStack)

- **Endpoint GraphQL**: `http://localhost:8080/graphql`
- **Endpoint HTTP REST**: `http://localhost:8080/v1/avaliacao`
- **Listagem HTTP REST**: `http://localhost:8080/v1/avaliacoes`
- **Modo de desenvolvimento do Quarkus**: `mvn quarkus:dev` (roda na porta 8080)

### Produção (AWS)

- **Endpoint GraphQL**: `https://[API-GATEWAY-URL]/v1/graphql`
- **Endpoint HTTP REST**: `https://[API-GATEWAY-URL]/v1/avaliacao`
- **Listagem HTTP REST**: `https://[API-GATEWAY-URL]/v1/avaliacoes`
- **Região**: configurável pela variável de ambiente `AWS_REGION` (padrão: `us-east-1`)

---

## Autenticação

### Token Bearer (`API_TOKEN`)

Todas as rotas (`POST /v1/avaliacao`, `GET /v1/avaliacoes` e `POST /v1/graphql`) exigem o header `Authorization` com um token Bearer.

```
Authorization: Bearer <token>
```

**Resposta quando o token está ausente ou é inválido**:
- Status: `401 Não autorizado`, com `WWW-Authenticate: Bearer`
- Corpo: `{"title":"Não autorizado","status":401,"detail":"Token Bearer ausente"}` (header ausente, vazio ou com esquema diferente de Bearer) ou `"detail":"Token Bearer inválido"` (token não confere).
- Implementação: a validação ocorre dentro dos handlers Lambda, então a mesma regra vale localmente e em produção.

**Como obter o token**:
1. Em produção: é o segredo do GitHub `API_TOKEN`. Crie-o com `openssl rand -hex 24 | tee >(pbcopy) | gh secret set API_TOKEN --repo jeffesa/12ADJT-fase-4`. O GitHub não mostra o valor de novo, então guarde uma cópia. Para trocar o token, substitua o segredo e rode um novo deploy. Em um `sam deploy` manual, passe `ApiToken=<token>` em `--parameter-overrides`.
2. Para desenvolvimento local: `./run.sh local-up` usa `minha-chave-local`, a menos que você exporte `API_TOKEN` antes.

Nos exemplos abaixo, `API_TOKEN` é uma variável de shell que guarda o token (`export API_TOKEN=<token>`).

---

## Schema GraphQL

### Tipos

#### `Feedback`
Representa um feedback com a urgência calculada automaticamente.

```graphql
type Feedback {
  id: UUID!
  description: String!
  rating: Int!
  urgency: String!
  createdAt: DateTime!
}
```

**Campos**:
- `id` (UUID): identificador único, gerado no servidor.
- `description` (String): descrição do feedback. Não pode ficar em branco.
- `rating` (Int): nota de 0 a 10 (inclusive). Define a urgência.
- `urgency` (String): nível de urgência calculado a partir da nota: `HIGH`, `MEDIUM` ou `LOW`.
- `createdAt` (DateTime): timestamp ISO 8601 de quando o feedback foi criado.

---

### Mutations

#### `receiveFeedback`

Recebe e grava um novo feedback. Calcula a urgência automaticamente e publica no SNS os feedbacks de urgência alta.

```graphql
type Mutation {
  receiveFeedback(input: ReceiveFeedbackInput!): Feedback!
}

input ReceiveFeedbackInput {
  description: String!
  rating: Int!
}
```

**Campos de entrada**:
- `description` (String): descrição do feedback. Restrições: não pode estar em branco; é removido o espaço das bordas ao gravar.
- `rating` (Int): nota de 0 a 10. Restrições: `@Min(0)`, `@Max(10)`.

**Retorno**: um objeto `Feedback` com o ID gerado, a urgência e o timestamp.

**Respostas de erro**:
- `400 Requisição inválida`: falha de validação (descrição em branco ou ausente, nota fora da faixa).
- `401 Não autorizado`: token Bearer ausente ou inválido (`Authorization: Bearer <token>`).
- `500 Erro interno do servidor`: erro inesperado no servidor (por exemplo, DynamoDB indisponível).

**Exemplo de resposta** (201 Created):
```json
{
  "receiveFeedback": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "description": "The application crashed on startup",
    "rating": 2,
    "urgency": "HIGH",
    "createdAt": "2026-10-22T10:30:45.123456"
  }
}
```

---

### Queries

#### `listFeedbacks`

Lista todos os feedbacks armazenados, ordenados pela data de criação.

```graphql
type Query {
  listFeedbacks: [Feedback!]!
}
```

**Retorno**: uma lista de objetos `Feedback`. Lista vazia se não houver feedbacks.

**Respostas de erro**:
- `401 Não autorizado`: token Bearer ausente ou inválido (`Authorization: Bearer <token>`).
- `500 Erro interno do servidor`: erro inesperado no servidor (por exemplo, DynamoDB indisponível).

**Exemplo de resposta** (200 OK):
```json
{
  "listFeedbacks": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "description": "The application crashed on startup",
      "rating": 2,
      "urgency": "HIGH",
      "createdAt": "2026-10-22T10:30:45.123456"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "description": "Good performance overall",
      "rating": 8,
      "urgency": "LOW",
      "createdAt": "2026-10-22T11:15:22.654321"
    }
  ]
}
```

---

## Cálculo da urgência

A urgência é calculada automaticamente quando o feedback é recebido, com base na nota:

| Faixa de nota | Nível de urgência | Observações |
|---|---|---|
| 0–5 | HIGH | Feedback crítico ou muito negativo, exige atenção imediata |
| 6–7 | MEDIUM | Feedback moderado, com espaço para melhoria |
| 8–10 | LOW | Feedback positivo, com pequenas ressalvas (se houver) |

**Implementação**: o método `Urgency.fromRating(int rating)`, na camada de domínio, faz este cálculo. Feedbacks de urgência HIGH são publicados automaticamente em um tópico SNS para processamento de notificação.

---

## Exemplos práticos

### Exemplo 1: criar um feedback de urgência HIGH (via GraphQL)

**Requisição**:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{
    "query": "mutation { receiveFeedback(input: { description: \"App crashes on login\", rating: 3 }) { id description rating urgency createdAt } }"
  }'
```

**Corpo GraphQL** (bruto):
```graphql
mutation {
  receiveFeedback(
    input: {
      description: "App crashes on login"
      rating: 3
    }
  ) {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

**Resposta** (201 Created):
```json
{
  "data": {
    "receiveFeedback": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "description": "App crashes on login",
      "rating": 3,
      "urgency": "HIGH",
      "createdAt": "2026-10-22T10:30:00"
    }
  }
}
```

---

### Exemplo 2: criar um feedback de urgência MEDIUM (via GraphQL)

**Requisição**:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{
    "query": "mutation { receiveFeedback(input: { description: \"UI could be more intuitive\", rating: 6 }) { id description rating urgency createdAt } }"
  }'
```

**Corpo GraphQL** (bruto):
```graphql
mutation {
  receiveFeedback(
    input: {
      description: "UI could be more intuitive"
      rating: 6
    }
  ) {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

**Resposta** (201 Created):
```json
{
  "data": {
    "receiveFeedback": {
      "id": "123e4567-e89b-12d3-a456-426614174001",
      "description": "UI could be more intuitive",
      "rating": 6,
      "urgency": "MEDIUM",
      "createdAt": "2026-10-22T10:31:00"
    }
  }
}
```

---

### Exemplo 3: criar um feedback de urgência LOW (via GraphQL)

**Requisição**:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{
    "query": "mutation { receiveFeedback(input: { description: \"Excellent support team\", rating: 9 }) { id description rating urgency createdAt } }"
  }'
```

**Corpo GraphQL** (bruto):
```graphql
mutation {
  receiveFeedback(
    input: {
      description: "Excellent support team"
      rating: 9
    }
  ) {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

**Resposta** (201 Created):
```json
{
  "data": {
    "receiveFeedback": {
      "id": "123e4567-e89b-12d3-a456-426614174002",
      "description": "Excellent support team",
      "rating": 9,
      "urgency": "LOW",
      "createdAt": "2026-10-22T10:32:00"
    }
  }
}
```

---

### Exemplo 4a: listar todas as avaliações (REST)

**Requisição**:
```bash
curl -X GET http://localhost:8080/v1/avaliacoes \
  -H "Authorization: Bearer $API_TOKEN"
```

**Resposta** (200):
```json
{
  "avaliacoes": [
    {
      "id": "uuid",
      "descricao": "Bom atendimento",
      "nota": 8,
      "urgencia": "LOW",
      "dataCriacao": "2026-11-15T17:36:13.239769912"
    }
  ],
  "total": 1
}
```

Sem token, ou com token inválido, a resposta é `401`. Erro interno retorna `500` genérico.

---

### Exemplo 4: listar todos os feedbacks (via GraphQL)

**Requisição**:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{
    "query": "query { listFeedbacks { id description rating urgency createdAt } }"
  }'
```

**Corpo GraphQL** (bruto):
```graphql
query {
  listFeedbacks {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

**Resposta** (200 OK):
```json
{
  "data": {
    "listFeedbacks": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "description": "App crashes on login",
        "rating": 3,
        "urgency": "HIGH",
        "createdAt": "2026-10-22T10:30:00"
      },
      {
        "id": "123e4567-e89b-12d3-a456-426614174001",
        "description": "UI could be more intuitive",
        "rating": 6,
        "urgency": "MEDIUM",
        "createdAt": "2026-10-22T10:31:00"
      },
      {
        "id": "123e4567-e89b-12d3-a456-426614174002",
        "description": "Excellent support team",
        "rating": 9,
        "urgency": "LOW",
        "createdAt": "2026-10-22T10:32:00"
      }
    ]
  }
}
```

---

### Exemplo 5: criar avaliação via HTTP REST (handler Lambda)

A mesma avaliação pode ser enviada por HTTP POST para a função Lambda, pelo API Gateway.

**Requisição**:
```bash
curl -X POST http://localhost:8080/v1/avaliacao \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{
    "descricao": "Fast response times",
    "nota": 8
  }'
```

**Resposta** (201 Created):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174003",
  "descricao": "Fast response times",
  "nota": 8,
  "urgencia": "LOW",
  "dataCriacao": "2026-10-22T10:33:00"
}
```

---

## Tratamento de erros

### Erros de validação

**Cenário**: entrada inválida (campo ausente ou valor fora da faixa).

**Status HTTP**: `400 Requisição inválida`

**Formato da resposta** (RFC 7807, Problem Detail):
```json
{
  "title": "Erro de validação",
  "status": 400,
  "detail": "Nota deve estar entre 0 e 10; recebido: 15"
}
```

**Erros de validação comuns**:
- `descricao` ausente: "Descrição não pode estar vazia"
- `nota` ausente ou nula: "Nota é obrigatória"
- `nota < 0`: "Nota deve ser no mínimo 0"
- `nota > 10`: "Nota deve ser no máximo 10"
- `descricao` em branco: "Descrição não pode estar vazia"

---

### Token Bearer ausente ou inválido

**Cenário**: requisição sem o header `Authorization`, com esquema diferente de `Bearer` ou com token errado.

**Status HTTP**: `401 Não autorizado`

**Resposta**:
```json
{
  "title": "Não autorizado",
  "status": 401,
  "detail": "Token Bearer ausente"
}
```
O `detail` é `Token Bearer inválido` quando o token é enviado, mas não confere.

**Solução**:
1. Adicione `Authorization: Bearer <token>` a todas as requisições.
2. Confirme o token com o administrador.
3. O nome do header e o esquema `Bearer` não diferenciam maiúsculas de minúsculas, mas o token diferencia.

---

### GraphQL malformado

**Cenário**: sintaxe GraphQL inválida ou estrutura de consulta incorreta.

**Status HTTP**: `400 Requisição inválida`

**Resposta**:
```json
{
  "errors": [
    {
      "message": "Erro de sintaxe na consulta",
      "locations": [{"line": 1, "column": 10}]
    }
  ]
}
```

**Problemas comuns**:
- Chaves ou aspas de fechamento ausentes na consulta.
- Erro de digitação no nome da mutation ou da query (por exemplo, `recieveFeedback` em vez de `receiveFeedback`).
- Uso de um campo que não existe no tipo (por exemplo, `score` em vez de `rating`).

---

### DynamoDB indisponível

**Cenário**: o banco está fora do ar ou inacessível.

**Status HTTP**: `500 Erro interno do servidor`

**Resposta**:
```json
{
  "title": "Erro interno do servidor",
  "status": 500,
  "detail": "Ocorreu um erro inesperado. Tente novamente mais tarde."
}
```

**Diagnóstico**:
1. Confirme que o LocalStack está rodando: `docker ps | grep localstack`
2. Confirme que a tabela do DynamoDB existe: `aws --endpoint-url=http://localhost:4566 dynamodb list-tables`
3. Veja o erro detalhado nos logs da aplicação: saída de `mvn quarkus:dev`.

---

### SNS indisponível (publicação de urgência HIGH)

**Cenário**: o tópico SNS está fora do ar ou a Lambda não tem permissão para publicar.

**Impacto**: o feedback continua gravado no DynamoDB, mas a notificação não é enviada.

**Status HTTP**: a requisição tem sucesso (`201`), mas a entrega da notificação falha de forma assíncrona.

**Diagnóstico**:
1. Confirme que o tópico SNS existe: `aws --endpoint-url=http://localhost:4566 sns list-topics`
2. Confirme que o ARN do tópico de feedback está correto: `echo $FEEDBACK_TOPIC_ARN`
3. Confirme que o papel IAM da Lambda tem a permissão `sns:Publish`.

---

## Modelos de dados

### Armazenamento de feedbacks (DynamoDB)

Os feedbacks são gravados em uma tabela DynamoDB chamada `feedbacks`, com o seguinte schema:

| Atributo | Tipo | Descrição |
|---|---|---|
| `id` | String (PK) | Identificador único (formato UUID) |
| `description` | String | Texto do feedback |
| `rating` | Number | Nota de 0 a 10 |
| `urgency` | String | HIGH, MEDIUM ou LOW |
| `createdAt` | String | Timestamp ISO 8601 |
| `notificationSent` | Boolean | Indica se a notificação foi enviada (padrão: false) |

---

### Formato da mensagem SNS (urgência HIGH)

Quando um feedback de urgência HIGH é recebido, ele é publicado no SNS com a seguinte estrutura JSON:

```json
{
  "feedbackId": "123e4567-e89b-12d3-a456-426614174000",
  "description": "App crashes on login",
  "rating": 3,
  "urgency": "HIGH",
  "createdAt": "2026-10-22T10:30:00"
}
```

Essa mensagem dispara a Lambda `send-notification`, que envia o e-mail pelo SES.

---

### Relatório semanal (e-mail)

A Lambda `generate-report` roda toda segunda-feira às 08:00 UTC e gera um relatório JSON com os feedbacks da semana anterior:

```json
{
  "reportPeriod": {
    "startDate": "2026-10-19",
    "endDate": "2026-10-23"
  },
  "summary": {
    "totalFeedback": 42,
    "highUrgency": 5,
    "mediumUrgency": 12,
    "lowUrgency": 25,
    "averageRating": 7.1
  },
  "feedbacks": [
    {
      "id": "...",
      "description": "...",
      "rating": "...",
      "urgency": "...",
      "createdAt": "..."
    }
  ]
}
```

---

## Configuração do ambiente de desenvolvimento local

### Pré-requisitos

- Java 17+
- Maven 3.8+
- Docker (para o LocalStack)

### Iniciar o LocalStack

```bash
docker-compose up -d
```

Isso inicia:
- LocalStack (DynamoDB, SNS e SES na porta 4566)
- Quarkus em modo de desenvolvimento (GraphQL na porta 8080)

### Inicializar a tabela do DynamoDB

```bash
./run.sh
```

Ou manualmente:

```bash
aws --endpoint-url=http://localhost:4566 dynamodb create-table \
  --table-name feedbacks \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### Rodar o Quarkus em modo de desenvolvimento

```bash
mvn quarkus:dev
```

A interface do GraphQL fica disponível em: `http://localhost:8080/q/graphql-ui`

### Testar com curl

Criar feedback:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{"query": "mutation { receiveFeedback(input: { description: \"Test\", rating: 5 }) { id urgency } }"}'
```

---

## Implantação em produção (AWS)

### Pré-requisitos

- Conta AWS com as permissões necessárias
- SAM CLI ou CloudFormation para implantar a infraestrutura
- `API_TOKEN` configurado nas Lambdas (parâmetro de deploy `ApiToken`)

### Variáveis de ambiente

Defina estas variáveis no ambiente da Lambda:

- `AWS_REGION`: região da AWS (padrão: `us-east-1`)
- `FEEDBACK_TOPIC_ARN`: ARN do tópico SNS para feedbacks de urgência HIGH
- `ADMIN_EMAIL`: e-mail que recebe os relatórios
- `DYNAMODB_TABLE_NAME`: nome da tabela (padrão: `feedbacks`)

### Deploy

```bash
sam deploy --guided
```

### Verificação

```bash
curl -X POST https://[API-GATEWAY-URL]/v1/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_TOKEN" \
  -d '{"query": "query { listFeedbacks { id } }"}'
```

---

## Limites de taxa e cotas

- **API Gateway**: padrão de 10.000 requisições por segundo por conta.
- **DynamoDB**: cobrança sob demanda (não é preciso provisionar throughput).
- **SNS**: throughput padrão, conforme as cotas da AWS.
- **SES**: modo sandbox por padrão (100 e-mails por dia). Solicite acesso de produção para limites maiores.

---

## Suporte e solução de problemas

### Problemas comuns

1. **401 "Token Bearer ausente" / "Token Bearer inválido"**: adicione `Authorization: Bearer <token>` a todas as requisições.
2. **"Nota deve estar entre 0 e 10"**: confirme que `rating` é um inteiro dentro da faixa.
3. **"DynamoDB indisponível"**: inicie o LocalStack e confirme que a tabela existe.
4. **"Endpoint GraphQL retorna 404"**: confirme que o Quarkus está rodando e que o caminho é `/v1/graphql`.

### Ativar log de depuração

Defina em `application.properties`:

```properties
quarkus.log.level=DEBUG
quarkus.log.category."com.fiap".level=DEBUG
```

Depois, consulte os logs:

```bash
mvn quarkus:dev 2>&1 | grep -i "com.fiap"
```

---

## Resumo

Esta API oferece uma plataforma de gestão de feedbacks serverless, robusta, com cálculo automático de urgência, encaminhamento de notificações e relatórios. Todas as operações são RESTful e expostas via GraphQL, com validação forte e tratamento de erros. O sistema foi projetado para escalar na AWS e para desenvolvimento local fácil com LocalStack.

Para dúvidas ou problemas, consulte o README do projeto ou entre em contato com a equipe de desenvolvimento.
