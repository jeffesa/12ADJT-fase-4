# Guia do vídeo de demonstração: Plataforma de feedback serverless

**Duração**: ~10-12 minutos  
**Formato**: Gravação de tela com narração  
**Público-alvo**: Stakeholders técnicos, arquitetos, desenvolvedores

---

## Resumo executivo

Esta demonstração mostra uma plataforma de feedback serverless em nível de produção, construída com AWS Lambda, DynamoDB, SNS, EventBridge e SES. A demonstração foca no fluxo de dados de ponta a ponta, mostrando como a urgência do feedback dispara notificações automaticamente e como a plataforma escala sem que se gerencie infraestrutura.

---

## Visão geral do roteiro

**Duração total: 12 minutos**

| Segmento | Duração | O que mostrar |
|---------|----------|--------------|
| **1. Arquitetura e configuração** | 1,5 min | Diagrama de arquitetura, ambiente local pronto |
| **2. Criar feedback de urgência HIGH** | 2,5 min | Mutação GraphQL, notificação por e-mail, DynamoDB |
| **3. Criar feedback de urgência LOW** | 1,5 min | Mutação GraphQL, verificar que NENHUM e-mail foi enviado |
| **4. Consultar todos os feedbacks** | 1 min | Listagem REST (GET /v1/avaliacoes) e consulta GraphQL (listFeedbacks) |
| **5. Logs e métricas do CloudWatch** | 2,5 min | Logs de execução das Lambdas, métricas de desempenho |
| **6. Passeio pelo console da AWS** | 2,5 min | Políticas IAM, fluxo de eventos, explicação da arquitetura |
| **7. Resumo e escalabilidade** | 1 min | Principais conclusões |

---

## Segmento 1: Arquitetura e configuração (1,5 minuto)

**Pontos da narração:**
- "Esta é uma plataforma de feedback serverless que roda inteiramente na AWS."
- "A plataforma tem três componentes principais: ingestão de feedback, entrega de notificações e geração de relatórios."
- "Toda a infraestrutura é definida como código usando AWS SAM e implantada no LocalStack para fins de demonstração."

**Ações na tela:**
1. Abra um terminal mostrando a estrutura de diretórios do worktree:
   ```bash
   cd /Users/jefferson/Documents/FIAP/fase-4/.worktrees/final-docs
   tree -L 2
   ```

2. Mostre o diagrama de arquitetura do README.md:
   - Abra o README.md em um editor
   - Mostre o diagrama de arquitetura em ASCII, que ilustra:
     - API Gateway (endpoint GraphQL)
     - Lambda 1 (Receive Feedback) → DynamoDB
     - Tópico SNS (urgência HIGH)
     - Lambda 2 (Send Notification) → SES
     - EventBridge → Lambda 3 (Generate Report)

3. Verifique se o LocalStack está em execução:
   ```bash
   ./run.sh health
   ```
   - Esperado: `✅ LocalStack (edge:4566): UP`

**Fechamento da narração:**
- "Tudo o que vocês veem é implantado localmente com o LocalStack, que emula serviços da AWS."
- "Em produção, isso escalaria para milhares de requisições por segundo sem nenhum gerenciamento de infraestrutura."

---

## Segmento 2: Criar feedback de urgência HIGH (2,5 minutos)

**Pontos da narração:**
- "Vamos criar um feedback de urgência alta dando a ele uma nota baixa (0-5)."
- "Ele será marcado automaticamente como urgência HIGH e disparará uma notificação por e-mail."
- "A API GraphQL valida a entrada e armazena o registro no DynamoDB."

**Ações na tela:**

