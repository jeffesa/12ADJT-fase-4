# Handoff para Nova Sessão - Tech Challenge Fase 4

## Contexto do Projeto

Plataforma de feedback de aulas com funções serverless (AWS Lambda), persistência DynamoDB, notificações via SNS/SES e relatórios periódicos via EventBridge. Utiliza Quarkus 3.8.x com Clean Architecture, seguindo o padrão das Fases 2 e 3.

**Repositório:** https://github.com/jeffesa/12ADJT-fase-4
**Kanban:** (criar projeto no GitHub após criar as issues)
**Branch principal de desenvolvimento:** `develop`

---

## Estado Atual

### O que já existe no projeto:
- Estrutura de pastas criada (módulos, docs, .github/workflows, infra)
- `docs/planejamento/BACKLOG.md` — todas as tasks definidas
- `docs/planejamento/RESUMO_CONVERSA.md` — decisões técnicas e stack
- PDF dos requisitos preservado em `docs/`

### O que ainda NÃO existe:
- Nenhum `pom.xml` (nem parent nem módulos)
- Nenhum código Java
- Nenhum `docker-compose.yml`
- Nenhum `infra/template.yaml`
- Nenhum `.gitignore`
- Nenhum workflow `.github/workflows/`
- Repositório GitHub ainda não inicializado com código

### Próxima task a executar:
**TASK-001** — Criar estrutura multi-module Maven com Quarkus

---

## Instruções para a Nova Sessão

### 1. VALIDAR O ESTADO ATUAL

Antes de continuar, verificar:
```bash
# Checar estrutura atual
ls /Users/jefferson/Documents/FIAP/fase-4/

# Checar issues no kanban (após criar)
gh issue list --repo jeffesa/12ADJT-fase-4 --state open

# Verificar estado do repositório
cd /Users/jefferson/Documents/FIAP/fase-4
git log --oneline -10
```

### 2. FLUXO DE DESENVOLVIMENTO (seguir sempre)

Para cada task:
1. Mover issue para "In Progress" no Kanban
2. Criar branch: `git checkout develop && git checkout -b feature/task-XXX-descricao`
3. Implementar conforme critérios de aceitação no BACKLOG.md
4. Commitar com conventional commits: `feat(task-XXX): descrição`
5. Push: `git push -u origin feature/task-XXX-descricao`
6. PR para develop: `gh pr create --base develop`
7. Merge: `gh pr merge --merge`
8. Fechar issue e mover para Done no Kanban
9. Voltar para develop: `git checkout develop && git pull`

### 3. PADRÕES A SEGUIR

- **Arquitetura:** Clean Architecture — domain → application → infra (mesmo padrão fases 2 e 3)
- **Pacotes:**
  - `com.fiap.feedback.{domain,application,infra}` (receive-feedback)
  - `com.fiap.notification.{domain,application,infra}` (send-notification)
  - `com.fiap.report.{domain,application,infra}` (generate-report)
- **Commits:** conventional commits (`feat()`, `fix()`, `ci()`, `docs()`, `test()`)
- **Branches:** `feature/task-XXX-descricao`, `bugfix/descricao`
- **Testes:** JUnit 5 + Mockito, cobertura ≥ 80% (JaCoCo)
- **Código:** inglês; **Documentação/comentários:** português
- **Deploy AWS:** apenas na TASK-019 (Sprint 5) — usar LocalStack até lá

### 4. IMPORTANTE — ECONOMIA DE CRÉDITOS

- NÃO lançar workflows automatizados sem necessidade
- NÃO criar código antes de confirmar com o usuário
- Desenvolver task por task, aguardando aprovação
- Deploy na AWS real apenas quando o usuário pedir (TASK-019)
- Usar LocalStack para todos os testes locais até a Sprint 5

### 5. REFERÊNCIAS

- **Fase 2 (referência Clean Architecture):** /Users/jefferson/Documents/FIAP/fase-2
- **Fase 3 (referência multi-module):** /Users/jefferson/Documents/FIAP/fase-3
- **BACKLOG completo:** /Users/jefferson/Documents/FIAP/fase-4/docs/planejamento/BACKLOG.md
- **Decisões técnicas:** /Users/jefferson/Documents/FIAP/fase-4/docs/planejamento/RESUMO_CONVERSA.md
- **PDF requisitos:** /Users/jefferson/Documents/FIAP/fase-4/docs/ADJT - Fase 4 - Tech Challenge_rev.pdf

### 6. ESTRUTURA ESPERADA DO PROJETO

```
fase-4/
├── .github/workflows/          ← ci-cd.yml, branch-naming.yml, auto-pr-to-main.yml
├── docs/
│   ├── planejamento/           ← BACKLOG.md, RESUMO_CONVERSA.md, HANDOFF_SESSION.md
│   ├── api-collection/         ← Postman collection
│   ├── ci-cd/                  ← documentação CI/CD
│   ├── deploy/                 ← AWS_DEPLOY_GUIDE.md
│   └── qualidade/              ← MONITORING_GUIDE.md
├── receive-feedback/           ← Lambda 1 (Quarkus)
│   └── src/main/java/com/fiap/feedback/{domain,application,infra}
├── send-notification/          ← Lambda 2 (Quarkus)
│   └── src/main/java/com/fiap/notification/{domain,application,infra}
├── generate-report/            ← Lambda 3 (Quarkus)
│   └── src/main/java/com/fiap/report/{domain,application,infra}
├── infra/
│   ├── template.yaml           ← AWS SAM
│   └── samconfig.toml          ← parâmetros por ambiente (AdminEmail, etc.)
├── pom.xml                     ← parent POM multi-module
├── docker-compose.yml          ← LocalStack
├── run.sh
├── .gitignore
├── .dockerignore
├── sonar-project.properties
└── README.md
```

---

*Criado em: Outubro/2026*
*Atualizar este arquivo após cada sessão com o estado atual das tasks*
