#!/usr/bin/env bash
# Publica a APLICACAO no cluster criado pelo Terraform (infra/).
# Pre-requisito: cd infra && terraform apply
# Uso: bash scripts/deploy-app.sh
set -euo pipefail
cd "$(dirname "$0")/.."

CLUSTER="${CLUSTER_NAME:-oficina}"
NAMESPACE="oficina"
IMAGE="${IMAGE:-oficina-api:local}"

echo ">> [1/5] Verificando cluster '$CLUSTER' e banco (criados pelo Terraform)"
kind get clusters | grep -qx "$CLUSTER" || { echo "Cluster nao encontrado. Rode: cd infra && terraform apply"; exit 1; }
kubectl get secret oficina-secret -n "$NAMESPACE" >/dev/null
kubectl -n "$NAMESPACE" rollout status statefulset/postgres --timeout=180s

if [ "${SKIP_BUILD:-false}" = "true" ]; then
  echo ">> [2/5] Build ignorado (SKIP_BUILD=true): usando imagem $IMAGE ja existente"
else
  echo ">> [2/5] Build da imagem $IMAGE"
  docker build -t "$IMAGE" .
fi

echo ">> [3/5] Carregando imagem no cluster"
kind load docker-image "$IMAGE" --name "$CLUSTER" || { echo "kind load falhou; importando direto no containerd do no"; docker save "$IMAGE" -o /tmp/oficina-api.tar && docker cp /tmp/oficina-api.tar "$CLUSTER-control-plane":/oficina-api.tar && docker exec "$CLUSTER-control-plane" ctr -n k8s.io images import /oficina-api.tar; }

echo ">> [4/5] Aplicando manifestos da aplicacao (ConfigMap, App, HPA, Mailpit)"
kubectl apply -f k8s/01-configmap.yaml
kubectl apply -f k8s/04-app.yaml
kubectl apply -f k8s/05-hpa.yaml
kubectl apply -f k8s/06-mailpit.yaml
kubectl -n "$NAMESPACE" set image deploy/oficina-api oficina-api="$IMAGE"
kubectl -n "$NAMESPACE" rollout restart deploy/oficina-api

echo ">> [5/5] Aguardando a aplicacao"
kubectl -n "$NAMESPACE" rollout status deploy/oficina-api --timeout=300s
kubectl get pods,svc,hpa -n "$NAMESPACE"
echo ">> OK. Teste: kubectl port-forward svc/oficina-api 8080:80 -n $NAMESPACE"
