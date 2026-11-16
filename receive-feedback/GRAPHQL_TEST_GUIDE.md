# Guia de teste de consulta GraphQL: listFeedbacks

## Seção 1: pré-requisitos

Antes de testar, confirme que:
- A aplicação receive-feedback está rodando (porta 8080 por padrão)
- O endpoint GraphQL está disponível em `http://localhost:8080/graphql`
- O DynamoDB (LocalStack ou AWS) está acessível com as credenciais configuradas
- O Bruno ou o Postman está instalado para os testes
- O build foi bem-sucedido: `mvn clean verify -pl receive-feedback` ✓

## Seção 2: criar 3 feedbacks pela mutation

Execute a mutation GraphQL abaixo três vezes, em sequência, uma para cada feedback:

### Mutation 1: criar feedback de urgência HIGH (nota 4)

```graphql
mutation {
  receiveFeedback(input: {
    description: "Bad experience"
    rating: 4
  }) {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

Resposta esperada:
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "description": "Bad experience",
      "rating": 4,
      "urgency": "HIGH",
      "createdAt": "2024-10-26T16:37:57"
    }
  }
}
```

### Mutation 2: criar feedback de urgência MEDIUM (nota 6)

```graphql
mutation {
  receiveFeedback(input: {
    description: "Okay service"
    rating: 6
  }) {
    id
    description
    rating
    urgency
    createdAt
  }
}
```

Resposta esperada:
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "description": "Okay service",
      "rating": 6,
      "urgency": "MEDIUM",
      "createdAt": "2024-10-26T16:37:58"
    }
  }
}
```

### Mutation 3: criar feedback de urgência LOW (nota 9)

```graphql
mutation {
  receiveFeedback(input: {
    description: "Excellent service"
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

Resposta esperada:
```json
{
  "data": {
    "receiveFeedback": {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "description": "Excellent service",
      "rating": 9,
      "urgency": "LOW",
      "createdAt": "2024-10-26T16:37:59"
    }
  }
}
```

## Seção 3: listar todos os feedbacks pela query

Execute a query GraphQL abaixo para buscar todos os feedbacks:

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

Resposta esperada (lista com 3 feedbacks):
```json
{
  "data": {
    "listFeedbacks": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "description": "Bad experience",
        "rating": 4,
        "urgency": "HIGH",
        "createdAt": "2024-10-26T16:37:57"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "description": "Okay service",
        "rating": 6,
        "urgency": "MEDIUM",
        "createdAt": "2024-10-26T16:37:58"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "description": "Excellent service",
        "rating": 9,
        "urgency": "LOW",
        "createdAt": "2024-10-26T16:37:59"
      }
    ]
  }
}
```

### Checklist de validação

Depois de rodar a query, confirme:
- [ ] A query retorna 3 itens
- [ ] O primeiro item tem urgency: "HIGH" (nota 4)
- [ ] O segundo item tem urgency: "MEDIUM" (nota 6)
- [ ] O terceiro item tem urgency: "LOW" (nota 9)
- [ ] Todos os itens têm UUIDs válidos no campo id
- [ ] Todos os itens têm createdAt preenchido
- [ ] As descrições são as que foram enviadas

## Seção 4: testar com o Bruno ou o Postman

### Configurar o endpoint GraphQL

1. Crie uma nova requisição no Bruno/Postman
2. Defina o método como POST
3. Defina a URL como `http://localhost:8080/graphql`
4. Adicione o header: `Content-Type: application/json`

### Fluxo de teste

1. **Rodar a mutation 1** (urgência HIGH): envie a mutation com "Bad experience" e nota 4
   - Confirme que a resposta contém urgency: "HIGH"
2. **Rodar a mutation 2** (urgência MEDIUM): envie a mutation com "Okay service" e nota 6
   - Confirme que a resposta contém urgency: "MEDIUM"
3. **Rodar a mutation 3** (urgência LOW): envie a mutation com "Excellent service" e nota 9
   - Confirme que a resposta contém urgency: "LOW"
4. **Rodar a query**: envie a query `listFeedbacks` para buscar todos os feedbacks
   - Confirme que 3 itens são retornados
   - Confirme que as urgências foram mapeadas corretamente (HIGH, MEDIUM, LOW)
   - Confirme que a ordem foi preservada

### Critérios de sucesso

✓ As 3 mutations foram executadas com sucesso
✓ A query retorna os 3 feedbacks
✓ As urgências foram calculadas corretamente a partir das notas (4→HIGH, 6→MEDIUM, 9→LOW)
✓ Todos os campos de saída estão preenchidos (id, description, rating, urgency, createdAt)

## Seção 5: definição da query GraphQL (referência)

A query `listFeedbacks` está definida em `ReceiveFeedbackQueryResolver.java`:

```java
@Query
public List<FeedbackOutput> listFeedbacks() {
    // Calls receiveFeedbackUseCase.listAllFeedbacks()
    // Maps each Feedback to FeedbackOutput using InputOutputMapper
    // Returns list of all feedbacks from DynamoDB
}
```

## Solução de problemas

| Problema | Causa | Solução |
|-------|-------|-------|
| "Query not found" | Resolver não implantado | Recompile com `mvn clean compile` e reinicie a aplicação |
| Lista vazia retornada | Nenhum feedback criado | Execute as mutations da Seção 2 primeiro |
| Valor de urgência errado | Faixa de nota incompatível | Confirme que a nota é 4 (HIGH), 6 (MEDIUM) ou 9 (LOW) |
| Erro 401/403 | Autenticação exigida | Configure o token Bearer, se estiver habilitado |
| Connection refused | Aplicação não está rodando | Inicie a aplicação na porta 8080 |
