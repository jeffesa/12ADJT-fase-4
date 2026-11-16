# BACKLOG - Tech Challenge Fase 4
## Plataforma de Feedback Serverless (AWS)

> **Continuação da Fase 3** — Este projeto implementa uma plataforma de feedback de aulas
> com funções serverless (AWS Lambda), comunicação assíncrona (SNS) e relatórios periódicos
> (EventBridge), utilizando Quarkus 3.8.x e Clean Architecture conforme padrão das fases anteriores.

---

## 📋 ÉPICO 1: Configuração Inicial e Infraestrutura

### TASK-001: Criar estrutura multi-module Maven com Quarkus

**Labels:** `priority: high`, `épico: setup`, `pontos: 5`, `type: infra`
**Milestone:** Sprint 1 - Fundação
**Status:** ✅ CONCLUÍDA

## 📋 Descrição
Configurar o projeto Maven multi-module com parent POM e 3 módulos filhos (receive-feedback, send-notification, generate-report). Cada módulo é uma função Lambda independente com Quarkus 3.8.x e Java 17.

## ✅ Critérios de Aceitação
- [ ] Parent POM na raiz com `<modules>` definidos
- [ ] Módulo `receive-feedback` com pom.xml próprio
- [ ] Módulo `send-notification` com pom.xml próprio
- [ ] Módulo `generate-report` com pom.xml próprio
- [ ] Quarkus BOM 3.8.x gerenciado no parent
- [ ] Extensões Quarkus: `quarkus-amazon-lambda`, `quarkus-amazon-dynamodb`, `quarkus-amazon-sns`, `quarkus-amazon-ses`
- [ ] Java 17 configurado em todos os módulos
- [ ] Cada módulo compila sem erros (`mvn clean compile`)
- [ ] `.gitignore` configurado (target/, .idea/, .DS_Store, .env, etc.)
- [ ] Profiles configurados: dev (LocalStack), test (mocks), prod (AWS real)

## 🔧 Dependências Técnicas
- Java 17+
- Quarkus 3.8.x (LTS)
- Maven 3.8+

---

### TASK-002: Configurar Clean Architecture em cada módulo

**Labels:** `priority: high`, `épico: setup`, `pontos: 3`, `type: infra`
**Milestone:** Sprint 1 - Fundação

## 📋 Descrição
Organizar pacotes seguindo Clean Architecture em cada módulo Lambda, reutilizando o padrão das Fases 2 e 3: domain → application → infra.

## ✅ Critérios de Aceitação
- [ ] `receive-feedback`: `com.fiap.feedback.{domain,application,infra}`
- [ ] `send-notification`: `com.fiap.notification.{domain,application,infra}`
- [ ] `generate-report`: `com.fiap.report.{domain,application,infra}`
- [ ] Subpacotes: `domain/{entity,gateway}`, `application/usecase`, `infra/{handler,persistence,messaging,config}`
- [ ] Dependências fluem de fora para dentro (infra → application → domain)
- [ ] Classes de domínio são POJOs puros (sem anotações de framework)

## 🔧 Dependências Técnicas
- [TASK-001] concluída

---

### ✅ TASK-003: Configurar docker-compose com LocalStack — **CONCLUÍDA**

**Labels:** `priority: high`, `épico: setup`, `pontos: 4`, `type: infra`
**Milestone:** Sprint 1 - Fundação
**Status:** ✅ CONCLUÍDA

## 📋 Descrição
Criar docker-compose.yml na raiz com LocalStack para simular os serviços AWS localmente: DynamoDB, SNS, SES, Lambda, API Gateway, EventBridge. Criar script `run.sh` para inicialização local.

## ✅ Critérios de Aceitação
- [x] `docker-compose.yml` com serviço LocalStack 3.x
- [x] Serviços habilitados: dynamodb, sns, ses, lambda, apigateway, events
- [x] Porta padrão: 4566
- [x] Variáveis de ambiente para dev: `AWS_ACCESS_KEY_ID=test`, `AWS_SECRET_ACCESS_KEY=test`, `AWS_DEFAULT_REGION=us-east-1`
- [x] `run.sh` com comandos para subir LocalStack e criar recursos (tabela DynamoDB, tópico SNS)
- [x] `.dockerignore` configurado
- [x] `application.properties` de cada módulo com URLs do LocalStack para profile dev

## 🔧 Dependências Técnicas
- [TASK-001] concluída

---

### ✅ TASK-004: Configurar AWS SAM (template.yaml e samconfig.toml) — **CONCLUÍDA**

**Labels:** `priority: high`, `épico: setup`, `pontos: 5`, `type: infra`
**Milestone:** Sprint 1 - Fundação
**Status:** ✅ CONCLUÍDA

## 📋 Descrição
Criar o template SAM completo (`infra/template.yaml`) definindo todos os recursos AWS: 3 Lambdas, API Gateway com API Key, DynamoDB, SNS topic, EventBridge rule e permissões IAM. Criar `infra/samconfig.toml` com parâmetros por ambiente.

## ✅ Critérios de Aceitação
- [x] `infra/template.yaml` com todos os recursos:
  - [x] Lambda `ReceiveFeedbackFunction` — trigger: API Gateway POST /graphql
  - [x] Lambda `SendNotificationFunction` — trigger: SNS topic (urgência HIGH)
  - [x] Lambda `GenerateReportFunction` — trigger: EventBridge cron (toda segunda 8h)
  - [x] DynamoDB table `feedbacks` com PK `id` (String)
  - [x] SNS topic `feedback-urgent-topic`
  - [x] API Gateway com API Key obrigatória
  - [x] IAM Roles mínimas para cada Lambda (least privilege)
  - [x] CloudWatch Log Groups para cada Lambda (retenção 30 dias)
