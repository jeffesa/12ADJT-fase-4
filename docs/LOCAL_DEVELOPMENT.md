# Desenvolvimento local com Quarkus Dev Mode

Este guia explica como preparar o ambiente de desenvolvimento local da Fase 4 usando o Quarkus Dev Mode, com hot reload, e como testar contra a AWS para testes de integração.

## Início rápido

### Pré-requisitos

- **Java 17+** (verifique com `java -version`)
- **Maven 3.8+** (verifique com `mvn -version`)
- **AWS CLI** (opcional, para testes de integração com a AWS)

### 1. Iniciar o Quarkus Dev Mode (3 terminais)

Cada módulo roda em uma porta diferente, com hot reload ativado.

**Terminal 1 — receive-feedback (porta 8080):**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/receive-feedback
mvn quarkus:dev
```

**Terminal 2 — send-notification (porta 8081):**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/send-notification
mvn quarkus:dev
```

**Terminal 3 — generate-report (porta 8082):**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/generate-report
mvn quarkus:dev
```

### 2. Verificar se os módulos estão rodando

Depois que os três estiverem no ar, confira a saúde de cada um:

```bash
# Deve retornar 200/404/405 (os endpoints existem)
curl -v http://localhost:8080
curl -v http://localhost:8081
curl -v http://localhost:8082
```

Ou use o script de apoio:

```bash
./run.sh health
```

### 3. Teste de integração na AWS

Todos os testes de integração apontam para o deploy ativo na AWS:

```
https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/
```

Use o Postman ou o curl para testar:

```bash
curl -X POST https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/v1/avaliacao \
  -H "Content-Type: application/json" \
  -d '{"descricao": "Test feedback", "nota": 5}'
```

## Fluxo de hot reload

O Quarkus Dev Mode aplica as mudanças de código na hora:

1. Edite um arquivo Java (por exemplo, `ReceiveFeedbackUseCase.java`)
2. **Salve o arquivo**: o Maven detecta a mudança
3. **Compilação e recarga automáticas**: em 1 a 2 segundos
4. **Teste em seguida**: não é preciso reiniciar

### Exemplo: adicionar uma instrução de log

```java
// Em receive-feedback/src/main/java/com/fiap/feedback/application/usecase/ReceiveFeedbackUseCase.java
@Override
public Feedback receiveFeedback(String description, Integer rating) {
    LOGGER.info("Received feedback: " + description); // Add this line
    // ... rest of code
}
```

Ao salvar o arquivo, o hot reload é disparado e o log aparece na saída do terminal.

## Parar os módulos

Para parar um módulo, pressione **Ctrl+C** no terminal dele.

```bash
# Terminal 1
^C

# Terminal 2
^C

# Terminal 3
^C
```

Para confirmar que pararam:

```bash
curl http://localhost:8080
# Deve falhar: Connection refused
```

## Solução de problemas

### 1. Porta já em uso

Se aparecer `Address already in use`:

**Encontre e encerre o processo:**
```bash
# macOS/Linux
lsof -i :8080  # Descobre o que está usando a porta 8080
kill -9 <PID>

# Ou encerre todos os processos Java (use com cuidado)
pkill -f "java.*quarkus:dev"
```

**Ou use outras portas:**
```bash
cd receive-feedback
mvn quarkus:dev -Dquarkus.http.port=9080
```

### 2. Falta de memória

Se o Maven ficar sem memória durante a compilação:

```bash
export MAVEN_OPTS="-Xmx2g"
mvn quarkus:dev
```

### 3. Classpath ou dependência ausente

Se aparecer `ClassNotFoundException` ou `NoClassDefFoundError`:

```bash
# Limpe e recompile
cd receive-feedback
mvn clean compile
mvn quarkus:dev
```

### 4. O módulo não sobe (os logs mostram erros)

Causas comuns:

- **Credenciais AWS ausentes**: defina `AWS_REGION`, `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY`
  ```bash
  export AWS_REGION=us-east-1
  export AWS_ACCESS_KEY_ID=test
  export AWS_SECRET_ACCESS_KEY=test
  mvn quarkus:dev
  ```

- **Tabela do DynamoDB não encontrada**: esperado ao rodar localmente sem a AWS. Use a configuração do perfil `%dev`
  - Veja `src/main/resources/application.properties` para a configuração de dev
  - Os erros são seguros; o serviço foi feito para rodar na AWS

- **Schema GraphQL não carrega**: reinicie o módulo
  ```bash
  Ctrl+C
  mvn quarkus:dev
  ```

## Teste de integração com a AWS

### Usando o Postman

1. Abra `docs/api-collection/fase4-feedback.postman_collection.json` no Postman
2. Na collection, defina a variável `prodUrl` como:
   ```
   https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod
   ```
3. Execute as requisições da pasta `prod`

### Usando o curl

```bash
# Criar avaliação
curl -X POST https://9799a6pv98.execute-api.sa-east-1.amazonaws.com/prod/v1/avaliacao \
  -H "Content-Type: application/json" \
  -d '{
    "descricao": "Testing integration",
    "nota": 7
  }'

