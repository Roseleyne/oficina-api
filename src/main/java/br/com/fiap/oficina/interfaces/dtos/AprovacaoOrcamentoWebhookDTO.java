package br.com.fiap.oficina.interfaces.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Notificacao externa de aprovacao ou recusa do orcamento pelo cliente.
 */
public record AprovacaoOrcamentoWebhookDTO(

        @NotNull(message = "Ordem de serviço é obrigatória")
        Long ordemServicoId,

        @NotNull(message = "Decisão de aprovação é obrigatória")
        Boolean aprovado,

        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
        String observacao

) {
}