- [x] `infra/samconfig.toml` com:
  - [x] Ambiente `default` (dev)
  - [x] Parâmetro `AdminEmail` (e-mail configurável)
  - [x] Parâmetro `Environment` (dev/prod)
  - [x] Region, stack name, capabilities
- [x] `sam validate` passa sem erros

## 🔧 Dependências Técnicas
- [TASK-001] concluída

---

### ✅ TASK-005: Configurar CI/CD e SonarCloud (GitHub Actions) — **DONE**

**Labels:** `priority: high`, `épico: setup`, `pontos: 4`, `type: infra`
**Milestone:** Sprint 1 - Fundação
**Status:** ✅ CONCLUÍDA

## 📋 Descrição
Configurar GitHub Actions com pipeline de build, testes e análise SonarCloud desde o início do projeto. O SonarCloud entra na Sprint 1 porque análise de qualidade de código deve acontecer desde o primeiro PR — não só no final. O SAM deploy é adicionado ao pipeline na TASK-019 (quando o template SAM estiver pronto).

## ✅ Critérios de Aceitação
- [x] `.github/workflows/ci-cd.yml`:
  - [x] Trigger: push/PR para `develop` e `main`
  - [x] Build multi-module Maven (`mvn -B clean verify`)
  - [x] Cache Maven configurado
  - [x] Java 17 (temurin)
  - [x] SonarCloud analysis (`org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`)
  - [x] Secrets necessários: `GITHUB_TOKEN`, `SONAR_TOKEN`
  - [x] Build falha se testes falharem
- [x] `.github/workflows/branch-naming.yml`:
  - [x] Prefixos permitidos: feature/, bugfix/, hotfix/, release/, chore/, docs/, test/, refactor/, perf/, ci/, style/
- [x] `.github/workflows/auto-pr-to-main.yml`:
  - [x] Auto PR develop → main
- [x] `sonar-project.properties` na raiz configurado para multi-module
- [x] SonarCloud projeto criado em sonarcloud.io (organização: jeffesa)
- [x] `SONAR_TOKEN` adicionado nos secrets do repositório GitHub
- [x] Pipeline verde no primeiro push

## 🔧 Dependências Técnicas
- [TASK-001] concluída (projeto compila)

---

## 📋 ÉPICO 2: Lambda 1 — receive-feedback

### ✅ TASK-006: Criar entidade de domínio Feedback e gateway — **DONE**

**Labels:** `priority: high`, `épico: receive-feedback`, `pontos: 3`, `type: feature`
**Milestone:** Sprint 2 - Lambda receive-feedback
**Status:** ✅ CONCLUÍDA

## 📋 Descrição
Criar entidade `Feedback` no domain do módulo receive-feedback (POJO puro) e interface `FeedbackGateway`. Incluir enum `Urgency` com lógica de cálculo pela nota. Extrair variáveis globais para `application.yaml`.

## ✅ Critérios de Aceitação
- [x] Classe `Feedback` com campos: id (String/UUID), description, rating (int 0-10), urgency, createdAt, notificationSent (boolean)
- [x] Enum `Urgency`: HIGH (rating 0-5), MEDIUM (rating 6-7), LOW (rating 8-10)
- [x] Método estático `Urgency.fromRating(int rating)` que calcula a urgência
- [x] POJO puro sem anotações de framework
- [x] Interface `FeedbackGateway`: `save(Feedback)`, `findAll()`, `findByPeriod(LocalDate startDate, LocalDate endDate)`
- [x] Interface `UrgencyPublisher`: `publish(Feedback)` — para publicar no SNS
- [x] Validações de domínio: rating entre 0 e 10, descrição não vazia
- [x] Classe `FeedbackConfiguration` — Quarkus ConfigMapping para injetar thresholds
- [x] Classe `UrgencyCalculator` — serviço que calcula urgência via config
- [x] `application.yaml` com todas as variáveis globais (urgency thresholds, validation rules)
- [x] `package-info.java` com documentação Clean Architecture
- [x] Compilação: `mvn clean compile` SUCCESS
- [x] Todo código em inglês

## 🔧 Dependências Técnicas
- [TASK-002] concluída

---

### TASK-007: Criar caso de uso ReceiveFeedbackUseCase

**Labels:** `priority: high`, `épico: receive-feedback`, `pontos: 3`, `type: feature`
**Milestone:** Sprint 2 - Lambda receive-feedback

## 📋 Descrição
Implementar o use case principal do módulo receive-feedback: recebe a avaliação, valida, persiste e dispara notificação se urgente.

## ✅ Critérios de Aceitação
- [ ] Classe `ReceiveFeedbackUseCase` no pacote `application/usecase`
- [ ] Depende apenas de interfaces do domain (`FeedbackGateway`, `UrgencyPublisher`)
- [ ] Fluxo: valida input → cria Feedback → calcula Urgencia → salva → se HIGH publica
- [ ] Lança exceção de domínio se nota inválida (< 0 ou > 10)
- [ ] Lança exceção de domínio se descrição vazia
- [ ] Retorna o Feedback criado (com id e dataCriacao preenchidos)
- [ ] Zero imports de framework (POJO puro)

## 🔧 Dependências Técnicas
- [TASK-006] concluída

---

### TASK-008: Criar persistência DynamoDB (receive-feedback)

**Labels:** `priority: high`, `épico: receive-feedback`, `pontos: 4`, `type: feature`
**Milestone:** Sprint 2 - Lambda receive-feedback

