<div align="center">

FIAP - Faculdade de Informática e Administração Paulista

Pós-Graduação em Arquitetura e Desenvolvimento Java

<br><br><br><br>

Jefferson Ricardo dos Santos

RM371825

<br><br><br><br>

**PLATAFORMA SERVERLESS DE FEEDBACK: AWS LAMBDA, DYNAMODB, SNS E RELATÓRIO SEMANAL**

Tech Challenge — Fase 4

<br><br><br><br><br><br><br><br>

São Paulo

2026

</div>

---

## DESCRIÇÃO DO PROJETO

Plataforma serverless para coleta de feedbacks de alunos sobre as aulas. O sistema recebe as avaliações por API, classifica cada uma por urgência, envia um e-mail de aviso ao administrador quando a urgência é alta, e gera um relatório semanal com as médias e as contagens do período.

A aplicação roda na AWS, em região `sa-east-1`, sem servidores para gerenciar. Todas as funções são AWS Lambda, o armazenamento é DynamoDB, a mensageria é Amazon SNS, o agendamento é Amazon EventBridge e o envio de e-mails é Amazon SES.

**Repositório:** https://github.com/jeffesa/12ADJT-fase-4

**API em produção:** https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod

**Segurança:** a API é pública, mas exige o cabeçalho `Authorization: Bearer <token>` em todas as rotas. Sem o token, a requisição é recusada com `401 Unauthorized`, e nenhum dado é retornado. O token fica em um GitHub Secret e é passado às funções na implantação, sem aparecer no código nem nos logs.

**Vídeo de demonstração:** [PENDENTE: inserir o link do vídeo]

---

## SERVIÇOS AWS UTILIZADOS

A solução usa somente serviços gerenciados da AWS. A tabela mostra o papel de cada um no projeto, o recurso criado e o comando para conferir na conta, na região `sa-east-1`.

| Serviço AWS | Papel no projeto | Recurso criado | Como conferir |
|---|---|---|---|
| **AWS Lambda** | Executa as funções serverless | `prod-receive-feedback`, `prod-graphql`, `prod-send-notification`, `prod-generate-report` | `aws lambda list-functions --region sa-east-1` |
| **Amazon S3** | Guarda o pacote (zip) de cada função antes do deploy | Bucket `sam-artifacts-sa-east-1-prod` | `aws s3 ls s3://sam-artifacts-sa-east-1-prod` |
| **AWS CloudFormation** | Cria e atualiza todos os recursos a partir do `infra/template.yaml` | Stack `feedback-api-prod` | `aws cloudformation describe-stacks --stack-name feedback-api-prod` |
| **Amazon API Gateway** | Recebe as requisições HTTPS e encaminha às funções | API `prod-feedback-api`, com as rotas `/v1/feedback`, `/v1/graphql` e `/graphql` | Acessar a URL de produção |
| **Amazon DynamoDB** | Armazena os feedbacks | Tabela `prod-feedback-table` | `aws dynamodb describe-table --table-name prod-feedback-table` |
| **Amazon SNS** | Envia o aviso de urgência e os alarmes | Tópicos `prod-feedback-urgent-topic` e `prod-monitoring-alarms-topic` | `aws sns list-topics` |
| **Amazon EventBridge** | Dispara o relatório toda segunda-feira, às 08h UTC | Regra `prod-generate-report-schedule` | `aws events list-rules` |
| **Amazon SES** | Envia os e-mails de urgência, de relatório e de alarme | Identidades verificadas em `sa-east-1` | `aws ses list-identities` |
| **Amazon CloudWatch** | Guarda os logs das funções e dispara os alarmes | 4 grupos de logs (30 dias) e 3 alarmes `prod-severe-errors-*` | `aws cloudwatch describe-alarms` |
| **AWS IAM** | Define as roles das funções e o acesso do GitHub, por OIDC | 3 roles de função, e a role `github-actions-deploy-prod`, criada fora do template | `aws iam list-roles` |

### Onde as funções ficam guardadas

Esta é a resposta objetiva para a pergunta "qual serviço grava as funções":

