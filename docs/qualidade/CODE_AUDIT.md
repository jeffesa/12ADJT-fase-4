# Auditoria de código: verificação de 100% inglês (TASK-030)

**Resultado: FAIL**
**Data e hora da auditoria:** 2026-11-15 16:17 (UTC-03:00)
**Branch auditada:** `refactor/english-identifiers` (commit `154e1f5`), ponta da pilha de branches de TASK-030
**Branch deste relatório:** `docs/code-audit`

O FAIL vem de mensagens de log, de erro e de saída de usuário em português que ainda existem em scripts, workflows, templates e na collection do Postman. Os nomes de código Java já estão em inglês. Os itens de contrato legado listados na seção 3 ficaram fora da correção, de forma intencional.

---

## 1. Escopo e contagem de arquivos

Comando base: `git ls-files` sobre a árvore da branch auditada.

| Extensão | Arquivos escaneados |
|---|---|
| `.java` | 118 |
| `.md` | 28 |
| `.properties` | 6 |
| `.xml` | 5 |
| `.yml` | 3 |
| `.yaml` | 3 |
| `.toml` | 2 |
| `.sh` | 2 |
| `.json` | 2 |
| `.orig` | 1 |
| `.bak` | 1 |
| `.gitignore` | 1 |
| `.dockerignore` | 1 |
| **Total de arquivos de texto escaneados** | **173** |

Há 174 arquivos rastreados. O único arquivo não escaneado é `docs/ADJT - Fase 4 - Tech Challenge_rev.pdf`, um binário.

Também foram verificados os nomes de arquivos e diretórios rastreados. Nenhum nome contém português, exceto documentos `.md` sob `docs/`, que estão fora do escopo de nomes de código.

`docs/planejamento/BACKLOG.md` ficou fora da varredura de palavras. O próprio critério da TASK-030 permite que o backlog permaneça em português.

## 2. Busca por palavras-chave em português

Busca com fronteira de palavra, sem diferenciar maiúsculas de minúsculas, sobre os arquivos de texto rastreados, exceto `BACKLOG.md`. Os números são linhas com ocorrência, e entre parênteses, arquivos.

| Termo | Linhas | Arquivos |
|---|---|---|
| descrição (com acento) | 18 | 7 |
| descricao | 158 | 30 |
| nota | 221 | 32 |
| urgência (com acento) | 43 | 10 |
| urgencia | 140 | 21 |
| avaliação (com acento) | 13 | 4 |
| avaliacao | 87 | 22 |

Nos arquivos `.java`, há 124 linhas com esses termos. Quase todas são nomes do contrato legado (seção 3) ou comentários Javadoc que os descrevem.

Outros termos verificados: `usuario`, `validacao`, `configuracao`, `notificacao`, `relatorio`, `criacao`, `receber`, `pendente` e `teste`. A maioria aparece em strings de teste, em arquivos de documentação ou em texto de saída do shell, não como identificadores de código.

## 3. Exceções documentadas: contrato legado

Estes nomes são mantidos de propósito. Trocá-los quebraria clientes, a collection do Postman e as consultas existentes. Os locais abaixo são os principais, não uma lista completa.

| Item | Locais principais | Justificativa |
|---|---|---|
| Rota `/avaliacao` (POST e GET) | `local-http/src/main/java/com/fiap/feedback/localhttp/LocalHttpRoutes.java:38-39`; `receive-feedback/src/main/java/com/fiap/feedback/infra/handler/ReceiveFeedbackHandler.java:73,112,149`; `infra/template.yaml:281,322,608,609,632`; `run.sh:34,182,183`; `docs/SECURITY.md:19` | Contrato HTTP legado. Os clientes continuam usando a rota, e a rota `/v1/feedback` em inglês já existe em paralelo. |
| Campo `descricao` | `receive-feedback/src/main/java/com/fiap/feedback/infra/dto/ReceiveFeedbackInput.java:19`; `ReceiveFeedbackOutput.java:21`; `infra/handler/GraphQLHandler.java:137`; `infra/handler/ListFeedbackHandler.java:78`; `infra/handler/ReceiveFeedbackHandler.java:124,184,193` | Nome do campo JSON e do argumento GraphQL. Trocá-lo mudaria o payload. |
| Campo `nota` | `ReceiveFeedbackInput.java:23`; `ReceiveFeedbackOutput.java:22`; `GraphQLHandler.java:138`; `ListFeedbackHandler.java:79`; `ReceiveFeedbackHandler.java:125,173,185,194` | Mesma justificativa de `descricao`. |
| Campo `urgencia` | `ReceiveFeedbackOutput.java:23`; `GraphQLHandler.java:139`; `ListFeedbackHandler.java:80`; `ReceiveFeedbackHandler.java:126,195` | Nome do campo de saída. O valor do enum continua em inglês (HIGH, MEDIUM, LOW). |
| Campo `dataCriacao` | `ReceiveFeedbackOutput.java:24`; `GraphQLHandler.java:140`; `ListFeedbackHandler.java:81`; `ReceiveFeedbackHandler.java:127,196` | Nome do campo de saída. |
| Chave `avaliacoes` | `ListFeedbackHandler.java:86`; `ReceiveFeedbackHandler.java:132` | Chave da lista na resposta REST. |
| Query `listarAvaliacoes` | `receive-feedback/src/main/java/com/fiap/feedback/infra/graphql/ReceiveFeedbackQueryResolver.java:56,57,69`; `GraphQLHandler.java:40,121,123,146`; `infra/template.yaml:332` | Nome da query GraphQL usado pelos clientes. A query `listFeedbacks` em inglês foi adicionada em paralelo. |
| `receiveFeedback` | `infra/template.yaml:271` (`QUARKUS_LAMBDA_HANDLER`) | Já está em inglês. Consta na lista de contrato legado e foi mantido porque o nome do handler é referenciado no deploy. |

