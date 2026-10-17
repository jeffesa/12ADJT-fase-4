# 📝 Resumo do Projeto - Tech Challenge Fase 4

## 🎯 Contexto

Este projeto é a continuação do Tech Challenge Fase 3 da Pós-Graduação em Arquitetura e Desenvolvimento Java (FIAP). A Fase 3 entregou um sistema hospitalar de agendamento de consultas com microsserviços, RabbitMQ, JWT e GraphQL. A Fase 4 muda o paradigma: **Cloud Computing + Serverless**.

**Repositório Fase 1:** https://github.com/jeffesa/12ADJT-fase-1
**Repositório Fase 2:** https://github.com/jeffesa/12ADJT-fase-2
**Repositório Fase 3:** https://github.com/jeffesa/12ADJT-fase-3
**Repositório Fase 4:** https://github.com/jeffesa/12ADJT-fase-4

---

## 🏗️ Decisões Técnicas

### Por que Quarkus em vez de Spring Boot?

O PDF da Fase 4 não exige framework específico. Quarkus foi escolhido porque:
- **Cold start menor em Lambda:** Quarkus JVM mode inicia em ~500ms vs 3-8s do Spring Boot
- **Consumo de memória reduzido:** menor custo em Lambda (cobrança por GB-segundo)
- **Extensões nativas AWS:** `quarkus-amazon-lambda`, `quarkus-amazon-dynamodb`, `quarkus-amazon-sns`, `quarkus-amazon-ses` via Quarkiverse
- Usamos **JVM mode** (não native/GraalVM) para evitar complexidade de build nativo

### Por que AWS?

Free tier adequado para o volume acadêmico:
- Lambda: 1M requests/mês grátis permanente
- DynamoDB: 25GB + 25 WCU/RCU grátis permanente
- SNS: 1M notificações/mês grátis
- API Gateway: 1M chamadas/mês grátis (12 primeiros meses)
- SES: 62.000 emails/mês grátis quando enviado de Lambda
- **Custo estimado: ~$0/mês**

### Por que AWS SAM?

PDF não exige ferramenta específica. SAM foi escolhido por ser a ferramenta oficial da AWS para serverless, mais simples que Terraform para Lambda puro. `infra/samconfig.toml` parametriza deploy por ambiente.

### Por que DynamoDB?

Serverless nativo, sem instância rodando 24h, free tier permanente, sem overhead de conexão JDBC em Lambda.

### Por que API Key?

PDF não exige autenticação robusta. API Key no API Gateway é simples, suficiente e sem custo adicional.

### Por que manter Clean Architecture?

Mantemos o padrão das Fases 2 e 3 para consistência, testabilidade e separação de responsabilidades em cada Lambda.

### Por que LocalStack?

Simula serviços AWS localmente para desenvolvimento e testes sem gastar créditos. Deploy real apenas na Sprint 5.

---

## 🔄 Diferenças em relação à Fase 3

| Aspecto | Fase 3 | Fase 4 |
|---------|--------|--------|
| Domínio | Hospital (consultas) | Plataforma de Feedback |
| Arquitetura | Microsserviços Docker | Serverless (AWS Lambda) |
| Framework | Spring Boot 3.2.x | Quarkus 3.8.x (LTS) |
| Persistência | PostgreSQL por serviço | DynamoDB (NoSQL serverless) |
| Comunicação async | RabbitMQ + DLQ | SNS + EventBridge (cron) |
| Deploy | Docker Compose local | AWS SAM (cloud real) |
| Monitoramento | Logs Docker | CloudWatch Logs + Métricas |
| Segurança | JWT + Roles | API Key + IAM Roles |
| Teste local | docker-compose real | LocalStack (simulação AWS) |
| Módulos | 3 microsserviços | 3 Lambdas (multi-module Maven) |
| CI/CD | GitHub Actions + Maven | GitHub Actions + Maven + SAM deploy |

---

## 🏛️ Arquitetura da Solução

```
Internet
    │ POST /avaliacao (API Key)
    ▼
API Gateway
    │ invoca
    ▼
Lambda 1: receive-feedback
  • Valida input (descricao + nota 0-10)
  • Calcula urgência: HIGH (0-5) / MEDIUM (6-7) / LOW (8-10)
  • Persiste no DynamoDB
  • Se HIGH → publica no SNS
    │
    ▼ (SNS trigger)
Lambda 2: send-notification
  • Envia e-mail ao admin via SES
  • Dados: descrição, urgência, data de envio

EventBridge cron (toda segunda 8h)
    │
    ▼
Lambda 3: generate-report
  • Consulta DynamoDB (últimos 7 dias)
  • Gera relatório: médias, qtd por dia, qtd por urgência
  • Envia e-mail ao admin via SES

DynamoDB: tabela feedbacks
CloudWatch: logs e métricas de todas as Lambdas
```

---

## 🛠️ Stack Tecnológica

| Tecnologia | Uso |
|-----------|-----|
| Java 17 | Linguagem principal |
| Quarkus 3.8.x LTS | Framework (JVM mode) |
| quarkus-amazon-lambda | Runtime Lambda |
| quarkus-amazon-dynamodb | Cliente DynamoDB |
| quarkus-amazon-sns | Publicação SNS |
| quarkus-amazon-ses | Envio de e-mail |
| AWS Lambda | Funções serverless |
| AWS API Gateway | Entrada HTTP + API Key |
| AWS DynamoDB | Persistência NoSQL |
| AWS SNS | Notificações assíncronas |
| AWS SES | Envio de e-mail |
| AWS EventBridge | Cron relatório semanal |
| AWS CloudWatch | Monitoramento e logs |
| AWS SAM | Deploy automatizado (IaC) |
| LocalStack | Simulação AWS local |
| Docker + Compose | Dev local com LocalStack |
| Maven multi-module | Build |
| JUnit 5 + Mockito | Testes unitários |
| JaCoCo | Cobertura ≥ 80% |
| SonarCloud | Qualidade de código |
| GitHub Actions | CI/CD |

---

## 📋 Entregáveis Obrigatórios (PDF Fase 4)

1. Repositório aberto com código-fonte
2. Vídeo de demonstração (funções serverless ativas + configurações)
3. Ambiente de nuvem configurado (segurança + governança IAM)
4. Componentes de suporte configurados (DynamoDB)
5. Deploy automatizado (GitHub Actions + SAM)
6. Aplicação monitorada (CloudWatch)
7. Notificações automáticas para problemas críticos (SNS + SES)
8. Relatório semanal dos feedbacks (EventBridge + Lambda + SES)
9. Mínimo 2 funções serverless com Responsabilidade Única (3 Lambdas)
10. Documentação: arquitetura + deploy + monitoramento + funções

---

## 📅 Planejamento

Detalhes completos em: [BACKLOG.md](BACKLOG.md)

---

*Última atualização: Outubro/2026*
