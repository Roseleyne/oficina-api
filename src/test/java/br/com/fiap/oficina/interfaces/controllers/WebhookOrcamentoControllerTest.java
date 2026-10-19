package br.com.fiap.oficina.interfaces.controllers;

import br.com.fiap.oficina.application.services.OrdemServicoService;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.fiap.oficina.interfaces.dtos.AprovacaoOrcamentoWebhookDTO;
import br.com.fiap.oficina.interfaces.dtos.StatusOrdemServicoResponseDTO;
import br.com.fiap.oficina.interfaces.exceptions.WebhookNaoAutorizadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookOrcamentoControllerTest {

    private static final String TOKEN = "token-de-teste";

    @Mock
    private OrdemServicoService ordemServicoService;

    private WebhookOrcamentoController controller;

    @BeforeEach
    void setUp() {
        controller = new WebhookOrcamentoController(ordemServicoService, TOKEN);
    }

    @Test
    @DisplayName("Deve processar a decisão quando o token é válido")
    void deveProcessarDecisaoComTokenValido() {
        StatusOrdemServicoResponseDTO statusDTO = new StatusOrdemServicoResponseDTO(
                1L, StatusOrdemServico.APROVADA, "Aprovada", LocalDateTime.now(), null, null);
        when(ordemServicoService.processarDecisaoOrcamento(1L, true)).thenReturn(statusDTO);

        ResponseEntity<StatusOrdemServicoResponseDTO> response = controller.receberDecisao(
                TOKEN, new AprovacaoOrcamentoWebhookDTO(1L, true, "Cliente aprovou por e-mail"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(StatusOrdemServico.APROVADA, response.getBody().status());
    }

    @Test
    @DisplayName("Deve rejeitar quando o token é inválido")
    void deveRejeitarTokenInvalido() {
        AprovacaoOrcamentoWebhookDTO request = new AprovacaoOrcamentoWebhookDTO(1L, true, null);

        assertThrows(WebhookNaoAutorizadoException.class,
                () -> controller.receberDecisao("token-errado", request));
        verify(ordemServicoService, never()).processarDecisaoOrcamento(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Deve rejeitar quando o token está ausente")
    void deveRejeitarTokenAusente() {
        AprovacaoOrcamentoWebhookDTO request = new AprovacaoOrcamentoWebhookDTO(1L, false, null);

        assertThrows(WebhookNaoAutorizadoException.class,
                () -> controller.receberDecisao(null, request));
        verify(ordemServicoService, never()).processarDecisaoOrcamento(anyLong(), anyBoolean());
    }
}