## 📋 Descrição
Implementar o adapter de saída para DynamoDB: entidade DynamoDB, implementação do `FeedbackGateway` e configuração do cliente DynamoDB via Quarkus.

## ✅ Critérios de Aceitação
- [ ] Classe `DynamoFeedbackGateway` implementando `FeedbackGateway`
- [ ] Usa `DynamoDbClient` do AWS SDK v2 injetado pelo Quarkus
- [ ] Mapeamento `Feedback` ↔ `Map<String, AttributeValue>` (sem ORM)
- [ ] Tabela: `feedbacks`, PK: `id`
- [ ] `save()`: PUT item no DynamoDB
- [ ] `findAll()`: Scan (aceitável para volume acadêmico)
- [ ] `findByPeriod()`: Scan com FilterExpression por `dataCriacao`
- [ ] `application.properties` configurado para LocalStack (dev) e AWS real (prod)
- [ ] Funciona com LocalStack localmente

## 🔧 Dependências Técnicas
- [TASK-007] concluída
- [TASK-003] concluída (LocalStack rodando)

---

### TASK-009: Criar publisher SNS (receive-feedback)

**Labels:** `priority: high`, `épico: receive-feedback`, `pontos: 3`, `type: feature`
**Milestone:** Sprint 2 - Lambda receive-feedback

## 📋 Descrição
Implementar o adapter de saída para SNS: publicar mensagem quando urgência é HIGH.

## ✅ Critérios de Aceitação
- [ ] Classe `SnsUrgencyPublisher` implementando `UrgencyPublisher`
- [ ] Usa `SnsClient` do AWS SDK v2 injetado pelo Quarkus
- [ ] Mensagem publicada em JSON: `{ "id", "descricao", "urgencia", "nota", "dataCriacao" }`
- [ ] ARN do topic SNS via variável de ambiente `FEEDBACK_TOPIC_ARN`
- [ ] Funciona com LocalStack localmente
- [ ] Log de confirmação quando mensagem publicada

## 🔧 Dependências Técnicas
- [TASK-007] concluída
- [TASK-003] concluída (LocalStack rodando)

---

### TASK-010: Criar handler Lambda (receive-feedback)

**Labels:** `priority: high`, `épico: receive-feedback`, `pontos: 4`, `type: feature`
**Milestone:** Sprint 2 - Lambda receive-feedback

## 📋 Descrição
Implementar o handler Lambda que recebe o evento do API Gateway, converte para DTO, chama o use case e retorna a resposta HTTP.

## ✅ Critérios de Aceitação
- [ ] Classe `ReceiveFeedbackHandler` implementando `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`
- [ ] DTO `FeedbackRequest` como record com Bean Validation: `descricao` (notBlank), `nota` (min=0, max=10)
- [ ] DTO `FeedbackResponse` como record: id, descricao, nota, urgencia, dataCriacao
- [ ] Resposta HTTP 201 Created com corpo JSON do feedback criado
- [ ] Resposta HTTP 400 Bad Request para input inválido (ProblemDetail RFC 7807)
- [ ] Resposta HTTP 500 para erros internos (sem expor detalhes)
- [ ] GlobalExceptionHandler com `@Provider` (Quarkus CDI)
- [ ] Logs CloudWatch via `java.util.logging` ou `org.jboss.logging`

## 🔧 Dependências Técnicas
- [TASK-008] e [TASK-009] concluídas

---

### TASK-011: Testes unitários do módulo receive-feedback

**Labels:** `priority: high`, `épico: receive-feedback`, `pontos: 4`, `type: test`
**Milestone:** Sprint 2 - Lambda receive-feedback

## 📋 Descrição
Testes unitários para o módulo receive-feedback cobrindo use case, enum de urgência e handler.

## ✅ Critérios de Aceitação
- [ ] Testes do `ReceiveFeedbackUseCase` com mocks de `FeedbackGateway` e `UrgencyPublisher`
- [ ] Testes do enum `Urgencia.fromNota()` para todos os casos (0, 5, 6, 7, 8, 10)
- [ ] Testes do `ReceiveFeedbackHandler` com `@QuarkusTest`
- [ ] Cenários: sucesso (nota válida), erro (nota inválida), erro (descrição vazia)
- [ ] Verificação que `UrgencyPublisher.publish()` é chamado apenas quando nota ≤ 5
- [ ] Cobertura ≥ 80% no módulo (JaCoCo)

## 🔧 Dependências Técnicas
- [TASK-010] concluída

---

## 📋 ÉPICO 3: Lambda 2 — send-notification

### TASK-012: Criar domínio e use case de notificação

**Labels:** `priority: high`, `épico: send-notification`, `pontos: 3`, `type: feature`
**Milestone:** Sprint 3 - Lambda send-notification

## 📋 Descrição
Criar entidade `Notification` no domain e use case `SendNotificationUseCase`.

## ✅ Critérios de Aceitação
- [ ] Classe `Notification` com campos: feedbackId, descricao, urgencia, dataCriacao, adminEmail
- [ ] Interface `NotificationSender`: `send(Notification)`
- [ ] `SendNotificationUseCase`: recebe evento SNS, monta Notification, delega para NotificationSender
- [ ] POJO puro sem anotações de framework no domain e use case

## 🔧 Dependências Técnicas
- [TASK-002] concluída

---

### TASK-013: Criar sender SES e handler Lambda (send-notification)

**Labels:** `priority: high`, `épico: send-notification`, `pontos: 4`, `type: feature`
**Milestone:** Sprint 3 - Lambda send-notification

## 📋 Descrição
Implementar o sender de e-mail via SES e o handler Lambda que recebe eventos SNS.