1. Abra um terminal e crie um feedback de urgência HIGH usando GraphQL:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{
       "query": "mutation { receiveFeedback(input: {description: \"Critical issue: System crashes on login\", rating: 2}) { id description rating urgency createdAt } }"
     }'
   ```

2. Mostre a resposta:
   - Destaque os campos retornados:
     - `id`: identificador único (UUID)
     - `description`: descrição do feedback
     - `rating`: nota (2 neste caso)
     - `urgency`: "HIGH" (calculado automaticamente)
     - `createdAt`: data e hora

3. Anote o ID do feedback para a verificação posterior (copie e cole em um bloco de notas).

4. Troque imediatamente para o cliente de e-mail ou a caixa de entrada:
   - Abra o navegador ou o cliente de e-mail mostrando os e-mails recebidos
   - Mostre que um e-mail foi recebido em `admin@feedback-fase-4.com`
   - Assunto: "Alerta crítico de feedback - Nota: 2"
   - O corpo deve conter a descrição do feedback e o horário

5. Troque para o Console da AWS → DynamoDB (ou a interface do LocalStack):
   - Acesse a tabela `feedbacks`
   - Mostre o feedback que acabou de ser criado:
     - Consulte pelo ID ou faça um scan da tabela
     - Destaque os campos: `id`, `rating`, `urgency`, `description`, `createdAt`
     - Aponte: "Repare que a urgência é HIGH porque a nota é 2"

**Fechamento da narração:**
- "Em poucos segundos, o feedback foi armazenado no DynamoDB e uma notificação por e-mail foi enviada."
- "Todo o fluxo é automatizado pelo nosso pipeline serverless."

---

## Segmento 3: Criar feedback de urgência LOW (1,5 minuto)

**Pontos da narração:**
- "Agora vamos criar um feedback positivo com nota alta (8-10)."
- "Ele será marcado com urgência LOW e NENHUM e-mail será enviado."
- "Isso demonstra como filtramos as notificações pela urgência."

**Ações na tela:**

1. Crie um feedback de urgência LOW usando GraphQL:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{
       "query": "mutation { receiveFeedback(input: {description: \"Great service! Very satisfied with the experience.\", rating: 9}) { id description rating urgency createdAt } }"
     }'
   ```

2. Mostre a resposta:
   - Destaque: `urgency: "LOW"` (calculado automaticamente a partir da nota 9)

3. Volte ao cliente de e-mail:
   - Mostre que NENHUM e-mail novo foi recebido para este feedback
   - Aponte: "Nenhuma notificação foi enviada porque feedback de urgência LOW não dispara e-mails."

4. Troque para o DynamoDB:
   - Faça um scan da tabela de feedbacks novamente
   - Mostre que agora existem os dois feedbacks:
     - Primeiro: nota 2, urgência HIGH
     - Segundo: nota 9, urgência LOW

5. Opcional: crie um feedback de urgência MEDIUM (nota 6-7) para mostrar os três estados:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{
       "query": "mutation { receiveFeedback(input: {description: \"Average experience, room for improvement.\", rating: 6}) { id description rating urgency createdAt } }"
     }'
   ```

**Fechamento da narração:**
- "A mesma mutação GraphQL trata todos os níveis de urgência automaticamente."
- "A plataforma roteia as notificações de forma inteligente com base em regras de negócio, sem configuração manual."

---

## Segmento 4: Consultar todos os feedbacks (1 minuto)

**Pontos da narração:**
- "Vamos verificar que todo feedback criado pode ser consultado pelo GraphQL."
- "O endpoint de consulta fornece uma única fonte de verdade para todos os dados de feedback."

**Ações na tela:**

1. Execute a consulta GraphQL para listar todos os feedbacks:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{
       "query": "{ listFeedbacks { id description rating urgency createdAt } }"
     }'
   ```

2. Mostre a resposta contendo os três feedbacks (HIGH, MEDIUM, LOW).

