# Infraestrutura como Código (Terraform)

Provisiona o ambiente Kubernetes local da Oficina API usando **kind** (Kubernetes IN Docker).

## Recursos criados

| Recurso | Arquivo | Descrição |
|---|---|---|
| `kind_cluster.oficina` | `cluster.tf` | Cluster Kubernetes local (1 nó control-plane) |
| `helm_release.metrics_server` | `cluster.tf` | metrics-server (necessário para o HPA escalar por CPU/memória) |
| `kubernetes_namespace_v1.oficina` | `cluster.tf` | Namespace `oficina` |
| `random_password` / `random_id` | `secrets.tf` | Senha do banco, chave JWT e token do webhook gerados automaticamente |
| `kubernetes_secret_v1.app` | `secrets.tf` | Secret `oficina-secret` (credenciais do banco, JWT, token do webhook) |
| `kubernetes_config_map_v1.postgres_init` | `database.tf` | Scripts SQL de `init-scripts/` |
| `kubernetes_service_v1.postgres` | `database.tf` | Service interno `postgres:5432` |
| `kubernetes_stateful_set_v1.postgres` | `database.tf` | PostgreSQL 16 com volume persistente de 1Gi |

A **aplicação** (Deployment, Service, ConfigMap, HPA e Mailpit) é publicada pelos manifestos em `/k8s`,
via `scripts/deploy-app.sh` ou pela pipeline de CI/CD.

## Pré-requisitos
Docker, Terraform >= 1.6, kubectl e kind (já incluídos no devcontainer do Codespaces).

## Como aplicar
```bash
cd infra
terraform init        # baixa os providers
terraform plan        # mostra o que será criado
terraform apply       # cria cluster + metrics-server + namespace + secret + banco
cd ..
bash scripts/deploy-app.sh   # build da imagem + deploy da aplicação
```

## Verificar
```bash
kubectl get nodes
kubectl get pods,svc,pvc -n oficina
terraform -chdir=infra output
```

## Destruir tudo
```bash
cd infra && terraform destroy
```
