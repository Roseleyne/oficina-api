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

    RECEBIDA(5, "Recebida"),
    EM_DIAGNOSTICO(4, "Em diagnóstico"),
    AGUARDANDO_APROVACAO(3, "Aguardando aprovação"),
    APROVADA(2, "Aprovada"),
    EM_EXECUCAO(1, "Em execução"),
    FINALIZADA(null, "Finalizada"),
    ENTREGUE(null, "Entregue"),
    CANCELADA(null, "Cancelada");

    private final Integer prioridadeListagem;
    private final String descricao;

    StatusOrdemServico(Integer prioridadeListagem, String descricao) {
        this.prioridadeListagem = prioridadeListagem;
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isExibidoNaListagem() {
        return prioridadeListagem != null;
    }

    public int getPrioridadeListagem() {
        return prioridadeListagem == null ? Integer.MAX_VALUE : prioridadeListagem;
    }
}