3. Formate a saída com `jq` para facilitar a leitura:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{"query": "{ listFeedbacks { id description rating urgency createdAt } }"}' | jq '.data.listFeedbacks[]'
   ```

4. Repita a listagem pela API REST, que retorna `avaliacoes` e `total`:
   ```bash
   curl -X GET http://localhost:8080/v1/avaliacoes \
     -H "Authorization: Bearer $API_TOKEN" | jq '.avaliacoes[]'
   ```

**Fechamento da narração:**
- "Todo feedback é persistido e pode ser consultado em tempo real."
- "A tabela do DynamoDB está configurada com cobrança sob demanda, então você paga apenas pelo que usa."

---

## Segmento 5: Logs e métricas do CloudWatch (2,5 minutos)

**Pontos da narração:**
- "Vamos examinar os logs do CloudWatch para entender o que aconteceu por trás dos panos."
- "Cada execução de Lambda é registrada, e podemos verificar as métricas de desempenho."
- "As três Lambdas terminaram em menos de 1 segundo."

**Ações na tela:**

1. Abra o Console da AWS (ou a interface de CloudWatch do LocalStack):
   - Acesse CloudWatch → Log Groups
   - Mostre os grupos de log de cada função:
     - `/aws/lambda/dev-receive-feedback`
     - `/aws/lambda/dev-send-notification`
     - `/aws/lambda/dev-generate-report`

2. Clique no grupo de log do receive-feedback:
   - Mostre o log stream da criação do feedback de urgência HIGH
   - Destaque os eventos principais:
     - "Mutação GraphQL receiveFeedback recebida"
     - "Validando entrada: description, rating (0-10)"
     - "Calculando urgência a partir da nota 2 → HIGH"
     - "Persistindo feedback no DynamoDB"
     - "Publicando mensagem no tópico SNS: feedback-urgent-topic"
     - "Duração: 247ms"

3. Clique no grupo de log do send-notification:
   - Mostre o log stream com o disparo pelo SNS
   - Destaque os eventos principais:
     - "Disparado por mensagem SNS do feedback-urgent-topic"
     - "Extraindo detalhes do feedback do evento"
     - "Enviando e-mail via SES para admin@feedback-fase-4.com"
     - "Assunto do e-mail: Alerta crítico de feedback"
     - "Duração: 412ms"

4. Troque para a aba de Métricas:
   - Mostre a contagem de invocações da Lambda (3 invocações)
   - Mostre a contagem de erros (0)
   - Mostre o histograma de duração (todas abaixo de 500ms)

**Fechamento da narração:**
- "Os logs mostram exatamente o que aconteceu em cada etapa do pipeline."
- "O desempenho é excelente: todas as operações terminaram em menos de 500ms."
- "Este é o benefício do serverless: registro, monitoramento e escalabilidade automáticos."

---

## Segmento 6: Passeio pelo Console da AWS (2,5 minutos)

**Pontos da narração:**
- "Vamos afastar o zoom e entender a arquitetura do ponto de vista da AWS."
- "Vamos revisar o modelo de segurança do IAM, o fluxo de eventos e por que este desenho escala."

**Ações na tela:**

1. **Abra o Console da AWS (stack do CloudFormation)**:
   - Mostre a stack implantada: `fase-4-stack`
   - Expanda os recursos para mostrar:
     - Tabela do DynamoDB: `dev-feedback-table`
     - Tópico SNS: `dev-feedback-urgent-topic`
     - Funções Lambda (3 listadas)
     - Funções IAM (3 listadas)
     - Grupos de log do CloudWatch (3 listados)

2. **Mostre as funções IAM**:
   - Clique em `ReceiveFeedbackRole`:
     - Política inline: `DynamoDBWritePolicy` → permite `PutItem` na tabela de feedbacks
     - Política inline: `SNSPublishPolicy` → permite `Publish` no feedback-urgent-topic
     - Política gerenciada: `AWSLambdaBasicExecutionRole` → permite gravar logs
     - **Princípio do menor privilégio**: apenas as permissões necessárias

   - Clique em `SendNotificationRole`:
     - Política inline: `SESEmailPolicy` → permite `SendEmail` e `SendRawEmail`
     - Sem acesso ao DynamoDB (não precisa dele)
     - Política gerenciada: `AWSLambdaBasicExecutionRole` → permite gravar logs
     - **Segurança**: responsabilidade isolada

   - Clique em `GenerateReportRole`:
     - Política inline: `DynamoDBScanPolicy` → permite `Scan` e `Query` na tabela de feedbacks
     - Política inline: `SESEmailPolicy` → permite enviar e-mails
     - Política gerenciada: `AWSLambdaBasicExecutionRole` → permite gravar logs
     - **Separação de responsabilidades**: esta Lambda apenas lê, não escreve

3. **Mostre o tópico SNS**:
   - ARN do tópico: `arn:aws:sns:us-east-1:000000000000:dev-feedback-urgent-topic`
   - Assinaturas: 1 (a Lambda send-notification)
   - **Padrão de projeto**: o SNS atua como barramento de mensagens para desacoplamento

4. **Mostre a tabela do DynamoDB**:
   - Modo de cobrança: sob demanda (paga por requisição)
   - TTL: desabilitado nas tabelas de dev e prod (verificado com `describe-time-to-live`, sem exclusão automática)
   - Chave de partição: `id` (UUID)
   - **Escalabilidade**: escala sob demanda

5. **Mostre a regra do EventBridge** (se visível no template SAM):
   - Nome: `feedback-report-rule`
   - Padrão: casa com eventos do tópico SNS
   - Destino: invocar a Lambda `generate-report`
   - **Arquitetura orientada a eventos**: assíncrona, com baixo acoplamento

**Fechamento da narração:**
- "A arquitetura inteira é definida em código usando AWS SAM (Serverless Application Model)."
- "A segurança está embutida: cada Lambda tem apenas as permissões mínimas necessárias."
- "A escala é automática: o Lambda absorve picos de tráfego, o DynamoDB escala sob demanda e não é preciso planejar capacidade."

---

## Segmento 7: Resumo e escalabilidade (1 minuto)

**Pontos da narração:**
- "Vamos recapitular o que foi demonstrado."
- "Esta arquitetura atende requisitos reais de escala."

**Ações na tela:**

1. Volte ao terminal e mostre a ajuda do run.sh:
   ```bash
   ./run.sh help
   ```

2. Mostre os comandos disponíveis para destacar as capacidades da plataforma:
   - `./run.sh health` → Monitoramento da infraestrutura
   - `./run.sh resources` → Criação e listagem de recursos
   - `./run.sh logs` → Visualização unificada de logs
   - `./run.sh test` → Testes automatizados

**Principais pontos para destacar:**

- **Confiabilidade**: redundância em 3 camadas (DynamoDB, SNS e SES têm replicação integrada)
- **Escalabilidade**: projetada para milhares de requisições por segundo; paga-se apenas pelo uso
- **Segurança**: as políticas IAM aplicam o menor privilégio; não há acesso direto ao banco pelas APIs
- **Manutenibilidade**: infraestrutura como código (SAM); os serviços cuidam de correções e atualizações
- **Monitoramento**: o CloudWatch oferece observabilidade completa desde o início
- **Custo**: não há servidores rodando 24/7; a cobrança sob demanda acompanha o uso real

**Narração final:**

"Esta plataforma de feedback serverless demonstra como serviços modernos da AWS podem ser combinados para construir aplicações seguras, escaláveis e fáceis de manter. Os três fluxos de feedback (HIGH → notificação, MEDIUM → enfileirado, LOW → armazenado) mostram a arquitetura orientada a eventos em ação. Em produção, este desenho suportaria milhões de registros de feedback sem nenhuma carga de gerenciamento de infraestrutura."

---

## Notas técnicas para o dia da demonstração

### Checklist antes da demonstração

- [ ] LocalStack em execução e saudável (`./run.sh health`)
- [ ] Os três módulos em execução no modo dev (receive-feedback, send-notification, generate-report)
- [ ] Recursos criados (`./run.sh resources`)
- [ ] Cliente de e-mail aberto e configurado para mostrar a caixa de entrada de `admin@feedback-fase-4.com`
- [ ] Console da AWS ou painel do LocalStack acessível
- [ ] Terminal limpo e pronto para os comandos
- [ ] Software de gravação (OBS, QuickTime etc.) configurado

### Dicas de gravação

- **Zoom**: defina a fonte do terminal em 14pt para facilitar a leitura
- **Ritmo**: fale com clareza e faça uma pausa após cada ação
- **Tempo**: use o silêncio estrategicamente (por exemplo, aguarde o e-mail chegar)
- **Anotações**: use ferramentas de destaque na tela para apontar os elementos principais
- **Erros**: se ocorrer um erro, pare e explique a causa raiz ou repita
- **Qualidade de vídeo**: grave em 1080p, no mínimo 30 FPS

### Resumo dos comandos

**Verificar a saúde:**
```bash
./run.sh health
```

**Criar feedback (HIGH):**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { receiveFeedback(input: {description: \"Critical issue: System crashes on login\", rating: 2}) { id description rating urgency createdAt } }"}'
```