## ✅ Critérios de Aceitação
- [ ] Classe `SesNotificationSender` implementando `NotificationSender`
- [ ] Usa `SesClient` do AWS SDK v2
- [ ] Template de e-mail com: Assunto "⚠️ Feedback Crítico Recebido", corpo com descrição, urgência e data
- [ ] `ADMIN_EMAIL` configurável via variável de ambiente
- [ ] Classe `SendNotificationHandler` implementando `RequestHandler<SNSEvent, Void>`
- [ ] Deserializa mensagem SNS → chama use case
- [ ] Funciona com LocalStack localmente

## 🔧 Dependências Técnicas
- [TASK-012] concluída
- [TASK-003] concluída (LocalStack rodando)

---

### TASK-014: Testes unitários do módulo send-notification

**Labels:** `priority: high`, `épico: send-notification`, `pontos: 3`, `type: test`
**Milestone:** Sprint 3 - Lambda send-notification

## 📋 Descrição
Testes unitários para o módulo send-notification.

## ✅ Critérios de Aceitação
- [ ] Testes do `SendNotificationUseCase` com mock de `NotificationSender`
- [ ] Testes do handler com evento SNS simulado
- [ ] Verificação do conteúdo do e-mail (assunto, corpo)
- [ ] Cobertura ≥ 80% no módulo (JaCoCo)

## 🔧 Dependências Técnicas
- [TASK-013] concluída

---

## 📋 ÉPICO 4: Lambda 3 — generate-report

### TASK-015: Criar domínio e use case de relatório

**Labels:** `priority: high`, `épico: generate-report`, `pontos: 3`, `type: feature`
**Milestone:** Sprint 4 - Lambda generate-report

## 📋 Descrição
Criar entidades de domínio para relatório e use case `GenerateReportUseCase`.

## ✅ Critérios de Aceitação
- [ ] Classe `WeeklyReport` com campos: periodoInicio, periodoFim, totalAvaliacoes, mediaGeral, avaliacoesPorDia (Map), avaliacoesPorUrgencia (Map)
- [ ] Interface `ReportGateway`: `findByPeriod(LocalDate inicio, LocalDate fim)` retorna lista de Feedback
- [ ] Interface `ReportSender`: `send(WeeklyReport, String adminEmail)`
- [ ] `GenerateReportUseCase`: busca feedbacks da semana, agrega dados, envia relatório
- [ ] Lógica de agregação: conta por dia, conta por urgência, calcula média das notas
- [ ] POJO puro sem framework

## 🔧 Dependências Técnicas
- [TASK-002] concluída

---

### TASK-016: Criar gateway DynamoDB, sender SES e handler Lambda (generate-report)

**Labels:** `priority: high`, `épico: generate-report`, `pontos: 5`, `type: feature`
**Milestone:** Sprint 4 - Lambda generate-report

## ✅ Critérios de Aceitação
- [ ] Classe `DynamoReportGateway` implementando `ReportGateway`
- [ ] Reutiliza estrutura do `DynamoFeedbackGateway` (consulta por período)
- [ ] Classe `SesReportSender` implementando `ReportSender`
- [ ] Template de e-mail HTML com: total de avaliações, média geral, tabela por dia, tabela por urgência
- [ ] Classe `GenerateReportHandler` implementando `RequestHandler<ScheduledEvent, Void>`
- [ ] Triggered pelo EventBridge cron
- [ ] `ADMIN_EMAIL` configurável via variável de ambiente
- [ ] Funciona com LocalStack localmente

## 🔧 Dependências Técnicas
- [TASK-015] concluída
- [TASK-003] concluída

---

### TASK-017: Testes unitários do módulo generate-report

**Labels:** `priority: high`, `épico: generate-report`, `pontos: 3`, `type: test`
**Milestone:** Sprint 4 - Lambda generate-report

## ✅ Critérios de Aceitação
- [ ] Testes do `GenerateReportUseCase` com mocks de `ReportGateway` e `ReportSender`
- [ ] Testes da lógica de agregação (médias, contagens por dia, por urgência)
- [ ] Testes do handler com evento ScheduledEvent simulado
- [ ] Cobertura ≥ 80% no módulo (JaCoCo)

## 🔧 Dependências Técnicas
- [TASK-016] concluída

---

## 📋 ÉPICO 5: Integração e Testes End-to-End

### TASK-018: Testar fluxo completo localmente com LocalStack

**Labels:** `priority: high`, `épico: integração`, `pontos: 3`, `type: test`
**Milestone:** Sprint 5 - Integração

## ✅ Critérios de Aceitação
- [ ] `docker-compose up` sobe LocalStack sem erros
- [ ] `run.sh` cria tabela DynamoDB e tópico SNS no LocalStack
- [ ] POST /avaliacao com nota ≤ 5 → persiste + publica SNS + trigger send-notification
- [ ] POST /avaliacao com nota ≥ 8 → persiste apenas (sem SNS)
- [ ] Execução manual do generate-report → relatório gerado com dados da semana
- [ ] Logs visíveis e corretos para cada Lambda

## 🔧 Dependências Técnicas
- TASK-010, 013, 016 concluídas

---

### ✅ TASK-019: Deploy e testes na AWS real — **CONCLUÍDA**

**Labels:** `priority: high`, `épico: integração`, `pontos: 5`, `type: infra`
**Milestone:** Sprint 5 - Integração
**Status:** ✅ CONCLUÍDA (Nov 8, 2026)

## 📋 Descrição
Deploy de todas as 3 funções Lambda na AWS com Quarkus Arc CDI iniciado corretamente, API Gateway funcional, DynamoDB persistindo dados, e SNS publicando feedbacks urgentes.

