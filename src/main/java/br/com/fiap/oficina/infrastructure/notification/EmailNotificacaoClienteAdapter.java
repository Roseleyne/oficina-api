package br.com.fiap.oficina.infrastructure.notification;

import br.com.fiap.oficina.application.ports.NotificacaoClientePort;
import br.com.fiap.oficina.domain.ordemservico.OrdemServicoStatusAlteradoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adaptador de saida: envia e-mail ao cliente via SMTP (Mailpit em dev/K8s).
 * Falha no envio NAO interrompe o fluxo da OS: apenas registra no log.
 */
@Component
public class EmailNotificacaoClienteAdapter implements NotificacaoClientePort {

    private static final Logger LOG = LoggerFactory.getLogger(EmailNotificacaoClienteAdapter.class);

    private final JavaMailSender mailSender;
    private final boolean habilitado;
    private final String remetente;

    public EmailNotificacaoClienteAdapter(
            JavaMailSender mailSender,
            @Value("${notificacao.email.habilitado:true}") boolean habilitado,
            @Value("${notificacao.email.remetente:oficina@oficina.local}") String remetente) {

        this.mailSender = mailSender;
        this.habilitado = habilitado;
        this.remetente = remetente;
    }

    @Override
    public void notificarMudancaDeStatus(OrdemServicoStatusAlteradoEvent evento) {

        if (!habilitado || evento.emailCliente() == null || evento.emailCliente().isBlank()) {
            return;
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(evento.emailCliente());
        mensagem.setSubject("Ordem de Serviço #" + evento.ordemServicoId()
                + " - " + evento.status().getDescricao());
        mensagem.setText(montarTexto(evento));

        try {
            mailSender.send(mensagem);
            LOG.info("Notificação enviada: OS {} -> {}", evento.ordemServicoId(), evento.status());
        } catch (MailException e) {
            LOG.warn("Falha ao enviar e-mail da OS {}: {}", evento.ordemServicoId(), e.getMessage());
        }
    }

    private String montarTexto(OrdemServicoStatusAlteradoEvent evento) {

        String nome = evento.nomeCliente() != null ? evento.nomeCliente() : "cliente";
        String placa = evento.placaVeiculo() != null ? evento.placaVeiculo() : "-";

        return "Olá, " + nome + "!\n\n"
                + "A ordem de serviço #" + evento.ordemServicoId()
                + " do veículo " + placa
                + " está agora com o status: " + evento.status().getDescricao() + ".\n\n"
                + "Acompanhe pelo endpoint GET /ordens-servico/" + evento.ordemServicoId() + "/status.\n\n"
                + "Oficina Mecânica";
    }
}
