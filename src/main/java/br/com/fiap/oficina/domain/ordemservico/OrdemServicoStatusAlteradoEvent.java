package br.com.fiap.oficina.domain.ordemservico;

/**
 * Evento de dominio: a Ordem de Servico mudou de status (ou foi aberta).
 * Publicado pelo caso de uso e consumido por adaptadores de notificacao.
 */
public record OrdemServicoStatusAlteradoEvent(
        Long ordemServicoId,
        StatusOrdemServico status,
        String nomeCliente,
        String emailCliente,
        String placaVeiculo
) {

    public static OrdemServicoStatusAlteradoEvent de(OrdemServico ordemServico) {

        String nome = null;
        String email = null;
        String placa = null;

        if (ordemServico.getVeiculo() != null) {
            placa = ordemServico.getVeiculo().getPlaca();
            if (ordemServico.getVeiculo().getCliente() != null) {
                nome = ordemServico.getVeiculo().getCliente().getNome();
                email = ordemServico.getVeiculo().getCliente().getEmail();
            }
        }

        return new OrdemServicoStatusAlteradoEvent(
                ordemServico.getId(),
                ordemServico.getStatus(),
                nome,
                email,
                placa
        );
    }
}