1. O Maven gera o pacote de cada função.
2. O SAM envia o pacote ao bucket **Amazon S3** `sam-artifacts-sa-east-1-prod`.
3. O **AWS CloudFormation** cria a função a partir desse pacote.
4. A função passa a existir no **AWS Lambda**, pronta para ser executada.

Ou seja: o código fica no **S3**, e a função roda no **Lambda**.

---

## ARQUITETURA

### Visão geral

A solução segue o modelo serverless: cada responsabilidade é uma função Lambda própria, disparada por um evento. Não há servidor ativo esperando requisições.

O enunciado pede a entrada de feedbacks por REST, o envio de avisos de urgência e o relatório semanal. A consulta por GraphQL não é exigência: foi adotada como melhoria de acesso aos dados. Com ela, o cliente envia uma única requisição em JSON e recebe apenas os campos que escolheu, sem receber a lista completa de campos de cada feedback. A consulta usa a mesma tabela e o mesmo token da API REST.

```
                         +-------------------------+
  Cliente (Bruno/curl)   |                         |
  Authorization: Bearer ---->  API Gateway (HTTPS)  |
                         |                         |
                         +-----+-------------+-----+
                               |             |
                 POST/GET      |             |   POST
                 /v1/feedback  |             |   /v1/graphql
                               v             v
                   +-------------------+   +-------------------+
                   | receive-feedback  |   | graphql           |
                   | (Lambda)          |   | (Lambda)          |
                   +---------+---------+   +---------+---------+
                             |                       |
              grava          |                       |  lê (listFeedbacks)
                             v                       v
                   +----------------------------------------+
                   |  DynamoDB: prod-feedback-table         |
                   +----------------------------------------+
                             ^                       ^
                             |  lê (período)         |
                             |                       |
                   +---------+-----------------------+
                   | generate-report (Lambda)        |<---- EventBridge
                   +---------+-----------------------+      cron: seg 08h UTC
                             |
                             |  envia relatório
                             v
                          +-----+        +-------+
                          | SES |------->| Admin |
                          +-----+        +-------+
                             ^
                             |  envia aviso de urgência
                   +---------+-----------------------+
                   | send-notification (Lambda)      |<---- SNS
                   +---------------------------------+      prod-feedback-urgent-topic
                                                             (publicado só se urgência = HIGH)

  Alarmes CloudWatch (SEVERE nas funções) --> SNS prod-monitoring-alarms-topic --> e-mail do administrador
```

### Componentes e responsabilidades

| Componente | Tipo na AWS | Responsabilidade | Gatilho | Permissões principais |
|---|---|---|---|---|
| **receive-feedback** | Lambda (`prod-receive-feedback`) | Valida a entrada, calcula a urgência, grava o feedback e publica o aviso no SNS quando a urgência é alta | API Gateway: `POST /v1/feedback` e `GET /v1/feedback` | DynamoDB: gravar e ler. SNS: publicar no tópico de urgência |
| **graphql** | Lambda (`prod-graphql`) | Responde à consulta GraphQL `listFeedbacks` | API Gateway: `POST /v1/graphql` e `POST /graphql` | DynamoDB: ler |
| **send-notification** | Lambda (`prod-send-notification`) | Envia o e-mail de aviso de urgência | SNS: `prod-feedback-urgent-topic` | SES: enviar e-mail |
| **generate-report** | Lambda (`prod-generate-report`) | Monta o relatório do período e envia por e-mail | EventBridge: cron `seg 08h UTC` | DynamoDB: ler (Scan e Query). SES: enviar e-mail |
| **DynamoDB** | Tabela `prod-feedback-table` | Guarda os feedbacks (chave `id`) | Acessada pelas funções | Sob demanda (PAY_PER_REQUEST) |
| **SNS** | Tópico `prod-feedback-urgent-topic` | Desacopla a coleta do envio do e-mail de urgência | Publicado por receive-feedback | — |
| **SNS** | Tópico `prod-monitoring-alarms-topic` | Recebe os alarmes e envia e-mail ao administrador | Alarmes CloudWatch | — |
| **EventBridge** | Regra `prod-generate-report-schedule` | Dispara o relatório toda segunda-feira às 08h UTC | Agenda (cron) | — |
| **API Gateway** | API `prod-feedback-api` | Ponto de entrada HTTPS, com autenticação Bearer | Requisições HTTP | — |
| **SES** | Identidades verificadas em `sa-east-1` | Envio de e-mails | Chamado por send-notification e generate-report | — |
| **CloudWatch** | Logs (4 grupos) e alarmes (3) | Logs com retenção de 30 dias; alarmes para erros SEVERE | Automático | — |

