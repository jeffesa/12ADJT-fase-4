# Guia de Deploy AWS

Este guia descreve como o projeto FIAP Tech Challenge Fase 4 (plataforma de feedback serverless) é publicado na AWS com AWS SAM, e como operar o deploy depois: pipeline, deploy manual, verificação, token, monitoramento, remoção e troubleshooting.

Convenções: o ambiente principal é **prod** (stack `feedback-api-prod`, região `sa-east-1`). Comandos, URLs e nomes técnicos ficam em inglês. Valores entre `<...>` são placeholders que você deve trocar.

## Sumário

1. [Visão geral do fluxo](#1-visão-geral-do-fluxo)
2. [Pré-requisitos](#2-pré-requisitos)
3. [Configurar o AWS CLI e o perfil `fase4`](#3-configurar-o-aws-cli-e-o-perfil-fase4)
4. [Configuração única da conta AWS](#4-configuração-única-da-conta-aws)
5. [Secrets do GitHub](#5-secrets-do-github)
6. [Pipeline de deploy (GitHub Actions)](#6-pipeline-de-deploy-github-actions)
7. [Deploy manual](#7-deploy-manual)
8. [Verificar o deploy](#8-verificar-o-deploy)
9. [Bearer token (`API_TOKEN`)](#9-bearer-token-api_token)
10. [Monitoramento essencial](#10-monitoramento-essencial)
11. [Remover o stack](#11-remover-o-stack)
12. [Teste local](#12-teste-local)
13. [Nota sobre o ambiente dev](#13-nota-sobre-o-ambiente-dev)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Visão geral do fluxo

```
feature/fix -> PR -> develop -> PR (release) -> main -> pipeline faz o deploy em prod
```

- O deploy em produção é feito **pelo pipeline** (`.github/workflows/ci-cd.yml`), somente em `push` na `main`.
- O pipeline autentica na AWS por OIDC (sem chaves de acesso fixas) e roda `sam deploy --config-env prod`.
- Recursos criados pelo stack `feedback-api-prod` (definidos em `infra/template.yaml`): API Gateway (REST), 5 Lambdas (`receive-feedback`, `list-feedback`, `graphql`, `send-notification`, `generate-report`), tabela DynamoDB, tópico SNS, regra do EventBridge (relatório semanal), log groups do CloudWatch e as roles IAM das Lambdas.
- Recursos criados **manualmente, fora do stack** (seção 4): identidade do SES, buckets S3 de artefatos, OIDC provider e a role `github-actions-deploy-prod`. Os secrets do GitHub também ficam fora do stack.

## 2. Pré-requisitos

| Ferramenta | Para quê | Observação |
|---|---|---|
| Conta AWS | Hospedar o stack | Região `sa-east-1` (São Paulo) |
| AWS CLI v2 | Comandos `aws ...` | Instalação: [guia oficial](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html). No macOS: `brew install awscli` |
| SAM CLI | `sam build`, `sam deploy`, `sam delete` | [Instalação](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html). No macOS: `brew install aws-sam-cli` |
| JDK 17 | Build e testes | **Use JDK 17.** Com JDK mais novo (por exemplo 25) os testes quebram (Mockito). O `./run.sh` localiza o JDK 17 sozinho |
| Maven 3.8+ | `mvn package` | |
| GitHub CLI (`gh`) | Cadastrar secrets e acompanhar o pipeline | [Instalação](https://cli.github.com/). Faça `gh auth login` |

Docker **não** é necessário (o projeto não usa LocalStack nem `sam local`).

Confira as versões:

```bash
aws --version
sam --version
java -version
mvn -version
gh --version
```

O `./run.sh deps` também valida JDK 17, Maven, AWS CLI, SAM CLI, curl e o perfil `fase4`.

## 3. Configurar o AWS CLI e o perfil `fase4`

O projeto usa o perfil nomeado `fase4` (o `run.sh` usa esse nome por padrão).

```bash
aws configure --profile fase4
# AWS Access Key ID:     <sua access key>
# AWS Secret Access Key: <sua secret key>
# Default region name:   sa-east-1
# Default output format: json

export AWS_PROFILE=fase4
export AWS_DEFAULT_REGION=sa-east-1
aws sts get-caller-identity
```

> Esta é a credencial do **seu usuário** para operar a conta (deploy manual, consultas). O pipeline não usa chaves: ele usa OIDC (seção 6). Nunca grave chaves de acesso em arquivos do repositório nem em secrets do GitHub.

Os exemplos deste guia passam `--region sa-east-1` explicitamente. Atenção ao parâmetro `Region` do template, que tem default `us-east-1` e precisa ser sobrescrito no deploy (seção 7).

## 4. Configuração única da conta AWS

Faça uma vez por conta. Os comandos desta seção **alteram estado** na AWS.

### 4.1. Identidade de e-mail no SES

O projeto envia e-mails (notificação de feedback urgente e relatório semanal) pelo SES em `sa-east-1`. A conta SES começa em **sandbox**: o remetente **e** o destinatário precisam estar verificados.

```bash
aws sesv2 create-email-identity --email-identity <email> --region sa-east-1
```

A AWS envia um e-mail com um link. Clique nele e confira:

```bash
aws sesv2 get-email-identity --email-identity <email> --region sa-east-1 \
  --query '[VerificationStatus,VerifiedForSendingStatus]' --output text
# SUCCESS   True
```

Verifique o e-mail usado em `SENDER_EMAIL` (remetente) e em `ADMIN_EMAIL` (destinatário). Se forem o mesmo endereço, uma identidade basta.

> O console do SES pode mostrar "região indisponível" para `sa-east-1` em contas do plano gratuito. A CLI continua funcionando.

### 4.2. Buckets S3 de artefatos

O `infra/samconfig.toml` usa um bucket por ambiente para guardar os pacotes do `sam deploy`:

```bash
aws s3 mb s3://sam-artifacts-sa-east-1-prod --region sa-east-1
aws s3 mb s3://sam-artifacts-sa-east-1-dev --region sa-east-1   # só se for usar dev
```

Confira:

```bash
aws s3api head-bucket --bucket sam-artifacts-sa-east-1-prod --region sa-east-1
```

### 4.3. OIDC provider e role `github-actions-deploy-prod`

Para o pipeline obter credenciais temporárias sem chaves fixas, a conta precisa de um OIDC provider do GitHub e de uma role que o repositório possa assumir.

**Provider** (uma vez por conta):

```bash
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com
```

**Trust policy** da role (`trust-policy.json`). Troque os placeholders:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:<owner>@<OWNER_ID>/<repo>@<REPO_ID>:environment:prod"
        }
      }
    }
  ]
}
```

O `sub` precisa ser **exatamente** o que o GitHub envia no token. Neste repositório ele traz os IDs numéricos do dono e do repositório (`<OWNER_ID>` e `<REPO_ID>`). Veja como descobrir o valor real na [seção 4.4](#44-descobrir-o-sub-real-do-token-oidc). O formato sem IDs (`repo:<owner>/<repo>:environment:prod`) causa o erro `Not authorized to perform sts:AssumeRoleWithWebIdentity`.

```bash
aws iam create-role \
  --role-name github-actions-deploy-prod \
  --assume-role-policy-document file://trust-policy.json
```

**Permissões** da role (`deploy-policy.json`). A role em uso hoje tem uma policy inline com este formato:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudformation:*", "lambda:*", "apigateway:*", "dynamodb:*",
        "sns:*", "ses:*", "events:*", "logs:*", "scheduler:*"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:CreateRole", "iam:DeleteRole", "iam:GetRole", "iam:PassRole",
        "iam:AttachRolePolicy", "iam:DetachRolePolicy",
        "iam:PutRolePolicy", "iam:DeleteRolePolicy", "iam:GetRolePolicy",
        "iam:TagRole", "iam:UntagRole"
      ],
      "Resource": "arn:aws:iam::<ACCOUNT_ID>:role/*"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:*"],
      "Resource": [
        "arn:aws:s3:::sam-artifacts-sa-east-1-prod",
        "arn:aws:s3:::sam-artifacts-sa-east-1-prod/*"
      ]
    }
  ]
}
```

```bash
aws iam put-role-policy \
  --role-name github-actions-deploy-prod \
  --policy-name sam-deploy-prod \
  --policy-document file://deploy-policy.json
```

Cobre CloudFormation, Lambda, API Gateway, DynamoDB, SNS, SES, EventBridge, CloudWatch Logs, IAM (roles das Lambdas, com `PassRole`) e o bucket de artefatos de prod. É uma policy ampla, pensada para um projeto acadêmico; em um ambiente real, restrinja os recursos.

O ARN da role (`arn:aws:iam::<ACCOUNT_ID>:role/github-actions-deploy-prod`) é o valor do secret `AWS_DEPLOY_ROLE_ARN`.

> O arquivo `docs/AWS_DEPLOY_PERMISSIONS_REQUEST.md` descreve um pedido de permissões antigo (outra conta, outro nome de stack) e **não** deve ser usado como referência.

### 4.4. Descobrir o `sub` real do token OIDC

Se o pipeline falhar com `Not authorized to perform sts:AssumeRoleWithWebIdentity`, o `sub` da trust policy não bate com o do token. Para ver o que o GitHub envia, use um workflow temporário. Crie `.github/workflows/debug-oidc.yml` na branch padrão do repositório (o GitHub só permite `workflow_dispatch` para workflows que existem nela):

```yaml
name: debug-oidc
on: workflow_dispatch
jobs:
  claims:
    runs-on: ubuntu-latest
    environment: prod
    permissions:
      id-token: write
    steps:
      - name: Print OIDC claims
        run: |
          TOKEN=$(curl -sS -H "Authorization: Bearer $ACTIONS_ID_TOKEN_REQUEST_TOKEN" \
            "$ACTIONS_ID_TOKEN_REQUEST_URL&audience=sts.amazonaws.com" | jq -r .value)
          PAYLOAD=$(echo "$TOKEN" | cut -d. -f2 | tr '_-' '/+')
          while [ $(( ${#PAYLOAD} % 4 )) -ne 0 ]; do PAYLOAD="${PAYLOAD}="; done
          echo "$PAYLOAD" | base64 -d | jq '{sub, aud, iss, ref, environment, repository}'
```

Rode o workflow (`gh workflow run debug-oidc.yml --repo <owner>/<repo>` ou o botão "Run workflow") e leia os claims no log. Copie o valor de `sub` para a condição `token.actions.githubusercontent.com:sub` da trust policy:

```bash
aws iam update-assume-role-policy \
  --role-name github-actions-deploy-prod \
  --policy-document file://trust-policy.json
```

O workflow imprime só as chaves `sub`, `aud`, `iss`, `ref`, `environment` e `repository`, nunca o token. **Remova o arquivo `debug-oidc.yml` depois de usar.**

O formato esperado do `sub` neste projeto é:

```
repo:<owner>@<OWNER_ID>/<repo>@<REPO_ID>:environment:prod
```

Os IDs numéricos também podem ser lidos em `gh api repos/<owner>/<repo> --jq '.id, .owner.id'`, e a configuração do subject do repositório em `gh api repos/<owner>/<repo>/actions/oidc/customization/sub`.

## 5. Secrets do GitHub

O job de deploy exige 4 secrets. O build usa mais um:

| Secret | Usado em | Conteúdo |
|---|---|---|
| `AWS_DEPLOY_ROLE_ARN` | deploy | ARN da role `github-actions-deploy-prod` |
| `ADMIN_EMAIL` | deploy | Destinatário das notificações e relatórios (verificado no SES) |
| `SENDER_EMAIL` | deploy | Remetente (verificado no SES) |
| `API_TOKEN` | deploy | Bearer token da API (24 a 128 caracteres: letras, dígitos, `_` e `-`) |
| `SONAR_TOKEN` | build | Token do SonarCloud |

O workflow lê `secrets.<NOME>` dentro do job com `environment: prod`, então funcionam tanto secrets de **repositório** quanto do environment `prod`. O projeto usa secrets de repositório.

Cadastro:

```bash
gh secret set AWS_DEPLOY_ROLE_ARN --repo <owner>/<repo>    # cole o valor e tecle Enter, depois Ctrl+D
gh secret set ADMIN_EMAIL --repo <owner>/<repo> --body "<email>"
gh secret set SENDER_EMAIL --repo <owner>/<repo> --body "<email>"
openssl rand -hex 24 | tee >(pbcopy) | gh secret set API_TOKEN --repo <owner>/<repo>
gh secret list --repo <owner>/<repo>
```

Para gravar no environment `prod`, acrescente `--env prod`. O `gh secret list` mostra só os nomes, nunca os valores.

> `openssl rand -hex 24` gera 48 caracteres hexadecimais, dentro do padrão exigido. O comando acima também copia o token para a área de transferência (`pbcopy`, macOS): guarde-o em um gerenciador de senhas, porque o GitHub não mostra o valor depois. Se algum secret obrigatório faltar, o job de deploy falha logo no início, com a mensagem `Secret ausente` (seção 14).

## 6. Pipeline de deploy (GitHub Actions)

Arquivo: `.github/workflows/ci-cd.yml`. Gatilhos: `push` em `main` e `develop`, `pull_request` para `develop` e `main`, e `workflow_dispatch`.

**Job 1: `Build & Test & SonarCloud`** (todos os eventos)

1. Confere a política do `SONAR_TOKEN`: PR para `main` falha se o token faltar.
2. `mvn -B clean verify` (JDK 17) com cobertura JaCoCo.
3. Análise SonarCloud com quality gate bloqueante. **Não roda em push na `main`**: a análise vem da `develop`, no mesmo commit, já aprovada antes do PR.

**Job 2: `Deploy to prod (SAM)`** (depende do job 1; só em `push` na `main`)

1. Confere os secrets obrigatórios e falha cedo se algum faltar.
2. Autentica na AWS por OIDC (`aws-actions/configure-aws-credentials`, role `AWS_DEPLOY_ROLE_ARN`, região `sa-east-1`). Sem chaves fixas.
3. `mvn -B package -DskipTests` na raiz (gera os `function.zip`).
4. `cd infra && sam build`.
5. `sam deploy --config-env prod --no-confirm-changeset --parameter-overrides "Environment=prod AdminEmail=... SenderEmail=... ApiToken=... Region=sa-east-1"`.

O job usa `environment: prod` e `concurrency: deploy-prod` (`cancel-in-progress: false`): dois deploys nunca rodam ao mesmo tempo, o segundo espera o primeiro.

Acompanhar:

```bash
gh run list --repo <owner>/<repo> --branch main --limit 5
gh run watch --repo <owner>/<repo>
```

Re-executar o deploy (por exemplo, depois de trocar um secret): abra o último run da `main` em **Actions** e clique em **Re-run all jobs**. O disparo manual (`workflow_dispatch`) **não** faz deploy, porque o job só roda em `push` na `main`.

## 7. Deploy manual

Use quando precisar publicar sem passar pelo pipeline. A ordem importa: o template **não compila Java**, ele aponta para `../receive-feedback/target/function.zip` e equivalentes. Sem o `package`, o `sam build` falha por falta do zip.

```bash
export AWS_PROFILE=fase4

# 1. Na raiz do repositório (JDK 17)
mvn -B package -DskipTests

# 2. Build e deploy
cd infra
sam build
sam deploy --config-env prod \
  --parameter-overrides "Environment=prod AdminEmail=<email> SenderEmail=<email> ApiToken=<token> Region=sa-east-1"
```

Pontos de atenção:

- **`infra/samconfig.toml`** tem os perfis `dev` e `prod` (`stack_name`, `s3_bucket`, `region`, `capabilities`, `parameter_overrides`). Os e-mails nele (`example.com`) são placeholders e o `ApiToken` fica **de propósito fora do arquivo**, porque é segredo e nunca deve ser versionado.
- `--parameter-overrides` na linha de comando **substitui a string inteira** do samconfig. Por isso repita sempre `Environment`, `AdminEmail`, `SenderEmail`, `ApiToken` e `Region`.
- `Region` tem default `us-east-1` no template e é usado nos ARNs, nos endpoints e na variável `APP_REGION` das Lambdas. Passe sempre `Region=sa-east-1`.
- `AdminEmail`, `SenderEmail` e `ApiToken` não têm default no template. Sem eles o deploy falha na validação de parâmetros (seção 14).
- Para não deixar o token no histórico do shell, use uma variável: `read -rs API_TOKEN` e depois `ApiToken=$API_TOKEN` no `--parameter-overrides`.
- Um deploy manual com um `ApiToken` diferente do secret do GitHub será sobrescrito no próximo deploy do pipeline.

## 8. Verificar o deploy

Status do stack e URLs (outputs `ApiEndpoint` e `GraphQLEndpoint`):

```bash
aws cloudformation describe-stacks --stack-name feedback-api-prod --region sa-east-1 \
  --query 'Stacks[0].StackStatus' --output text

aws cloudformation describe-stacks --stack-name feedback-api-prod --region sa-east-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`||OutputKey==`GraphQLEndpoint`].[OutputKey,OutputValue]' \
  --output text
```

Os outputs do stack são `ApiEndpoint`, `GraphQLEndpoint`, `DynamoDBTableName` (`prod-feedback-table`), `SNSTopicArn` e o ARN de cada Lambda. As URLs têm o formato `https://<api-id>.execute-api.sa-east-1.amazonaws.com/prod/v1/feedback` e `.../prod/v1/graphql`.

Lambdas (cada uma com 512 MB e timeout de 30 s):

```bash
aws lambda list-functions --region sa-east-1 \
  --query 'Functions[?starts_with(FunctionName,`prod-`)].[FunctionName,MemorySize,Timeout]' --output text
```

Pelo `run.sh` (somente leitura):

```bash
./run.sh aws-list     # stacks, Lambdas, tabelas DynamoDB e tópicos SNS
PROD_API_TOKEN=<token> ./run.sh prod-check   # sem token espera 401; com PROD_API_TOKEN espera 200
```

## 9. Bearer token (`API_TOKEN`)

Todas as rotas (`POST /v1/feedback`, `GET /v1/feedback`, `POST /v1/graphql`) exigem `Authorization: Bearer <token>`. O token é o valor do secret `API_TOKEN`, que o deploy grava como variável de ambiente `API_TOKEN` nas Lambdas `receive-feedback`, `list-feedback` e `graphql`. A validação acontece dentro das Lambdas.

### Criar

Ver [seção 5](#5-secrets-do-github). O token precisa ter 24 a 128 caracteres (letras, dígitos, `_` e `-`); o CloudFormation rejeita outro formato.

### Usar

```bash
export API_TOKEN=<token>
export API_URL=<valor do output ApiEndpoint>   # termina em /prod/v1/feedback

# Listar feedbacks
curl -i "$API_URL" -H "Authorization: Bearer $API_TOKEN"

# Criar um feedback (grava um registro real em prod)
curl -i -X POST "$API_URL" \
  -H "Authorization: Bearer $API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"description":"Teste de deploy","rating":8}'
```

Campos do `POST /v1/feedback`: `description` (texto, não vazio) e `rating` (inteiro de 0 a 10). O contrato completo está em `docs/API.md`. Notas de 0 a 5 geram urgência alta (`HIGH`) e disparam SNS e e-mail; 6 a 7 é média e 8 a 10 é baixa.

Respostas de erro (HTTP 401, header `WWW-Authenticate: Bearer`):

| `detail` | Causa |
|---|---|
| `Missing bearer token` | Header `Authorization` ausente, vazio ou com esquema diferente de `Bearer` |
| `Invalid bearer token` | Token enviado, mas diferente do `API_TOKEN` da Lambda |

### Recuperar o token

O GitHub não mostra o valor de um secret. A Lambda guarda o token na variável `API_TOKEN` e, com permissão IAM de leitura da configuração, dá para recuperá-lo.

**Variante A: sem imprimir na tela (copia para a área de transferência, macOS)**

```bash
aws lambda get-function-configuration --profile fase4 --region sa-east-1 \
  --function-name prod-graphql \
  --query 'Environment.Variables.API_TOKEN' --output text | pbcopy
```

**Variante B: imprimindo no terminal**

```bash
aws lambda get-function-configuration --profile fase4 --region sa-east-1 \
  --function-name prod-graphql \
  --query 'Environment.Variables.API_TOKEN' --output text
```

> **Aviso:** a variante B imprime o segredo. Ele fica visível no scrollback do terminal e pode parar em logs de sessão do terminal, em gravações de tela e em compartilhamentos de tela; se você guardar a saída em uma variável ou arquivo, ela também pode ir para o histórico do shell e para logs. Prefira a variante A. Nunca cole o token em chats, issues, PRs ou commits.

Para conferir só que a variável existe, sem ver o valor:

```bash
aws lambda get-function-configuration --profile fase4 --region sa-east-1 \
  --function-name prod-graphql --query 'keys(Environment.Variables)'
```

### Trocar

1. Gere e grave o novo token (o comando também copia para a área de transferência):

   ```bash
   openssl rand -hex 24 | tee >(pbcopy) | gh secret set API_TOKEN --repo <owner>/<repo>
   ```

2. Faça um novo deploy: abra o último run da `main` em **Actions** e clique em **Re-run all jobs**. O `workflow_dispatch` não serve, porque o job de deploy só roda em `push` na `main`.
3. Só depois do deploy as Lambdas passam a aceitar o token novo. Até lá vale o token antigo, e o novo responde `Invalid bearer token`.
4. Atualize o token nos clientes (Bruno/Postman, scripts).

Em um deploy manual, passe `ApiToken=<token>` no `--parameter-overrides` (seção 7).

## 10. Monitoramento essencial

Log groups reais em prod (retenção de 30 dias), criados pelo template:

| Log group | Lambda |
|---|---|
| `/aws/lambda/prod-receive-feedback` | `prod-receive-feedback` **e** `prod-list-feedback` (as duas escrevem neste grupo) |
| `/aws/lambda/prod-graphql` | `prod-graphql` |
| `/aws/lambda/prod-send-notification` | `prod-send-notification` |
| `/aws/lambda/prod-generate-report` | `prod-generate-report` |

```bash
aws logs tail /aws/lambda/prod-receive-feedback --since 1h --region sa-east-1
aws logs tail /aws/lambda/prod-graphql --since 1h --region sa-east-1 --follow
aws logs tail /aws/lambda/prod-send-notification --since 1d --region sa-east-1
```

`--since` aceita `5m`, `1h`, `1d`, e funciona igual no macOS e no Linux. Use `--follow` para acompanhar em tempo real e `Ctrl+C` para sair.

O relatório semanal roda toda segunda-feira às 08:00 UTC (EventBridge, `cron(0 8 ? * MON *)`).

Alarmes, dashboards e métricas ficam para a TASK-025 (`docs/qualidade/MONITORING_GUIDE.md`). Observação conhecida: o `MethodSettings` do template aponta para `/feedback`, que não é uma rota real (as rotas são `/v1/feedback` e `/v1/graphql`), então as métricas e logs de método do API Gateway não cobrem as rotas reais.

## 11. Remover o stack

```bash
sam delete --stack-name feedback-api-prod --region sa-east-1 --profile fase4
```

Avisos:

- A tabela DynamoDB `prod-feedback-table` é **apagada junto, sem backup**: o template não tem `DeletionPolicy` e o point-in-time recovery está desligado. Exporte os dados antes, se precisar deles.
- Ficam na conta, porque não pertencem ao stack: os buckets `sam-artifacts-sa-east-1-*`, as identidades do SES, o OIDC provider, a role `github-actions-deploy-prod` e os secrets do GitHub.
- Para recriar tudo, basta um novo deploy (pipeline ou manual). O `sam delete` é também a saída quando o stack fica em `ROLLBACK_COMPLETE` e precisa ser recriado.

## 12. Teste local

Não use `sam local` nem Docker. O teste local usa o módulo `local-http` em Quarkus Dev Mode:

```bash
./run.sh local-up      # instala o receive-feedback e sobe o local-http (Ctrl+C para parar)
./run.sh local-check   # em outro terminal: confere GET /v1/feedback com HTTP 200
```

- Rotas em `http://localhost:8080` (`POST /v1/feedback`, `GET /v1/feedback`, `POST /v1/graphql`), todas com `Authorization: Bearer <token>`. Sem `API_TOKEN` exportada, o script usa `minha-chave-local`.
- As portas 8080 (HTTP) e 18081 (Mock Event Server do Lambda) precisam estar livres.
- A execução local **grava na tabela real `dev-feedback-table`** (`sa-east-1`, perfil `fase4`), nunca em `prod`. Apague os itens de teste depois.
- Subir pela raiz (`mvn quarkus:dev` na raiz) não expõe as rotas HTTP e o `GET /v1/feedback` responde 405: o comando precisa rodar em `local-http` (o `run.sh` já faz isso).

Detalhes em `README.md` (seções de Quarkus Dev Mode e `run.sh`).

## 13. Nota sobre o ambiente dev

O stack `feedback-api-dev` existe e o `infra/samconfig.toml` tem o perfil `dev`, mas o pipeline só faz deploy em **prod**. O deploy em dev é manual e precisa dos parâmetros completos:

```bash
cd infra
sam deploy --config-env dev \
  --parameter-overrides "Environment=dev AdminEmail=<email> SenderEmail=<email> ApiToken=<token> Region=sa-east-1"
```

Aviso: o template usa `StageName: 'prod'` fixo no API Gateway, mas os outputs `ApiEndpoint` e `GraphQLEndpoint` interpolam `${Environment}` na URL. Em dev a URL publicada pode, portanto, não bater com o stage real. **Não presuma URLs `/dev/...`**: copie a URL real do output do stack e confira o stage antes de usar.

## 14. Troubleshooting

| Sintoma | Causa e solução |
|---|---|
| Testes quebram com Mockito / `mvn` falha em erros estranhos de reflexão | JDK diferente de 17 (por exemplo 25). Use JDK 17: `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` no macOS. O `./run.sh` já escolhe o 17 |
| `sam build` falha por falta de `function.zip` | Faltou `mvn -B package -DskipTests` na raiz antes do `sam build` (seção 7) |
| `InvalidClientTokenId` logo depois de criar uma access key | A credencial ainda está propagando. Espere alguns minutos e tente de novo. Se persistir, confira a chave com `aws sts get-caller-identity --profile fase4` |
| Pipeline: `Not authorized to perform sts:AssumeRoleWithWebIdentity` | O `sub` da trust policy não bate com o do token. O formato correto é `repo:<owner>@<OWNER_ID>/<repo>@<REPO_ID>:environment:prod`, com IDs numéricos, e **não** `repo:<owner>/<repo>:environment:prod`. Descubra o valor real pela [seção 4.4](#44-descobrir-o-sub-real-do-token-oidc). Confira também `aud` = `sts.amazonaws.com` e que o job usa `environment: prod` |
| Deploy da `main` falha cedo com `Secret ausente` | Falta `AWS_DEPLOY_ROLE_ARN`, `ADMIN_EMAIL`, `SENDER_EMAIL` ou `API_TOKEN`. Cadastre (seção 5) e use **Re-run all jobs** |
| `Parameter validation failed` / `Parameter ... failed to satisfy constraint` no CloudFormation | `AdminEmail` e `SenderEmail` precisam ser e-mails válidos (`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`). `ApiToken` precisa casar com `^[A-Za-z0-9_-]{24,128}$` (24 a 128 caracteres, só letras, dígitos, `_` e `-`). Parâmetro faltando = `--parameter-overrides` incompleto (ele substitui o samconfig inteiro) |
| Outputs, permissões das Lambdas ou `APP_REGION` apontando para `us-east-1` | Faltou `Region=sa-east-1` no `--parameter-overrides` (o default do template é `us-east-1`) |
| E-mail de notificação ou relatório não chega | SES em sandbox: remetente **e** destinatário precisam estar verificados (`get-email-identity` com `VerificationStatus: SUCCESS`, seção 4.1). Confira `SENDER_EMAIL` e `ADMIN_EMAIL` e os logs de `prod-send-notification` |
| Console do SES indisponível em `sa-east-1` | Limitação do console em contas do plano gratuito. Use a CLI (`aws sesv2 ...`) |
| `401 Missing bearer token` | O header `Authorization: Bearer <token>` não foi enviado (ou o esquema não é `Bearer`) |
| `401 Invalid bearer token` | Token diferente do `API_TOKEN` das Lambdas. Se você acabou de trocar o secret, falta o novo deploy (seção 9) |
| `405` ao testar localmente | O módulo foi iniciado na pasta errada. Rode em `local-http` (`./run.sh local-up`) |
| Módulo local não sobe (porta ocupada) | Libere as portas 8080 e 18081 |
| Stack em `ROLLBACK_COMPLETE` | Remova com `sam delete --stack-name feedback-api-prod --region sa-east-1 --profile fase4` e faça um novo deploy |

Para investigar uma falha em runtime, comece pelos logs da Lambda (seção 10) e pelos eventos do stack:

```bash
aws cloudformation describe-stack-events --stack-name feedback-api-prod --region sa-east-1 \
  --query 'StackEvents[?contains(ResourceStatus,`FAILED`)].[LogicalResourceId,ResourceStatusReason]' --output text
```

> A antiga API Key e o Usage Plan do API Gateway foram removidos do projeto. A autenticação é feita somente pelo Bearer token.
