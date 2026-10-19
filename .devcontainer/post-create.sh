#!/usr/bin/env bash
set -u
echo ">> Instalando kind..."
for i in 1 2 3; do
  curl -fsSLo /tmp/kind https://kind.sigs.k8s.io/dl/v0.30.0/kind-linux-amd64 && break
  echo "tentativa $i falhou, repetindo..."; sleep 5
done
sudo install -m 0755 /tmp/kind /usr/local/bin/kind || echo "!! kind não instalado"
chmod +x mvnw || true
echo ">> Ferramentas:"
java -version 2>&1 | head -1
docker --version || true
kind version || true
kubectl version --client 2>/dev/null | head -1 || true
helm version --short || true
terraform -version | head -1 || true
