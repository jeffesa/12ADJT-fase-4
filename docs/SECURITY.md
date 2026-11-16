# Documentação de segurança

## Modelo de nuvem

Este sistema é construído sobre **AWS Serverless**, uma arquitetura nativa da nuvem que usa serviços gerenciados da AWS para eliminar o trabalho de gestão de infraestrutura. Todos os componentes de computação, armazenamento e integração são totalmente gerenciados pela AWS, o que permite escalar rapidamente e ter alta disponibilidade sem carga operacional.

O modelo serverless oferece:
- **Escala sob demanda**: os recursos escalam automaticamente conforme a demanda
- **Cobrança por uso**: o custo acompanha o uso real
- **Menos carga operacional**: não há provisionamento nem aplicação de patches em servidores
- **Resiliência integrada**: os serviços gerenciados incluem redundância e failover

---

## Componentes de segurança

### 1. Autenticação por token Bearer

Todas as rotas (`POST /v1/feedback`, `GET /v1/feedback` e `POST /v1/graphql`) exigem o header `Authorization: Bearer <token>`. O token é validado dentro dos handlers Lambda, então a mesma regra vale localmente e em produção.

- **Origem do token**: um único token compartilhado, guardado como segredo do GitHub `API_TOKEN` e passado ao deploy pelo parâmetro `ApiToken` (`NoEcho`, de 24 a 128 caracteres: letras, dígitos, `_` e `-`). As Lambdas leem o valor da variável de ambiente `API_TOKEN`.
- **Criação do segredo**: `openssl rand -hex 24 | tee >(pbcopy) | gh secret set API_TOKEN --repo jeffesa/12ADJT-fase-4`. O GitHub não mostra o valor de novo, então guarde uma cópia.
- **Rotação**: troque o segredo `API_TOKEN` e rode um novo deploy. O token antigo deixa de funcionar depois do deploy.
- **Respostas**: `401` com `Token Bearer ausente` (header ausente, vazio ou com esquema diferente) ou `Token Bearer inválido` (token não confere), ambos com `WWW-Authenticate: Bearer`. Se `API_TOKEN` não estiver configurado, toda requisição é recusada com `500` (falha fechada).
- **Tratamento**: a comparação é feita em tempo constante, e o token nunca é gravado em log. Nunca faça commit do token no controle de versão.

### 2. Papéis IAM e acesso de privilégio mínimo

Cada função Lambda roda com um papel IAM próprio, com permissões restritas aos recursos e ações de que precisa. Isso segue o princípio do privilégio mínimo e limita o impacto caso uma função seja comprometida.

#### Papel 1: ReceiveFeedback

**Finalidade**: recebe os feedbacks pelo API Gateway, valida a entrada, grava no DynamoDB e publica feedbacks urgentes (urgency=HIGH) no SNS.

**Permissões**:
- `dynamodb:PutItem` na tabela FeedbacksTable do DynamoDB
- `sns:Publish` no tópico FeedbackUrgentTopic do SNS
- `logs:*` (via AWSLambdaBasicExecutionRole) para o CloudWatch Logs

**Justificativa do privilégio mínimo**:
- Acesso somente de escrita ao DynamoDB (sem leitura, exclusão ou atualização)
- Publicação no SNS limitada ao tópico de feedback urgente
- Sem acesso a outros serviços ou recursos fora do seu escopo

#### Papel 2: SendNotification

**Finalidade**: consome os eventos de feedback urgente do SNS e envia e-mails de notificação aos administradores pelo AWS SES.

**Permissões**:
- `ses:SendEmail` e `ses:SendRawEmail` em todos os recursos
- `logs:*` (via AWSLambdaBasicExecutionRole) para o CloudWatch Logs

**Justificativa do privilégio mínimo**:
- Somente ações de e-mail; sem acesso a banco de dados nem ao SNS
- Não pode ler, alterar nem excluir dados de feedback
- Restrito às operações de envio de e-mail

#### Papel 3: GenerateReport

**Finalidade**: executado semanalmente (segunda-feira, 08:00 UTC) para ler os feedbacks do DynamoDB, agregar estatísticas e enviar relatórios pelo SES.

**Permissões**:
- `dynamodb:Scan` e `dynamodb:Query` na tabela FeedbacksTable do DynamoDB
- `ses:SendEmail` e `ses:SendRawEmail` em todos os recursos
- `logs:*` (via AWSLambdaBasicExecutionRole) para o CloudWatch Logs

**Justificativa do privilégio mínimo**:
- Acesso somente de leitura ao DynamoDB (sem escrita, exclusão ou atualização)
- Pode apenas fazer scan ou query, sem alterar a estrutura da tabela
- Envio de e-mail limitado à entrega de relatórios

### 3. Logs e monitoramento no CloudWatch

Cada função Lambda grava logs em um grupo de logs próprio do CloudWatch, com:

- **Grupo de logs**: `/aws/lambda/{environment}-{function-name}`
- **Retenção**: 30 dias (limpeza automática de logs antigos)
- **Níveis de log**:
  - INFO: entrada e saída dos handlers e operações principais
  - FINE: informações operacionais detalhadas (dados de requisição e resposta)
  - SEVERE: erros e detalhes de exceções

**Recursos de monitoramento**:
- Todas as invocações Lambda são registradas no CloudWatch
- Os erros são capturados com o contexto completo da exceção
- As consultas do Log Insights permitem diagnóstico e trilhas de auditoria

---

## Limitação de taxa

A API não usa Usage Plan nem API Key, e não há limite de taxa por cliente. A proteção contra acesso indevido é o token Bearer, exigido em todas as rotas. Os limites de taxa padrão da conta AWS continuam valendo. Um limite por cliente é uma melhoria futura.

