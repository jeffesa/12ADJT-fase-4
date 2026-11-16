# Auditoria final de código: padrão de idiomas

**Veredito: PASS (com exceções documentadas)**

**Data e hora da varredura:** 2026-11-15 21:49:32 (-03:00), saída de `date`.
**Branch:** `docs/code-audit-final-3`, a partir de `chore/email-messages-pt` (`1ed4ffb`).
**Base de referência:** relatório da branch `docs/code-audit-final-2` (commit `255c83f`), usado como ponto de partida e reexecutado contra o estado atual.
**Build:** `mvn -B clean verify` na raiz (módulos `receive-feedback`, `send-notification`, `generate-report`) e `mvn -B -f local-http/pom.xml clean verify`, JDK 17, todos com `BUILD SUCCESS`.

Este relatório substitui a versão anterior. Ele aplica as decisões do responsável registradas em 2026-11-15:
- Documentos de planejamento (`docs/planejamento/*`, incluindo `BACKLOG.md`) ficam fora da varredura.
- Todos os `.md` em escopo ficam em português: prose traduzida, blocos de código, comandos, nomes de campos e rotas mantidos.
- Exemplos de erro usam o texto real da API.
- `infra/template.yaml.bak` e `infra/template.yaml.orig` foram removidos do git.
- Os registros históricos listados por nome (seção 7.2) continuam como exceção (b).

---

## 1. Regras da auditoria

| Tema | Regra | Resultado |
|---|---|---|
| Código e identificadores | Em inglês (`*.java`, `*.properties`, `*.xml`, `*.yml`, `*.yaml`, nomes de arquivo) | PASS |
| Comentários e Javadoc | Em inglês | PASS |
| Mensagens para pessoas em código | Em português: textos de e-mail, assunto de SNS, alarmes, menus, workflows, POM, nomes de requisição da collection, `.md` | PASS |
| Logs | Em português, e a documentação cita os logs com o texto atual do código | PASS |
| Mensagens de API (`detail`, `title`) | Em português no código e na documentação | PASS |
| Identificadores de contrato externo | Só as exceções (a) e (b) da seção 5 e 7.2 | PASS com exceções |
| Arquivos de cópia de trabalho | `infra/template.yaml.bak` e `.orig` não rastreados | PASS (seção 8) |
| Documentos `.md` em escopo | Prose em português | PASS (seção 7) |

---

## 2. Comandos de varredura

Todos os comandos rodam na raiz do worktree `R=/Users/jefferson/Documents/FIAP/fase-4/.worktrees/code-audit-final-3`, com `git -C` e caminho absoluto. A busca usa `git grep`, compatível com macOS. Não usa `\b`.

