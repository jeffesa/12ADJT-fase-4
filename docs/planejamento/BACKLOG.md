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

### TASK-003: Configurar docker-compose com LocalStack

**Labels:** `priority: high`, `épico: setup`, `pontos: 4`, `type: infra`
**Milestone:** Sprint 1 - Fundação

## 📋 Descrição
Criar docker-compose.yml na raiz com LocalStack para simular os serviços AWS localmente: DynamoDB, SNS, SES, Lambda, API Gateway, EventBridge. Criar script `run.sh` para inicialização local.

## ✅ Critérios de Aceitação
- [ ] `docker-compose.yml` com serviço LocalStack 3.x
- [ ] Serviços habilitados: dynamodb, sns, ses, lambda, apigateway, events
- [ ] Porta padrão: 4566
- [ ] Variáveis de ambiente para dev: `AWS_ACCESS_KEY_ID=test`, `AWS_SECRET_ACCESS_KEY=test`, `AWS_DEFAULT_REGION=us-east-1`
- [ ] `run.sh` com comandos para subir LocalStack e criar recursos (tabela DynamoDB, tópico SNS)
- [ ] `.dockerignore` configurado
- [ ] `application.properties` de cada módulo com URLs do LocalStack para profile dev

## 🔧 Dependências Técnicas
- [TASK-001] concluída

---

### TASK-004: Configurar AWS SAM (template.yaml e samconfig.toml)

**Labels:** `priority: high`, `épico: setup`, `pontos: 5`, `type: infra`
**Milestone:** Sprint 1 - Fundação

## 📋 Descrição
Criar o template SAM completo (`infra/template.yaml`) definindo todos os recursos AWS: 3 Lambdas, API Gateway com API Key, DynamoDB, SNS topic, EventBridge rule e permissões IAM. Criar `infra/samconfig.toml` com parâmetros por ambiente.

## ✅ Critérios de Aceitação
- [ ] `infra/template.yaml` com todos os recursos:
  - [ ] Lambda `ReceiveFeedbackFunction` — trigger: API Gateway POST /avaliacao
  - [ ] Lambda `SendNotificationFunction` — trigger: SNS topic (urgência HIGH)
  - [ ] Lambda `GenerateReportFunction` — trigger: EventBridge cron (toda segunda 8h)
  - [ ] DynamoDB table `feedbacks` com PK `id` (String)
  - [ ] SNS topic `feedback-urgent-topic`
  - [ ] API Gateway com API Key obrigatória
  - [ ] IAM Roles mínimas para cada Lambda (least privilege)
  - [ ] CloudWatch Log Groups para cada Lambda (retenção 30 dias)
- [ ] `infra/samconfig.toml` com:
  - [ ] Ambiente `default` (dev)
  - [ ] Parâmetro `AdminEmail` (e-mail configurável)
  - [ ] Parâmetro `Environment` (dev/prod)
  - [ ] Region, stack name, capabilities
- [ ] `sam validate` passa sem erros

## 🔧 Dependências Técnicas
- [TASK-001] concluída

---

### TASK-005: Configurar CI/CD e SonarCloud (GitHub Actions)

**Labels:** `priority: high`, `épico: setup`, `pontos: 4`, `type: infra`
**Milestone:** Sprint 1 - Fundação

## 📋 Descrição
Configurar GitHub Actions com pipeline de build, testes e análise SonarCloud desde o início do projeto. O SonarCloud entra na Sprint 1 porque análise de qualidade de código deve acontecer desde o primeiro PR — não só no final. O SAM deploy é adicionado ao pipeline na TASK-019 (quando o template SAM estiver pronto).

## ✅ Critérios de Aceitação
- [ ] `.github/workflows/ci-cd.yml`:
  - [ ] Trigger: push/PR para `develop` e `main`
  - [ ] Build multi-module Maven (`mvn -B clean verify`)
  - [ ] Cache Maven configurado
  - [ ] Java 17 (temurin)
  - [ ] SonarCloud analysis (`org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`)
  - [ ] Secrets necessários: `GITHUB_TOKEN`, `SONAR_TOKEN`
  - [ ] Build falha se testes falharem
- [ ] `.github/workflows/branch-naming.yml`:
  - [ ] Prefixos permitidos: feature/, bugfix/, hotfix/, release/, chore/, docs/, test/, refactor/, perf/, ci/, style/
- [ ] `.github/workflows/auto-pr-to-main.yml`:
  - [ ] Auto PR develop → main
