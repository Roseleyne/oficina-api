# ---------------------------------------------------------------------------
# Segredos gerados pelo Terraform (nunca ficam versionados no Git)
# ---------------------------------------------------------------------------
resource "random_password" "db" {
  length  = 24
  special = false
}

resource "random_id" "jwt" {
  byte_length = 32
}

resource "random_password" "webhook" {
  length  = 32
  special = false
}

resource "kubernetes_secret_v1" "app" {
  metadata {
    name      = "oficina-secret"
    namespace = kubernetes_namespace_v1.oficina.metadata[0].name
  }

  type = "Opaque"

  data = {
    SPRING_DATASOURCE_USERNAME = var.db_user
    SPRING_DATASOURCE_PASSWORD = random_password.db.result
    JWT_SECRET                 = random_id.jwt.b64_std
    WEBHOOK_TOKEN              = random_password.webhook.result
  }
}