Os nomes `ReceiveFeedbackInput` e `ReceiveFeedbackOutput` contêm `Feedback` em inglês. Os componentes de record `descricao` e `nota` e as tags Javadoc `@param` que os mencionam fazem parte do mesmo contrato.

## 4. Problemas restantes (FAIL)

Estes itens estão fora das exceções documentadas e violam o critério de TASK-030 de mensagens de erro e log em inglês, nomes e documentação em inglês, ou descrições em inglês.

### 4.1 Scripts de shell (`run.sh`, `monitor-stack.sh`)

- `run.sh`: 51 linhas com texto em português visível ao usuário. Exemplos: 66, 85, 88, 91, 94, 97, 101, 109, 111, 113, 122, 124, 126, 136, 142, 161, 171, 177, 180, 185, 200, 204, 206, 217, 219, 225, 230, 238, 246, 250, 280, 285, 287, 301, 304, 306, 309, 325–332, 336, 337, 341, 348, 355, 356, 370–377. Os textos são `echo`, `info`, `warn`, `err`, menus e a saída de `usage`. Esses textos são saídas de log e de erro, não comentários.
- `monitor-stack.sh:14`: `"✅ CREATE_COMPLETE - Stack foi criada com sucesso!"`.

Ação pendente: traduzir as mensagens. Isso muda o que o usuário vê no terminal, então precisa de confirmação antes de ser feito.

### 4.2 Workflows do GitHub Actions

- `.github/workflows/ci-cd.yml:44,47,122,126,130,134`: mensagens `::error` e `::notice` em português (`ausente`, `não está configurado`, `Análise SonarCloud ignorada`).
- `.github/workflows/auto-pr-to-main.yml:29,37,38,48,49`: mensagens de `echo` em português (`Nenhuma diferença`, `já existe`, `criado`, `Auto-merge já habilitado`, `não pôde ser habilitado`).

### 4.3 Templates e metadados de build

- `infra/template.yaml:544,565,586`: `AlarmDescription` em português.
- `receive-feedback/pom.xml:15`, `send-notification/pom.xml:15`, `generate-report/pom.xml:15`: `<description>` em português.

### 4.4 Documentação e collection

- `docs/api-collection/fase4-feedback.postman_collection.json`: 76 linhas com acento, mais outras linhas sem acento. Inclui nomes de requisição (`Criar Avaliação`, `Listar Avaliações`), descrições, nomes de testes do Postman e corpos de exemplo em português. A collection é documentação de API, não código, mas o critério cobre a documentação de API.
- `docs/api-collection/API.md:246,248,276,278,279,280,285`: exemplo de e-mail e de conteúdo em português. `API.md` está no critério como documento em inglês.

### 4.5 Fora do critério desta auditoria

Os demais `.md` em português (`README.md`, `docs/deploy/*`, `docs/planejamento/*` exceto `BACKLOG.md`, `docs/LOCAL_SETUP.md`, `docs/E2E_*`, entre outros) não entram no critério de TASK-030. Só `SECURITY.md` e `API.md` estão listados como obrigatoriamente em inglês. `docs/SECURITY.md` está em inglês e não tem problemas além da rota legada, já listada na seção 3.

### 4.6 Arquivos sem extensão de código

`infra/template.yaml.bak` e `infra/template.yaml.orig` estão rastreados. Não contêm nomes em português novos, mas são cópias de trabalho que não deveriam estar no repositório. Não foram alterados.

## 5. Ações corretivas por branch

| Branch | PR | Estado | Ação |
|---|---|---|---|
| `chore/english-files` | não existe | não criada | Nenhum nome de arquivo em português foi encontrado. |
| `chore/english-comments` | [#103](https://github.com/jeffesa/12ADJT-fase-4/pull/103) | aberta, não mesclada | Comentários de 11 arquivos traduzidos para inglês. Não traduziu mensagens de runtime (seção 4.1 e 4.2). |
| `chore/english-messages` | não aberta | local, sem commit | Nenhuma alteração. A conclusão da verificação não cobriu scripts nem workflows com mensagens em português; esta auditoria encontrou esses casos. |
| `refactor/english-identifiers` | [#104](https://github.com/jeffesa/12ADJT-fase-4/pull/104) | aberta, não mesclada | Quatro identificadores internos renomeados para inglês em oito arquivos Java. Contrato legado preservado. |
| `test/english-test-names` | não existe | removida | Nada para traduzir nos testes. |
| `docs/code-audit` | esta PR | nova | Este relatório. Não altera código. |

Este relatório substitui o `docs/qualidade/CODE_AUDIT.md` anterior, que marcava PASS e citava outra árvore de trabalho e outro número de arquivos. Esse conteúdo deixou de valer com esta auditoria.

## 6. Verificação

- Não houve alteração de código nesta branch. Build e testes não foram executados, porque a mudança é só documental.
- Nenhum segredo foi lido nem escrito. A verificação não usou credenciais.
- Nenhum deploy, merge ou escrita na AWS foi feito.

## 7. Próximos passos para virar PASS

1. Decidir se as mensagens de runtime em `run.sh`, `monitor-stack.sh` e nos workflows devem ser traduzidas. Essa escolha muda o texto que o usuário vê.
2. Traduzir `AlarmDescription`, as `<description>` dos POMs e os exemplos de `API.md`.
3. Traduzir a collection do Postman, ou registrar uma exceção documentada para ela.
4. Rodar esta auditoria de novo. O comando é `git grep -n -I -E '(^|[^A-Za-z])(descrição|nota|urgência|avaliação|usuário|configuração|notificação|relatório|erro|mensagem)'`, com as exceções da seção 3 filtradas.
