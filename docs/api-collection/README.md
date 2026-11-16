# FIAP Fase-4 Feedback API — Coleção Postman

## Visão geral

Esta coleção do Postman cobre os endpoints da Feedback API da FIAP Fase-4, com casos de teste para criar feedbacks e validar o cálculo de urgência com base na nota. A coleção inclui os ambientes local (LocalStack) e produção (AWS API Gateway), permitindo alternar entre os cenários de teste.

### Cobertura da API
- **POST `/v1/feedback`**: cria um feedback com classificação automática de urgência
  - Casos de sucesso: 3 faixas de urgência (HIGH, MEDIUM, LOW) com teste de valores limite
  - Casos de erro: 9 cenários de validação e violação de restrições
- **GET `/v1/feedback`**: lista todos os feedbacks
- **POST `/v1/graphql`**: consulta GraphQL `listFeedbacks`

### Arquitetura
- **Baseada em REST**, com uma consulta GraphQL (`listFeedbacks`)
- **Contrato da requisição:** JSON com `description` (string) e `rating` (inteiro de 0 a 10)
- **Formato da resposta:** RFC 7807 Problem Detail para erros; objeto JSON simples (sem wrapper) para a resposta de sucesso da v1
- **Códigos de status:** 201 Created (sucesso), 400 Bad Request (erros de validação)

---

## Instruções de importação

### No Postman
1. Abra o Postman
2. Clique em **File** → **Import**
3. Selecione `docs/api-collection/fase4-feedback.postman_collection.json` (somente a coleção; não há arquivos de ambiente)
4. A coleção aparece na barra lateral esquerda

### No Bruno
1. Abra o Bruno
2. Clique em **+ New** → **Collection**
3. Importe o arquivo JSON
4. A coleção é carregada com as pastas `local` e `prod`
5. Abra **Collection > Variables** e defina `prodToken` (veja abaixo)

---

## Variáveis da coleção

Todas as variáveis estão embutidas na coleção (não é preciso um arquivo de ambiente separado). Cada requisição envia `Authorization: Bearer <token>` usando `localToken` (pasta `local`) ou `prodToken` (pasta `prod`), exceto as requisições chamadas "sem token", que testam a resposta 401.

| Variável | Valor padrão | Finalidade |
|----------|---------------|---------|
| `localUrl` | `http://localhost:8080` | URL base HTTP local |
| `prodUrl` | `https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod` | URL base do AWS API Gateway |
| `localToken` | `minha-chave-local` | Token local (não é segredo; corresponde ao padrão do `./run.sh local-up`) |
| `prodToken` | *(vazio)* | Token de produção: cole o valor do segredo `API_TOKEN` somente na sua máquina; nunca o versione |
| `feedbackId` | *(vazio)* | Capturado a partir da resposta de criação bem-sucedida |
| `timestamp` | *(gerado automaticamente por requisição)* | Garante descrições de feedback únicas |

---

## Fluxo de execução

A coleção tem duas pastas: **local** e **prod**. Cada uma contém os mesmos casos de teste, mas apontando para endpoints diferentes.

### Para testar o LOCAL (local-http):
1. Expanda a pasta **"local"**
2. Execute qualquer requisição em **"Criar Avaliação"**
3. Todas as requisições vão para `{{localUrl}}/v1/feedback` = `http://localhost:8080/v1/feedback`

### Para testar a PRODUÇÃO (AWS):
1. Expanda a pasta **"prod"**
2. Execute qualquer requisição em **"Criar Avaliação"**
3. Todas as requisições vão para `{{prodUrl}}/v1/feedback` = `https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod/v1/feedback`

### Casos de teste em "Criar Avaliação"
| Requisição | Nota | Urgência | Observação |
|---------|--------|--------|------|
| nota 1 | 1 | HIGH | Captura o ID do feedback para consultas de lista futuras |
| nota 6 | 6 | MEDIUM | Feedback de faixa intermediária |
| nota 9 | 9 | LOW | Alta satisfação |
| nota 0 | 0 | HIGH | Limite: nota mínima válida |
| nota 10 | 10 | LOW | Limite: nota máxima válida |

### Casos de teste em "Listar Avaliações"
| Requisição | Finalidade | Resposta |
|---------|---------|------|
| listFeedbacks | Consulta todos os feedbacks armazenados no DynamoDB | HTTP 200 com o array `data.listFeedbacks` |

