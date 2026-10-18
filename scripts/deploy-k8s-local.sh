#!/usr/bin/env bash
# Deploy local da oficina-api em cluster kind
# Uso: bash scripts/deploy-k8s-local.sh
set -euo pipefail
cd "$(dirname "$0")/.."

echo ">> [1/6] Gerando manifestos em k8s/"
mkdir -p k8s

cat > k8s/00-namespace.yaml <<'EOF'
apiVersion: v1
kind: Namespace
metadata:
  name: oficina
EOF

cat > k8s/01-configmap.yaml <<'EOF'
apiVersion: v1
kind: ConfigMap
metadata:
  name: oficina-config
  namespace: oficina
data:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres:5432/oficina"
  SPRING_JPA_SHOW_SQL: "false"
  JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=75"
EOF

cat > k8s/secret.example.yaml <<'EOF'
# NAO aplicar este arquivo. O Secret real e criado via kubectl ou pela pipeline CI/CD.
apiVersion: v1
kind: Secret
metadata:
  name: oficina-secret
  namespace: oficina
type: Opaque
stringData:
  SPRING_DATASOURCE_USERNAME: "postgres"
  SPRING_DATASOURCE_PASSWORD: "<senha>"
  JWT_SECRET: "<base64-32-bytes>"
EOF

kubectl create configmap postgres-init --from-file=init-scripts/ \
  -n oficina --dry-run=client -o yaml > k8s/02-postgres-init-configmap.yaml

cat > k8s/03-postgres.yaml <<'EOF'
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: oficina
spec:
  selector:
    app: postgres
  ports:
    - port: 5432
      targetPort: 5432
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: oficina
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:16-alpine
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              value: oficina
            - name: POSTGRES_USER
              valueFrom:
                secretKeyRef:
                  name: oficina-secret
                  key: SPRING_DATASOURCE_USERNAME
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: oficina-secret
                  key: SPRING_DATASOURCE_PASSWORD
            - name: PGDATA
              value: /var/lib/postgresql/data/pgdata
          readinessProbe:
            exec:
              command: ["sh", "-c", "pg_isready -U $POSTGRES_USER -d oficina"]
            initialDelaySeconds: 5
            periodSeconds: 5
          resources:
            requests:
              cpu: "100m"
              memory: "128Mi"
            limits:
              memory: "256Mi"
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
            - name: init
              mountPath: /docker-entrypoint-initdb.d
      volumes:
        - name: init
          configMap:
            name: postgres-init
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 1Gi
EOF

cat > k8s/04-app.yaml <<'EOF'
apiVersion: v1
kind: Service
metadata:
  name: oficina-api
  namespace: oficina
spec:
  selector:
    app: oficina-api
  ports:
    - port: 80
      targetPort: 8080
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: oficina-api
  namespace: oficina
spec:
  replicas: 1
  selector:
    matchLabels:
      app: oficina-api
  template:
    metadata:
      labels:
        app: oficina-api
    spec:
      containers:
        - name: oficina-api
          image: oficina-api:local
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: oficina-config
            - secretRef:
                name: oficina-secret
          resources:
            requests:
              cpu: "100m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "768Mi"
          startupProbe:
            tcpSocket:
              port: 8080
            periodSeconds: 5
            failureThreshold: 36
          readinessProbe:
            tcpSocket:
              port: 8080
            periodSeconds: 10
          livenessProbe:
            tcpSocket:
              port: 8080
            periodSeconds: 20
EOF

cat > k8s/05-hpa.yaml <<'EOF'
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: oficina-api
  namespace: oficina
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: oficina-api
  minReplicas: 1
  maxReplicas: 3
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
EOF

echo ">> [2/6] Build da imagem Docker"
docker build -t oficina-api:local .

echo ">> [3/6] Carregando imagem no cluster kind"
kind load docker-image oficina-api:local --name oficina

echo ">> [4/6] Namespace e Secret"
kubectl apply -f k8s/00-namespace.yaml
kubectl create secret generic oficina-secret -n oficina \
  --from-literal=SPRING_DATASOURCE_USERNAME=postgres \
  --from-literal=SPRING_DATASOURCE_PASSWORD=postgres \
  --from-literal=JWT_SECRET="$(openssl rand -base64 32)" \
  --dry-run=client -o yaml | kubectl apply -f -

echo ">> [5/6] Aplicando manifestos"
for f in k8s/0*.yaml; do kubectl apply -f "$f"; done

echo ">> [6/6] Aguardando pods ficarem prontos"
kubectl -n oficina rollout status statefulset/postgres --timeout=180s
kubectl -n oficina rollout status deploy/oficina-api --timeout=300s

kubectl get pods,svc,hpa -n oficina
echo ">> Deploy concluido. Teste com:"
echo "   kubectl port-forward svc/oficina-api 8080:80 -n oficina"