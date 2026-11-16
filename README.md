# Plataforma Serverless de Feedback

Plataforma de feedback de alunos construída na AWS com funções serverless. Recebe as avaliações por API, classifica cada uma por urgência, envia um e-mail de aviso quando a urgência é alta e gera um relatório semanal.

- **API em produção:** https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod (exige `Authorization: Bearer <token>`)
- **Região:** `sa-east-1` · **Stack:** `feedback-api-prod` · **Ambiente de desenvolvimento:** `feedback-api-dev`

## Diagrama de arquitetura

```
                      ALUNO / CLIENTE
   POST /v1/avaliacao   POST /v1/graphql
   Authorization: Bearer <token>
                              │
                              ▼
                  API GATEWAY (prod-feedback-api, HTTPS)
                    │                              │
                    ▼                              ▼
   LAMBDA receive-feedback                 LAMBDA graphql
    1. valida: descrição ≥ 5 caracteres,      lê os feedbacks
       nota de 0 a 10                         (listFeedbacks)
    2. urgência pela nota:
       HIGH 0–5 · MEDIUM 6–7 · LOW 8–10
    3. grava no DynamoDB ─────────┐
    4. se HIGH, publica no SNS    │
                    │             │                 │
                    │             ▼                 ▼
                    │      DYNAMODB prod-feedback-table
                    │      id · description · rating · urgency · createdAt
                    ▼
        SNS prod-feedback-urgent-topic   (somente urgência HIGH)
                    │
                    ▼
        LAMBDA send-notification
          envia e-mail pelo SES ao AdminEmail
                    │
                    ▼
                  SES ──────────────────────────────► ADMINISTRADOR


   EVENTBRIDGE cron(0 8 ? * MON *)   (segunda-feira, 08h UTC)
                    │
                    ▼
        LAMBDA generate-report
          lê o período (últimos 7 dias) no DynamoDB
          monta totais, média, contagem por dia,
          contagem por urgência e a lista de feedbacks
                    │
                    ▼
                  SES ──────────────────────────────► ADMINISTRADOR


   ALARMES CloudWatch (linhas SEVERE) ──► SNS prod-monitoring-alarms-topic ──► ADMINISTRADOR
```

## Como funciona

- **Urgência:** calculada pela nota. HIGH de 0 a 5, MEDIUM de 6 a 7, LOW de 8 a 10. As faixas ficam em `receive-feedback/src/main/resources/application.properties`.
- **E-mail de aviso:** enviado somente para urgência **HIGH**. Avaliações MEDIUM e LOW não geram e-mail.
- **Relatório semanal:** toda segunda-feira, às 08h UTC. Cobre os últimos 7 dias, com total, média, contagem por dia e por urgência, e a lista de feedbacks.
- **Validação:** descrição com no mínimo 5 caracteres e nota de 0 a 10.

## Endpoints

| Método | Caminho | Função | Descrição |
|---|---|---|---|
| POST | `/v1/avaliacao` | receive-feedback | Cria uma avaliação (`descricao`, `nota`) |
| POST | `/v1/graphql` | graphql | Consulta `listFeedbacks` |

Sem o token, a API responde `401`. A documentação completa está em [docs/API.md](docs/API.md).

## Execução local

Pré-requisitos: JDK 17, Maven 3.8+, AWS CLI com o perfil `fase4`, e SAM CLI.

```bash
./run.sh          # menu interativo
./run.sh local-up # sobe a API local em http://localhost:8080
```

O ambiente local grava na tabela `dev-feedback-table`, nunca na de produção. O token local padrão é `minha-chave-local`, e só vale localmente. Para testar com o Bruno, importe `docs/api-collection/fase4-feedback.postman_collection.json`. Detalhes em [docs/LOCAL_DEVELOPMENT.md](docs/LOCAL_DEVELOPMENT.md) e [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md).

### Menu do `run.sh`

```
╔════════════════════════════════════════════════════╗
║   Tech Challenge Fase 4 — Feedback Serverless      ║
║   Quarkus Dev Mode + AWS (sa-east-1)               ║
╚════════════════════════════════════════════════════╝

  🔨 Build & Testes
   1) Validar dependências
   2) Compilar todos os módulos
   3) Rodar testes de todos os módulos
   4) Rodar testes de um módulo

  🚀 HTTP local (porta 8080, grava em dev-feedback-table)
   5) Instalar receive-feedback e subir local-http
   6) Checar se o módulo local está no ar

  ☁️  AWS (somente leitura)
   7) Validar template SAM
   8) Verificar produção (GET)
   9) Listar recursos AWS

   0) Sair
```

