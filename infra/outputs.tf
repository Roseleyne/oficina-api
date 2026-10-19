output "cluster_name" {
  description = "Nome do cluster kind"
  value       = kind_cluster.oficina.name
}

output "kube_context" {
  description = "Contexto do kubectl para o cluster"
  value       = "kind-${kind_cluster.oficina.name}"
}

output "namespace" {
  description = "Namespace da aplicacao"
  value       = kubernetes_namespace_v1.oficina.metadata[0].name
}

output "database_service" {
  description = "Endereco interno do banco (usado pela aplicacao)"
  value       = "${kubernetes_service_v1.postgres.metadata[0].name}.${var.namespace}.svc.cluster.local:5432"
}

output "proximo_passo" {
  description = "Comando para publicar a aplicacao no cluster"
  value       = "bash scripts/deploy-app.sh"
}