## ✅ Solução dos Bloqueios Anteriores

**Problema 1: Quarkus Arc CDI não inicializava**
- Raiz: EventHandlerLoader do AWS Lambda SDK não chama Quarkus Arc initialization
- Solução: Usar `io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest` como entry point (não classe direta)
- Resultado: ✅ Arc now initialized, @Inject fields populated

**Problema 2: Configuração de properties não encontrada**
- Raiz: application.yaml com syntax YAML errada (dot notation vs nested) + properties obrigatórias faltando
- Solução: Adicionar `application.properties` com defaults + corrigir YAML syntax + `quarkus.config.mapping.validate-unknown=false`
- Resultado: ✅ Quarkus inicializa sem erros

**Problema 3: Lambda não conseguia acessar DynamoDB**
- Raiz: `@WithDefault("feedbacks")` substituía o valor do TABLE_NAME do Lambda
- Solução: Adicionar `dynamodb.table.feedbacks=${TABLE_NAME:prod-feedback-table}` no properties
- Resultado: ✅ DynamoDB writes successful

**Problema 4: SNS message parsing com erro**
- Raiz: Handler procurava por `feedbackId` mas o publisher enviava `id`
- Solução: Corrigir handler para ler campo `id` correto
- Resultado: ✅ SendNotificationHandler processa mensagens SNS

**Problema 5: GenerateReport falhava com empty string validation**
- Raiz: SmallRye trata "" como null, ValidationException para @WithDefault("")
- Solução: Usar `@WithDefault("default")` ao invés de empty string
- Resultado: ✅ GenerateReport Lambda inicializa

## ✅ Critérios de Aceitação — TODOS ATENDIDOS
- [x] AWS CLI configurado localmente
- [x] SES com e-mail do admin verificado
- [x] Template validated with `sam validate --lint` (PASS)
- [x] Maven build successful (todos testes pass)
- [x] SAM build successful (artifacts packaged)
- [x] `sam deploy` successful (stack CREATE_COMPLETE)
- [x] API Gateway com public URL e sem API Key (removido)
- [x] POST /avaliacao funcional — `https://x250lwug12.execute-api.us-east-1.amazonaws.com/prod/avaliacao`
- [x] Feedback salvo em DynamoDB prod-feedback-table
- [x] HIGH urgency feedback publicado no SNS prod-feedback-urgent-topic
- [x] SendNotificationHandler processa SNS corretamente
- [x] GenerateReport Lambda operacional
- [x] CloudWatch Logs para todas as 3 Lambdas
- [x] Public URL documentada

## 📊 Testes End-to-End (Nov 8, 2026 19:38)

**Test 1: LOW urgency feedback (nota=9)**
```bash
POST /avaliacao {"descricao":"Ótimo produto","nota":9}
→ 201 Created
→ urgencia: LOW
→ Saved to DynamoDB ✅
```

**Test 2: HIGH urgency feedback (nota=1)**
```bash
POST /avaliacao {"descricao":"Produto não funciona","nota":1}
→ 201 Created
→ urgencia: HIGH
→ Saved to DynamoDB ✅
→ Published to SNS ✅
→ SendNotificationHandler invoked ✅
```

## 📝 Known Limitations
- SES in sandbox mode: requires verified email addresses for both sender and recipient (expected)
- GenerateReport triggered manually for testing; EventBridge cron will execute Monday 8 AM UTC

## 🔧 Dependências Técnicas
- [TASK-018] concluída
- [TASK-004] concluída (template.yaml)

---

## 📋 ÉPICO 6: Documentação, Qualidade e Entregáveis

### TASK-020: Configurar JaCoCo e SonarCloud

**Labels:** `priority: high`, `épico: qualidade`, `pontos: 3`, `type: infra`
**Milestone:** Sprint 6 - Finalização

## ✅ Critérios de Aceitação
- [ ] JaCoCo configurado em cada módulo (≥ 80%)
- [ ] `sonar-project.properties` configurado para multi-module
- [ ] SonarCloud com Quality Gate (0 bugs, 0 vulnerabilidades, cobertura ≥ 80%)
- [ ] Badge no README

## 🔧 Dependências Técnicas
- Todos os testes escritos (TASK-011, 014, 017)

---

### TASK-021: Criar README.md completo

**Labels:** `priority: high`, `épico: documentação`, `pontos: 3`, `type: docs`
**Milestone:** Sprint 6 - Finalização
**Status:** ✅ CONCLUÍDA

## ✅ Critérios de Aceitação
- [ ] Descrição do projeto e objetivo
- [ ] Diagrama de arquitetura AWS (ASCII art)
- [ ] Tecnologias utilizadas com justificativas
- [ ] Instruções de execução local (LocalStack + docker-compose)
- [ ] Instruções de deploy na AWS (SAM)
- [ ] Como configurar variáveis de ambiente (AdminEmail, etc.)
- [ ] Como rodar testes e ver cobertura
- [ ] URL pública da API (após deploy)
- [ ] Badges (CI/CD, cobertura, SonarCloud)

## 🔧 Dependências Técnicas
- [TASK-019] concluída

---

### TASK-022: Criar documentação da API GraphQL (docs/API.md)

**Labels:** `priority: high`, `épico: documentação`, `pontos: 2`, `type: docs`
**Milestone:** Sprint 6 - Finalização

