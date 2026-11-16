# Auditoria final de código: padrão de idiomas

**Veredito: FAIL**

**Data e hora da varredura:** 2026-11-15 21:29:12 (-03:00), saída de `date` no início da revisão 2 da etapa.
**Branch:** `docs/code-audit-final-3`, criada a partir de `chore/email-messages-pt` no commit `1ed4ffb`.
**Base de referência:** conteúdo do relatório da branch `docs/code-audit-final-2` (commit `255c83f`), usado como ponto de partida e reexecutado contra o estado atual desta branch.
**Build:** `mvn -B clean verify` na raiz do projeto, JDK 17.0.18, Maven 3.9.11, `BUILD SUCCESS`.

Este relatório substitui a versão anterior. Nesta branch, os textos de e-mail e de assunto SNS já estão em português, e a query `listarAvaliacoes` não existe mais em código. Mesmo assim, há mensagens em inglês em documentos vivos e nos testes da collection (seção 7).

---

## 1. Regras da auditoria

| Tema | Regra | Resultado |
|---|---|---|
| Código e identificadores | Em inglês (`*.java`, `*.properties`, `*.xml`, `*.yml`, `*.yaml`, nomes de arquivo) | PASS |
| Comentários e Javadoc | Em inglês | PASS (ver observação em 5.3) |
| Mensagens para pessoas em código | Em português: e-mails, assunto SNS, alarmes, menus, workflows, POM, nomes de requisição da collection, `.md` | PASS no código |
| Logs | Em português | PASS para textos escritos no código (ver 6.1) |
| Mensagens de API (`detail`, `title`) | Em português | PASS no código. Documentação e testes da collection com mensagens em inglês: FAIL (seção 7) |
| Identificadores de contrato externo | Nenhum pode permanecer, exceto as exceções documentadas: rota `/avaliacao`, query `listarAvaliacoes`, chaves `descricao`, `nota`, `urgencia`, `dataCriacao`, `avaliacoes` | PASS em código e configuração. FAIL em documentos vivos: nenhum restante após a etapa (ver 7.1) |
| Arquivos de cópia de trabalho | `infra/template.yaml.bak` e `infra/template.yaml.orig` não podem estar rastreados | FAIL: ambos continuam rastreados (seção 8) |

---

## 2. Comandos de varredura

Todos os comandos rodam com `R=/Users/jefferson/Documents/FIAP/fase-4/.worktrees/code-audit-final-3`, usando `git -C "$R"` e caminho absoluto. A busca usa `git grep`, que funciona no macOS. Não usa `\b`. Quando é preciso fronteira de palavra, usa `[[:<:]]` ou `[^A-Za-z]`.