- [ ] `sonar-project.properties` na raiz configurado para multi-module
- [ ] SonarCloud projeto criado em sonarcloud.io (organização: jeffesa)
- [ ] `SONAR_TOKEN` adicionado nos secrets do repositório GitHub
- [ ] Pipeline verde no primeiro push

## 🔧 Dependências Técnicas
- [TASK-001] concluída (projeto compila)

---

## 📋 ÉPICO 2: Lambda 1 — receive-feedback

### TASK-006: Criar entidade de domínio Feedback e gateway

**Labels:** `priority: high`, `épico: receive-feedback`, `pontos: 3`, `type: feature`
**Milestone:** Sprint 2 - Lambda receive-feedback

## 📋 Descrição
Criar entidade `Feedback` no domain do módulo receive-feedback (POJO puro) e interface `FeedbackGateway`. Incluir enum `Urgencia` com lógica de cálculo pela nota.

## ✅ Critérios de Aceitação
- [ ] Classe `Feedback` com campos: id (String/UUID), descricao, nota (int 0-10), urgencia, dataCriacao, notificacaoEnviada (boolean)
- [ ] Enum `Urgencia`: HIGH (nota 0-5), MEDIUM (nota 6-7), LOW (nota 8-10)
- [ ] Método estático `Urgencia.fromNota(int nota)` que calcula a urgência
- [ ] POJO puro sem anotações de framework
- [ ] Interface `FeedbackGateway`: `save(Feedback)`, `findAll()`, `findByPeriod(LocalDate inicio, LocalDate fim)`
- [ ] Interface `UrgencyPublisher`: `publish(Feedback)` — para publicar no SNS
- [ ] Validações de domínio: nota entre 0 e 10, descrição não vazia

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

### TASK-019: Deploy e testes na AWS real

**Labels:** `priority: high`, `épico: integração`, `pontos: 5`, `type: infra`
**Milestone:** Sprint 5 - Integração

## ✅ Critérios de Aceitação
- [ ] AWS CLI configurado localmente
- [ ] SES com e-mail do admin verificado
- [ ] `sam build` bem-sucedido para todos os módulos
- [ ] `sam deploy` bem-sucedido (stack criada na AWS)
- [ ] API Gateway com URL pública e API Key ativa
- [ ] POST /avaliacao funcional na URL pública
- [ ] E-mail de notificação recebido para nota crítica
- [ ] CloudWatch Logs com logs das 3 Lambdas
- [ ] URL pública documentada no README

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

### TASK-024: Criar guia de deploy AWS (docs/deploy/AWS_DEPLOY_GUIDE.md)

**Labels:** `priority: high`, `épico: documentação`, `pontos: 3`, `type: docs`
**Milestone:** Sprint 6 - Finalização

## ✅ Critérios de Aceitação
- [ ] Pré-requisitos: conta AWS, AWS CLI, SAM CLI, Java 17, Maven
- [ ] Passo a passo: configurar AWS CLI, verificar e-mail no SES, build, deploy
- [ ] Como configurar `infra/samconfig.toml` com e-mail e ambiente
- [ ] Como obter a API Key após deploy
- [ ] Como monitorar via CloudWatch
- [ ] Como remover o stack (`sam delete`)
- [ ] Troubleshooting de problemas comuns

---

### TASK-025: Criar guia de monitoramento (docs/qualidade/MONITORING_GUIDE.md)

**Labels:** `priority: medium`, `épico: documentação`, `pontos: 2`, `type: docs`
**Milestone:** Sprint 6 - Finalização

## ✅ Critérios de Aceitação
- [ ] Como acessar logs de cada Lambda no CloudWatch
- [ ] Métricas disponíveis (invocações, erros, duração, cold start)
- [ ] Como criar alarmes CloudWatch para erros
- [ ] Explicação dos dashboards configurados no template SAM
- [ ] Como interpretar logs de notificação e relatório

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

### TASK-027: Criar documentação de segurança (Security Guidelines)

**Labels:** `priority: high`, `épico: documentação`, `pontos: 2`, `type: docs`
**Milestone:** Sprint 6 - Finalização

## 📋 Descrição
Documentar todas as decisões de segurança, configuração de IAM, governança de acesso e como explicar no vídeo de demonstração.

