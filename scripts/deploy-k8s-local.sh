#!/usr/bin/env bash
# Deploy local da oficina-api em cluster kind (idempotente)
# Uso: bash scripts/deploy-k8s-local.sh
set -euo pipefail
cd "$(dirname "$0")/.."

echo ">> [1/7] Cluster kind"
if ! kind get clusters 2>/dev/null | grep -qx oficina; then
  kind create cluster --name oficina
fi

echo ">> [2/7] metrics-server (necessario para o HPA)"
helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/ >/dev/null 2>&1 || true
helm repo update >/dev/null
helm upgrade --install metrics-server metrics-server/metrics-server \
  -n kube-system --set args="{--kubelet-insecure-tls}" --wait

echo ">> [3/7] ConfigMap com os scripts do banco"
kubectl create configmap postgres-init --from-file=init-scripts/ \
  -n oficina --dry-run=client -o yaml > k8s/02-postgres-init-configmap.yaml

echo ">> [4/7] Build da imagem"
docker build -t oficina-api:local .
kind load docker-image oficina-api:local --name oficina

echo ">> [5/7] Namespace e Secret"
kubectl apply -f k8s/00-namespace.yaml
if ! kubectl get secret oficina-secret -n oficina >/dev/null 2>&1; then
  kubectl create secret generic oficina-secret -n oficina \
    --from-literal=SPRING_DATASOURCE_USERNAME=postgres \
    --from-literal=SPRING_DATASOURCE_PASSWORD=postgres \
    --from-literal=JWT_SECRET="$(openssl rand -base64 32)"
fi

echo ">> [6/7] Aplicando manifestos"
for f in k8s/0*.yaml; do kubectl apply -f "$f"; done
kubectl -n oficina rollout restart deploy/oficina-api

echo ">> [7/7] Aguardando pods"
kubectl -n oficina rollout status statefulset/postgres --timeout=180s
kubectl -n oficina rollout status deploy/oficina-api --timeout=300s
kubectl get pods,svc,hpa -n oficina
echo ">> OK. Teste: kubectl port-forward svc/oficina-api 8080:80 -n oficina"