```bash
# Arquivos rastreados e contagem por extensão
git -C "$R" ls-files | wc -l
git -C "$R" ls-files | sed -E 's/.*\.([^./]+)$/\1/' | sort | uniq -c | sort -rn

# Arquivos de cópia de trabalho (deve retornar vazio)
git -C "$R" ls-files | grep -E 'template\.yaml\.(bak|orig)'

# Identificadores de contrato em código e configuração
git -C "$R" grep -n -I -i -E 'avaliacao|listarAvaliacoes|descricao|urgencia|dataCriacao|avaliacoes|[[:<:]]nota[[:>:]]' -- '*.java' '*.properties' '*.xml' '*.yml' '*.yaml' '*.json' '*.sh' '*.toml'

# Identificadores de contrato em documentação (sem o próprio relatório)
git -C "$R" grep -c -I -E '/avaliacao|listarAvaliacoes|criarAvaliacao|dataCriacao|avaliacoes|descricao|[Uu]rgencia|Urgencia' -- '*.md' ':!docs/qualidade/CODE_AUDIT.md'

# Literais JSON legados em código de produção
git -C "$R" grep -n -I -E '"(descricao|nota|urgencia|dataCriacao)"' -- '*/src/main/*'

# Chaves legadas em testes
git -C "$R" grep -n -I -E 'descricao|"nota"' -- 'receive-feedback/src/test/*'

# Palavras em inglês dentro de literais de código de produção
git -C "$R" grep -n -I -E '"[^"]*[[:<:]](Feedback|Report|Notification|Urgency|Description|Rating|Error|Total|Average|Date|Period|Count|Admin|Email|Subject|Alert|Failed|Please|Critical|Received|High|Low|Medium)[[:>:]][^"]*"' -- '*/src/main/*.java'

# Mensagens em inglês em documentação e collection
git -C "$R" grep -n -I -E 'must be|cannot be|at least|Invalid request body|Description must|Email address|not verified|Missing bearer|Invalid bearer|Unauthorized|Validation Error|Rating must' -- 'docs/*' 'README.md' '*.md' ':!docs/qualidade/CODE_AUDIT.md'

# Mensagens em inglês citadas em código de produção (deve retornar só o que o código atual emite)
git -C "$R" grep -n -I -E 'Invalid request body|at least 5|Description must' -- '*/src/main/*'

# Caracteres acentuados e comentários Java
git -C "$R" grep -c -I -E '[áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]' -- '*.java'
```

Comandos de documentação com mensagens (seção 7 do relatório):

```bash
# Mensagens em inglês em documentação e collection (saída na seção 7.1)
git -C "$R" grep -n -I -E 'must be|cannot be|at least|Invalid request body|Description must|Email address|not verified|Missing bearer|Invalid bearer|Unauthorized|Validation Error|Rating must' -- 'docs/*' 'README.md' '*.md' ':!docs/qualidade/CODE_AUDIT.md'

# Identificadores legados em código de produção, sem literais JSON (deve retornar vazio)
git -C "$R" grep -n -I -E '"(descricao|nota|urgencia|dataCriacao)"|listarAvaliacoes|/avaliacao' -- '*/src/main/*'

# Ocorrências amplas que incluem palavras comuns em português (mensagens e texto de API, não identificadores)
git -C "$R" grep -n -I -i -E 'avaliacao|listarAvaliacoes|descricao|urgencia|dataCriacao|avaliacoes|[[:<:]]nota[[:>:]]' -- '*/src/main/*'
```

Na varredura ampla de código, as ocorrências são frases em português (`Nota deve ser no mínimo 0`, `Alerta de feedback de urgencia alta`, `Nota média:`). Elas são mensagens, e não identificadores. As únicas chaves legadas em teste são as de exceção (a).

Contagem de ocorrências por arquivo em documentação, com `git -C "$R" grep -c`: ver seção 7.2.

Esta revisão não alterou código. O `DEMO_GUIDE.md` e a `api-collection/README.md` foram traduzidos apenas na prosa. Os blocos de código foram restaurados ao texto original da branch `chore/email-messages-pt`. As 30 cercas do `DEMO_GUIDE.md` e as 18 da `api-collection/README.md` são idênticas à base.

---

## 3. Arquivos varridos por extensão

Contagem com `git ls-files` nesta branch. Não houve remoção de arquivos nesta etapa.

| Extensão | Arquivos | Escopo da regra |
|---|---|---|
| `java` | 115 | Código: identificadores, comentários, nomes de arquivo |
| `md` | 28 | Documentação: português obrigatório, identificadores de contrato e mensagens sujeitos à seção 7 |
| `properties` | 6 | Configuração: chaves e comentários em inglês. Textos de e-mail em português |
| `xml` | 5 | POM: `<description>` em português permitido |
| `yml` | 3 | Configuração e workflows |
| `yaml` | 3 | Configuração e template |
| `toml` | 2 | `build.toml`, `infra/samconfig.toml` |
| `sh` | 2 | `run.sh`, `monitor-stack.sh`: menus e mensagens em português |
| `json` | 2 | Collection e registro de review: nomes de requisição em português. Os testes da collection têm mensagens em inglês (seção 7.1) |
| `pdf` | 1 | Documento de entrega, fora da varredura |
| `orig` | 1 | `infra/template.yaml.orig`, rastreado (seção 8) |
| `bak` | 1 | `infra/template.yaml.bak`, rastreado (seção 8) |
| `gitignore` | 1 | Fora da varredura |
| `dockerignore` | 1 | Fora da varredura |