### Fluxos principais

**1. Recebimento de feedback**
1. O cliente envia `POST /v1/feedback` com `description` e `rating`, e o cabeçalho Bearer.
2. A função valida: descrição com no mínimo 5 caracteres e nota de 0 a 10.
3. A urgência é calculada pela nota: **HIGH** de 0 a 5, **MEDIUM** de 6 a 7 e **LOW** de 8 a 10.
4. O feedback é gravado no DynamoDB.
5. Se a urgência for HIGH, a função publica o evento no SNS.

**2. Aviso de urgência**
1. O SNS aciona a função send-notification.
2. A função envia um e-mail ao administrador com a descrição, a urgência e a data de envio.

**3. Relatório semanal**
1. O EventBridge aciona a função generate-report toda segunda-feira, às 08h UTC.
2. O período é de 7 dias, terminando no dia anterior à execução.
3. A função lê os feedbacks do período e monta o relatório.
4. O relatório é enviado por e-mail ao administrador.

**4. Consulta**
1. O cliente envia a query `listFeedbacks` para `POST /v1/graphql`, com o cabeçalho Bearer.
2. A função lê o DynamoDB e devolve a lista.

### Por que quatro funções

Cada função tem uma única responsabilidade, o que segue o princípio de responsabilidade única:
- **receive-feedback** só recebe e grava. Ele não envia e-mail, então um problema no SES não impede a coleta.
- **send-notification** só envia avisos. Ele roda por evento, então o custo só existe quando há urgência alta.
- **generate-report** só faz a agregação e o envio do relatório, e roda no agendamento.
- **graphql** só consulta. Ele não grava, e por isso usa uma permissão de leitura (ver a seção Segurança).

### Organização do código (Clean Architecture)

Cada módulo Maven segue as camadas `domain` → `application` → `infra`, com dependência apontando para dentro:
- **domain:** entidades (como `Feedback` e `Urgency`) e interfaces de gateway. Sem dependência de framework.
- **application:** casos de uso (como `ReceiveFeedbackUseCase`).
- **infra:** handlers Lambda, GraphQL, acesso ao DynamoDB, SNS e SES, e configuração.

Módulos: `receive-feedback`, `send-notification`, `generate-report`, e `local-http` (usado só para testes locais).

---

## TECNOLOGIAS UTILIZADAS

| Categoria | Tecnologia | Uso |
|---|---|---|
| Linguagem | Java 17 | Linguagem das funções |
| Framework | Quarkus 3.8 (Amazon Lambda) | Funções serverless e injeção de dependências |
| Build | Maven (multi-módulo) | Compilação, testes e pacotes das funções |
| Infraestrutura como código | AWS SAM (CloudFormation) | Template `infra/template.yaml` |
| Armazenamento | Amazon DynamoDB | Feedbacks |
| Mensageria | Amazon SNS | Aviso de urgência e alarmes |
| Agendamento | Amazon EventBridge | Relatório semanal |
| E-mail | Amazon SES | Avisos e relatórios |
| API | API Gateway (REST) e GraphQL (SmallRye) | Entrada HTTP |
| Monitoramento | CloudWatch Logs e Alarms | Logs e alertas |
| Testes | JUnit 5 e Mockito | Unitários e de contrato |
| Cobertura | JaCoCo | Relatório de linhas |
| Qualidade | SonarCloud | Análise estática no pipeline |
| CI/CD | GitHub Actions com OIDC | Build, testes e deploy |
| Teste manual | Bruno (collection em `docs/api-collection/`) | Requisições à API |

---

## AMBIENTES E CONFIGURAÇÃO

O mesmo template serve para dois ambientes, escolhidos pelo parâmetro `Environment`:

