# Fase-4: Plataforma Serverless de Feedback

Uma plataforma serverless de feedback de nível produção construída em AWS Lambda, DynamoDB, SNS e EventBridge. Esta plataforma demonstra princípios de Clean Architecture com uma stack Java moderna, fornecendo alta throughput e baixa latência na coleta e processamento de feedbacks.

## Diagrama de Arquitetura

### Fluxo Completo do Sistema

```
┌───────────────────────────────────────────────────────────────────────────┐
│                    SERVERLESS FEEDBACK PLATFORM                           │
│                   3 Lambda Functions + Event-Driven                       │
└───────────────────────────────────────────────────────────────────────────┘

                            ┌─────────────────┐
                            │   USER/CLIENT   │
                            └────────┬────────┘
                                     │
                    POST /avaliacao  │  GET /avaliacao
                    {"descricao":..  │
                     "nota": 1}      │
                                     ▼
                        ┌────────────────────────┐
                        │    API GATEWAY         │
                        │  v32wl8xbtk (prod)     │
                        │  /prod/avaliacao       │
                        └──────────┬─────────────┘
                                   │ invoke
                                   ▼
        ┌──────────────────────────────────────────────────────────┐
        │  LAMBDA 1: receive-feedback                              │
        │  ┌────────────────────────────────────────────────────┐  │
        │  │ ReceiveFeedbackUseCase                             │  │
        │  │                                                    │  │
        │  │  1. Validates: nota 0-10, descrição não vazia     │  │
        │  │  2. Creates: new Feedback(desc, rating)           │  │
        │  │  3. Calculates Urgency:                           │  │
        │  │     • HIGH:   nota 0-5                            │  │
        │  │     • MEDIUM: nota 6-7                            │  │
        │  │     • LOW:    nota 8-10                           │  │
        │  │  4. Persists: DynamoDB → prod-feedback-table      │  │
        │  │  5. Decision: if (urgency == HIGH) publish SNS    │  │
        │  └──────────────┬─────────────────────┬──────────────┘  │
        └─────────────────┼─────────────────────┼─────────────────┘
                          │                     │
              ┌───────────┘                     └──────────────┐
              │ ALL feedbacks                   │ HIGH only    │
              ▼                                 ▼              │
    ┌──────────────────┐          ┌────────────────────────┐  │
    │    DYNAMODB      │          │    SNS TOPIC           │  │
    │ prod-feedback-   │          │ prod-feedback-urgent-  │  │
    │    table         │          │    topic               │  │
    │                  │          │                        │  │
    │ Stores:          │          │ Message:               │  │
    │ • id (PK)        │          │ {                      │  │
    │ • descricao      │          │   "id": "uuid",        │  │
    │ • nota           │          │   "description": "...", │  │
    │ • urgencia       │          │   "urgency": "HIGH",   │  │
    │ • dataCriacao    │          │   "rating": 1          │  │
    └──────┬───────────┘          │ }                      │  │
           │                      └───────┬────────────────┘  │
           │                              │ trigger           │
           │                              ▼                   │
           │              ┌──────────────────────────────────────┐
           │              │ LAMBDA 2: send-notification          │
           │              │ ┌──────────────────────────────────┐ │
           │              │ │ SendNotificationUseCase          │ │
           │              │ │                                  │ │
           │              │ │ 1. Receives SNSEvent (auto)      │ │
           │              │ │ 2. Parses JSON message           │ │
           │              │ │ 3. Creates Notification entity   │ │
           │              │ │ 4. Sends email via SES:          │ │
           │              │ │    • To: admin@example.com       │ │
           │              │ │    • Subject: "⚠️ Crítico"      │ │
           │              │ │    • Body: HTML with details     │ │
           │              │ └──────────────┬───────────────────┘ │
           │              └─────────────────┼─────────────────────┘
           │                                ▼
           │                    ┌───────────────────────┐
           │                    │   AMAZON SES          │
           │                    │   Email sent to admin │
           │                    └───────────────────────┘
           │
           │ Weekly scan (every Monday 8h UTC)
           │
           ▼
    ┌──────────────────────────────────────────────────────────┐
    │  EVENTBRIDGE CRON                                        │
    │  Schedule: cron(0 8 ? * MON *)                           │
    │  Trigger: Every Monday 8 AM UTC (5 AM Brasília)          │
    └─────────────────────┬────────────────────────────────────┘
                          │ invoke weekly
                          ▼
        ┌──────────────────────────────────────────────────────────┐
        │  LAMBDA 3: generate-report                               │
        │  ┌────────────────────────────────────────────────────┐  │
        │  │ GenerateReportUseCase                              │  │
        │  │                                                    │  │
        │  │ 1. Period: last 7 days (startDate → endDate)      │  │
        │  │ 2. Query: DynamoDB.scan(period)                   │  │
        │  │ 3. Aggregates:                                    │  │
        │  │    • Total feedbacks                              │  │
        │  │    • Average rating                               │  │
        │  │    • Count by day (7 days)                        │  │
        │  │    • Count by urgency (HIGH/MEDIUM/LOW)           │  │
        │  │ 4. Builds HTML report                             │  │
        │  │ 5. Sends via SES:                                 │  │
        │  │    • Subject: "Weekly Feedback Report"            │  │
        │  │    • Body: HTML tables with statistics            │  │
        │  └────────────────────────────────────────────────────┘  │
        └─────────────────────┬────────────────────────────────────┘
                              ▼
                  ┌───────────────────────────┐
                  │   AMAZON SES              │
                  │   Weekly report sent      │
                  │   to admin@example.com    │
                  └───────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│  DECISÕES ARQUITETURAIS PRINCIPAIS                                        │
├───────────────────────────────────────────────────────────────────────────┤
│  ✓ Event-Driven: SNS desacopla submissão de feedback da notificação      │
│  ✓ Serverless: Auto-scaling, pagamento por requisição, sem infraestrutura│
│  ✓ Clean Architecture: Domain → Application → Infrastructure              │
│  ✓ Notificação Seletiva: Apenas urgência HIGH dispara email imediato     │
│  ✓ Relatórios Agendados: EventBridge cron para agregação semanal         │
│  ✓ Persistência: DynamoDB para escritas rápidas e consultas flexíveis    │
└───────────────────────────────────────────────────────────────────────────┘
```