Total rastreado: 171 arquivos.

---

## 4. Nomes de arquivo

Os 115 arquivos `.java`, os 6 `.properties`, os 5 `.xml`, os 3 `.yml` e os 3 `.yaml` têm nome em inglês. Os nomes de documentos `.md` com termos em português (`BACKLOG.md`, `HANDOFF_SESSION.md`, `RESUMO_CONVERSA.md`, `TASK-019-AWS-DEPLOYMENT-BLOQUEADO.md`) estão fora da regra de nomes de código e são permitidos.

**Resultado: PASS.**

---

## 5. Identificadores e contrato em código

### 5.1 Identificadores e comentários

- Não há identificador em português. As ocorrências de `urgency` e `description` são palavras em inglês.
- Os campos JSON atuais são `description`, `rating`, `urgency`, `createdAt` e `id`, em inglês.
- Os valores `HIGH`, `MEDIUM` e `LOW` são valores do contrato de urgência.

**Resultado: PASS.**

### 5.2 Contrato legado em código e configuração

| Arquivo | Linha | Ocorrência | Situação |
|---|---|---|---|
| `receive-feedback/src/test/java/com/fiap/feedback/infra/handler/ReceiveFeedbackHandlerV1Test.java` | 83 | Corpo de requisição com `"descricao"` e `"nota"`, esperando 400 "Nota é obrigatória" | Exceção (a): teste negativo que confirma que a v1 rejeita o formato legado |
| `receive-feedback/src/test/java/com/fiap/feedback/infra/handler/ReceiveFeedbackHandlerV1Test.java` | 172 | `assertFalse(item.has("descricao"))` | Exceção (a): asserção negativa que confirma que a v1 não devolve a chave legada |

Nenhuma ocorrência em código de produção (`src/main`), `.properties`, `.xml`, `.yml`, `.yaml`, `infra/template.yaml`, `docs/api-collection/*.json`, `run.sh`, `monitor-stack.sh` ou `infra/samconfig.toml`. A rota atual é `/v1/feedback`.

**Resultado: PASS, com as duas exceções (a) documentadas.**

### 5.3 Comentários stale em Javadoc

Dois comentários Javadoc em inglês citam o assunto de e-mail com o texto antigo em inglês, mas o código envia o texto em português:

- `generate-report/src/main/java/com/fiap/report/infra/messaging/SesReportSender.java`, linha 37: `Subject: "Weekly Feedback Report — ..."`. O código usa `"Relatório semanal de feedback — ..."`.
- `generate-report/src/main/java/com/fiap/report/infra/messaging/ReportEmailBuilder.java`, linha 33: `"Weekly Feedback Report" title`. O código usa `Relatório semanal de feedback`.

A regra de comentários (em inglês) é atendida. Os comentários estão desatualizados, e não há mudança de comportamento. Não entram como FAIL. A correção fica como pendência opcional.

---

## 6. Logs, mensagens de API e textos para pessoas

### 6.1 Logs e exceções