**Criar feedback (LOW):**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { receiveFeedback(input: {description: \"Great service! Very satisfied with the experience.\", rating: 9}) { id description rating urgency createdAt } }"}'
```

**Consultar todos os feedbacks:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ listFeedbacks { id description rating urgency createdAt } }"}'
```

**Ver a tabela do DynamoDB:**
```bash
docker compose exec localstack awslocal dynamodb scan \
  --table-name feedbacks \
  --region us-east-1 \
  --output json | jq '.Items[] | {id: .id.S, rating: .rating.N, urgency: .urgency.S}'
```

**Ver os logs do CloudWatch:**
```bash
./run.sh logs
```

---

## Resultados esperados da demonstração

### Fluxo de urgência HIGH
- ✅ Mutação GraphQL aceita com nota 2
- ✅ O campo urgency retorna "HIGH"
- ✅ Feedback armazenado no DynamoDB em até 300ms
- ✅ Mensagem SNS publicada no feedback-urgent-topic
- ✅ Lambda send-notification disparada
- ✅ E-mail recebido na caixa de entrada de `admin@feedback-fase-4.com` em até 500ms
- ✅ Os logs do CloudWatch mostram todas as etapas com horários

### Fluxo de urgência LOW
- ✅ Mutação GraphQL aceita com nota 9
- ✅ O campo urgency retorna "LOW"
- ✅ Feedback armazenado no DynamoDB em até 300ms
- ✅ Nenhuma mensagem SNS publicada (urgência LOW não dispara notificações)
- ✅ Nenhum e-mail enviado
- ✅ Feedback ainda consultável pelo GraphQL

### Fluxo de consulta
- ✅ A consulta GraphQL retorna todos os feedbacks criados
- ✅ Todos os campos estão presentes e formatados corretamente
- ✅ A consulta é concluída em até 100ms

### Observabilidade
- ✅ Os logs do CloudWatch mostram todas as execuções das Lambdas
- ✅ Os tempos de execução são menores que 500ms por função
- ✅ Nenhum erro nos logs
- ✅ O painel de métricas mostra a contagem de invocações e a taxa de erro corretas (0)

---

## Apêndice: princípios de arquitetura explicados

### Cálculo de urgência
```
Rating 0-5  → HIGH    (critical feedback requires immediate attention)
Rating 6-7  → MEDIUM  (moderate feedback, queued for review)
Rating 8-10 → LOW     (positive feedback, logged for analytics)
```

### Pipeline orientado a eventos
1. **Gatilho**: a mutação GraphQL invoca a Lambda receive-feedback
2. **Validação**: a entrada é validada (description, nota de 0 a 10)
3. **Persistência**: o feedback é armazenado no DynamoDB com UUID gerado
4. **Roteamento**: se a urgência for HIGH, a mensagem é publicada no tópico SNS
5. **Notificação**: a Lambda send-notification é disparada pelo SNS
6. **Entrega**: e-mail enviado via SES para o administrador
7. **Análise**: o EventBridge captura todos os eventos para a Lambda de relatórios

### Benefícios do serverless
- **Sem servidores para gerenciar**: a AWS cuida da infraestrutura
- **Escala automática**: atende de 1 req/s a 10.000 req/s
- **Pague pelo uso**: cobrança apenas pelo tempo de execução e armazenamento de dados
- **Segurança embutida**: papéis IAM, criptografia em trânsito e em repouso
- **Monitoramento integrado**: logs e métricas do CloudWatch incluídos
- **Desenvolvimento rápido**: foco na regra de negócio, não na infraestrutura

---

## Modelo de roteiro em vídeo

**[INTRODUÇÃO - 30 segundos]**

"Bem-vindos à demonstração da Plataforma de feedback serverless da Fase-4 da FIAP. Nos próximos 12 minutos, vocês verão como AWS Lambda, DynamoDB, SNS e SES funcionam juntos para criar um sistema de coleta de feedback escalável e seguro. Vamos criar feedbacks com diferentes níveis de urgência, acompanhar as notificações sendo disparadas em tempo real e explorar os logs do CloudWatch que sustentam nossa observabilidade."

**[DEMONSTRAÇÃO PRINCIPAL - 10 minutos]**
*Siga os segmentos 1 a 6 conforme descrito acima*

**[ENCERRAMENTO - 1 minuto]**

"Esta arquitetura demonstra boas práticas em design serverless: cálculo automático de urgência, comunicação orientada a eventos, segurança por meio de papéis IAM e observabilidade completa. Toda a infraestrutura é definida como código usando AWS SAM, o que a torna reproduzível e fácil de manter. Obrigado por assistir."

---

