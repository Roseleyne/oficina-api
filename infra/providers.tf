# Os providers kubernetes/helm usam as credenciais do cluster criado pelo kind_cluster.
provider "kind" {}

provider "kubernetes" {
  host                   = kind_cluster.oficina.endpoint
  client_certificate     = kind_cluster.oficina.client_certificate
  client_key             = kind_cluster.oficina.client_key
  cluster_ca_certificate = kind_cluster.oficina.cluster_ca_certificate
}

provider "helm" {
  kubernetes {
    host                   = kind_cluster.oficina.endpoint
    client_certificate     = kind_cluster.oficina.client_certificate
    client_key             = kind_cluster.oficina.client_key
    cluster_ca_certificate = kind_cluster.oficina.cluster_ca_certificate
  }
}