- Os textos de log escritos no código estão em português. Termos técnicos (`Handler`, `Lambda`, `GraphQL`, `SNS`, `SES`, `DynamoDB`, `MessageId`, `receiveFeedback`, `listFeedbacks`) permanecem em inglês, como nomes de tecnologia ou de operação.
- Os e-mails e assuntos SNS estão em português: `Relatório semanal de feedback`, `Alerta de feedback de urgencia alta`, `Feedback crítico recebido`, `Total de feedbacks:`, `Nota média:`, `Descrição:`, `Urgência:`.
- Os rótulos `High`, `Medium` e `Low` no `Urgency` de `receive-feedback` e de `generate-report` são o parâmetro de construtor de `description`, e não são lidos em nenhum caminho que vá para pessoas. Não entram como FAIL.
- Os textos `toString()` (`Feedback{`, `Notification{`) são de depuração e não entram como FAIL.
- Observação, sem contagem como FAIL: `e.getMessage()` concatenado em logs e exceções produz texto da biblioteca ou do SDK da AWS. Esse texto pode estar em inglês.

**Resultado: PASS.**

### 6.2 Mensagens de API no código

Os valores de `title` e `detail` emitidos pelo código estão em português: `Erro de validação`, `Erro interno do servidor`, `Requisição inválida`, `Método não permitido`, `Não autorizado`, `Token Bearer ausente`, `Token Bearer inválido`, `Nota deve ser no mínimo 0`, `Descrição não pode estar vazia`.

**Resultado: PASS no código.**

---

## 7. Documentação e testes com mensagens em inglês

A regra de português para documentação permite o texto. A regra de mensagens de API exige que a documentação mostre a mensagem que a API realmente emite. Nesta branch, a API emite mensagens em português. Os textos abaixo não correspondem ao que o código emite.

### 7.1 Documentos vivos e testes com FAIL

| Arquivo | Linhas | Problema |
|---|---|---|
| `docs/API.md` | 39, 415, 417, 422, 424, 425, 426, 439, 441, 444, 684, 685 | Mensagens em inglês (`Missing bearer token`, `Invalid bearer token`, `Validation Error`, `Rating must be ...`, `Description cannot be empty`, `Unauthorized` como `title`). A API emite textos em português. |
| `docs/api-collection/API.md` | 92, 94, 126, 127, 142, 144, 150, 151 | `Validation Error`, `Rating must be between 0 and 10`, `Rating must be at least 0`, `Rating must be at most 10`. Mensagens que o código não emite. |
| `docs/SECURITY.md` | 24 | `Missing bearer token` e `Invalid bearer token` em inglês. |
| `docs/SECURITY.md` | 154 | Rótulo `Description:` em inglês dentro de exemplo de variável de ambiente. Palavra em inglês em documento vivo. |
| `docs/deploy/AWS_DEPLOY_GUIDE.md` | 396, 397, 437, 518, 519 | `Missing bearer token` e `Invalid bearer token` em tabelas de erro e em instruções. |
| `docs/api-collection/fase4-feedback.postman_collection.json` | 559, 596, 736, 769, 806, 845, 1422, 1459, 1599, 1632, 1669, 1708 | Os testes de 401 fazem `pm.expect(detail).to.equal('Missing bearer token')` e `'Invalid bearer token'`. O código devolve `Token Bearer ausente` e `Token Bearer inválido`. **Os testes não batem com a API atual.** Não rodei a collection contra a API. A divergência vem da leitura de `BearerTokenAuthenticator.java`, linhas 64 e 111. |
| `docs/qualidade/MONITORING_GUIDE.md` | 166, 180, 237, 254 | Cita `Invalid request body` e `Description must be at least 5 characters`. Esses textos não existem em `src/main`. O código emite `Descrição deve ter no mínimo 5 caracteres` e `Descrição não pode estar vazia`. A linha 254 também descreve um comportamento que o código atual não tem. |
| `docs/qualidade/MONITORING_GUIDE.md` | 206, 258 | Cita `Email address is not verified`, texto que a AWS devolve no SES e que aparece nos logs de produção. Trecho de terceiro, marcado como observação de produção. Mantido como FAIL pela regra estrita. |
| `docs/api-collection/README.md` | 145, 147, 263, 265, 272 | Exemplos de resposta de erro em bloco de código, com `Validation Error`, `Rating must be at least 0`, `Unauthorized`, `Missing bearer token \| Invalid bearer token`. Os blocos foram restaurados ao original, como pedido (traduzir só a prosa). Os exemplos batem com o texto em inglês da documentação, não com o que a API emite. Mantido como FAIL. Alternativa: traduzir esses quatro exemplos para o texto real da API (`Erro de validação`, `Nota deve ser no mínimo 0`, `Não autorizado`, `Token Bearer ausente \| Token Bearer inválido`). Isso muda só documentação, mas é uma tradução de bloco de código, fora do pedido. |
| `docs/qualidade/DEMO_GUIDE.md` | — | Sem mensagem em inglês da API. Os exemplos de faixa de nota (`Rating 0-5 → HIGH`) estão em bloco de código, restaurados ao original. |

