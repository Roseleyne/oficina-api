package br.com.fiap.oficina.infrastructure.notification;

import br.com.fiap.oficina.application.ports.NotificacaoClientePort;
import br.com.fiap.oficina.domain.ordemservico.OrdemServicoStatusAlteradoEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escuta o evento de mudanca de status e dispara a notificacao somente
 * APOS o commit da transacao: se a operacao falhar (rollback), o cliente
 * nao recebe e-mail de algo que nao aconteceu.
 */
@Component
public class OrdemServicoNotificacaoListener {

    private final NotificacaoClientePort notificacaoClientePort;

    public OrdemServicoNotificacaoListener(NotificacaoClientePort notificacaoClientePort) {
        this.notificacaoClientePort = notificacaoClientePort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoAlterarStatus(OrdemServicoStatusAlteradoEvent evento) {
        notificacaoClientePort.notificarMudancaDeStatus(evento);
    }
}