# Resposta esperada (201 Created):
# {
#   "id": "uuid-here",
#   "descricao": "Testing integration",
#   "nota": 7,
#   "urgencia": "MEDIUM",
#   "dataCriacao": "2024-11-09T..."
# }
```

### Testes em lote com o Newman

```bash
# Instalar o Newman (uma vez)
npm install -g newman

# Rodar a collection do Postman contra a AWS
newman run docs/api-collection/fase4-feedback.postman_collection.json \
  --folder prod \
  -r htmlextra \
  --reporter-htmlextra-export results.html

# Ver os resultados
open results.html
```

## Executar os testes

### Testes unitários e de integração

```bash
# Todos os módulos
mvn clean verify

# Um módulo específico
mvn clean verify -pl receive-feedback

# Com relatório de cobertura
open target/site/jacoco/index.html
```

### Testes E2E

```bash
# Suíte completa de testes ponta a ponta
./run.sh e2e-test

# Verificar os resultados
./run.sh e2e-verify
```

## Estratégia de desenvolvimento local

| Cenário | Ferramenta | Comando |
|----------|------------|---------|
| **Editar o código e testar localmente** | Quarkus Dev Mode | `mvn quarkus:dev` em 3 terminais |
| **Rodar os testes unitários** | Maven/JUnit | `mvn test` ou `mvn test -pl receive-feedback` |
| **Verificar a integração com a AWS** | Postman/curl | Teste contra o endpoint `prod` |
| **Teste E2E completo** | script bash | `./run.sh e2e-test` |
| **Cobertura de código** | JaCoCo | `mvn verify && open target/site/jacoco/index.html` |

## Notas de arquitetura

### Por que Quarkus Dev Mode e não LocalStack?

1. **Confiabilidade**: o LocalStack 3.0 trava depois de cerca de 5 operações; o Quarkus Dev Mode é estável
2. **Sem mocks**: os testes rodam contra a AWS real, o que valida o deploy de verdade
3. **Simplicidade**: não é preciso configurar Docker; basta `mvn quarkus:dev`
4. **Hot reload**: ciclo de feedback instantâneo durante o desenvolvimento
5. **Padrão acadêmico**: para um projeto da FIAP, usar a AWS diretamente é mais autêntico e demonstra o design nativo da nuvem

### Local x produção

- **Local (Dev Mode)**: use para iterar rápido, depurar e aproveitar o hot reload
- **Produção (AWS)**: use para testes de integração, testes E2E e verificação final
- **Testes**: rode localmente e verifique contra a AWS antes do commit

## Visão geral dos módulos

### receive-feedback (porta 8080)

- **Handler**: endpoint REST para POST /v1/avaliacao (criação)
- **Papel**: valida o feedback, calcula a urgência, grava no DynamoDB e publica no SNS
- **Dependências**: DynamoDB, SNS
- **Teste**: `curl -X POST http://localhost:8080/...`

### send-notification (porta 8081)

- **Handler**: consome os eventos do SNS vindos do receive-feedback
- **Papel**: envia notificações por e-mail para feedbacks de urgência HIGH
- **Dependências**: SNS (consumo), SES (envio de e-mail)
- **Teste**: pelo evento do SNS (teste de integração)

### generate-report (porta 8082)

- **Handler**: consome os eventos do EventBridge
- **Papel**: agrega métricas e gera relatórios de resumo
- **Dependências**: EventBridge, DynamoDB
- **Teste**: pelo EventBridge (teste de integração)

## Próximos passos

- Edite o código-fonte em uma IDE (por exemplo, VS Code ou IntelliJ)
- Salve as mudanças e acompanhe o hot reload no terminal
- Teste em `http://localhost:8080` (local) ou no endpoint da AWS (integração)
- Rode `mvn test` antes de fazer commit
- Faça o deploy na AWS com o SAM quando estiver pronto

Para mais detalhes sobre build e deploy, veja o [README.md](../README.md) principal.
