# Documentação da API GraphQL — Plataforma de Feedback

**URL base:** 
- **Local:** `http://localhost:4566/v1/graphql`
- **Produção:** `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/v1/graphql`

**Autenticação:** token Bearer no header `Authorization: Bearer <token>` (obrigatório em todas as rotas; token = segredo `API_TOKEN`)

---

## Schema GraphQL

### Tipos

#### Feedback
```graphql
type Feedback {
  id: String!                    # UUID (gerado automaticamente)
  description: String!           # Descrição do feedback (1-1000 caracteres)
  rating: Int!                   # Nota (0-10)
  urgency: Urgency!              # Calculada a partir da nota
  createdAt: String!             # Timestamp ISO 8601
}
```

#### Urgency (enum)
```graphql
enum Urgency {
  HIGH                           # nota 0-5
  MEDIUM                         # nota 6-7
  LOW                            # nota 8-10
}
```

#### FeedbackInput
```graphql
input FeedbackInput {
  description: String!          # Obrigatório, mínimo 1 caractere
  rating: Int!                   # Obrigatório, 0-10
}
```

---

## Mutations

### receiveFeedback

Cria um novo feedback com descrição e nota. Calcula a urgência automaticamente.

**Assinatura:**
```graphql
mutation receiveFeedback(
  input: FeedbackInput!
): Feedback!
```

**Exemplo de requisição:**
```graphql
mutation ReceiveFeedback {
  receiveFeedback(input: {
    description: "Excelente produto, muito satisfeito!"
    rating: 9
  }) {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

**Resposta de sucesso (HTTP 201):**
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "description": "Excelente produto, muito satisfeito!",
      "rating": 9,
      "urgency": "LOW",
      "createdAt": "2026-11-08T18:30:45.123456789"
    }
  }
}
```

**Resposta de erro de validação (HTTP 400):**
```json
{
  "title": "Erro de validação",
  "status": 400,
  "detail": "Nota deve ser no mínimo 0"
}
```

---

### Regras de cálculo da urgência

| Nota | Urgência | Prioridade |
|--------|---------|----------|
| 0-5 | HIGH | 🔴 Crítica — e-mail de notificação enviado |
| 6-7 | MEDIUM | 🟡 Normal — armazenado no DynamoDB |
| 8-10 | LOW | 🟢 Baixa prioridade — armazenado no DynamoDB |

---

## Regras de validação

### description
- **Tipo:** String
- **Obrigatório:** Sim
- **Tamanho mínimo:** 1 caractere
- **Tamanho máximo:** 1000 caracteres
- **Restrição:** não pode ser vazio nem nulo
- **Erro:** "Descrição não pode estar vazia"

### rating
- **Tipo:** Inteiro
- **Obrigatório:** Sim
- **Valor mínimo:** 0
- **Valor máximo:** 10
- **Restrição:** nota entre 0 e 10
- **Erro (abaixo do mínimo):** "Nota deve ser no mínimo 0"
- **Erro (acima do máximo):** "Nota deve ser no máximo 10"

---

## Respostas de erro

Todos os erros seguem o formato RFC 7807 (Problem Detail):

### HTTP 400 — Requisição inválida

**Causa:** falha de validação

**Resposta:**
```json
{
  "title": "Erro de validação",
  "status": 400,
  "detail": "Nota deve ser no mínimo 0"
}
```

**Mensagens de erro comuns:**
- "Descrição não pode estar vazia"
- "Nota deve ser no mínimo 0"
- "Nota deve ser no máximo 10"

### HTTP 500 — Erro interno do servidor

**Causa:** erro inesperado no handler ou no runtime do Lambda

**Resposta:**
```json
{
  "title": "Erro interno do servidor",
  "status": 500,
  "detail": "Ocorreu um erro inesperado. Tente novamente mais tarde."
}
```

---

## Testar com Bruno/Postman

### 1. Importar a collection

Baixe `fase4-feedback.postman_collection.json` do repositório:

```bash
git clone https://github.com/jeffesa/12ADJT-fase-4.git
cd fase-4/docs/api-collection
```

**Bruno:** File → Import → selecione `fase4-feedback.postman_collection.json`  
**Postman:** Import → selecione o arquivo

### 2. Definir os tokens

Abra **Collection > Variables** e defina `prodToken` (valor do segredo `API_TOKEN`, guardado somente na sua máquina). `localToken` já tem o padrão local.

**Local (local-http):**
```
POST http://localhost:8080/v1/feedback
Content-Type: application/json
Authorization: Bearer <localToken>

{
  "description": "Test feedback",
  "rating": 8
}
```