| Ambiente | Stack | Recursos | Uso |
|---|---|---|---|
| **prod** | `feedback-api-prod` | `prod-*` | Ambiente de produção, com a API pública |
| **dev** | `feedback-api-dev` | `dev-*` | Testes e validação de integração |

Os nomes dos recursos recebem o prefixo do ambiente, o que isola os dados e as funções de cada um.

### Parâmetros do template

| Parâmetro | Obrigatório | Função |
|---|---|---|
| `Environment` | Sim (`dev` ou `prod`) | Define o prefixo dos recursos |
| `AdminEmail` | Sim | Destinatário dos avisos e dos relatórios. Em prod, precisa estar verificado no SES |
| `SenderEmail` | Sim | Remetente dos e-mails. Precisa ser uma identidade verificada no SES |
| `ApiToken` | Sim | Token Bearer exigido nas rotas. Não aparece em logs (`NoEcho`) |
| `Region` | Não | Região dos recursos (`sa-east-1` na implantação) |

Os valores de `AdminEmail` e `SenderEmail` vêm dos GitHub Secrets, e o token vem do GitHub Secret `API_TOKEN`. Nenhum desses valores fica no código.

### Configurações das funções

- **Runtime:** Java 17, arquitetura x86_64.
- **Memória e timeout:** 512 MB e 30 segundos, nas funções.
- **Variáveis de ambiente:** `TABLE_NAME`, `API_TOKEN`, `ADMIN_EMAIL`, `SENDER_EMAIL`, `QUARKUS_PROFILE`, entre outras.

---

## ENDPOINTS E ACESSO

Todas as rotas exigem o cabeçalho `Authorization: Bearer <token>`.

| Método | Caminho | Função | Descrição |
|---|---|---|---|
| POST | `/v1/feedback` | receive-feedback | Cria um feedback |
| GET | `/v1/feedback` | receive-feedback | Lista os feedbacks |
| POST | `/v1/graphql` | graphql | Consulta GraphQL (`listFeedbacks`) |
| POST | `/graphql` | graphql | Mesma consulta, em caminho legado |

### Exemplos de uso

Criar um feedback:

```bash
curl -X POST "https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod/v1/feedback" \
  -H "Authorization: Bearer $API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"description": "Aula muito clara e bem organizada", "rating": 9}'
```

Resposta (`201`):

```json
{
  "id": "0b8e7c1e-...",
  "description": "Aula muito clara e bem organizada",
  "rating": 9,
  "urgency": "LOW",
  "createdAt": "2026-11-16T10:30:00.123456789"
}
```

Listar os feedbacks:

```bash
curl "https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod/v1/feedback" \
  -H "Authorization: Bearer $API_TOKEN"
```

Consultar por GraphQL:

```bash
curl -X POST "https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod/v1/graphql" \
  -H "Authorization: Bearer $API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query": "{ listFeedbacks { id description rating urgency createdAt } }"}'
```

### Respostas de erro

| Situação | Código | Corpo |
|---|---|---|
| Sem token | `401` | `Token Bearer ausente` |
| Token inválido | `401` | `Token Bearer inválido` |
| Descrição com menos de 5 caracteres | `400` | `Erro de validação` |
| Nota fora de 0 a 10 | `400` | `Erro de validação` |
| Falha inesperada | `500` | Mensagem genérica, sem detalhes internos |

### Aderência ao enunciado

O enunciado pede `POST /avaliação` com os campos `descricao` e `nota`. A implementação expõe a mesma funcionalidade com os nomes em inglês:

| Enunciado | Implementado |
|---|---|
| `POST /avaliação` | `POST /v1/feedback` |
| Campo `descricao` | Campo `description` |
| Campo `nota` (0 a 10) | Campo `rating` (0 a 10) |
| Dados do e-mail: descrição, urgência e data de envio | Descrição, urgência e data de envio |
| Dados do relatório: descrição, urgência, data de envio, quantidade por dia e por urgência | Todos, e também a nota média |


---

## SEGURANÇA

### 1. Autenticação