Sobre os três guias pedidos nesta etapa:

- `docs/qualidade/DEMO_GUIDE.md`: prosa traduzida. `Urgencia` trocado por `urgency` nas duas ocorrências de prosa (linhas 400 e 409). Blocos de código idênticos à base.
- `docs/api-collection/README.md`: prosa traduzida. Blocos de código idênticos à base (ver linhas acima).
- `docs/qualidade/MONITORING_GUIDE.md`: não tinha prosa em inglês para traduzir. O arquivo já estava em português, e nenhuma linha foi alterada. As linhas com texto em inglês listadas acima são citações de mensagens, e ficam como FAIL.

### 7.2 Registros históricos (exceção b)

Estes arquivos são registros de testes, tarefas e sessões. Citam `/avaliacao`, `POST /avaliacao`, `listarAvaliacoes`, `criarAvaliacao`, `descricao`, `nota`, `urgencia` ou `dataCriacao`:

| Arquivo | Ocorrências | Tipo |
|---|---|---|
| `TEST_RESULTS.md` | 14 | Registro de testes do `/avaliacao`. Mensagens em inglês nas linhas 32–34 também são histórico. |
| `TASK_018_COMPLETION_SUMMARY.md` | 3 | Registro de tarefa |
| `docs/E2E_TEST_RESULTS.md` | 19 | Registro de testes |
| `docs/E2E_TEST_RESULTS_SAMPLE.md` | 26 | Registro de testes |
| `docs/TASK-019-AWS-DEPLOYMENT-BLOQUEADO.md` | 1 | Registro de deploy, cita `POST /avaliacao` |
| `docs/planejamento/BACKLOG.md` | 27 | Especificação de tarefas antigas. Não foi alterado, conforme pedido. |
| `docs/planejamento/HANDOFF_SESSION.md` | 3 | Guia de sessão. Inclui `descricao` em nomes de branch, falso positivo. |
| `docs/planejamento/RESUMO_CONVERSA.md` | 2 | Guia de sessão com `POST /avaliacao` |
| `docs/planejamento/SESSION_SUMMARY_20261026.md` | 1 | Registro de sessão com `/avaliacao` |
| `docs/planejamento/TASK-022-023-COMPLETION-CHECKLIST.md` | 7 | Checklist histórico com `criarAvaliacao` e `POST /avaliacao` |

A exceção (b) vem da regra da etapa de auditoria, que cita "relatórios, TASK-*, E2E_TEST_RESULTS*, CODE_AUDIT antigo, guias de sessão". Essa lista não está em nenhum arquivo do repositório. Por isso, a classificação de `BACKLOG.md` e `docs/planejamento/` como históricos é possível, e não confirmada. Se o responsável não aceitar essa leitura, esses arquivos viram FAIL.

### 7.3 Falsos positivos sem ação

- `README.md`: "descrição" e "nota" em português. Não são identificadores.
- `docs/api-collection/README.md`: "descrição" é texto em português.
- `docs/api-collection/fase4-feedback.postman_collection.json`: `urgência HIGH` e "nota de 0 a 5" em descrições de requisição são português.
- Dados de exemplo em comandos do `DEMO_GUIDE.md` (`Critical issue: System crashes on login` e similares) são payload de entrada, não mensagens de sistema.