## ✅ Critérios de Aceitação
- [ ] Endpoint GraphQL documentado (URL, port, GraphiQL)
- [ ] Schema GraphQL completo: types, queries, mutations, enums
- [ ] Mutation `criarAvaliacao(descricao: String!, nota: Int!): Feedback` documentada
- [ ] Exemplos de GraphQL query/mutation com nota LOW, MEDIUM e HIGH
- [ ] Exemplos de response (success, validation error, internal error)
- [ ] Como obter e usar a API Key (header `x-api-key`)
- [ ] Formato do e-mail de notificação documentado
- [ ] Formato do relatório semanal documentado
- [ ] Base URL local (LocalStack) e produção (AWS)
- [ ] Como testar no GraphiQL

---

### TASK-023: Criar collection Postman/GraphQL

**Labels:** `priority: high`, `épico: documentação`, `pontos: 2`, `type: docs`
**Milestone:** Sprint 6 - Finalização

## ✅ Critérios de Aceitação
- [ ] Collection Postman com requests GraphQL para mutations
- [ ] Mutation `criarAvaliacao` com cenários: nota HIGH (0-5), MEDIUM (6-7), LOW (8-10)
- [ ] Cenários de erro: nota inválida, descricao vazia, sem API Key
- [ ] Query `getFeedbacks` para listar todas as avaliações
- [ ] Query `getFeedbacksByPeriod` para buscar por período (opcional)
- [ ] Variáveis de ambiente: `localUrl`, `prodUrl`, `apiKey`
- [ ] Arquivo JSON exportado em `docs/api-collection/`
- [ ] `docs/api-collection/README.md` com instruções de importação
- [ ] GraphQL endpoint: `POST /graphql`

---

### ✅ TASK-024: Criar guia de deploy AWS (docs/deploy/AWS_DEPLOY_GUIDE.md) — **CONCLUÍDA**

**Labels:** `priority: high`, `épico: documentação`, `pontos: 3`, `type: docs`
**Milestone:** Sprint 6 - Finalização
**Status:** ✅ CONCLUÍDA

## ✅ Critérios de Aceitação
- [x] Pré-requisitos: conta AWS, AWS CLI, SAM CLI, JDK 17 (aviso sobre JDK 25), Maven, gh CLI e perfil `fase4`
- [x] Passo a passo: configurar AWS CLI, verificar e-mail no SES (CLI `sesv2`), buckets de artefatos, build (`mvn package` antes de `sam build`) e deploy
- [x] Como configurar `infra/samconfig.toml` (perfis `dev`/`prod`, `SenderEmail` placeholder, `ApiToken` fora do arquivo) e usar `--parameter-overrides`
- [x] Como obter, definir e trocar o Bearer token (`API_TOKEN`) após o deploy
- [x] Pipeline de deploy e OIDC: role `github-actions-deploy-prod`, trust policy (`sub` com IDs), secrets exigidos e re-run do deploy
- [x] Como monitorar via CloudWatch (`aws logs tail`, log groups reais)
- [x] Como remover o stack (`sam delete`) e o que permanece na conta
- [x] Troubleshooting de problemas reais (JDK, OIDC, SES, 401, 405, parâmetros do CloudFormation)

---

### ✅ TASK-025: Criar guia de monitoramento (docs/qualidade/MONITORING_GUIDE.md) — **CONCLUÍDA**

**Labels:** `priority: medium`, `épico: documentação`, `pontos: 2`, `type: docs`
**Milestone:** Sprint 6 - Finalização
**Status:** ✅ CONCLUÍDA

## ✅ Critérios de Aceitação
- [x] Como acessar logs de cada Lambda no CloudWatch
- [x] Métricas disponíveis (invocações, erros, duração, cold start)
- [x] Como criar alarmes CloudWatch para erros (alarmes baseados em linhas SEVERE, pois a métrica `Errors` não captura as falhas tratadas)
- [x] Como interpretar logs de notificação e relatório

**Fora do escopo:** "Explicação dos dashboards configurados no template SAM". O template não cria dashboard. Removido em 2026-11-15, conforme a regra de revisar critérios obsoletos antes de desenvolver.

---

### TASK-026: Preparar entrega final

**Labels:** `priority: high`, `épico: documentação`, `pontos: 2`, `type: docs`
**Milestone:** Sprint 6 - Finalização

## ✅ Critérios de Aceitação
- [ ] Repositório público no GitHub
- [ ] README.md completo e atualizado com URL da API em produção
- [ ] Collection Postman incluída no repositório
- [ ] Todos os testes passando com cobertura ≥ 80%
- [ ] Deploy funcionando na AWS
- [ ] CloudWatch mostrando logs das 3 Lambdas
- [ ] Documentação completa (API.md, guia de deploy, monitoramento)
- [ ] Link do repositório pronto para envio

---

---

### 🟡 TASK-027: Criar documentação de segurança (Security Guidelines) — **PARCIAL**

**Labels:** `priority: high`, `épico: documentação`, `pontos: 2`, `type: docs`
**Milestone:** Sprint 6 - Finalização
**Status:** 🟡 PARCIAL (revisada em 2026-11-16)

## 📋 Descrição
Documentar todas as decisões de segurança, configuração de IAM, governança de acesso e como explicar no vídeo de demonstração.

## ✅ Critérios de Aceitação (revisados em 2026-11-16)

Mudanças em relação ao texto original: a autenticação é por token Bearer, no lugar da API Key. O Usage Plan saiu do escopo, porque não existe no template.