- Todas as rotas exigem o cabeçalho `Authorization: Bearer <token>`.
- A comparação é feita em tempo constante, com hash SHA-256.
- Sem token, a resposta é `401`, com o cabeçalho `WWW-Authenticate: Bearer`.
- Se o token do servidor não estiver configurado, toda requisição é recusada com `500`. Assim a API nunca fica aberta por falta de configuração.
- O token fica no GitHub Secret `API_TOKEN`, é passado à implantação como parâmetro `NoEcho` e chega às funções como variável de ambiente.
- Para trocar o token: atualizar o GitHub Secret `API_TOKEN` e refazer o deploy de produção.

### 1.1 Ciclo de vida do token

O diagrama mostra por onde o token passa, da criação até a validação de cada requisição:

```
┌───────────────────────────────────┐
│ 1. GITHUB (secret API_TOKEN)      │  o token é gerado com openssl e salvo
│    guardado de forma criptografada│  como secret do repositório
│    não pode ser lido depois       │
└─────────────────┬─────────────────┘
                  ▼  push na main: job "Deploy to prod (SAM)"
┌───────────────────────────────────┐
│ 2. PIPELINE e CLOUDFORMATION      │  o workflow lê o secret e envia ao
│    parâmetro ApiToken (NoEcho)    │  CloudFormation, sem exibir o valor
└─────────────────┬─────────────────┘
                  ▼  grava como variável de ambiente
┌───────────────────────────────────┐
│ 3. LAMBDAs                        │  variável API_TOKEN nas funções
│    receive-feedback e graphql     │  receive-feedback e graphql
│    texto legível na configuração  │  quem pode ler a configuração da
│                                   │  função na AWS consegue ver o valor
└─────────────────┬─────────────────┘
                  ▼  a cada requisição
┌───────────────────────────────────┐
│ 4. VALIDAÇÃO NA API               │  o header Authorization: Bearer <token>
│    compara com a variável         │  é comparado com API_TOKEN
│                                   │  igual: a requisição segue
│                                   │  ausente ou diferente: 401
└───────────────────────────────────┘
```

O ponto de atenção é a etapa 3. O token não fica em código nem em arquivo versionado, mas fica na configuração de cada Lambda, e quem tem acesso de leitura à AWS pode consultá-lo. Por isso o acesso à conta AWS deve ser restrito, e o token deve ser trocado se houver suspeita de vazamento.

### 2. Privilégio mínimo (IAM)

| Função | Permissões concedidas |
|---|---|
| receive-feedback | DynamoDB: `PutItem`, `Scan`, `Query`, `GetItem` na tabela. SNS: `Publish` no tópico de urgência |
| send-notification | SES: `SendEmail`, `SendRawEmail` |
| generate-report | DynamoDB: `Scan`, `Query` na tabela. SES: `SendEmail`, `SendRawEmail` |
| graphql | Usa a role do receive-feedback |

Cada função tem uma role própria, exceto a graphql. As permissões de envio de e-mail usam o recurso `*`, porque o SES não permite restringir `SendEmail` a um e-mail específico por política.

### 3. Dados

- **Em repouso:** o DynamoDB criptografa os dados por padrão.
- **Em trânsito:** o API Gateway aceita apenas HTTPS.
- **Sem dados sensíveis nos logs:** o token nunca é registrado.

### 4. Governança e auditoria

- Logs de todas as funções no CloudWatch, com retenção de 30 dias.
- Separação dos ambientes dev e prod, com recursos e configurações próprios.
- Os e-mails de envio só saem de identidades verificadas no SES.
- Alterações na infraestrutura passam pelo pipeline, com revisão em pull request.

### 5. Documentação de segurança

O documento completo está em `docs/SECURITY.md`.

---

## DEPLOY AUTOMATIZADO

**Como está hoje, em resumo:**
- O deploy de produção roda sozinho quando o código entra na `main`, pelo job "Deploy to prod (SAM)" do pipeline.
- O job só começa depois do build, que roda os testes e o SonarCloud.
- O deploy usa `sam build` e `sam deploy` na stack `feedback-api-prod`, com a autenticação OIDC.
- Para conferir depois do deploy: `./run.sh sam-validate` (opção 7) e `./run.sh prod-check` (opção 8).
- O passo a passo completo, incluindo a configuração inicial da AWS e do GitHub, está em `docs/deploy/AWS_DEPLOY_GUIDE.md`.