### Resumo do Fluxo de Dados

| Etapa | Componente | Entrada | Saída | Latência |
|------|-----------|-------|--------|---------|
| 1 | API Gateway | POST /avaliacao | → Lambda 1 | ~10ms |
| 2 | Lambda 1 (receive) | JSON payload | → DynamoDB + SNS (se HIGH) | ~50-200ms |
| 3 | DynamoDB | Entidade Feedback | Persistido | ~20-50ms |
| 4 | SNS Topic | Mensagem JSON | → Lambda 2 (se HIGH) | ~100-500ms |
| 5 | Lambda 2 (notify) | Evento SNS | → Email SES | ~50-200ms |
| 6 | Lambda 3 (report) | Trigger cron | → Scan DynamoDB → Email SES | ~2-5s |

**Latência total (urgência HIGH):** ~250ms - 1s (usuário recebe resposta 201, email enviado assincronamente)

## Como Funciona

### 🔄 Fluxo 1: Notificação Imediata (Feedback de Urgência HIGH)

Quando um usuário submete feedback com avaliação baixa (0-5), o sistema dispara uma notificação imediata:

1. **Usuário submete** `POST /avaliacao` com `nota: 1` (feedback crítico)
2. **Lambda 1 (receive-feedback)**:
   - Valida entrada (avaliação 0-10, descrição não vazia)
   - Calcula urgência: `nota 1 → HIGH`
   - Persiste no DynamoDB
   - **Publica no SNS** (apenas se HIGH)
3. **SNS Topic** recebe mensagem e **automaticamente invoca** Lambda 2
4. **Lambda 2 (send-notification)**:
   - Recebe evento SNS
   - Faz parse dos detalhes do feedback
   - Envia email via SES para admin
   - Assunto do email: "⚠️ Feedback Crítico Recebido"

**Resultado:** Admin recebe email dentro de 1 segundo da submissão.

### 📊 Fluxo 2: Relatório Semanal (Agendado)

Toda segunda-feira às 8h UTC (5h Brasília), o sistema gera um relatório semanal automatizado:

1. **EventBridge Cron** dispara Lambda 3 no horário agendado
2. **Lambda 3 (generate-report)**:
   - Calcula período: últimos 7 dias
   - Consulta DynamoDB por todos os feedbacks no período
   - Agrega estatísticas:
     - Total de feedbacks
     - Média de avaliação (ex: 6.20)
     - Contagem de feedbacks por dia (7 dias)
     - Contagem de feedbacks por urgência (HIGH/MEDIUM/LOW)
   - Constrói relatório HTML com tabelas
   - Envia email via SES para admin
3. **Admin recebe** resumo semanal abrangente

**Relatório inclui:**
- Total de feedbacks no período
- Média de avaliação
- Tabela de distribuição diária
- Tabela de distribuição por urgência

### 🎯 Regras de Cálculo de Urgência

| Avaliação | Urgência | Notificação | Relatório Semanal |
|--------|---------|--------------|---------------|
| 0-5    | HIGH    | ✅ Email imediato | ✅ Incluído |
| 6-7    | MEDIUM  | ❌ Sem notificação | ✅ Incluído |
| 8-10   | LOW     | ❌ Sem notificação | ✅ Incluído |

**Configuração:** Limites são definidos em `application.yaml` e podem ser ajustados sem mudanças no código.

## Badges

