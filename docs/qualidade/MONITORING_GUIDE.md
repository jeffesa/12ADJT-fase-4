# Guia de Monitoramento

Este guia explica como acompanhar as funções serverless da plataforma de feedback: onde estão os logs, quais métricas e alarmes existem, como testar os alarmes e como ler as mensagens de log.

Ambiente de referência: produção (`prod`), conta AWS `fase4`, região `sa-east-1`. O stack `dev` existe, mas o pipeline só faz deploy em produção.

Legenda dos comandos:
- **verificado (leitura)**: executado contra a conta de produção, somente leitura, com a saída conferida.
- **sintaxe verificada, não executado**: a sintaxe foi conferida, mas o comando não rodou porque altera estado, dispara uma função ou depende de um recurso que ainda não existe.

---

## 1. O que é monitorado

A plataforma tem três fontes de sinal:

| Fonte | O que cobre | Onde fica |
|---|---|---|
| Logs | Cada invocação das funções, mensagens INFO, WARNING e SEVERE, e a linha `REPORT` no fim de cada invocação | CloudWatch Logs, retenção de 30 dias. Há um grupo por função, exceto `receive-feedback`, que também recebe os logs de `list-feedback` |
| Métricas nativas do Lambda | Invocações, duração, erros, throttles e concorrência de cada função | Namespace `AWS/Lambda` no CloudWatch |
| Alarmes | Três alarmes, um para cada grupo com código de erro (receive-feedback, send-notification e generate-report). Disparam quando aparece pelo menos uma linha SEVERE em 5 minutos | CloudWatch Alarms, com notificação por e-mail via SNS |

Não existe dashboard do CloudWatch configurado no projeto. Para ver os números, use o console ou os comandos deste guia.

---

## 2. Logs por função

### Grupos de log

| Grupo de log | Função que grava | Observação |
|---|---|---|
| `/aws/lambda/prod-receive-feedback` | `prod-receive-feedback` (POST /v1/feedback) e `prod-list-feedback` (GET /v1/feedback) | As duas funções gravam no mesmo grupo |
| `/aws/lambda/prod-graphql` | `prod-graphql` (POST /v1/graphql) | Sem alarme (ver seção 7) |
| `/aws/lambda/prod-send-notification` | `prod-send-notification` (disparada pelo tópico SNS de urgência) | |
| `/aws/lambda/prod-generate-report` | `prod-generate-report` (agendada, segunda-feira 8h UTC) | Não tem registros em produção até agora |

Todos os grupos têm retenção de 30 dias. Conferido em `aws logs describe-log-groups` (verificado, leitura).

### Ver logs recentes

```bash
aws logs tail /aws/lambda/prod-receive-feedback --since 1h --region sa-east-1 --profile fase4
```

Troque o nome do grupo para ver as outras funções. Para filtrar só erros:

```bash
aws logs filter-log-events --log-group-name /aws/lambda/prod-receive-feedback --filter-pattern SEVERE --region sa-east-1 --profile fase4
```

Ambos os comandos são **verificado (leitura)**.

---

## 3. Métricas

### Métricas nativas do Lambda

O namespace `AWS/Lambda` traz, por função:

- `Invocations`: número de invocações.
- `Errors`: invocações que terminaram com exceção não tratada.
- `Duration`: tempo de execução.
- `Throttles`: invocações recusadas por limite de concorrência.
- `ConcurrentExecutions`: execuções simultâneas.

Exemplo para ver os erros nativos de `prod-receive-feedback` na última hora (**sintaxe verificada, não executado**):

```bash
aws cloudwatch get-metric-statistics --namespace AWS/Lambda --metric-name Errors --dimensions Name=FunctionName,Value=prod-receive-feedback --statistics Sum --period 300 --start-time <inicio-ISO8601> --end-time <fim-ISO8601> --region sa-east-1 --profile fase4
```

### Por que o alarme não usa `Errors`

As três funções capturam as exceções e não as relançam. Por exemplo, `SendNotificationHandler` e `GenerateReportHandler` gravam um SEVERE e retornam normalmente. Nesses casos a métrica `Errors` continua em zero, mesmo com falha de envio. Por isso os alarmes contam linhas SEVERE nos logs, não a métrica `Errors`.

`prod-receive-feedback` responde 500 nas falhas, sem relançar a exceção. Pela mesma razão, `Errors` não mostra essas falhas.

### Cold start (Init Duration)