Documento `docs/SECURITY.md`:
- [x] Explicação do modelo de cloud escolhido (AWS Serverless)
- [x] Componentes de segurança: token Bearer, IAM Roles e CloudWatch Logs (a API Key foi substituída pelo Bearer)
- [x] Descritivo de cada IAM Role (ReceiveFeedback, SendNotification, GenerateReport)
- [x] Explicação de least privilege (cada Lambda só tem permissões que precisa)
- [x] Autenticação obrigatória em todas as rotas (`/v1/feedback`, `/v1/graphql`), com token Bearer no lugar da API Key
- [ ] ~~Usage Plan com throttling~~: **removido do escopo**. O template não cria Usage Plan. O documento foi corrigido em 2026-11-16 para não descrever esse controle.
- [x] CloudWatch Logs com retenção de 30 dias
- [x] Separação de ambientes (dev/prod) via parâmetros do CloudFormation
- [x] Encriptação em trânsito (HTTPS obrigatório no API Gateway)
- [x] Verificação de identidade no SES em produção (seção "Verificação de e-mail (SES)")
- [x] Como o AdminEmail é configurável (sem hardcoding)

Roteiro para o vídeo (`docs/qualidade/DEMO_GUIDE.md`):
- [x] Como mostrar template.yaml (IAM, permissões): Segmento 1
- [x] Como mostrar CloudWatch (logs das Lambdas): Segmento 5
- [x] Como explicar modelo serverless vs tradicional: apêndice "Benefícios do serverless"
- [ ] Como testar o endpoint GraphQL com token Bearer (o roteiro ainda cita API Key e precisa de ajuste)
- [ ] Benefícios de segurança (escalabilidade, menor superfície de ataque): a verificar no roteiro

Vídeo de demonstração (~5 minutos):
- [ ] Gravar o vídeo e salvar em `docs/videos/` ou colocar o link no README
- [ ] Demonstrar `receiveFeedback` com nota crítica e confirmar o e-mail. **Pendência técnica:** a mutation não é alcançável por HTTP, porque `POST /graphql` só executa `listFeedbacks`. Decidir: implementar a mutation ou mostrar `POST /v1/feedback` no vídeo.
- [ ] Demonstrar `receiveFeedback` com nota normal e confirmar que não há e-mail (mesma pendência acima)
- [ ] Execução manual do generate-report e e-mail de relatório (depende de autorização para executar em produção)
- [ ] Mostrar CloudWatch Logs das 3 Lambdas em execução
- [ ] Mostrar configurações da AWS (Lambda, DynamoDB, SNS, API Gateway)
- [ ] Explicar modelo de cloud e configurações de segurança (IAM)
- [ ] Duração de ~5 minutos

## 🔧 Dependências Técnicas
- [TASK-004] concluída (SAM template com IAM)

---

### TASK-028: Gravar vídeo de demonstração

**Labels:** `priority: high`, `épico: documentação`, `pontos: 2`, `type: docs`
**Milestone:** Sprint 6 - Finalização

## ✅ Critérios de Aceitação
- [ ] Demonstração da Mutation GraphQL `criarAvaliacao` com nota crítica → e-mail de notificação recebido
- [ ] Demonstração da Mutation GraphQL `criarAvaliacao` com nota normal → sem notificação
- [ ] Execução manual do generate-report → e-mail de relatório recebido
- [ ] Mostrar CloudWatch Logs das 3 Lambdas em execução
- [ ] Mostrar configurações AWS (Lambda, DynamoDB, SNS, API Gateway)
- [ ] Explicar modelo de cloud escolhido e configurações de segurança (IAM)
- [ ] Demonstrar GraphQL endpoint com API Key
- [ ] Duração ~5 minutos

---

### TASK-029: Criar menu interativo (CLI) para facilitar desenvolvimento local

**Labels:** `priority: medium`, `épico: tooling`, `pontos: 3`, `type: infra`
**Milestone:** Sprint 6 - Finalização
**Status:** ✅ CONCLUÍDA (menu refeito para Quarkus Dev Mode + AWS real; todos os itens abaixo testados)

## 📝 Descrição
Menu interativo e comandos diretos no `run.sh` para as operações comuns do projeto atual: validar dependências, compilar, testar, subir o HTTP local (`local-http`), validar o SAM e consultar a AWS em modo somente leitura.

## ✅ Critérios de Aceitação
- [x] Menu interativo (sem argumentos) com cores e bordas ASCII
- [x] Modo direto (com argumentos): `./run.sh compile`, `tests`, `local-up`, etc.; `./run.sh help` lista os comandos
- [x] Validar dependências (JDK 17, Maven, AWS CLI, SAM CLI, curl, profile AWS `fase4`), com mensagens claras; localiza e força JDK 17 sozinho
- [x] Compilar todos os módulos (`mvn clean package -DskipTests`)
- [x] Rodar testes de todos os módulos (`mvn clean verify`) ou de um módulo (`-pl`)
- [x] Instalar o `receive-feedback` e subir o `local-http` (portas 8080 e 18081; se ocupadas, mostra o processo sem encerrá-lo)
- [x] Checar se o módulo local está no ar (`GET /avaliacao` = HTTP 200)
- [x] Validar o template SAM (`sam validate --lint`)
- [x] Verificar produção, somente leitura (`GET` no `ApiEndpoint` da stack `feedback-api-prod` = HTTP 200)
- [x] Listar recursos AWS, somente leitura (stacks, Lambdas, DynamoDB, SNS); sai com código diferente de zero se uma consulta falhar
- [x] Saída colorida com status (✅ sucesso, ❌ erro, ⚠ aviso)
- [x] Documentação no README (seção "Menu de desenvolvimento (run.sh)")

**Removido do escopo:** LocalStack, Docker, Colima, docker-compose, volumes, SQS e Newman. O projeto migrou para Quarkus Dev Mode + AWS real em 10/11/2026 (commit `9a1af29`), então essas opções do menu original deixaram de existir. Os scripts `e2e-test.sh` e `e2e-verify.sh` continuam no repositório, mas o menu não os usa.