---
## Separação de ambientes (dev/prod)

A infraestrutura oferece ambientes de desenvolvimento e de produção isolados, controlados pelo parâmetro `Environment`:

### Nomenclatura dos recursos
Todos os recursos recebem o nome do ambiente como prefixo, para garantir isolamento completo:
- Tabela DynamoDB: `{environment}-feedback-table`
- Tópico SNS: `{environment}-feedback-urgent-topic`
- Funções Lambda: `{environment}-{function-name}`
- Grupos de logs: `/aws/lambda/{environment}-{function-name}`
- API Gateway: `{environment}-feedback-api`

### Implantações separadas
- **Desenvolvimento (dev)**: usado para testes, experimentos e validação de integração
- **Produção (prod)**: ambiente real, atendendo usuários reais

### Verificação de e-mail (SES)
- Em produção, o parâmetro `AdminEmail` precisa apontar para um e-mail verificado no Amazon SES
- Em desenvolvimento, qualquer e-mail pode ser usado para testes

### Parâmetros de deploy
```yaml
Parameters:
  Environment:
    AllowedValues:
      - dev
      - prod
  AdminEmail:
    Description: Email for notifications (must be verified in SES for prod)
  Region:
    Default: us-east-1
```

---

## Segurança de dados e controle de acesso

### Criptografia no DynamoDB
- Todos os dados em repouso no DynamoDB são criptografados por padrão, com chaves gerenciadas pela AWS
- A recuperação point-in-time está desativada nesta configuração (pode ser ativada se for necessária para recuperação de desastres)

### Segurança de rede
- Toda a comunicação entre as Lambdas e os serviços da AWS ocorre em redes privadas da AWS
- O API Gateway exige HTTPS em todas as requisições recebidas (TLS 1.2+)
- Nenhum dado é transmitido por canais sem criptografia

### Trilha de auditoria de acesso
- O CloudWatch Logs registra todas as invocações de funções e os erros
- A entrega de mensagens do SNS é registrada em log
- O acesso ao DynamoDB fica registrado de forma implícita nos logs das Lambdas (via CloudWatch)

---

## Boas práticas de segurança implementadas

1. **Papéis IAM com privilégio mínimo**: cada Lambda tem somente as permissões necessárias
2. **Autenticação por token Bearer**: todas as requisições à API exigem `Authorization: Bearer <token>`
3. **Proteção contra abuso**: o token Bearer é obrigatório em todas as rotas (não há Usage Plan; ver Limitação de taxa)
4. **Criptografia**: os dados em repouso (DynamoDB) e em trânsito (HTTPS) são criptografados
5. **Log de auditoria**: o CloudWatch Logs registra todas as operações para conformidade e diagnóstico
6. **Isolamento de ambientes**: stacks separadas para dev e prod impedem interferência entre ambientes
7. **Tratamento de erros**: os erros são registrados em log sem expor detalhes internos aos clientes

---

## Implantação e configuração

### Pré-requisitos
1. Conta AWS com as permissões necessárias
2. AWS SAM CLI instalado
3. Endereço de e-mail verificado no SES (para implantações em produção)
4. Bucket S3 para armazenar o código empacotado das Lambdas

### Passos de implantação
```bash
# Compilar todas as funções Lambda
sam build

# Empacotar (substitua o nome do bucket S3)
sam package --s3-bucket <your-s3-bucket> --output-template-file packaged.yaml

# Implantar no ambiente dev
sam deploy --template-file packaged.yaml \
  --parameter-overrides Environment=dev AdminEmail=dev@example.com \
  --stack-name fiap-fase4-dev

# Implantar no ambiente prod
sam deploy --template-file packaged.yaml \
  --parameter-overrides Environment=prod AdminEmail=admin@example.com \
  --stack-name fiap-fase4-prod
```

### Configuração específica de cada ambiente
- Atualize o parâmetro `AdminEmail` em cada ambiente
- Garanta que o e-mail remetente do SES esteja verificado em produção

---

## Resposta a incidentes

### Monitoramento e alertas
- Os logs do CloudWatch são retidos por 30 dias
- Os logs devem ser monitorados quanto a mensagens de nível ERROR e SEVERE
- Considere ativar alarmes do CloudWatch para:
  - Taxas de erro das Lambdas
  - Erros 5xx do API Gateway
  - Throttling do DynamoDB

### Análise de logs
```bash
# Exemplo: consultar erros recentes no CloudWatch Logs Insights
fields @timestamp, @message, @logStream
| filter @message like /ERROR|SEVERE/
| stats count() by @logStream
```

### Recuperação
- As funções Lambda não mantêm estado e se recuperam automaticamente de falhas transitórias
- A política de retentativa do SNS (padrão de 3 tentativas) garante que as notificações urgentes sejam entregues
- Relatórios agendados podem ser disparados manualmente invocando a GenerateReportFunction pelo console do AWS Lambda

---

## Considerações de conformidade

- **Retenção de dados**: o DynamoDB e o CloudWatch Logs são retidos conforme a configuração
- **Trilha de auditoria**: todas as operações são registradas no CloudWatch para auditorias de conformidade
- **Criptografia**: os dados em repouso e em trânsito atendem aos requisitos de segurança padrão
- **Controle de acesso**: os papéis IAM impõem acesso com privilégio mínimo
- **Separação de ambientes**: as cargas de produção e de desenvolvimento são isoladas

---

## Contato e suporte

Para questões de segurança ou vulnerabilidades, entre em contato imediatamente com a equipe de desenvolvimento. Não divulgue problemas de segurança publicamente.