Não existe métrica de cold start no Lambda. A informação aparece na linha `REPORT` que o Lambda grava no fim de cada invocação. Quando a invocação inicializa um ambiente novo, a linha traz `Init Duration`. Invocações em ambiente já aquecido não trazem esse campo. Conferido em produção: nas últimas linhas `REPORT` de `prod-send-notification` e `prod-receive-feedback`, algumas trazem `Init Duration` e outras não. As que não trazem rodaram em ambiente já iniciado (verificado, leitura).

Para listar as linhas `REPORT` que trazem `Init Duration` (**verificado (leitura)**):

```bash
aws logs filter-log-events --log-group-name /aws/lambda/prod-send-notification --filter-pattern '"Init Duration"' --region sa-east-1 --profile fase4
```

Para ver todas as linhas `REPORT` (duração, memória e cold start):

```bash
aws logs filter-log-events --log-group-name /aws/lambda/prod-receive-feedback --filter-pattern '"REPORT"' --region sa-east-1 --profile fase4
```

---

## 4. Alarmes configurados

O template `infra/template.yaml` cria um tópico SNS de alarmes, três filtros de métrica e três alarmes. Os alarmes só existem depois do deploy. Antes disso, `aws cloudwatch describe-alarms --alarm-name-prefix prod-` retorna lista vazia (verificado, leitura).

| Alarme | Métrica | O que mede | Quando dispara | O que fazer |
|---|---|---|---|---|
| `prod-severe-errors-receive-feedback` | `FeedbackPlatform/prod` / `SevereErrors-ReceiveFeedback` | Linhas SEVERE no grupo `prod-receive-feedback` | Pelo menos 1 linha SEVERE em 5 minutos | Ver o log com `aws logs tail /aws/lambda/prod-receive-feedback --since 1h --region sa-east-1 --profile fase4`. Descrição com menos de 5 caracteres também gera SEVERE (seção 7). |
| `prod-severe-errors-send-notification` | `FeedbackPlatform/prod` / `SevereErrors-SendNotification` | Linhas SEVERE no grupo `prod-send-notification` | Pelo menos 1 linha SEVERE em 5 minutos | Ver o log e checar a mensagem `Erro do SES ao enviar e-mail`. Na produção observada, a causa foi identidade de e-mail não verificada no SES (seção 7). |
| `prod-severe-errors-generate-report` | `FeedbackPlatform/prod` / `SevereErrors-GenerateReport` | Linhas SEVERE no grupo `prod-generate-report` | Pelo menos 1 linha SEVERE em 5 minutos | Ver o log e checar `Falha na geração do relatório` ou `Erro ao enviar relatório`. |

Configuração comum dos alarmes:
- Estatística `Sum`, período de 300 segundos, 1 período de avaliação, limite 1, comparação `>=`.
- `TreatMissingData: notBreaching`: sem log no período, o alarme fica em OK.
- Ação: publicação no tópico SNS `prod-monitoring-alarms-topic`, que envia e-mail para `AdminEmail`.
- Os filtros usam o padrão `SEVERE` (texto simples), porque os logs de produção são texto, não JSON. Conferido com `aws logs filter-log-events --filter-pattern SEVERE` nos grupos `prod-receive-feedback` e `prod-send-notification`, que retornaram linhas SEVERE (verificado, leitura).

O tópico `prod-feedback-urgent-topic` é diferente. Ele dispara `prod-send-notification` para enviar o feedback de urgência. Os alarmes não usam esse tópico, para não gerar e-mails de feedback falsos.

### Custo

- A cobrança de alarme é por alarme, pelas horas em que ele existe. Não é cobrada por disparo. Dez erros em um alarme continuam custando um alarme.
- Cada métrica de filtro conta como métrica personalizada. Os três alarmes e as três métricas deste projeto ficam dentro do free tier do CloudWatch, que inclui 10 métricas de alarme e 10 métricas personalizadas por mês.
- Logs: o free tier inclui 5 GB por mês de ingestão, armazenamento e consultas.
- Fora do free tier, a cobrança unitária de alarme e de métrica personalizada deve ser conferida na calculadora de preços da região `sa-east-1`. Os valores unitários não foram lidos na página oficial nesta revisão.
- O custo das notificações de e-mail pelo SNS não foi medido neste projeto.