**Detalhes do pipeline:**

O deploy é feito pelo pipeline do GitHub Actions, definido em `.github/workflows/ci-cd.yml`:

1. **Pull request:** o build roda os testes dos quatro módulos e a análise do SonarCloud. Um branch com nome inválido é recusado.
2. **Merge na `main`:** dispara o job **Deploy to prod (SAM)**, que só roda depois do build.
3. **Verificações antes do deploy:** o job confere se os GitHub Secrets `AWS_DEPLOY_ROLE_ARN`, `ADMIN_EMAIL`, `SENDER_EMAIL` e `API_TOKEN` existem, e falha cedo se algum faltar.
4. **Autenticação na AWS:** por OIDC, com a role `github-actions-deploy-prod`. Não há chave de acesso guardada no GitHub.
5. **Build e deploy:** `mvn package` gera os pacotes das funções, depois `sam build` e `sam deploy --config-env prod` atualizam a stack `feedback-api-prod`.

### GitHub Secrets

O pipeline usa cinco GitHub Secrets. Eles ficam no repositório e não aparecem no código.

| Nome | Usado no | Função |
|---|---|---|
| `AWS_DEPLOY_ROLE_ARN` | Job de deploy | ARN da role `github-actions-deploy-prod`, assumida pelo pipeline na AWS |
| `ADMIN_EMAIL` | Job de deploy | E-mail do administrador, destinatário de avisos, relatórios e alarmes |
| `SENDER_EMAIL` | Job de deploy | Remetente dos e-mails, uma identidade verificada no SES |
| `API_TOKEN` | Job de deploy | Token Bearer da API (ver "Ciclo de vida do token") |
| `SONAR_TOKEN` | Job de build | Token para enviar a análise ao SonarCloud |

Não há chaves de acesso da AWS guardadas no GitHub. A autenticação na AWS é feita por OIDC: o GitHub gera uma credencial temporária, e a AWS aceita apenas o repositório e o ambiente `prod` definidos na trust policy da role.

Para conferir: no repositório, *Settings* → *Secrets and variables* → *Actions*. A lista mostra os nomes e a data da última atualização. O valor não aparece depois de salvo.

Para trocar um valor: atualize o GitHub Secret e rode o deploy de produção de novo. A Lambda só recebe o valor novo quando o deploy termina.

Para executar: o deploy de produção roda a cada merge na `main`. Para repetir sem alterar código, use "Re-run all jobs" na execução do GitHub Actions.

O passo a passo completo, com a configuração inicial da AWS e do GitHub, está em `docs/deploy/AWS_DEPLOY_GUIDE.md`.

Cada merge na `main` dispara o job **Deploy to prod (SAM)**. O job só roda depois do build e dos testes. Ele gera os pacotes das funções, executa `sam build` e `sam deploy`, e termina com a stack `feedback-api-prod` em `UPDATE_COMPLETE`.

**[PENDENTE: print da execução bem-sucedida do job "Deploy to prod (SAM)" no GitHub Actions, com o status verde.]**

---

## MONITORAMENTO

### Logs

| Grupo de logs | Função | Retenção |
|---|---|---|
| `/aws/lambda/prod-receive-feedback` | receive-feedback | 30 dias |
| `/aws/lambda/prod-graphql` | graphql | 30 dias |
| `/aws/lambda/prod-send-notification` | send-notification | 30 dias |
| `/aws/lambda/prod-generate-report` | generate-report | 30 dias |

As mensagens de erro são gravadas com o nível `SEVERE`.

### Alarmes

| Alarme | Métrica | Quando dispara |
|---|---|---|
| `prod-severe-errors-receive-feedback` | Linhas SEVERE do receive-feedback | Pelo menos 1 em 5 minutos |
| `prod-severe-errors-send-notification` | Linhas SEVERE do send-notification | Pelo menos 1 em 5 minutos |
| `prod-severe-errors-generate-report` | Linhas SEVERE do generate-report | Pelo menos 1 em 5 minutos |

