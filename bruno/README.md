# Collection Bruno - FIAP Fase 4

Requisições da API de feedbacks (`/avaliacao` e `/graphql`) para os ambientes local e prod.

## Como abrir

1. No Bruno: **Open Collection** e selecione a pasta `bruno/` deste repositório.
2. No canto superior direito, escolha o ambiente `local` ou `prod`.

## Ambientes e apiKey

| Ambiente | baseUrl |
|---|---|
| `local` | `http://localhost:8080` |
| `prod` | `https://v32wl8xbtk.execute-api.sa-east-1.amazonaws.com/prod` |

`apiKey` é uma variável secreta, sem valor no repositório. Preencha em **Environments**, campo `apiKey`. O Bruno guarda o valor só na sua máquina.

- **local:** o mesmo valor de `GRAPHQL_API_KEY` usado ao subir o módulo `local-http` (veja a seção de HTTP local no `README.md` da raiz).
- **prod:** a chave da função graphql. Mantenha-a apenas na sua máquina e nunca faça commit.

Para subir o ambiente local (JDK 17):

```bash
mvn -DskipTests install -pl .,receive-feedback
cd local-http && GRAPHQL_API_KEY=<chave-local> mvn quarkus:dev -Ddebug=false
```

## Requisições

| Pasta | Requisição | Status esperado | Prod |
|---|---|---|---|
| `avaliacao` | Listar avaliações | 200 | Seguro (leitura) |
| `avaliacao` | Criar LOW (nota 8) | 201 | **Cria registro real** |
| `avaliacao` | Criar MEDIUM (nota 6) | 201 | **Cria registro real** |
| `avaliacao` | Criar HIGH (nota 1) | 201 | **Cria registro real e dispara SNS/email** |
| `avaliacao` | Erro nota 11 | 400 | Seguro |
| `avaliacao` | Erro descrição vazia | 400 | Seguro |
| `avaliacao` | Erro nota ausente | 500 | Seguro (não cria registro) |
| `graphql` | Listar com chave | 200 | Seguro (leitura, exige apiKey de prod) |
| `graphql` | Listar sem chave | 401 | Seguro |
| `graphql` | Listar com chave errada | 401 | Seguro |

Nota ausente devolve 500 hoje porque o handler não trata `nota` nula (não é um 400). Se o handler mudar, atualize o assert.

Regras: nota 0 a 5 = HIGH, 6 e 7 = MEDIUM, 8 a 10 = LOW. A requisição HIGH tem a tag `high-sns`.

## Bruno CLI

Dentro de `bruno/`:

```bash
npx @usebruno/cli run --env local --env-var apiKey=<chave-local> --exclude-tags=high-sns
```

Local grava na `dev-feedback-table`. Apague os itens de teste depois.
