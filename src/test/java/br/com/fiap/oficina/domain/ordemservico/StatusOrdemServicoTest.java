package br.com.fiap.oficina.domain.ordemservico;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class StatusOrdemServicoTest {

    @Test
    @DisplayName("Prioridade da listagem segue: Em Execucao > Aprovada > Aguardando Aprovacao > Diagnostico > Recebida")
    void devePossuirPrioridadeDeListagemConformeRegraDeNegocio() {
        assertTrue(StatusOrdemServico.EM_EXECUCAO.getPrioridadeListagem()
                < StatusOrdemServico.APROVADA.getPrioridadeListagem());
        assertTrue(StatusOrdemServico.APROVADA.getPrioridadeListagem()
                < StatusOrdemServico.AGUARDANDO_APROVACAO.getPrioridadeListagem());
        assertTrue(StatusOrdemServico.AGUARDANDO_APROVACAO.getPrioridadeListagem()
                < StatusOrdemServico.EM_DIAGNOSTICO.getPrioridadeListagem());
        assertTrue(StatusOrdemServico.EM_DIAGNOSTICO.getPrioridadeListagem()
                < StatusOrdemServico.RECEBIDA.getPrioridadeListagem());
    }

    @ParameterizedTest
    @EnumSource(value = StatusOrdemServico.class, names = {"FINALIZADA", "ENTREGUE", "CANCELADA"})
    @DisplayName("OS encerradas nao sao exibidas na listagem")
    void naoDeveExibirStatusEncerradosNaListagem(StatusOrdemServico status) {
        assertFalse(status.isExibidoNaListagem());
        assertEquals(Integer.MAX_VALUE, status.getPrioridadeListagem());
    }

    @ParameterizedTest
    @EnumSource(value = StatusOrdemServico.class,
            names = {"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "APROVADA", "EM_EXECUCAO"})
    @DisplayName("OS ativas sao exibidas na listagem")
    void deveExibirStatusAtivosNaListagem(StatusOrdemServico status) {
        assertTrue(status.isExibidoNaListagem());
    }
}