| Opção | O que faz |
|---|---|
| 1 | Verifica JDK 17, Maven, AWS CLI, SAM CLI, curl e o perfil `fase4` |
| 2 | Compila os quatro módulos (`mvn clean package -DskipTests`) |
| 3 | Roda os testes de todos os módulos (`mvn clean verify`) |
| 4 | Roda os testes de um módulo, informado na hora |
| 5 | Instala o `receive-feedback` e sobe a API local. Ctrl+C para parar |
| 6 | Confere se a API local responde `POST /v1/graphql` (`listFeedbacks`) com 200 |
| 7 | Valida o template SAM (`sam validate --lint`) |
| 8 | Confere produção: sem token deve dar 401; com `PROD_API_TOKEN`, deve dar 200 |
| 9 | Lista stacks, Lambdas e tabelas, sem alterar nada |
| 0 | Sai |

Os mesmos comandos podem ser usados direto: `./run.sh compile`, `./run.sh tests [módulo]`, `./run.sh local-up`, `./run.sh local-check`, `./run.sh sam-validate`, `./run.sh prod-check`, `./run.sh aws-list`.

## Deploy

Ao mesclar na `main`, o pipeline do GitHub Actions (`.github/workflows/ci-cd.yml`) roda os testes e o SonarCloud e, em seguida, faz `sam build` e `sam deploy` na stack `feedback-api-prod`. A autenticação na AWS é por OIDC. Os secrets exigidos estão listados no guia. Passo a passo em [docs/deploy/AWS_DEPLOY_GUIDE.md](docs/deploy/AWS_DEPLOY_GUIDE.md).

## Testes e qualidade

| Módulo | Testes | Cobertura de linhas |
|---|---|---|
| receive-feedback | 147 | 92,4% |
| send-notification | 86 | 96,6% |
| generate-report | 167 | 99,5% |
| local-http (só desenvolvimento) | 6 | — |

Comando: `mvn clean verify` (JDK 17). O SonarCloud roda no pipeline, e o quality gate está em OK. Painel: https://sonarcloud.io/dashboard?id=jeffesa_12ADJT-fase-4

## Segurança e monitoramento

- Token Bearer em todas as rotas. O token fica no GitHub Secret `API_TOKEN`. Detalhes em [docs/SECURITY.md](docs/SECURITY.md).
- Logs das funções no CloudWatch, com retenção de 30 dias.
- Três alarmes de erros `SEVERE`, com aviso por e-mail. Detalhes em [docs/qualidade/MONITORING_GUIDE.md](docs/qualidade/MONITORING_GUIDE.md).

## Estrutura do projeto

```
fase-4/
├── receive-feedback/     Lambda: recebe, valida, classifica e grava os feedbacks
├── send-notification/    Lambda: envia o e-mail de aviso de urgência HIGH
├── generate-report/      Lambda: gera e envia o relatório semanal
├── local-http/           API local para testes (só desenvolvimento)
├── infra/                template.yaml (SAM) e samconfig.toml
├── docs/                 API, segurança, deploy, monitoramento, demo e collection
├── .github/workflows/    CI/CD, validação de branches e PR automático para main
├── run.sh                menu e comandos do projeto
└── pom.xml               POM pai (multi-módulo)
```

Cada módulo Lambda segue Clean Architecture: `domain`, `application` e `infra`.

## Documentação

- [docs/API.md](docs/API.md): endpoints e exemplos
- [docs/SECURITY.md](docs/SECURITY.md): segurança
- [docs/deploy/AWS_DEPLOY_GUIDE.md](docs/deploy/AWS_DEPLOY_GUIDE.md): deploy
- [docs/qualidade/MONITORING_GUIDE.md](docs/qualidade/MONITORING_GUIDE.md): monitoramento
- [docs/qualidade/DEMO_GUIDE.md](docs/qualidade/DEMO_GUIDE.md): roteiro da demonstração
- [docs/api-collection/README.md](docs/api-collection/README.md): collection do Bruno
