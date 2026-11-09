# 📧 Solicitação de Permissões AWS para Deploy SAM

**Para: Administrador da Conta de Gerenciamento AWS Organization (557296934842)**

---

## Contexto

- **Projeto:** FIAP Tech Challenge Fase-4 (Plataforma de Feedback Serverless)
- **Usuário IAM:** `arn:aws:iam::825370131972:user/fase-4-feedback-deploy`
- **Região:** `sa-east-1` (São Paulo)
- **Stack CloudFormation:** `fase-4-feedback`
- **SCP bloqueante:** `arn:aws:organizations::557296934842:policy/o-a8jew4epoq/service_control_policy/p-2sk8b8dp`

## Problema

Todas as operações S3 e CloudFormation para deploy com SAM CLI estão bloqueadas pela SCP, impedindo a entrega do projeto.

---

## Solicitação

### **Passo 1: Criar bucket S3** (na conta 825370131972, região sa-east-1)

```bash
aws s3 mb s3://sam-artifacts-sa-east-1-dev --region sa-east-1
```

### **Passo 2: Anexar política IAM segura** ao usuário `fase-4-feedback-deploy`

Crie e anexe a seguinte política IAM (com escopos restritivos de least privilege):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "S3ArtifactsBucket",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket",
        "s3:DeleteObject",
        "s3:GetBucketLocation"
      ],
      "Resource": [
        "arn:aws:s3:::sam-artifacts-sa-east-1-dev",
        "arn:aws:s3:::sam-artifacts-sa-east-1-dev/*"
      ]
    },
    {
      "Sid": "CloudFormationDeploy",
      "Effect": "Allow",
      "Action": [
        "cloudformation:CreateStack",
        "cloudformation:UpdateStack",
        "cloudformation:DeleteStack",
        "cloudformation:DescribeStacks",
        "cloudformation:DescribeStackEvents",
        "cloudformation:DescribeStackResource",
        "cloudformation:DescribeStackResources",
        "cloudformation:GetTemplateSummary",
        "cloudformation:GetTemplate",
        "cloudformation:CreateChangeSet",
        "cloudformation:DescribeChangeSet",
        "cloudformation:ExecuteChangeSet",
        "cloudformation:DeleteChangeSet",
        "cloudformation:ListChangeSets",
        "cloudformation:ListStackResources"
      ],
      "Resource": "*"
    },
    {
      "Sid": "IAMRolesForStack",
      "Effect": "Allow",
      "Action": [
        "iam:CreateRole",
        "iam:DeleteRole",
        "iam:AttachRolePolicy",
        "iam:DetachRolePolicy",
        "iam:PutRolePolicy",
        "iam:DeleteRolePolicy",
        "iam:GetRole",
        "iam:GetRolePolicy",
        "iam:PassRole"
      ],
      "Resource": "arn:aws:iam::825370131972:role/fase-4-feedback-*"
    },
    {
      "Sid": "LambdaFunctions",
      "Effect": "Allow",
      "Action": [
        "lambda:CreateFunction",
        "lambda:UpdateFunctionCode",
        "lambda:UpdateFunctionConfiguration",
        "lambda:GetFunction",
        "lambda:DeleteFunction",
        "lambda:AddPermission",
        "lambda:RemovePermission",
        "lambda:GetPolicy",
        "lambda:ListTags",
        "lambda:TagResource"
      ],
      "Resource": "arn:aws:lambda:sa-east-1:825370131972:function:fase-4-feedback-*"
    },
    {
      "Sid": "APIGateway",
      "Effect": "Allow",
      "Action": [
        "apigateway:POST",
        "apigateway:PUT",
        "apigateway:PATCH",
        "apigateway:DELETE",
        "apigateway:GET"
      ],
      "Resource": [
        "arn:aws:apigateway:sa-east-1::/restapis",
        "arn:aws:apigateway:sa-east-1::/restapis/*",
        "arn:aws:apigateway:sa-east-1::/account"
      ]
    }
  ]
}
```

---

## Características de Segurança

✅ **IAM restrito ao prefixo `fase-4-feedback-*`** — não pode afetar outras stacks  
✅ **Lambda restrito ao prefixo `fase-4-feedback-*`** — não pode afetar outras funções  
✅ **S3 restrito ao bucket específico** — isolado de outros recursos  
✅ **Sem alteração na SCP** — impacto zero em outras contas da Organization  
✅ **Sem permissões wildcard abertas** — apenas ações necessárias

---

## O que NÃO precisa ser feito

❌ Alterar a SCP da Organization (afetaria outras contas)  
❌ Criar permissões com `Resource: "*"` para IAM/Lambda  
❌ Criar permissões administrativas (`AdministratorAccess`)

---

## Próximas Etapas (após conclusão acima)

Uma vez que o bucket seja criado e a política IAM seja anexada, o deploy funcionará com:

```bash
cd /Users/jefferson/Documents/FIAP/fase-4/infra
sam deploy --no-confirm-changeset
```

---

**Agradeço antecipadamente pela ajuda! 🙏**

---

_Documento preparado em: 8 de Novembro de 2026_  
_Projeto: FIAP Tech Challenge Fase-4_
