package br.com.fiap.oficina.interfaces.dtos;

import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;

import java.time.LocalDateTime;

/**
 * Resposta da consulta de status de uma Ordem de Servico.
 */
public record StatusOrdemServicoResponseDTO(
        Long id,
        StatusOrdemServico status,
        String descricaoStatus,
        LocalDateTime dataAbertura,
        LocalDateTime dataInicioExecucao,
        LocalDateTime dataFinalizacao
) {
}
