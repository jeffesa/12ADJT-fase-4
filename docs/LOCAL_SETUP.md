# Guia de configuração do ambiente local

Este guia explica como preparar a aplicação serverless da plataforma de feedback para desenvolvimento e testes locais, usando o LocalStack para simular os serviços da AWS.

**Observação**: os scripts legados `e2e-test.sh` e `e2e-verify.sh` foram removidos da raiz do repositório.

## Visão geral

O LocalStack é uma pilha de nuvem da AWS local e totalmente funcional, que permite desenvolver e testar aplicações serverless sem conectar à AWS. Esta plataforma usa:

- **DynamoDB**: guarda os registros de feedback com chave primária (id, createdAt)
- **SNS**: publica notificações de feedbacks de alta urgência no `feedback-urgent-topic`
- **SQS**: filas para o processamento assíncrono de mensagens
- **SES**: simula o envio de e-mails (mockado no LocalStack)
- **Lambda**: simulada pelo Quarkus dev mode, com disparo de eventos
- **CloudWatch**: logs para monitoramento e depuração

## Pré-requisitos

Confirme que o sistema tem os itens abaixo instalados:

### Software necessário

- **Docker**: versão 20.10 ou posterior
  - macOS: instale o [Docker Desktop](https://www.docker.com/products/docker-desktop)
  - Linux: `sudo apt-get install docker.io docker-compose`
  - Windows: instale o [Docker Desktop para Windows](https://www.docker.com/products/docker-desktop)

- **Docker Compose**: versão 1.29 ou posterior
  - Geralmente vem incluído no Docker Desktop
  - Verifique com `docker compose version`

- **Java 17**: para compilar e rodar os módulos
  - Baixe no [Amazon Corretto](https://aws.amazon.com/corretto/) ou no [Adoptium](https://adoptium.net/)
  - Verifique com `java -version`

- **Maven 3.8+**: para compilar o projeto e rodar os testes
  - Baixe no [Apache Maven](https://maven.apache.org/download.cgi)
  - Verifique com `mvn --version`

- **Git**: para controle de versão
  - macOS: `brew install git`
  - Linux: `sudo apt-get install git`
  - Windows: [Git para Windows](https://gitforwindows.org/)

### Ferramentas recomendadas

- **curl**: para testar os endpoints GraphQL manualmente
  - Geralmente já vem instalado no macOS e no Linux
  - Windows: instale o [Git Bash](https://gitforwindows.org/) ou o [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install)

- **jq**: para ler JSON em scripts de shell
  - macOS: `brew install jq`
  - Linux: `sudo apt-get install jq`
  - Windows: `choco install jq` ou use os [binários prontos](https://stedolan.github.io/jq/download/)

- **AWS CLI v2**: para interagir diretamente com o LocalStack
  - Instale pela [documentação do AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
  - Verifique com `aws --version`

## Início rápido (5 minutos)

### Passo 1: clonar e entrar no repositório

```bash
cd /Users/jefferson/Documents/FIAP/fase-4
```

### Passo 2: iniciar o LocalStack

```bash
./run.sh docker
```

Este comando:
- Inicia o contêiner do LocalStack com todos os serviços da AWS
- Roda automaticamente o script de inicialização (`infra/localstack-init.sh`), que cria as tabelas do DynamoDB, os tópicos do SNS e as filas do SQS
- Mostra os endpoints disponíveis depois que os serviços estiverem prontos

Saída esperada:
```
✅ LocalStack pronto!

📍 Endpoints LocalStack:
   Edge:      http://localhost:4566
   DynamoDB:  http://localhost:4569
   SNS:       http://localhost:4575
   SQS:       http://localhost:4576
```

### Passo 3: verificar a saúde do LocalStack

Em um segundo terminal:

```bash
./run.sh health
```

Saída esperada:
```
✅ LocalStack (edge:4566): UP
```

### Passo 4: iniciar os três módulos

Abra três terminais separados e rode cada comando no seu terminal:

**Terminal 1 - módulo Receive Feedback:**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/receive-feedback
mvn quarkus:dev
```

**Terminal 2 - módulo Send Notification:**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/send-notification
mvn quarkus:dev
```

**Terminal 3 - módulo Generate Report:**
```bash
cd /Users/jefferson/Documents/FIAP/fase-4/generate-report
mvn quarkus:dev
```

Cada módulo vai:
- Compilar e iniciar um servidor de desenvolvimento do Quarkus
- Mostrar a porta em que está escutando (normalmente 8080, 8081 e 8282)
- Monitorar as mudanças de arquivo para recarga automática
- Conectar-se ao LocalStack pelos endpoints configurados

### Passo 5: rodar os testes E2E

No terminal original (ou em um novo):

```bash
./run.sh e2e-setup
```

Este comando verifica se todos os recursos estão prontos:
- Confere se a tabela do DynamoDB existe
- Verifica os tópicos do SNS
- Confirma as filas do SQS
- Testa as identidades de e-mail do SES

Depois, rode os fluxos de teste:

```bash
./run.sh e2e-test
```

Por fim, verifique os resultados:

```bash
./run.sh e2e-verify
```

## Configuração do LocalStack

### Endpoints e serviços

Todos os serviços da AWS ficam disponíveis no endpoint de borda, na porta 4566:

| Serviço | Porta | Endpoint |
|---------|-------|----------|
| Borda (todos os serviços) | 4566 | http://localhost:4566 |
| DynamoDB | 4569 | http://localhost:4569 |
| SNS | 4575 | http://localhost:4575 |
| SQS | 4576 | http://localhost:4576 |

### Variáveis de ambiente

As variáveis abaixo são configuradas automaticamente:

```bash
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_ENDPOINT_URL_DYNAMODB=http://localhost:4569
AWS_ENDPOINT_URL_SNS=http://localhost:4575
AWS_ENDPOINT_URL_SQS=http://localhost:4576
AWS_ENDPOINT_URL_SES=http://localhost:4566
```

Elas são definidas em:
- `docker-compose.yml` (ambiente do contêiner do LocalStack)
- `application.properties` do Quarkus (de cada módulo)
- Arquivos `.env`, se existirem

### Credenciais da AWS

Credenciais de teste (válidas somente com o LocalStack):

```
Access Key ID: test
Secret Access Key: test
Region: us-east-1
```

**Nota de segurança**: estas credenciais valem apenas localmente e não têm permissões reais na AWS. Nunca as use em produção.

## Recursos criados pela inicialização do LocalStack

Quando o LocalStack sobe, o script de inicialização (`infra/localstack-init.sh`) cria automaticamente:

### Tabela do DynamoDB: `feedbacks`

**Chave primária:**
- Chave de partição (HASH): `id` (String)
- Chave de classificação (RANGE): `createdAt` (String)

**Atributos:**
- `id`: UUID do feedback (gerado automaticamente)
- `description`: texto da descrição do feedback
- `rating`: nota numérica (0 a 10)
- `urgency`: nível de urgência (HIGH, MEDIUM, LOW)
- `createdAt`: timestamp ISO 8601

**Modo de cobrança**: por requisição (sem capacidade provisionada)

### Tópicos do SNS

- `feedback-urgent-topic`: recebe mensagens quando um feedback de urgência HIGH é enviado
- `feedback-report-topic`: recebe as mensagens de relatórios agendados

### Filas do SQS

- `notification-queue`: processa as notificações (assinante de `feedback-urgent-topic`)
- `report-queue`: processa os pedidos de geração de relatório

## Modo de desenvolvimento do Quarkus

Cada módulo roda no modo de desenvolvimento do Quarkus, que oferece:

- **Recarga ao vivo**: mudanças no código Java são compiladas e implantadas automaticamente
- **Recarga do GraphQL**: mudanças no schema têm efeito sem reiniciar
- **Interface de dev**: acessível em `http://localhost:8080/q/dev`
- **Logs**: a saída do console mostra todas as requisições e erros

### Portas dos módulos do Quarkus

Por padrão:
- **receive-feedback**: http://localhost:8080/graphql
- **send-notification**: http://localhost:8081/graphql (ou Lambda em segundo plano)
- **generate-report**: http://localhost:8282/graphql

Confira a saída do console de cada módulo para ver a porta exata.

### Dependências entre os módulos

Os módulos funcionam em conjunto da seguinte forma:

1. **Receive Feedback** → grava no DynamoDB e publica no SNS se a urgência for HIGH
2. **Evento do SNS** → dispara a Lambda Send Notification
3. **Send Notification** → envia e-mail pelo SES
4. **Generate Report** → consulta o DynamoDB e envia o e-mail de resumo

### Propriedades da aplicação

Cada módulo tem um arquivo `application.properties` em `src/main/resources`:

```properties
# Endpoints do LocalStack
quarkus.dynamodb.endpoint-override=http://localhost:4569
quarkus.sns.endpoint-override=http://localhost:4575
quarkus.sqs.endpoint-override=http://localhost:4576
quarkus.ses.endpoint-override=http://localhost:4566

# Credenciais da AWS
aws.region=us-east-1
aws.credentials.static.provider.access-key-id=test
aws.credentials.static.provider.secret-access-key=test

# Configuração de e-mail (SES)
notification.sender-email=noreply@feedback-fase-4.com
notification.admin-email=admin@feedback-fase-4.com
notification.ses-region=us-east-1
```

## Configuração do Docker Compose

O arquivo `docker-compose.yml` orquestra todos os serviços:

### Serviço LocalStack

- **Imagem**: `localstack/localstack:3.0` (versão estável mais recente)
- **Portas**: 4566 (borda), 4569 (DynamoDB), 4575 (SNS), 4576 (SQS)
- **Volumes**: o estado do LocalStack persiste no volume `localstack-data`
- **Script de inicialização**: roda `infra/localstack-init.sh` ao subir
- **Rede**: `fase4-network` (rede bridge para a comunicação entre serviços)

### Rede

- **Driver**: rede bridge (`fase4-network`)
- **Finalidade**: permite que os módulos Quarkus (rodando no host) se comuniquem com o contêiner do LocalStack

## Problemas comuns e solução de problemas

### Conflito de portas

Se aparecer o erro "Port already in use":

**Problema**: outro serviço está usando as portas do LocalStack (4566, 4569, 4575, 4576)

**Solução 1**: pare o serviço que está em conflito
```bash
# Descubra o que está usando a porta 4566
lsof -i :4566

# Encerre o processo (macOS/Linux)
kill -9 <PID>
```

**Solução 2**: use outras portas no `docker-compose.yml`
```yaml
ports:
  - "4566:4566"  # Troque o primeiro número por outra porta
```

**Solução 3**: limpe e reinicie
```bash
./run.sh clean
./run.sh docker
```

### Docker não está rodando

**Problema**: "Cannot connect to Docker daemon"

**Solução - macOS (Docker Desktop)**:
```bash
# Inicie o Docker Desktop pela interface gráfica ou pela linha de comando
open /Applications/Docker.app

# Ou com o Colima
colima start
```

**Solução - Linux**:
```bash
sudo systemctl start docker
sudo usermod -aG docker $USER
# Saia e entre de novo na sessão para as mudanças de grupo valerem
```

### Os módulos não conectam ao LocalStack

**Problema**: "Connection refused" quando os módulos tentam acessar o LocalStack

**Causa**: os módulos podem estar usando um endpoint errado, ou o LocalStack ainda não está pronto

**Solução**:
1. Confirme que o LocalStack está rodando: `./run.sh health`
2. Confira os endpoints no `application.properties` de cada módulo
3. Garanta que os módulos usam `http://localhost:4566` (e não um IP)
4. Espere mais tempo pela inicialização do LocalStack (às vezes leva mais de 30 segundos)

**Diagnóstico**:
```bash
# Confira se o LocalStack está respondendo
curl -v http://localhost:4566

# Veja nos logs dos módulos os erros de conexão
# Procure no Terminal 1, 2 ou 3, onde você rodou o mvn quarkus:dev
```

### Os serviços do LocalStack não foram inicializados

**Problema**: a tabela do DynamoDB ou os tópicos do SNS não são encontrados

**Causa**: o script de inicialização não rodou ou falhou

**Solução**:
```bash
# Confira os logs do LocalStack
./run.sh logs

# Procure linhas como:
# "LocalStack Init: Recursos criados com sucesso"
# Ou linhas de erro começando com "ERR"

# Se necessário, rode o script de inicialização manualmente
docker compose exec localstack /docker-entrypoint-initaws.d/init-aws.sh
```

### Testes com timeout

**Problema**: o script de teste E2E trava ou estoura o tempo limite

**Causa**: os módulos não respondem, ou o endpoint GraphQL não está disponível

**Solução**:
1. Confirme que os módulos estão rodando: veja os Terminais 1, 2 e 3
2. Teste o endpoint manualmente:
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ __typename }"}'
```
3. Veja nos logs do módulo se houve erro na inicialização
4. Reinicie o módulo que não está respondendo

## Limpeza

Para remover o estado do LocalStack e zerar o ambiente de testes:

```bash
# Para todos os contêineres e remove os volumes
./run.sh clean

# Isso apaga todos os dados do DynamoDB, as mensagens do SNS etc.
# Útil antes de um novo ciclo de testes
```

## Confirmar que a configuração está completa

Rode este comando para verificar se tudo está funcionando:

```bash
./run.sh health && ./run.sh resources
```

Saída esperada:

```
🩺 Verificando saúde dos serviços...
  ✅ LocalStack (edge:4566): UP

📊 Listando recursos no LocalStack...

  📦 DynamoDB Tables:
     - feedbacks

  📨 SNS Topics:
     - arn:aws:sns:us-east-1:000000000000:feedback-urgent-topic
     - arn:aws:sns:us-east-1:000000000000:feedback-report-topic

  📬 SQS Queues:
     - http://localhost:4576/000000000000/notification-queue
     - http://localhost:4576/000000000000/report-queue
```

Se todos os recursos estiverem presentes e todos os endpoints estiverem UP, a configuração local está completa!

## Próximos passos

Depois de concluir esta configuração:

1. **Rodar os testes**: `mvn clean verify` na raiz do projeto (JDK 17)
2. **Testar a API local**: siga o passo a passo do [README](../README.md) e a [collection do Bruno](api-collection/README.md)
3. **Desenvolver funcionalidades**: os módulos já estão prontos para o desenvolvimento com hot reload

## Referências

- [Documentação do LocalStack](https://docs.localstack.cloud/)
- [Serviços da AWS no Quarkus](https://quarkus.io/guides/amazon-services)
- [AWS SDK para Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [Desenvolvimento local com DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html)

## Suporte

Para problemas ou dúvidas:

1. Consulte a seção de solução de problemas acima
2. Veja os logs do LocalStack: `./run.sh logs`
3. Confira os logs dos módulos nos respectivos terminais
4. Veja os logs de teste gerados em `/tmp/e2e-logs/`