## 🔧 Dependências Técnicas
- [TASK-003] concluída (docker-compose.yml existente)
- [TASK-018] concluída (testes criados)

---

## �📊 RESUMO DO BACKLOG

**Total de Tasks:** 30
**Estimativa Total:** ~91 pontos

### Por Prioridade:
- **Alta:** 26 tasks
- **Média:** 4 tasks

### Por Épico:
1. Configuração Inicial e Infra: 5 tasks (20 pontos)
2. Lambda receive-feedback: 6 tasks (21 pontos)
3. Lambda send-notification: 3 tasks (10 pontos)
4. Lambda generate-report: 3 tasks (11 pontos)
5. Integração e Testes E2E: 2 tasks (8 pontos)
6. Documentação e Entregáveis: 9 tasks (21 pontos)
7. Tooling: 1 task (3 pontos)
8. Qualidade: 1 task (2 pontos) — TASK-030 Code Audit 100% English

---

## 🎯 ORDEM SUGERIDA DE EXECUÇÃO

**Sprint 1 - Fundação (Tasks 001-005):**
- Estrutura multi-module Maven + Quarkus
- Clean Architecture nos módulos
- LocalStack + docker-compose
- SAM template
- CI/CD

**Sprint 2 - Lambda receive-feedback (Tasks 006-011):**
- Domínio: Feedback, Urgencia, gateways
- Use case ReceiveFeedbackUseCase
- Persistência DynamoDB
- Publisher SNS
- Handler Lambda
- Testes unitários

**Sprint 3 - Lambda send-notification (Tasks 012-014):**
- Domínio e use case de notificação
- SES sender + handler Lambda
- Testes unitários

**Sprint 4 - Lambda generate-report (Tasks 015-017):**
- Domínio e use case de relatório
- DynamoDB gateway + SES sender + handler Lambda
- Testes unitários

**Sprint 5 - Integração (Tasks 018-019):**
- Testes E2E com LocalStack
- Deploy e testes na AWS real

**Sprint 6 - Finalização (Tasks 020-030):**
- JaCoCo + SonarCloud
- README completo
- Documentação da API
- Collection Postman
- Guias de deploy e monitoramento
- Entrega final
- Vídeo de demonstração
- **Code Audit — Verificar 100% Inglês** ✨

---

### ✅ TASK-030: Code Audit — Verificar 100% Inglês — **CONCLUÍDA**

**Labels:** `priority: high`, `épico: qualidade`, `pontos: 2`, `type: quality`
**Milestone:** Sprint 6 - Finalização (executar no final)
**Status:** ✅ CONCLUÍDA em 2026-11-16

## 📋 Descrição
Audit final do projeto para garantir que **100% do código, comentários, nomes de variáveis, nomes de arquivos está em inglês**. Nenhum resquício de português no código-fonte. Task a ser executada no final do desenvolvimento (antes da entrega).

## ✅ Critérios de Aceitação (revisados em 2026-11-16)

Decisões que definiram o escopo final: código, comentários, identificadores e nomes de arquivo ficam em inglês. Mensagens para pessoas (respostas da API, logs, menus, e-mails, alarmes e workflows) e documentação em `.md` ficam em português. A API legada em português foi removida, e a v1 em inglês é a única versão.

- [x] **Código Java**: identificadores internos em inglês (PRs #104 e #107). Contrato legado removido (PRs #108, #109 e #111).
- [x] **Comentários**: Javadoc e comentários em inglês (PR #103).
- [x] **Package names**: sem português.
- [x] **Nomes de arquivos**: sem português. Cópias de trabalho `template.yaml.bak` e `.orig` removidas.
- [x] **XML/YAML**: comentários em inglês (PR #103).
- [x] **Properties files**: chaves em inglês; textos para pessoas em português.
- [x] **Mensagens da API**: em português (PR #112).
- [x] **Logs**: em português (PR #113).
- [x] **Exceções internas**: em português (PR #115).
- [x] **E-mails e assunto SNS**: em português (PR #116). O assunto SNS não tem acento, porque o SNS aceita só ASCII.
- [x] **Test files**: nomes e comentários em inglês, sem textos em português nas asserções de comportamento.
- [x] **Documentação**: `.md` em português, por decisão do usuário. Inclui `SECURITY.md` e `API.md`, que substituem o critério original de inglês (PR #117).
- [x] **Script shell**: comentários do `run.sh` em inglês. As mensagens do menu continuam em português.
- [x] **Relatório**: `docs/qualidade/CODE_AUDIT.md` gerado em PASS, com as exceções documentadas (PR #117).
- [x] **Busca por palavras-chave**: os termos restantes estão só em mensagens para pessoas, em documentação e em exceções documentadas.

**Exceções documentadas no CODE_AUDIT:**
- Asserções negativas do teste `ReceiveFeedbackHandlerV1Test`, que confirmam que a v1 não devolve as chaves legadas.
- Documentos históricos (relatórios, TASK-* e resultados de E2E) que citam rotas e campos removidos.


---

## 📝 NOTAS

- As estimativas são em pontos de história (Story Points)
- 1 ponto ≈ 1-2 horas de trabalho
- Para cada task: criar branch `feature/task-XXX-descricao`, implementar, PR para develop, merge
- Seguir conventional commits: `feat(task-XXX):`, `fix(task-XXX):`, `docs(task-XXX):`, `ci(task-XXX):`
- Não fazer deploy na AWS durante sprints 1-4 — usar LocalStack para economizar créditos
- Deploy real apenas na Sprint 5 (TASK-019)
