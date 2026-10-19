package br.com.fiap.oficina.interfaces.controllers;

import br.com.fiap.oficina.application.services.OrdemServicoService;
import br.com.fiap.oficina.interfaces.dtos.AprovacaoOrcamentoWebhookDTO;
import br.com.fiap.oficina.interfaces.dtos.StatusOrdemServicoResponseDTO;
import br.com.fiap.oficina.interfaces.exceptions.WebhookNaoAutorizadoException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Recebe notificacoes externas (ex.: link de aprovacao enviado ao cliente,
 * sistema de pagamentos) com a decisao sobre o orcamento da OS.
 *
 * Autenticacao: header X-Webhook-Token, cujo valor vem da variavel de
 * ambiente WEBHOOK_TOKEN (Secret no Kubernetes). Nao usa JWT.
 */
@RestController
@RequestMapping("/webhooks/orcamento")
public class WebhookOrcamentoController {

    public static final String HEADER_TOKEN = "X-Webhook-Token";

    private final OrdemServicoService ordemServicoService;
    private final String tokenEsperado;

    public WebhookOrcamentoController(
            OrdemServicoService ordemServicoService,
            @Value("${webhook.token}") String tokenEsperado) {

        this.ordemServicoService = ordemServicoService;
        this.tokenEsperado = tokenEsperado;
    }

    @PostMapping
    public ResponseEntity<StatusOrdemServicoResponseDTO> receberDecisao(
            @RequestHeader(value = HEADER_TOKEN, required = false) String token,
            @Valid @RequestBody AprovacaoOrcamentoWebhookDTO request) {

        validarToken(token);

        return ResponseEntity.ok(
                ordemServicoService.processarDecisaoOrcamento(
                        request.ordemServicoId(),
                        request.aprovado()
                )
        );
    }

    private void validarToken(String token) {

        boolean valido = token != null
                && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                tokenEsperado.getBytes(StandardCharsets.UTF_8));

        if (!valido) {
            throw new WebhookNaoAutorizadoException("Token do webhook inválido ou ausente");
        }
    }
}