**Asserções do teste (listFeedbacks):**
- ✅ Código de status 200
- ✅ A resposta tem o campo `data.listFeedbacks`
- ✅ Cada item de feedback tem: `id`, `description`, `rating`, `urgency`, `createdAt`
- ✅ `id` é um UUID válido
- ✅ `urgency` é um de: HIGH, MEDIUM, LOW
- ✅ `createdAt` está no formato ISO 8601
- ✅ O array é retornado (pode estar vazio)

#### Casos de erro (HTTP 400, RFC 7807 Problem Detail)
| Requisição | Problema | Campo de erro |
|---------|------------|-------------|
| nota -1 | Nota abaixo do mínimo | `detail` contém "Nota deve ser no mínimo 0" |
| nota 11 | Nota acima do máximo | `detail` contém "Nota deve ser no máximo 10" |
| descrição vazia | String vazia | `detail` contém "Descrição não pode estar vazia" |
| descrição com espaços | Apenas espaços em branco | `detail` contém a violação de verificação de vazio |
| nota ausente | Campo ausente | HTTP 400, campo obrigatório |
| descrição ausente | Campo ausente | HTTP 400, campo obrigatório |
| nota como string | Tipo incompatível | HTTP 400, número esperado |

---

## Mapeamento de urgência

A API calcula a urgência com base na nota:

| Faixa de nota | Urgência | Significado para o negócio |
|--------------|---------|------------------|
| 0–5 | **HIGH** | Feedback negativo que exige atenção imediata |
| 6–7 | **MEDIUM** | Feedback moderado: revisão padrão |
| 8–10 | **LOW** | Feedback positivo: nenhuma ação necessária |

Todos os testes verificam esse mapeamento, comparando o campo `urgency` com a faixa esperada.

---

## Resultados esperados dos testes

### Todos os casos de sucesso passam
- HTTP 201 Created
- A resposta é um objeto simples (sem o wrapper `receiveFeedback`)
- Campos: `id` (UUID), `description` (repetido da requisição), `rating` (repetido da requisição), `urgency` (calculada), `createdAt` (ISO 8601)
- O timestamp na descrição é único (sem duplicatas no DynamoDB)

### Todos os casos de erro retornam HTTP 400
- Formato da resposta: RFC 7807 Problem Detail
- Campos: `title` (ex.: "Erro de validação"), `status` (400), `detail` (violação específica)
- **Não** é um array `errors` do GraphQL

### Exemplo de resposta de sucesso
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Feedback crítico - teste 1699523045123",
  "rating": 1,
  "urgency": "HIGH",
  "createdAt": "2024-11-08T18:50:45.123Z"
}
```

### Exemplo de resposta de erro
```json
{
  "title": "Erro de validação",
  "status": 400,
  "detail": "Nota deve ser no mínimo 0"
}
```

---

## Consulta GraphQL: listFeedbacks

Recupera todos os feedbacks armazenados no DynamoDB.

### Requisição
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

### Resposta de sucesso (200 OK)
```json
{
  "data": {
    "listFeedbacks": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "description": "Feedback teste 1",
        "rating": 1,
        "urgency": "HIGH",
        "createdAt": "2024-11-08T18:50:45.123Z"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440001",
        "description": "Feedback teste 2",
        "rating": 8,
        "urgency": "LOW",
        "createdAt": "2024-11-08T19:22:30.456Z"
      }
    ]
  }
}
```

---

## Executando no ambiente local

### Pré-requisitos
1. Credenciais da AWS com o perfil `fase4` (o módulo local grava na tabela `dev-feedback-table`)
2. Inicie o módulo: na pasta `local-http`, execute `./run.sh local-up`
3. Rotas disponíveis em `http://localhost:8080` (`/v1/feedback` e `/v1/graphql`)

### Passos
1. Mantenha o `localToken` padrão (ou exporte `API_TOKEN` antes de `./run.sh local-up` e defina o mesmo valor em `localToken`)
2. Execute "✅ 201 — rating 1 (urgência HIGH)" (uma nota de 0 a 5 dispara SNS e e-mail)
3. Execute os demais casos de teste em qualquer ordem, incluindo as requisições 401 ("sem token" e "token inválido")

---

## Executando em produção (AWS)

### Pré-requisitos
1. Função AWS Lambda implantada e acessível
2. Endpoint do API Gateway ativo: `https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod/v1/feedback`
3. Tabela DynamoDB `prod-feedback-table` disponível em sa-east-1
4. SES com identidades verificadas (se as notificações por e-mail estiverem habilitadas)
5. `prodToken` definido com o valor do segredo `API_TOKEN`

### Passos
1. Defina `prodToken` (Collection > Variables)
2. Execute os casos de teste (mesma estrutura do local). Requisições com nota de 0 a 5 criam registros reais e enviam SNS e e-mail em produção
3. Acompanhe os logs de execução da Lambda no AWS CloudWatch Logs
4. Verifique os itens do DynamoDB criados no console