[![Build Status](https://github.com/jeffesa/12ADJT-fase-4/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/jeffesa/12ADJT-fase-4/actions)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jeffesa_12ADJT-fase-4&metric=coverage)](https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=jeffesa_12ADJT-fase-4&metric=alert_status)](https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4)
[![AWS Deployment](https://img.shields.io/badge/AWS%20Deployment-Live-brightgreen)]()
[![Tests](https://img.shields.io/badge/Tests-146%2B%20passing-green)]()

## Status de Implementação

✅ **TASK-019: Todas as 3 Lambdas Implantadas e Operacionais**
- Verificado na AWS: Todos os endpoints respondendo corretamente
- Endpoint: `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/avaliacao`
- 100% de testes end-to-end completados (POST /avaliacao, persistência DynamoDB, notificações SNS)

✅ **TASK-020: JaCoCo + SonarCloud Totalmente Configurados**
- Gate de cobertura: 80% (imposto)
- 146+ testes passando, 0 falhas
- Relatórios HTML e XML gerados para todos os módulos
- Pipeline CI/CD automatizado em push/PR

✅ **API Pronta para Produção**
- Endpoint GraphQL totalmente funcional
- Validação de entrada funcionando (restrição de avaliação 0-10 imposta)
- Tratamento de erros retornando códigos HTTP adequados
- Todos os campos CDI @Inject do Quarkus devidamente inicializados

## Início Rápido

### 1. Clonar o Repositório

```bash
git clone https://github.com/jeffesa/12ADJT-fase-4.git
cd fase-4
```

### 2. Construir o Projeto

```bash
mvn clean package -DskipTests
```

### 3. Executar Testes

**Todos os testes (todos os módulos):**
```bash
mvn clean test
```

**Módulo específico:**
```bash
mvn clean test -pl receive-feedback
mvn clean test -pl send-notification
mvn clean test -pl generate-report
```

**Com relatório de cobertura:**
```bash
mvn clean verify
```

Status atual: **146+ testes, 0 falhas, 0 erros**. Relatórios de cobertura são gerados em `target/site/jacoco/jacoco.html` para cada módulo e agregados no diretório target raiz.

## Desenvolvimento Local com Quarkus Dev Mode

### Pré-requisitos

- Java 17 ou posterior
- Maven 3.8+
- AWS CLI (opcional, para testes contra AWS)
- Editor de texto ou IDE (VS Code, IntelliJ, etc.)

### Fluxo de Desenvolvimento Local

Para desenvolvimento local, use o Quarkus Dev Mode com hot reload. Esta abordagem permite testar localmente sem LocalStack:

**Em 3 janelas de terminal separadas:**

Terminal 1 — receive-feedback (porta 8080):
```bash
cd receive-feedback
mvn quarkus:dev
```

Terminal 2 — send-notification (porta 8081):
```bash
cd send-notification
mvn quarkus:dev
```

Terminal 3 — generate-report (porta 8082):
```bash
cd generate-report
mvn quarkus:dev
```

Uma vez que todos os três módulos estejam rodando, você pode acessar:
- receive-feedback: `http://localhost:8080`
- send-notification: `http://localhost:8081`
- generate-report: `http://localhost:8082`

### Estratégia de Desenvolvimento Local + Testes AWS

**Para Testes Unitários/Integração:**
```bash
mvn clean verify
```

**Para Testes de Integração AWS:**

Aponte o Postman para o endpoint AWS real:
```
https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/
```

A coleção Postman está disponível em `docs/api-collection/fase4-feedback.postman_collection.json`. Você pode importá-la e alternar entre ambientes local e produção.

### Testando com Quarkus Dev Mode

Uma vez que os módulos estejam rodando via `mvn quarkus:dev`, você pode testar localmente:

### Executando o receive-feedback com HTTP local

Pré-requisito (uma vez): `mvn -B -q -DskipTests -Dquarkus.devservices.enabled=false install -pl .,receive-feedback`

Comando para iniciar (JDK 17, na raiz do repositório):

```bash
AWS_PROFILE=fase4 AWS_REGION=sa-east-1 GRAPHQL_API_KEY=<sua-chave-local> \
  mvn -B quarkus:dev -Ddebug=false -Dquarkus.console.enabled=false
```

Rotas disponíveis em `http://localhost:8080`:

- `POST /avaliacao`: cria um feedback (ex.: `{"descricao":"Teste local","nota":8}`)
- `GET /avaliacao`: lista os feedbacks
- `POST /graphql`: consulta GraphQL, exige o header `X-API-Key` com o valor de `GRAPHQL_API_KEY`

A variável `GRAPHQL_API_KEY` deve ser definida apenas no ambiente local (shell). Nunca faça commit dela.

> **Atenção:** a execução local grava na tabela de desenvolvimento `dev-feedback-table` (não na `prod-feedback-table`). Remova os itens de teste criados quando terminar.

## Estrutura do Projeto

```
fase-4/
├── README.md                      # Este arquivo
├── pom.xml                        # POM Maven pai (multi-módulo)
├── sonar-project.properties       # Configuração SonarCloud
│
├── receive-feedback/              # Lambda 1: Recebe e persiste feedbacks
│   ├── pom.xml
│   ├── GRAPHQL_TEST_GUIDE.md
│   └── src/
│       ├── main/java/com/fiap/feedback/
│       │   ├── domain/            # Clean Architecture: Camada de domínio
│       │   │   ├── entity/        # Entidades de negócio (Feedback, Urgency)
│       │   │   └── gateway/       # Interfaces de gateway
│       │   ├── application/       # Camada de aplicação
│       │   │   ├── usecase/       # Casos de uso (ReceiveFeedbackUseCase)
│       │   │   └── dto/           # DTOs de aplicação
│       │   └── infra/             # Camada de infraestrutura
│       │       ├── graphql/       # Resolvers GraphQL
│       │       ├── persistence/   # Implementação DynamoDB
│       │       ├── messaging/     # Implementação SNS
│       │       └── dto/           # DTOs Input/Output
│       └── test/java/             # Testes unitários (23 arquivos, 146 testes)
│
├── send-notification/             # Lambda 2: Envia notificações para urgência HIGH
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fiap/notification/
│       │   ├── domain/
│       │   ├── application/
│       │   └── infra/
│       └── test/java/
│
├── generate-report/               # Lambda 3: Gera relatórios agregados
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fiap/report/
│       │   ├── domain/
│       │   ├── application/
│       │   └── infra/
│       └── test/java/
│
├── infra/                         # Infraestrutura AWS (SAM)
│   ├── infra/                    # Infraestrutura AWS
│   │   ├── template.yaml              # Template SAM (Lambdas, DynamoDB, SNS, EventBridge)
│   │   ├── samconfig.toml             # Configuração de deployment SAM
│   │   └── (localstack-init.sh removido — use AWS para persistência)
│   │
├── docs/                          # Documentação
│   ├── planejamento/              # Planejamento e backlog
│   │   ├── BACKLOG.md
│   │   └── HANDOFF_SESSION.md
│   ├── qualidade/                 # Documentação de qualidade e testes
│   ├── deploy/                    # Guias de deployment
│   ├── api-collection/            # Coleções de requisições GraphQL/REST
│   └── ci-cd/                     # Documentação do pipeline CI/CD
│
├── run.sh                        # Script de workflow de desenvolvimento (Quarkus dev mode, testes AWS)
├── e2e-test.sh                    # Script de testes end-to-end
├── e2e-verify.sh                  # Script de verificação E2E
└── .github/
    └── workflows/
        ├── ci-cd.yml              # Build, teste e análise SonarCloud
        ├── branch-naming.yml      # Validação de nomenclatura de branches
        └── auto-pr-to-main.yml    # PR automático para main após merge em develop
```

## Tecnologias

| Tecnologia | Versão | Justificativa |
|---|---|---|
| **Java** | 17 | Versão LTS; forte desempenho e estabilidade de recursos para cargas de trabalho empresariais. |
| **Quarkus** | 3.8.x | Otimizado para AWS Lambda; redução do consumo de memória, tempos de inicialização mais rápidos, suporte à compilação nativa. |
| **Maven** | 3.8+ | Ferramenta de build padrão da indústria; excelente suporte para projetos multi-módulo. |
| **DynamoDB** | AWS gerenciado | Serverless, preço por requisição, escala automaticamente, suporte à replicação global. |
| **SNS** | AWS gerenciado | Notificações orientadas a eventos; desacopla processamento de feedback da entrega de email. |
| **EventBridge** | AWS gerenciado | Roteamento de eventos flexível; habilita workflows complexos entre serviços. |
| **SES** | AWS gerenciado | Entrega de email custo-efetiva; integra com tópicos SNS. |
| **Lambda** | Runtime Java 17 | Computação serverless; cobra apenas pelo tempo de execução; integração nativa com serviços AWS. |
| **GraphQL** | SmallRye 2.x | Design de API type-safe; endpoint único; linguagem de query forte; ideal para backends serverless. |
| **Quarkus** | 3.8.4 | Framework Java leve; hot reload para dev local; compilado nativo; ótimo para Lambda. |
| **JaCoCo** | 0.8.11 | Medição de cobertura de código; integra com SonarCloud. |
| **SonarCloud** | Mais recente | Qualidade de código contínua; imposição de gate de cobertura; escaneamento de segurança OWASP. |

## Testes e Cobertura

### Status Atual

- **Total de testes:** 146+ passando
- **Falhas:** 0
- **Erros:** 0
- **Gate de cobertura:** ≥80% (imposto pelo SonarCloud)
- **Integração CI/CD:** Automatizado em push/PR

### Executando Testes

**Executar todos os testes:**

```bash
mvn clean test
```

**Executar testes para um módulo específico:**

```bash
mvn clean test -pl receive-feedback
mvn clean test -pl send-notification
mvn clean test -pl generate-report
```

**Executar testes com relatório de cobertura:**

```bash
mvn clean verify
```

Relatórios de cobertura são gerados em `target/site/jacoco/jacoco.html` para cada módulo e agregados na raiz do projeto.

### Metas de Cobertura

- **Limite geral:** ≥80% (imposto pelo quality gate do SonarCloud)
- **Status da meta:** Todos os módulos testados e cobertos
- **Detalhamento de cobertura por módulo:**
  - **receive-feedback:** Camadas de domínio e aplicação testadas (casos de uso, validadores)
  - **send-notification:** Refatorado com @QuarkusTest; 14 testes unitários adicionados
  - **generate-report:** Alta cobertura (91.17% de cobertura de linha, 100% de cobertura de classe)

### Excluídos da Cobertura

- `**/config/**` — Classes de configuração (setup de ambiente)
- `**/dto/**` — Data Transfer Objects (mapeadores simples)
- `**/*Handler.java` — Handlers de requisição Lambda (apenas runtime, testados via integração)
- `**/exception/**` — Exceções customizadas (implementações triviais)
- `**/persistence/**` e `**/messaging/**` — Camada de infraestrutura (testados via integração)

### Visualizando Relatórios de Cobertura

Após executar os testes:

```bash
# Abrir relatório HTML no navegador
open target/site/jacoco/jacoco.html

# Ou visualizar relatórios por módulo
open receive-feedback/target/site/jacoco/jacoco.html
open send-notification/target/site/jacoco/jacoco.html
open generate-report/target/site/jacoco/jacoco.html
```

## CI/CD e SonarCloud

### Pipeline CI/CD

Disparado em:
- Push para branch `main` ou `develop`
- Pull request para branch `develop`
- Dispatch manual de workflow

**Etapas do pipeline:**

1. **Checkout code** — Clona repositório com histórico completo (necessário para SonarCloud)
2. **Setup Java 17** — Runtime de build Maven
3. **Build and Test** — Executa `mvn clean verify`
4. **Generate coverage** — JaCoCo cria relatórios XML
5. **SonarCloud Scan** — Analisa qualidade de código e cobertura
6. **Quality gate check** — Falha se cobertura < 80% ou problemas críticos detectados

### Configuração SonarCloud

- **Organização:** jeffesa
- **Project Key:** jeffesa_12ADJT-fase-4
- **URL:** https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4

**Regras do quality gate:**

- Cobertura: ≥80%
- Problemas críticos: 0
- Problemas bloqueadores: 0

### Acessando SonarCloud

```bash
# Visualizar dashboard do projeto
open https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4

# Visualizar métricas de qualidade de código
open https://sonarcloud.io/code?id=jeffesa_12ADJT-fase-4

# Visualizar problemas de segurança
open https://sonarcloud.io/security?id=jeffesa_12ADJT-fase-4
```

## FAQ

### Como testar a API GraphQL localmente?

Use o comando `sam local start-api` para executar o endpoint GraphQL em sua máquina, então envie requisições via curl ou Postman.

### O que acontece quando eu submeto feedback de urgência HIGH?

A Lambda `receive-feedback` persiste o feedback no DynamoDB e publica uma mensagem no tópico SNS. A Lambda `send-notification` se inscreve nesse tópico, recebe o evento e envia uma notificação por email.

### Posso executar o projeto sem credenciais AWS?

Para desenvolvimento local com Quarkus Dev Mode, você pode testar localmente sem credenciais AWS. No entanto, testes de integração requerem acesso à AWS. Para fins acadêmicos, use o endpoint de deployment AWS real para testes E2E.

### Como a cobertura de código é medida?

JaCoCo instrumenta seu código durante a execução de testes e gera relatórios XML. SonarCloud analisa esses relatórios e impõe um limite mínimo de cobertura (≥80%).

### Qual é a estrutura da tabela DynamoDB?

A `dev-feedback-table` (ou `prod-feedback-table`) tem:
- **Partition Key:** `id` (String, UUID)
- **Atributos:** `descricao`, `nota`, `createdAt`, `urgency`, `ttl`
- **TTL:** 90 dias (auto-exclusão de feedbacks antigos)

### Como fazer deploy em produção?

O deploy é automatizado pelo GitHub Actions (`.github/workflows/ci-cd.yml`) usando AWS SAM e autenticação OIDC, sem chaves de acesso de longa duração:

1. Abra um Pull Request de `develop` para `main`.
2. O workflow roda build, testes e análise SonarCloud. O quality gate falha o job se reprovar. Para bloquear o merge, o job `Build & Test & SonarCloud` deve ser configurado como status check obrigatório na proteção da branch `main`.
3. Após o merge em `main`, o job `deploy` executa `sam build` e `sam deploy --config-env prod` automaticamente.

Os secrets `AWS_DEPLOY_ROLE_ARN` e `ADMIN_EMAIL` são configurados no environment `prod` do GitHub.

### Posso usar endpoints REST ao invés de GraphQL?

Não. Esta plataforma implementa apenas GraphQL. Todas as operações (mutation e query) usam o protocolo GraphQL via um único endpoint.

### Como monitorar o desempenho das Lambdas?

Visualize CloudWatch Logs e traces X-Ray no Console AWS, ou configure dashboards CloudWatch para visualizar:
- Contagem de invocações Lambda
- Taxa de erros e duração
- Capacidade de leitura/escrita consumida do DynamoDB
- Contagem de mensagens SNS

## Links Úteis

- **Repositório:** https://github.com/jeffesa/12ADJT-fase-4
- **Dashboard SonarCloud:** https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4
- **Endpoint API AWS (Produção):** https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/avaliacao
- **Collection Bruno:** [bruno/README.md](bruno/README.md)
- **Documentação Quarkus:** https://quarkus.io
- **Guia do Desenvolvedor AWS Lambda:** https://docs.aws.amazon.com/lambda
- **AWS SAM:** https://aws.amazon.com/serverless/sam/
- **Guia do Desenvolvedor DynamoDB:** https://docs.aws.amazon.com/dynamodb
- **Guia do Desenvolvedor SNS:** https://docs.aws.amazon.com/sns
- **Documentação EventBridge:** https://docs.aws.amazon.com/eventbridge

