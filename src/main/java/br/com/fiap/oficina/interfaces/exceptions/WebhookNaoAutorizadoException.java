package br.com.fiap.oficina.interfaces.exceptions;

public class WebhookNaoAutorizadoException extends RuntimeException {

    public WebhookNaoAutorizadoException(String mensagem) {
        super(mensagem);
    }
}