```bash
date
git -C "$R" rev-parse --short HEAD

# Arquivos rastreados e contagem por extensão
git -C "$R" ls-files | wc -l
git -C "$R" ls-files | sed -E 's/.*\.([^./]+)$/\1/' | sort | uniq -c | sort -rn

# Arquivos de cópia de trabalho (deve retornar vazio)
git -C "$R" ls-files | grep -E 'template\.yaml\.(bak|orig)'

# Contrato legado em código de produção (deve retornar vazio)
git -C "$R" grep -n -I -E '"(descricao|nota|urgencia|dataCriacao)"|listarAvaliacoes|/avaliacao' -- '*/src/main/*'

# Contrato legado nos testes
git -C "$R" grep -n -I -E 'descricao|"nota"|[[:<:]]nota[[:>:]]|urgencia|dataCriacao' -- '*/src/test/*'

# Mensagens em inglês em código, collection e documentação (deve retornar vazio)
git -C "$R" grep -n -I -E 'Missing bearer|Invalid bearer|Validation Error|"Unauthorized"|Rating must|Description cannot|Description must|must not be empty' -- '*.md' '*.json' 'README.md'

# Identificadores de contrato em .md, fora de planejamento e deste relatório
git -C "$R" grep -c -I -E '/avaliacao|listarAvaliacoes|criarAvaliacao|dataCriacao|avaliacoes|descricao|[Uu]rgencia' -- '*.md' ':!docs/planejamento/*' ':!docs/qualidade/CODE_AUDIT.md'

# Guia de demo sem o nome legado
git -C "$R" grep -c 'Urgencia' -- docs/qualidade/DEMO_GUIDE.md

# BACKLOG sem alteração em relação à base
git -C "$R" diff --quiet chore/email-messages-pt HEAD -- docs/planejamento/BACKLOG.md

# Código e configuração sem alteração em relação à base
git -C "$R" diff --stat chore/email-messages-pt HEAD -- '*.java' '*.properties' '*.xml' '*.yml' '*.yaml' '*.toml' '*.sh'

# Prose em inglês fora de blocos de código (script Python que ignora cercas ```)
# Lista apenas palavras em inglês; ocorrências de "status", "use" e "test" em português são falsos positivos.
```

Os scripts de busca de comentários e de identificadores em português da auditoria anterior continuam válidos. O código Java não mudou desde a varredura anterior, então esses resultados se mantêm.

---

## 3. Arquivos varridos por extensão

Contagem com `git ls-files` depois da remoção dos arquivos de cópia de trabalho.

| Extensão | Arquivos | Escopo da regra |
|---|---|---|
| `java` | 115 | Código: identificadores, comentários, nomes de arquivo |
| `md` | 28 | Documentação: prose em português; identificadores de contrato sujeitos à seção 7 |
| `properties` | 6 | Configuração: chaves e comentários em inglês; textos de e-mail em português |
| `xml` | 5 | POM: `<description>` em português |
| `yml` | 3 | Configuração e workflows |
| `yaml` | 3 | Configuração e template |
| `toml` | 2 | `build.toml`, `infra/samconfig.toml` |
| `sh` | 2 | `run.sh`, `monitor-stack.sh`: menus e mensagens em português |
| `json` | 2 | Collection e registro de review: nomes de requisição e descrições em português |
| `pdf` | 1 | Documento de entrega, fora da varredura |
| `gitignore` | 1 | Fora da varredura |
| `dockerignore` | 1 | Fora da varredura |

Total rastreado: 169 arquivos (antes, 171; saíram `infra/template.yaml.bak` e `.orig`).

---

## 4. Nomes de arquivo

Os 115 arquivos `.java`, os 6 `.properties`, os 5 `.xml`, os 3 `.yml` e os 3 `.yaml` têm nome em inglês. Os documentos `.md` com termos em português (`BACKLOG.md`, `HANDOFF_SESSION.md`, `RESUMO_CONVERSA.md`, `TASK-019-AWS-DEPLOYMENT-BLOQUEADO.md`) estão fora da regra de nomes de código.

**Resultado: PASS.**

---

## 5. Identificadores e contrato em código

### 5.1 Identificadores e comentários
- Não há identificador em português em código. As ocorrências de `urgency` e `description` são palavras em inglês.
- Não há comentário Java com palavra comum em português.
- Os campos JSON atuais são `description`, `rating`, `urgency`, `createdAt` e `id`.

**Resultado: PASS.**

### 5.2 Contrato legado em código e configuração

Nenhuma ocorrência em `src/main`, `.properties`, `.xml`, `.yml`, `.yaml`, `infra/template.yaml`, `docs/api-collection/*.json`, `run.sh`, `monitor-stack.sh` ou `infra/samconfig.toml`. A query `listarAvaliacoes` e a rota `/avaliacao` não aparecem em nenhum arquivo rastreado de código ou configuração.

| Arquivo | Linha | Ocorrência | Situação |
|---|---|---|---|
| `receive-feedback/src/test/java/com/fiap/feedback/infra/handler/ReceiveFeedbackHandlerV1Test.java` | 83 | Corpo de requisição com `"descricao"` e `"nota"`, esperando 400 | Exceção (a): teste negativo que confirma que a v1 rejeita o formato legado |
| `receive-feedback/src/test/java/com/fiap/feedback/infra/handler/ReceiveFeedbackHandlerV1Test.java` | 172 | `assertFalse(item.has("descricao"))` | Exceção (a): asserção negativa que confirma que a v1 não devolve a chave legada |

Os outros dois resultados de `grep` nos testes são frases em português e não são identificadores legados: `ReportDynamoMapperTest.java:52` e `SnsUrgencyPublisherTest.java:56`.

**Resultado: PASS, com as duas exceções (a) documentadas.**

---

## 6. Logs, mensagens de API e textos para pessoas

### 6.1 Logs e exceções
- Os textos de log do código estão em português. Termos técnicos (`Handler`, `Lambda`, `GraphQL`, `SNS`, `SES`, `DynamoDB`, `MessageId`, `receiveFeedback`, `listFeedbacks`) permanecem em inglês, como nomes de tecnologia ou de operação.
- Os e-mails e assuntos SNS estão em português.
- `e.getMessage()` concatenado em logs produz, às vezes, texto da biblioteca ou do SDK da AWS. Esse texto não é escrito pelo código e não entra como falha.

**Resultado: PASS.**

### 6.2 Mensagens de API no código

Os valores de `title` e `detail` emitidos pelo código estão em português: `Erro de validação`, `Erro interno do servidor`, `Requisição inválida`, `Não autorizado`, `Token Bearer ausente`, `Token Bearer inválido`, `Nota deve ser no mínimo 0`, `Descrição não pode estar vazia`.

**Resultado: PASS.**

### 6.3 Logs citados na documentação

`docs/qualidade/MONITORING_GUIDE.md` cita os logs com o texto atual do código, por exemplo `Descrição deve ter no mínimo 5 caracteres`, `Erro do SES ao enviar e-mail para o feedback <id>` e `Handler Lambda invocado`. Os textos foram conferidos em `src/main`, e a citação que não existia no código foi trocada.

**Resultado: PASS.**

---

## 7. Documentação

### 7.1 Documentos em escopo, em português

Documentos traduzidos nesta etapa: `docs/API.md`, `docs/api-collection/API.md`, `docs/api-collection/README.md`, `docs/SECURITY.md`, `docs/deploy/AWS_DEPLOY_GUIDE.md`, `docs/qualidade/DEMO_GUIDE.md`, `docs/qualidade/MONITORING_GUIDE.md`, `docs/qualidade/FINAL_STATUS_REPORT.md`, `docs/LOCAL_SETUP.md`, `docs/E2E_TESTING_GUIDE.md`, `LOCAL_DEVELOPMENT.md`, `README.md` (passos do pipeline) e `receive-feedback/GRAPHQL_TEST_GUIDE.md`.

Mantidos sem tradução, por decisão: blocos de código, comandos, nomes de campos GraphQL e JSON (`description`, `rating`, `urgency`, `createdAt`), rotas, nomes de recursos da AWS, nomes de rótulos de interface do GitHub (`Re-run all jobs`, `Run workflow`) e o nome do job `Build & Test & SonarCloud`.

Exemplos de erro nos blocos de código foram trocados pelo texto real da API:
- `Token Bearer ausente` / `Token Bearer inválido`, com `title` `Não autorizado`
- `Erro de validação` com `Nota deve ser no mínimo 0`
- `Erro interno do servidor`

Collection do Postman: as asserções dos 12 testes 401 e os nomes dos testes usam o texto real da API.

**Resultado: PASS.**

### 7.2 Registros históricos (exceção b)

Estes arquivos são registros de testes e de tarefas. Citam `/avaliacao`, `listarAvaliacoes`, `criarAvaliacao`, `dataCriacao`, `avaliacoes`, `descricao` ou `urgencia`. Ficam como exceção por nome, conforme a decisão do responsável. O texto deles não foi traduzido.

| Arquivo | Ocorrências | Tipo |
|---|---|---|
| `TEST_RESULTS.md` | 14 | Registro de testes do `/avaliacao` |
| `TASK_018_COMPLETION_SUMMARY.md` | 3 | Registro de tarefa |
| `docs/E2E_TEST_RESULTS.md` | 19 | Registro de testes |
| `docs/E2E_TEST_RESULTS_SAMPLE.md` | 26 | Registro de testes |
| `docs/TASK-019-AWS-DEPLOYMENT-BLOQUEADO.md` | 1 | Registro de deploy, cita `POST /avaliacao` |

Total: 63 ocorrências em 5 arquivos.

`docs/planejamento/*` e `BACKLOG.md` estão fora da varredura (decisão c).

### 7.3 Falsos positivos sem ação
- `README.md`: "descrição" e "nota" em português. Não são identificadores.
- Dados de exemplo em `DEMO_GUIDE.md` (por exemplo `Critical issue: System crashes on login`) são payload de entrada, não mensagens do sistema.
- Palavras em português que coincidem com termos em inglês (`Status`, `use`, `Test`) não são prose em inglês.

**Resultado: PASS.**

---

## 8. Arquivos de cópia de trabalho

| Arquivo | Estado | Verificação |
|---|---|---|
| `infra/template.yaml.bak` | Removido com `git rm` | `git ls-files` não lista o arquivo |
| `infra/template.yaml.orig` | Removido com `git rm` | `git ls-files` não lista o arquivo |

Nenhuma referência aponta para os dois arquivos: `git grep -n -I -E 'yaml\.(bak|orig)'` só retornava este relatório antes da remoção. `infra/` agora tem `samconfig.toml` e `template.yaml`.

**Resultado: PASS.**

---

## 9. Testes

Comando: `mvn -B clean verify` na raiz e `mvn -B -f local-http/pom.xml clean verify`.

| Módulo | Testes | Falhas | Erros | Resultado |
|---|---|---|---|---|
| receive-feedback | 147 | 0 | 0 | SUCCESS |
| send-notification | 86 | 0 | 0 | SUCCESS |
| generate-report | 159 | 0 | 0 | SUCCESS |
| local-http | 6 | 0 | 0 | SUCCESS |

Logs: `/tmp/code-audit-final-3-build.log` (raiz, `MVN_ROOT_EXIT=0`) e `/tmp/code-audit-final-3-local-http.log` (local-http, `MVN_LOCALHTTP_EXIT=0`).

---

## 10. Veredito e pendências

**Veredito: PASS, com exceções documentadas.**

Exceções aceitas:
1. **(a)** `ReceiveFeedbackHandlerV1Test.java`, linhas 83 e 172 (seção 5.2).
2. **(b)** Registros históricos listados por nome na seção 7.2: 63 ocorrências em 5 arquivos.

Fora da varredura, por decisão: `docs/planejamento/*` e `BACKLOG.md`.

Nenhum item bloqueia o PASS.