Fonte: [página de preços do CloudWatch](https://aws.amazon.com/cloudwatch/pricing/), que confirma o free tier e a cobrança por hora de alarme. Para os valores unitários, consulte a [calculadora de preços da AWS](https://calculator.aws/).

---

## 5. Como testar os alarmes

### Passo 0: confirmar a assinatura de e-mail

Depois do deploy, a AWS envia um e-mail de confirmação para `AdminEmail`. Sem clicar no link, nenhum alarme chega ao e-mail.

Para ver a assinatura (**sintaxe verificada, não executado**):

```bash
aws sns list-subscriptions-by-topic --topic-arn <arn-do-AlarmsTopic> --region sa-east-1 --profile fase4
```

O status precisa ser diferente de `PendingConfirmation`.

### Passo 1: conferir que os alarmes existem

```bash
aws cloudwatch describe-alarms --alarm-name-prefix prod-severe-errors --region sa-east-1 --profile fase4
```

**Sintaxe verificada, não executado** (depende do deploy). Antes do deploy, a lista volta vazia.

### Passo 2: forçar o alarme para ALARM sem erro real

Este teste só confirma que o e-mail chega. Ele não cria erro nem log.

```bash
aws cloudwatch set-alarm-state --alarm-name prod-severe-errors-receive-feedback --state-value ALARM --state-reason "teste manual, sem erro real" --region sa-east-1 --profile fase4
```

**Sintaxe verificada, não executado.** Só funciona com o alarme já criado. Este comando envia e-mail quando a assinatura estiver confirmada.

O alarme volta ao estado calculado na próxima avaliação, em até 5 minutos. Para voltar antes, use o mesmo comando com `--state-value OK`.

### Passo 3: disparo real (só com autorização)

**Só com autorização. Este teste gera um SEVERE em produção, e o alarme envia um e-mail ao administrador depois que a assinatura estiver confirmada.**

Uma requisição com JSON inválido não serve para este teste. O JSON inválido gera WARNING (`Corpo da requisição inválido`), e o alarme conta só SEVERE. Para gerar SEVERE, use uma descrição com menos de 5 caracteres e um corpo JSON válido. A validação recusa a descrição e o handler registra SEVERE com status 500.

```bash
curl -X POST "<ApiEndpoint>" -H "Authorization: Bearer $API_TOKEN" -H "Content-Type: application/json" -d '{"description":"abc","rating":5}'
```

**Sintaxe verificada, não executado.** Guarde o token em variável de ambiente, sem colar o valor em logs, histórico compartilhado ou documentação. Substitua `<ApiEndpoint>` pela URL do output `ApiEndpoint` do stack (`aws cloudformation describe-stacks`). Não use a URL do README: ela aponta para `us-east-1`, e os logs de produção estão em `sa-east-1`.

Depois do disparo, confira o log:

```bash
aws logs tail /aws/lambda/prod-receive-feedback --since 10m --region sa-east-1 --profile fase4
```

A mensagem esperada no log é `SEVERE ... Erro: Descrição deve ter no mínimo 5 caracteres`, que vem do código atual (`ReceiveFeedbackUseCase`). Os logs de testes anteriores à tradução mostravam a versão em inglês dessa mensagem (verificado, leitura).

### Passo 4: conferir o alarme

```bash
aws cloudwatch describe-alarm-history --alarm-name prod-severe-errors-receive-feedback --region sa-east-1 --profile fase4
```

**Sintaxe verificada, não executado.** O histórico mostra a transição para ALARM e a notificação.

---

## 6. Como ler os logs de cada função

Todas as mensagens abaixo existem no código. As citações de produção vêm de logs reais de 2026-11-10.

### send-notification

Fluxo normal, na ordem:

1. `Handler Lambda invocado para envio de notificações` (INFO)
2. `Enviando notificação por e-mail para o feedback <id> para <e-mail>` (INFO). Esta linha só aparece quando o envio começa.
3. `Handler Lambda concluído` (INFO). Esta linha também aparece quando o envio falhou, porque o handler captura a exceção. Ela mostra que a invocação terminou, não que o e-mail foi enviado. Para saber se o envio deu certo, procure SEVERE na mesma invocação.

Falha de envio via SES:

- `Erro do SES ao enviar e-mail para o feedback <id>: <mensagem>` (SEVERE). Em produção, a mensagem do SES disse que o endereço não estava verificado.
- `Falha ao enviar notificação para o feedback <id>: <mensagem>` (SEVERE), gerada pela camada de caso de uso.
- `Falha no envio da notificação: <mensagem>` (SEVERE), gerada pelo handler.

Uma única falha gera as três linhas SEVERE. O alarme conta linhas, não falhas. Por isso uma falha pode ser contada mais de uma vez.

Outras linhas SEVERE:

- `Erro inesperado no handler Lambda: <mensagem>` (SEVERE), do tratamento geral do handler.

### generate-report

Fluxo normal, na ordem:

1. `Handler Lambda invocado para geração e envio do relatório` (INFO)
2. `Período do relatório calculado: início=..., fim=...` (FINE). Esta linha só aparece porque o nível `com.fiap` está em DEBUG (`generate-report/src/main/resources/application.properties`, linha 14). Para vê-la, o nível de log precisa estar nesse valor.
3. `Geração e envio do relatório concluídos com sucesso` (INFO)

Falhas:

- `Falha na geração do relatório (ReportException): <mensagem>` (SEVERE), gerada pelo handler.
- `Erro ao gerar relatório: <mensagem>` (SEVERE), gerada pelo caso de uso.
- `Erro ao enviar relatório <id> para <e-mail>: <mensagem>` (SEVERE), gerada pelo envio de e-mail.
- `Erro inesperado no handler Lambda: <mensagem>` (SEVERE), para exceções não previstas.

O grupo `prod-generate-report` não tem logs em produção até agora (`storedBytes: 0`, verificado, leitura). Os textos acima foram conferidos no código, não em produção.

### receive-feedback (POST e GET)

- `Handler Lambda invocado - POST /v1/feedback` (INFO) ou `Handler Lambda invocado - GET /v1/feedback` (INFO).
- `Corpo da requisição inválido: <mensagem>` (WARNING). Não gera alarme.
- `Erro: <mensagem>` (SEVERE), com a exceção. Gera alarme e resposta 500.
- `API_TOKEN não configurado; rejeitando todas as requisições` (SEVERE). Indica que a variável `API_TOKEN` está vazia no deploy.

### graphql

- `Handler Lambda invocado - POST /graphql (GraphQL interno)` (INFO).
- `Erro no handler GraphQL: <mensagem>` (SEVERE), com a exceção. O código grava esta linha, mas não há alarme para este grupo (seção 7).

---

## 7. Limitações

- **Sem dashboard.** O projeto não cria dashboard do CloudWatch. Para acompanhar os números, use os comandos deste guia ou o console.
- **Sem reserved concurrency.** As funções não têm limite de concorrência reservada, então throttling depende do limite da conta.
- **Sem log estruturado em JSON.** Os logs são texto. Os filtros usam o padrão `SEVERE`, que casa com a linha de texto.
- **X-API-Key, Usage Plan e Docker foram removidos.** O guia não descreve esses itens. A autenticação é feita por Bearer token.
- **Descrição com menos de 5 caracteres gera SEVERE.** O `ReceiveFeedbackUseCase` lança `IllegalArgumentException` com a mensagem `Descrição deve ter no mínimo 5 caracteres`. O handler registra SEVERE e responde 500. Por isso o alarme `prod-severe-errors-receive-feedback` também dispara por erro do cliente. Campos inválidos pelo Bean Validation, como JSON malformado ou nota fora de 0 a 10, respondem 400 com WARNING e não disparam o alarme. Corrigir o status para 400 exige mudar `ReceiveFeedbackHandler.java`, o que não faz parte do monitoramento.
- **GraphQL sem alarme.** O código de `prod-graphql` grava SEVERE (`GraphQLHandler.java`, linha 134), mas nenhuma linha SEVERE apareceu na janela consultada. O projeto configura só três alarmes, e este grupo não tem alarme.
- **Alarme de relatório sem teste real.** O grupo `prod-generate-report` não tem logs em produção. O padrão SEVERE desse grupo nunca foi visto em produção. Depois do deploy, teste com o passo 2 ou 3 da seção 5.
- **Sem alarme para erros 5xx do API Gateway.** Os alarmes contam logs das funções, não respostas HTTP.
- **Envio de e-mail depende das identidades do SES.** Em produção, o log mostra que o endereço remetente e o endereço do administrador não estão verificados no SES. Até que as identidades estejam verificadas no SES de `sa-east-1`, o alarme de send-notification dispara no primeiro envio. Verifique as identidades antes do deploy ou aceite esse alarme como ruído.
