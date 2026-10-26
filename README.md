# Tech Challenge Fase 4 - Plataforma de Feedback Serverless (AWS)

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=jeffesa_12ADJT-fase-4)

## Visão Geral

Plataforma serverless multi-módulo AWS para gerenciamento de feedback em aplicações. A solução implementa 3 lambdas independentes em Quarkus para processamento de feedback, envio de notificações e geração de relatórios.

### Arquitetura de Módulos

- **receive-feedback**: Lambda para recebimento e validação de feedback
- **send-notification**: Lambda para envio de notificações aos usuários
- **generate-report**: Lambda para geração de relatórios de feedback

## Pré-requisitos

- Java 17+
- Maven 3.9+
- AWS SDK configurado para testes locais (opcional)
- SONAR_TOKEN (para scans SonarCloud)

## Code Coverage & Quality Gate

### Requisitos de Qualidade

- **Cobertura mínima**: ≥80% por módulo
- **Quality Gate**: 0 bugs, 0 vulnerabilidades
- **Compliance**: Scans semanais no SonarCloud em pushes para `develop`

### Executando Verificação Local de Cobertura

Gere relatórios JaCoCo para todos os 3 módulos:

```bash
mvn clean verify
```

Isso irá:
1. Compilar todos os módulos
2. Executar todos os testes unitários
3. Gerar relatórios JaCoCo (XML + HTML) em cada módulo
4. Agregar relatórios no nível pai (target/site/jacoco/)

Após a execução, visualize o relatório agregado:

```bash
# Abrir em navegador (ou visualizar via IDE)
open target/site/jacoco/index.html
```

Ou visualize relatórios por módulo:

```bash
open receive-feedback/target/site/jacoco/index.html
open send-notification/target/site/jacoco/index.html
open generate-report/target/site/jacoco/index.html
```

### Scan Local do SonarCloud

Para testar a integração com SonarCloud localmente, é necessário um SONAR_TOKEN válido:

```bash
export SONAR_TOKEN=<seu-token-do-github-secrets>

mvn sonar:sonar \
  -Dsonar.projectKey=jeffesa_12ADJT-fase-4 \
  -Dsonar.organization=jeffesa \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.login=$SONAR_TOKEN
```

Após o scan, visualize os resultados no dashboard:
[SonarCloud Dashboard](https://sonarcloud.io/summary/new_code?id=jeffesa_12ADJT-fase-4)

### Métricas de Cobertura Esperadas

- **Linha Coverage**: ≥80% por módulo
- **Branch Coverage**: ≥75% (recomendado)
- **Complexidade Ciclomática**: ≤10 por método

### Exclusões de Cobertura

As seguintes classes estão excluídas da análise de cobertura:

- Classes de configuração (`**/config/**`)
- DTOs (`**/dto/**`)
- Handlers (`**/infra/handler/**`, `**/*Handler.java`)
- Application entry point (`**/*Application.java`)

## CI/CD Pipeline

A pipeline é acionada automaticamente em:

- Push para `main` ou `develop`
- Pull requests com destino `develop`

### Etapas da Pipeline

1. **Checkout**: Clone do repositório com histórico
2. **Setup Java**: Configura JDK 17 (build) e JDK 21 (SonarCloud scanner)
3. **Build & Test**: `mvn clean verify` com geração de cobertura JaCoCo
4. **SonarCloud Scan**: Análise de qualidade e upload de métricas
5. **Quality Gate**: Validação de critérios de aceição

### Secrets Necessários

- `SONAR_TOKEN`: Token de autenticação SonarCloud (criar em SonarCloud > Organization Settings > Security)
- `GITHUB_TOKEN`: Automaticamente disponível (usada para integração com GitHub)

## Development Setup

### Build Local

```bash
# Build completo com testes
mvn clean verify

# Build sem testes (rápido)
mvn clean package -DskipTests

# Build de módulo específico
mvn clean verify -pl receive-feedback
```

### Testes Locais com Bruno ou Postman

Para testar as APIs GraphQL/REST localmente:

1. Importe as collections fornecidas em `./bruno` ou `./postman`
2. Configure as variáveis de ambiente (endpoints, tokens)
3. Execute requisições contra os lambdas locais ou ambiente de teste

## Troubleshooting

### JaCoCo reports não estão sendo gerados

```bash
# Verificar se os arquivos XML foram criados
find . -name jacoco.xml -type f

# Se nenhum for encontrado, verificar a saída do Maven
mvn clean verify -X | grep -i jacoco
```

### SonarCloud scan falhando

```bash
# Verificar credenciais
echo $SONAR_TOKEN

# Executar scan em verbose
mvn sonar:sonar -X -Dsonar.projectKey=jeffesa_12ADJT-fase-4 \
  -Dsonar.organization=jeffesa \
  -Dsonar.host.url=https://sonarcloud.io
```

### Testes falhando localmente

```bash
# Rodar testes de um módulo específico com logs detalhados
mvn -pl send-notification test -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

## Referências

- [SonarCloud Documentação](https://docs.sonarcloud.io/)
- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Quarkus Guide](https://quarkus.io/guides/)
- [AWS Lambda Quarkus](https://quarkus.io/guides/aws-lambda)

---

**Última atualização**: Outubro 2025 | **Maintainer**: FIAP Tech Challenge Fase 4