### Limitações conhecidas
- **Bearer token obrigatório** em todas as rotas (`Authorization: Bearer <token>`); para criá-lo, consulte o segredo `API_TOKEN` em `docs/deploy/AWS_DEPLOY_GUIDE.md`
- **Notificações por e-mail**: ainda em desenvolvimento; teste com o modo sandbox do SES

---

## Resumo do contrato da API

### Endpoint
```
POST /v1/feedback
Host: {{localUrl}} or {{prodUrl}}
Authorization: Bearer <token>
Content-Type: application/json
```

### Corpo da requisição
```json
{
  "description": "string (required, non-blank)",
  "rating": "integer (required, 0-10 inclusive)"
}
```

### Resposta de sucesso (201 Created)
```json
{
  "id": "UUID",
  "description": "string (echoed from request)",
  "rating": "integer (echoed from request)",
  "urgency": "HIGH|MEDIUM|LOW (calculated)",
  "createdAt": "ISO 8601 timestamp"
}
```

### Resposta de erro (401 Não autorizado)
```json
{
  "title": "Não autorizado",
  "status": 401,
  "detail": "Token Bearer ausente | Token Bearer inválido"
}
```

### Resposta de erro (400 Bad Request)
```json
{
  "title": "Erro de validação|Requisição inválida|etc",
  "status": 400,
  "detail": "Specific error message explaining the violation"
}
```

---

## Solução de problemas

### Solução de problemas

#### 401 Não autorizado
- **Causa:** `localToken`/`prodToken` vazio ou diferente do token configurado na API (`API_TOKEN`)
- **Correção:** Defina a variável em Collection > Variables; localmente, use o mesmo valor passado ao `./run.sh local-up`

#### 404/405 em {{localUrl}}/v1/feedback
- **Causa:** o local-http não está em execução, ou foi iniciado na pasta errada (o módulo `receive-feedback` não tem rotas HTTP)
- **Correção:** Execute `./run.sh local-up` na pasta `local-http` e mantenha-o em execução

### 500 Erro interno do servidor
- **Causa:** falha na inicialização do handler da Lambda
- **Correção:** Verifique os logs do CloudWatch em busca de erros de bootstrap do Arc CDI; confirme se o handler do Quarkus está configurado

### Falhas nas asserções de teste
- **Causa:** a estrutura da resposta não corresponde (por exemplo, falta o wrapper `receiveFeedback`)
- **Correção:** Verifique se a API foi atualizada para o formato REST (não GraphQL); verifique os logs da Lambda

### Erro de descrição duplicada (se houver persistência em armazenamento externo)
- **Causa:** o mesmo teste foi executado várias vezes com descrições fixas
- **Correção:** Use `{{timestamp}}` em todas as descrições; ele é gerado automaticamente por requisição

---

## Verificação da importação

Depois de importar, verifique se a coleção carregou corretamente:

1. **Variáveis:** Collection > Variables mostra `localUrl`, `prodUrl`, `localToken`, `prodToken`, `feedbackId` e `timestamp`
2. **Pastas:** apenas `local` e `prod`, com 20 requisições cada
3. **Execute um caso 401:** "🔒 401 — GET /v1/feedback sem token" → HTTP 401
4. **Execute um caso de sucesso:** "✅ 200 — GET /v1/feedback com token" (local) → HTTP 200

---

## Problemas conhecidos e trabalhos futuros

### Ainda não implementado
- **Endpoint Listar Avaliações:** consulta para obter feedback por ID ou filtro
- **Notificações por e-mail:** a infraestrutura existe, mas ainda não está conectada ao handler Quarkus

### Descontinuado (da Fase-3)
- **Mutações GraphQL:** a coleção não testa mais mutações GraphQL; executa apenas a consulta `listFeedbacks`
- **Cabeçalho legado de chave GraphQL:** não é mais aceito; todas as rotas usam `Authorization: Bearer <token>`

---

## Suporte e feedback

Para problemas com a coleção:
1. Verifique o contrato da API em `/Users/jefferson/Documents/FIAP/fase-4/docs/api-collection/plan.md`
2. Consulte o código-fonte em `/Users/jefferson/Documents/FIAP/fase-4/receive-feedback/src/main/java/com/fiap/feedback/infra/handler/ReceiveFeedbackHandler.java`
3. Revise os logs de Lambda no AWS CloudWatch para ver os detalhes reais do erro
4. Confirme se o local-http está em execução para testes locais: `./run.sh local-check`