### 7.4 Prosa em inglês fora do escopo

Os demais `.md` que não são os três guias desta etapa têm prosa em inglês que não é mensagem nem identificador (por exemplo, descrições de variáveis em `docs/API.md`, linha 651). Esta auditoria não os classifica. Se a regra de "`.md` em português" for estendida à prosa, a lista de FAIL cresce.

**Resultado: FAIL.** Veja 7.1 e 10.

---

## 8. Arquivos de cópia de trabalho

| Arquivo | Estado nesta branch | Verificação |
|---|---|---|
| `infra/template.yaml.bak` | Rastreado | `git ls-files` lista o arquivo |
| `infra/template.yaml.orig` | Rastreado | `git ls-files` lista o arquivo |

Nesta etapa não houve remoção. A remoção de arquivos rastreados está fora do escopo de documentação pedido, e exige decisão antes de ser feita.

**Resultado: FAIL.**

---

## 9. Testes

Comando: `mvn -B clean verify` na raiz do projeto, executado uma vez em modo não interativo. Log completo: `/tmp/code-audit-final-3-build.log`.

| Módulo | Testes | Falhas | Erros | Resultado |
|---|---|---|---|---|
| receive-feedback | 147 | 0 | 0 | SUCCESS |
| send-notification | 86 | 0 | 0 | SUCCESS |
| generate-report | 159 | 0 | 0 | SUCCESS |

Rodado de novo nesta revisão, depois das edições de documentação. Os números são os mesmos da primeira rodada. As edições são só em `.md`, e não alteram código. Log: `/tmp/code-audit-final-3-build.log`, `MVN_EXIT=0`.

O módulo `local-http` não faz parte do reactor da raiz e não foi executado.

---

## 10. Veredito e pendências

**Veredito: FAIL.**

Itens que bloqueiam o PASS:

1. **Testes de 401 da collection Postman** (seção 7.1): esperam `Missing bearer token` e `Invalid bearer token`, mas a API responde `Token Bearer ausente` e `Token Bearer inválido`. Decisão necessária: ajustar as asserções ao texto atual, ou mudar a API para a mensagem em inglês (o que muda o que o cliente vê e não é o escopo desta etapa).
2. **Mensagens em inglês em documentação**: `docs/API.md`, `docs/api-collection/API.md`, `docs/SECURITY.md`, `docs/deploy/AWS_DEPLOY_GUIDE.md` e os exemplos de erro de `docs/api-collection/README.md` (seção 7.1). Decisão aberta: manter o bloco original, ou traduzir esses exemplos para o texto real da API.
3. **`docs/qualidade/MONITORING_GUIDE.md`**, linhas 166, 180, 237 e 254: mensagens que o código não emite. Linhas 206 e 258: texto da AWS, marcado como FAIL pela regra estrita.
4. **Arquivos `infra/template.yaml.bak` e `infra/template.yaml.orig` rastreados** (seção 8). A remoção depende de confirmação.
5. **Classificação de `BACKLOG.md` e `docs/planejamento/`** como históricos (seção 7.2).

Itens aceitos como exceção:

- `ReceiveFeedbackHandlerV1Test.java`, linhas 83 e 172 (exceção a).
- Registros históricos da seção 7.2 (exceção b).

Nenhuma ocorrência de `listarAvaliacoes`, `criarAvaliacao` ou `/avaliacao` em código, configuração ou nos três guias desta etapa. A rota antiga aparece apenas nos registros históricos da seção 7.2.

Pendência de decisão, fora desta branch: a collection `fase4-feedback.postman_collection.json` espera mensagens em inglês que a API não emite. Escolhas: ajustar as asserções da collection, ou mudar a API. Nenhuma foi feita.

