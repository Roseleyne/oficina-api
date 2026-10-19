variable "cluster_name" {
  description = "Nome do cluster kind (Kubernetes local em Docker)"
  type        = string
  default     = "oficina"
}

variable "namespace" {
  description = "Namespace da aplicacao"
  type        = string
  default     = "oficina"
}

variable "postgres_image" {
  description = "Imagem do PostgreSQL"
  type        = string
  default     = "postgres:16-alpine"
}

variable "db_name" {
  description = "Nome do banco de dados"
  type        = string
  default     = "oficina"
}

variable "db_user" {
  description = "Usuario do banco de dados"
  type        = string
  default     = "postgres"
}

variable "db_storage" {
  description = "Tamanho do volume persistente do banco"
  type        = string
  default     = "1Gi"
}
