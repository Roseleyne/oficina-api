package br.com.fiap.oficina.domain.ordemservico;

/**
 * Status do ciclo de vida da Ordem de Servico.
 *
 * prioridadeListagem: ordem de exibicao na listagem de OS ativas
 * (menor valor aparece primeiro). Status com prioridade nula nao
 * aparecem na listagem (OS encerradas: FINALIZADA, ENTREGUE, CANCELADA).
 *
 * Regra do enunciado (Fase 2):
 * Em Execucao > Aguardando Aprovacao > Diagnostico > Recebida.
 * APROVADA (status intermediario do nosso fluxo) fica logo apos EM_EXECUCAO.
 */
public enum StatusOrdemServico {

    RECEBIDA(5),
    EM_DIAGNOSTICO(4),
    AGUARDANDO_APROVACAO(3),
    APROVADA(2),
    EM_EXECUCAO(1),
    FINALIZADA(null),
    ENTREGUE(null),
    CANCELADA(null);

    private final Integer prioridadeListagem;

    StatusOrdemServico(Integer prioridadeListagem) {
        this.prioridadeListagem = prioridadeListagem;
    }

    public boolean isExibidoNaListagem() {
        return prioridadeListagem != null;
    }

    public int getPrioridadeListagem() {
        return prioridadeListagem == null ? Integer.MAX_VALUE : prioridadeListagem;
    }
}