Os alarmes enviam e-mail ao administrador pelo tópico `prod-monitoring-alarms-topic`. O alarme do send-notification foi testado em produção, e o e-mail chegou ao administrador.

### Evidências no Amazon CloudWatch

O CloudWatch é o serviço que guarda os logs e dispara os alarmes. Para conferir na conta, no console da AWS, região `sa-east-1`:

1. **Logs:** CloudWatch → *Logs* → *Log groups*. Os quatro grupos de produção são `/aws/lambda/prod-receive-feedback`, `/aws/lambda/prod-graphql`, `/aws/lambda/prod-send-notification` e `/aws/lambda/prod-generate-report`. Todos já têm eventos registrados, incluindo a execução do relatório semanal.
2. **Alarmes:** CloudWatch → *Alarms*. Os três alarmes `prod-severe-errors-*` aparecem com estado `OK`.
3. **Mensagens de erro:** nos logs, as falhas aparecem com o nível `SEVERE`, e é esse nível que os alarmes contam.

Pelo terminal, o mesmo pode ser conferido com:

```bash
aws logs tail /aws/lambda/prod-receive-feedback --since 1h --region sa-east-1
aws cloudwatch describe-alarms --alarm-name-prefix prod-severe --region sa-east-1
```

A retenção dos logs é de 30 dias, configurada no template.

No console da AWS, em CloudWatch → Logs → Log groups, os quatro grupos `/aws/lambda/prod-*` têm eventos. Em CloudWatch → Alarms, os três alarmes `prod-severe-errors-*` estão em `OK`.

**[PENDENTE: print da tela de Log groups com os quatro grupos de produção, e da tela de Alarms com os três alarmes.]**

O guia completo está em `docs/qualidade/MONITORING_GUIDE.md`.

Quando uma função grava uma linha `SEVERE`, o alarme correspondente conta essa linha. Se houver pelo menos uma em 5 minutos, o alarme muda para `ALARM` e publica no tópico `prod-monitoring-alarms-topic`. O SNS envia o aviso por e-mail ao administrador. Esse caminho foi testado em produção, e o e-mail chegou.

**[PENDENTE: print do e-mail de alarme recebido, com o assunto "ALARM: prod-severe-errors-send-notification".]**

---

## NOTIFICAÇÕES E RELATÓRIO SEMANAL

### Aviso de urgência

**Regra de envio:** o e-mail é enviado somente quando a urgência é **HIGH**, ou seja, quando a nota está entre 0 e 5. A função `receive-feedback` só publica no SNS nesse caso. Avaliações MEDIUM (nota 6 e 7) e LOW (nota 8 a 10) não geram e-mail e aparecem apenas no relatório semanal.

- **Quando:** toda avaliação com urgência HIGH (nota de 0 a 5).
- **Para quem:** o administrador (`AdminEmail`).
- **Conteúdo:** descrição, urgência e data de envio.
- **Teste realizado:** feedback de urgência alta enviado em produção, com e-mail recebido.

### Relatório semanal

- **Quando:** toda segunda-feira, às 08h UTC (05h em Brasília).
- **Período:** 7 dias, terminando no dia anterior à execução.
- **Conteúdo:**
  - total de avaliações e nota média;
  - quantidade de avaliações por dia;
  - quantidade de avaliações por urgência;
  - lista dos feedbacks do período, com descrição, urgência e data de envio.
- **Para quem:** o administrador (`AdminEmail`), a partir do remetente `SenderEmail`.

**Execução manual:** o relatório também pode ser disparado fora do agendamento, com o comando abaixo, que executa a função de produção:

```bash
aws lambda invoke --function-name prod-generate-report --payload '{}' response.json
```

O EventBridge dispara a função `prod-generate-report` toda segunda-feira às 08h UTC. A função monta o relatório dos últimos sete dias, até o dia anterior, e envia por SES ao `AdminEmail`, a partir do `SenderEmail`. Em 16/11/2026, a execução manual foi feita e o e-mail foi enviado com sucesso, conforme os logs.

