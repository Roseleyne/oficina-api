# ---------------------------------------------------------------------------
# Banco de dados PostgreSQL (StatefulSet + volume persistente + Service)
# ---------------------------------------------------------------------------
resource "kubernetes_config_map_v1" "postgres_init" {
  metadata {
    name      = "postgres-init"
    namespace = kubernetes_namespace_v1.oficina.metadata[0].name
  }

  # Scripts de criacao/seed do banco (mesmos usados no docker-compose)
  data = {
    for f in fileset("${path.module}/../init-scripts", "*.sql") :
    f => file("${path.module}/../init-scripts/${f}")
  }
}

resource "kubernetes_service_v1" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace_v1.oficina.metadata[0].name
  }

  spec {
    selector = {
      app = "postgres"
    }

    port {
      port        = 5432
      target_port = 5432
    }
  }
}

resource "kubernetes_stateful_set_v1" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace_v1.oficina.metadata[0].name
    labels = {
      app = "postgres"
    }
  }

  spec {
    service_name = kubernetes_service_v1.postgres.metadata[0].name
    replicas     = 1

    selector {
      match_labels = {
        app = "postgres"
      }
    }

    template {
      metadata {
        labels = {
          app = "postgres"
        }
      }

      spec {
        container {
          name  = "postgres"
          image = var.postgres_image

          port {
            container_port = 5432
          }

          env {
            name  = "POSTGRES_DB"
            value = var.db_name
          }

          env {
            name = "POSTGRES_USER"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.app.metadata[0].name
                key  = "SPRING_DATASOURCE_USERNAME"
              }
            }
          }

          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.app.metadata[0].name
                key  = "SPRING_DATASOURCE_PASSWORD"
              }
            }
          }

          env {
            name  = "PGDATA"
            value = "/var/lib/postgresql/data/pgdata"
          }

          readiness_probe {
            exec {
              command = ["sh", "-c", "pg_isready -U $POSTGRES_USER -d ${var.db_name}"]
            }
            initial_delay_seconds = 5
            period_seconds        = 5
          }

          resources {
            requests = {
              cpu    = "100m"
              memory = "128Mi"
            }
            limits = {
              memory = "256Mi"
            }
          }

          volume_mount {
            name       = "data"
            mount_path = "/var/lib/postgresql/data"
          }

          volume_mount {
            name       = "init"
            mount_path = "/docker-entrypoint-initdb.d"
          }
        }

        volume {
          name = "init"
          config_map {
            name = kubernetes_config_map_v1.postgres_init.metadata[0].name
          }
        }
      }
    }

    volume_claim_template {
      metadata {
        name = "data"
      }

      spec {
        access_modes = ["ReadWriteOnce"]

        resources {
          requests = {
            storage = var.db_storage
          }
        }
      }
    }
  }

  wait_for_rollout = true
}