**Produção (AWS):**
```
POST https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/v1/feedback
Content-Type: application/json
Authorization: Bearer <token>

{
  "description": "Test feedback",
  "rating": 8
}
```

### 3. Executar as requisições

A collection traz 20 requisições prontas por ambiente (`local` e `prod`), incluindo casos 401 para token ausente e token inválido:

- ✅ Casos de sucesso (nota 1, 5, 6, 7, 8, 10)
- ❌ Casos de erro (nota -1, 11, descrição vazia, descrição nula)

Cada requisição inclui:
- Script de pré-requisição: gera o timestamp automaticamente
- Script de teste: valida o status e a estrutura da resposta
- Variáveis pré-configuradas: localUrl, prodUrl, localToken, prodToken, feedbackId, timestamp

### 4. Ver os resultados

**Aba de resultados dos testes:** mostra passou/falhou em cada asserção  
**Corpo da resposta:** mostra o JSON devolvido pela API  
**Estatísticas de teste:** resumo de testes aprovados e reprovados

---

## Urgência e notificações

### Feedback com urgência HIGH (nota 0-5)

Quando um feedback com urgência HIGH é criado:

1. **DynamoDB:** o feedback é salvo em `prod-feedback-table`
2. **SNS:** a mensagem é publicada em `prod-feedback-urgent-topic`
3. **Lambda 2:** o SendNotificationHandler é disparado pelo SNS
4. **E-mail:** o SES envia o e-mail de notificação ao administrador

**Formato do e-mail:**
```
Para: admin@company.com
Assunto: ⚠️ Feedback CRÍTICO Recebido

Conteúdo:
- ID: 550e8400-e29b-41d4-a716-446655440000
- Feedback: "Produto não funciona corretamente"
- Nota: 2/10
- Urgência: HIGH
- Data: 2026-11-08 18:30:45
```

### Feedback com urgência MEDIUM/LOW (nota 6-10)

- **DynamoDB:** o feedback é salvo em `prod-feedback-table`
- **SNS:** nenhuma mensagem publicada
- **Lambda 2:** não é disparada
- **E-mail:** nenhuma notificação enviada

---

## Geração do relatório semanal

**Gatilho:** regra cron do EventBridge (segunda-feira, 8h UTC)

**Lambda 3 (GenerateReport):**
- Consulta no DynamoDB os feedbacks da semana anterior
- Agrega as estatísticas (total, nota média, por urgência, por dia)
- Gera o relatório em HTML
- Envia o relatório por e-mail ao administrador

**Formato do relatório:**
```
Para: admin@company.com
Assunto: Relatório de Feedback — Semana de 4-10 de Novembro

Conteúdo:
- Total de Avaliações: 15
- Média Geral: 7.2/10
- Urgências:
  - HIGH (0-5): 3 feedbacks
  - MEDIUM (6-7): 5 feedbacks
  - LOW (8-10): 7 feedbacks
- Por Dia: [tabela com contagem diária]
```

---

## Variáveis de ambiente

### Local (docker-compose)

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export TABLE_NAME=dev-feedback-table
export FEEDBACK_TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:dev-feedback-urgent-topic
```

### Produção (AWS)

```bash
export AWS_REGION=us-east-1
export TABLE_NAME=prod-feedback-table
export FEEDBACK_TOPIC_ARN=arn:aws:sns:us-east-1:422648464291:prod-feedback-urgent-topic
export ADMIN_EMAIL=admin@company.com
```

---

## Playground GraphiQL

**URL:** 
- Local: `http://localhost:4566/graphiql`
- Produção: não disponível (token Bearer obrigatório)

**Como usar o GraphiQL:**

1. Abra a URL no navegador
2. Painel esquerdo: escreva a query ou mutation GraphQL
3. Painel central: clique em "Play" para executar
4. Painel direito: veja o JSON de resposta

**Exemplo de consulta:**
```graphql
mutation {
  receiveFeedback(input: {
    description: "Test"
    rating: 8
  }) {
    id
    urgency
  }
}
```

---

## Links úteis

- **Repositório:** https://github.com/jeffesa/12ADJT-fase-4
- **Collection do Postman:** `docs/api-collection/fase4-feedback.postman_collection.json`
- **Painel do SonarCloud:** https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4
- **Endpoint AWS:** https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/v1/graphql
- **Guia do Quarkus GraphQL:** https://quarkus.io/guides/smallrye-graphql
- **RFC 7807 Problem Detail:** https://tools.ietf.org/html/rfc7807

---

**Última atualização:** 8 de novembro de 2026  
**Versão da API:** v1  
**Status:** pronto para produção ✅
