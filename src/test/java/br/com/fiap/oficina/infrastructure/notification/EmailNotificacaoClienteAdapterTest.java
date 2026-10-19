package br.com.fiap.oficina.infrastructure.notification;

import br.com.fiap.oficina.domain.ordemservico.OrdemServicoStatusAlteradoEvent;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificacaoClienteAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    private final OrdemServicoStatusAlteradoEvent evento = new OrdemServicoStatusAlteradoEvent(
            7L, StatusOrdemServico.AGUARDANDO_APROVACAO, "Maria Silva", "maria@email.com", "ABC1D23");

    @Test
    @DisplayName("Deve enviar e-mail ao cliente com o novo status")
    void deveEnviarEmailComNovoStatus() {
        EmailNotificacaoClienteAdapter adapter =
                new EmailNotificacaoClienteAdapter(mailSender, true, "oficina@oficina.local");

        adapter.notificarMudancaDeStatus(evento);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensagem = captor.getValue();

        assertArrayEquals(new String[]{"maria@email.com"}, mensagem.getTo());
        assertEquals("oficina@oficina.local", mensagem.getFrom());
        assertEquals("Ordem de Serviço #7 - Aguardando aprovação", mensagem.getSubject());
        assertTrue(mensagem.getText().contains("Maria Silva"));
        assertTrue(mensagem.getText().contains("ABC1D23"));
    }

    @Test
    @DisplayName("Não envia quando a notificação está desabilitada")
    void naoDeveEnviarQuandoDesabilitado() {
        EmailNotificacaoClienteAdapter adapter =
                new EmailNotificacaoClienteAdapter(mailSender, false, "oficina@oficina.local");

        adapter.notificarMudancaDeStatus(evento);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Não envia quando o cliente não tem e-mail")
    void naoDeveEnviarSemEmailDoCliente() {
        EmailNotificacaoClienteAdapter adapter =
                new EmailNotificacaoClienteAdapter(mailSender, true, "oficina@oficina.local");

        adapter.notificarMudancaDeStatus(new OrdemServicoStatusAlteradoEvent(
                7L, StatusOrdemServico.RECEBIDA, "Maria", null, "ABC1D23"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Falha no servidor de e-mail não interrompe o fluxo")
    void naoDevePropagarFalhaDeEnvio() {
        EmailNotificacaoClienteAdapter adapter =
                new EmailNotificacaoClienteAdapter(mailSender, true, "oficina@oficina.local");
        doThrow(new MailSendException("SMTP indisponível"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> adapter.notificarMudancaDeStatus(evento));
    }
}
