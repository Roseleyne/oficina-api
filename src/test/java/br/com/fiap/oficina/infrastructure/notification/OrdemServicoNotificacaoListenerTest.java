package br.com.fiap.oficina.infrastructure.notification;

import br.com.fiap.oficina.application.ports.NotificacaoClientePort;
import br.com.fiap.oficina.domain.ordemservico.OrdemServico;
import br.com.fiap.oficina.domain.ordemservico.OrdemServicoStatusAlteradoEvent;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrdemServicoNotificacaoListenerTest {

    @Test
    @DisplayName("Listener repassa o evento para a porta de notificação")
    void deveRepassarEventoParaPorta() {
        NotificacaoClientePort porta = mock(NotificacaoClientePort.class);
        OrdemServicoNotificacaoListener listener = new OrdemServicoNotificacaoListener(porta);
        OrdemServicoStatusAlteradoEvent evento = new OrdemServicoStatusAlteradoEvent(
                1L, StatusOrdemServico.APROVADA, "Maria", "maria@email.com", "ABC1D23");

        listener.aoAlterarStatus(evento);

        verify(porta).notificarMudancaDeStatus(evento);
    }

    @Test
    @DisplayName("Evento é criado mesmo sem veículo/cliente carregado")
    void deveCriarEventoSemVeiculo() {
        OrdemServico os = new OrdemServico();
        os.setId(3L);
        os.setStatus(StatusOrdemServico.RECEBIDA);

        OrdemServicoStatusAlteradoEvent evento = OrdemServicoStatusAlteradoEvent.de(os);

        assertEquals(3L, evento.ordemServicoId());
        assertNull(evento.emailCliente());
        assertNull(evento.placaVeiculo());
    }
}
