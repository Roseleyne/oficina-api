# ---------------------------------------------------------------------------
# Cluster Kubernetes local (kind = Kubernetes IN Docker)
# ---------------------------------------------------------------------------
resource "kind_cluster" "oficina" {
  name           = var.cluster_name
  wait_for_ready = true

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"
    }
  }
}

# metrics-server: necessario para o HPA ler CPU/memoria dos pods
resource "helm_release" "metrics_server" {
  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  namespace  = "kube-system"
  wait       = true

  # kind usa certificados autoassinados no kubelet
  set {
    name  = "args[0]"
    value = "--kubelet-insecure-tls"
  }

  depends_on = [kind_cluster.oficina]
}

resource "kubernetes_namespace_v1" "oficina" {
  metadata {
    name = var.namespace
  }

  depends_on = [kind_cluster.oficina]
}