**[PENDENTE: confirmar o envio automático na segunda-feira, 23/11/2026, às 08h UTC, com o registro nos logs.]**

**[PENDENTE: print do e-mail do relatório com a lista de feedbacks e os totais do período.]**

---

## EXECUÇÃO LOCAL

Para testar sem AWS em produção, a API roda na máquina local, com o módulo `local-http`.

### Pré-requisitos

- JDK 17 e Maven 3.8 ou superior.
- AWS CLI, com o perfil `fase4`. O ambiente local grava na tabela de desenvolvimento, nunca na de produção.

### Subir a API local

```bash
./run.sh local-up
```

O comando instala o módulo `receive-feedback` e sobe a API em `http://localhost:8080`. Em outro terminal, confira com:

```bash
./run.sh local-check
```

O token local é `minha-chave-local` (valor de teste, só local). Para testar com a collection do Bruno, importe `docs/api-collection/fase4-feedback.postman_collection.json` e use o ambiente `local`.

### Menu do `run.sh`

Execute `./run.sh` na raiz do projeto para abrir o menu:

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
| 2 | Compila os quatro módulos com `mvn clean package -DskipTests` |
| 3 | Roda os testes de todos os módulos com `mvn clean verify` |
| 4 | Pede o nome de um módulo (`receive-feedback`, `send-notification` ou `generate-report`) e roda só os testes dele |
| 5 | Instala o `receive-feedback` e sobe a API local em `http://localhost:8080`. Grava na tabela de desenvolvimento. Use Ctrl+C para parar |
| 6 | Faz `GET /v1/feedback` com token e confirma que a API local responde 200 |
| 7 | Valida o template SAM (`sam validate --lint`) |
| 8 | Lê o endereço da API da stack de produção e confirma que sem token a resposta é 401 |
| 9 | Lista as stacks, as funções Lambda e as tabelas DynamoDB. Só leitura |
| 0 | Sai do menu |

Os mesmos comandos podem ser usados direto, sem o menu: `./run.sh compile`, `./run.sh tests [módulo]`, `./run.sh local-up`, `./run.sh local-check`, `./run.sh sam-validate`, `./run.sh prod-check` e `./run.sh aws-list`.

O guia completo está em `docs/LOCAL_DEVELOPMENT.md` e `docs/LOCAL_SETUP.md`.

---

## TESTES E QUALIDADE

### Testes automatizados

| Módulo | Testes | Falhas | Cobertura de linhas (JaCoCo) |
|---|---|---|---|
| receive-feedback | 147 | 0 | 92,4% |
| send-notification | 86 | 0 | 96,6% |
| generate-report | 167 | 0 | 99,5% |
| local-http (apenas desenvolvimento) | 6 | 0 | — |

Comando para rodar: `mvn clean verify` (JDK 17, na raiz do projeto).

### Testes da API

A collection do Bruno cobre as rotas, os cenários de sucesso e de erro (400 e 401), e a consulta GraphQL. Ela roda no ambiente local com 20 requisições e 62 asserções, sem falhas.

### Análise estática

O SonarCloud roda no pipeline a cada pull request. O resultado fica no painel do projeto.

**Resultado do SonarCloud (consultado em 16/11/2026):**

| Indicador | Valor |
|---|---|
| Quality gate | OK |
| Cobertura de linhas | 95,7% |
| Bugs | 0 |
| Vulnerabilidades | 0 |
| Security hotspots | 0 |
| Code smells | 173 |

A análise roda a cada pull request. Os 173 code smells são apontamentos de manutenibilidade, e nenhum deles é bug ou vulnerabilidade.

O painel completo fica no SonarCloud, no projeto `jeffesa_12ADJT-fase-4`.

**[PENDENTE: print do painel do SonarCloud com o quality gate e a cobertura.]**

---

## VÍDEO DE DEMONSTRAÇÃO

[PENDENTE: inserir o link do vídeo (até 5 minutos), com a demonstração do fluxo de urgência, do relatório semanal, dos logs do CloudWatch e das configurações do projeto.]

Roteiro de referência: `docs/qualidade/DEMO_GUIDE.md`.

---