## ✅ Critérios de Aceitação
- [ ] Documento `docs/SECURITY.md` com:
  - [ ] Explicação do modelo de cloud escolhido (AWS Serverless)
  - [ ] Componentes de segurança: API Key, IAM Roles, CloudWatch Logs
  - [ ] Descritivo de cada IAM Role (ReceiveFeedback, SendNotification, GenerateReport)
  - [ ] Explicação de least privilege (cada Lambda só tem permissões que precisa)
  - [ ] API Gateway com API Key obrigatória no `/graphql` (endpoint GraphQL)
  - [ ] Usage Plan com throttling (rate limit, burst limit, quota diária)
  - [ ] CloudWatch Logs com retenção de 30 dias
  - [ ] Separação de ambientes (dev/prod) via CloudFormation parameters
  - [ ] Encriptação em trânsito (HTTPS obrigatório)
  - [ ] SES em Sandbox (dev) vs Production (prod)
  - [ ] Como o AdminEmail é configurável (sem hardcoding)
- [ ] Roteiro para o vídeo explicando:
  - [ ] Como mostrar template.yaml (IAM, API Key, permissions)
  - [ ] Como mostrar CloudWatch (logs das 3 Lambdas)
  - [ ] Como explicar modelo serverless vs tradicional
  - [ ] Como testar GraphQL endpoint com API Key
  - [ ] Quais são os benefícios de segurança (escalabilidade automática, less surface area)

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

## � Descrição
Melhorar o `run.sh` com um menu interativo (inspirado na fase-3) que permita ao desenvolvedor executar operações comuns sem memorizar comandos: subir/parar LocalStack, compilar, rodar testes, verificar saúde dos serviços, ver logs, etc.

## ✅ Critérios de Aceitação
- [ ] Menu interativo com interface visual (cores, bordas ASCII)
- [ ] Opção 1: Subir LocalStack + compilar (`docker-compose up -d` + compile modules)
- [ ] Opção 2: Parar LocalStack (`docker-compose down`)
- [ ] Opção 3: Rodar testes de todos os módulos (`mvn clean verify`)
- [ ] Opção 4: Limpar volumes/dados (`docker-compose down -v`)
- [ ] Opção 5: Ver logs do LocalStack (`docker-compose logs -f`)
- [ ] Opção 6: Health check das 3 Lambdas (curl para endpoints health)
- [ ] Opção 7: Listar recursos criados no LocalStack (DynamoDB tables, SNS topics, SQS queues)
- [ ] Opção 8: Rodar collection Postman (Newman) — opcional, se houver
- [ ] Modo interativo (sem argumentos): exibir menu
- [ ] Modo direto (com argumentos): executar comando diretamente (`./run.sh docker`, `./run.sh stop`, etc.)
- [ ] Validação de dependências (Docker, Maven, Java) com mensagens claras
- [ ] Suporte a Colima (macOS) e Docker Desktop
- [ ] Saída colorida com status (✅ sucesso, ❌ erro, ⚠️ aviso)
- [ ] Documentação no README sobre como usar o menu

## 🔧 Dependências Técnicas
- [TASK-003] concluída (docker-compose.yml existente)
- [TASK-018] concluída (testes criados)

---

## �📊 RESUMO DO BACKLOG

**Total de Tasks:** 29
**Estimativa Total:** ~89 pontos

### Por Prioridade:
- **Alta:** 25 tasks
- **Média:** 4 tasks

### Por Épico:
1. Configuração Inicial e Infra: 5 tasks (20 pontos)
2. Lambda receive-feedback: 6 tasks (21 pontos)
3. Lambda send-notification: 3 tasks (10 pontos)
4. Lambda generate-report: 3 tasks (11 pontos)
5. Integração e Testes E2E: 2 tasks (8 pontos)
6. Documentação e Entregáveis: 9 tasks (21 pontos) — **+1 TASK-027 segurança**
7. Tooling: 1 task (3 pontos)

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

**Sprint 6 - Finalização (Tasks 020-027):**
- JaCoCo + SonarCloud
- README completo
- Documentação da API
- Collection Postman
- Guias de deploy e monitoramento
- Entrega final
- Vídeo de demonstração

---

## 📝 NOTAS

- As estimativas são em pontos de história (Story Points)
- 1 ponto ≈ 1-2 horas de trabalho
- Para cada task: criar branch `feature/task-XXX-descricao`, implementar, PR para develop, merge
- Seguir conventional commits: `feat(task-XXX):`, `fix(task-XXX):`, `docs(task-XXX):`, `ci(task-XXX):`
- Não fazer deploy na AWS durante sprints 1-4 — usar LocalStack para economizar créditos
- Deploy real apenas na Sprint 5 (TASK-019)
