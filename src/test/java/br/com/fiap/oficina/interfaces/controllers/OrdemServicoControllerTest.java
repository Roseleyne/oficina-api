package br.com.fiap.oficina.interfaces.controllers;

import br.com.fiap.oficina.application.services.OrdemServicoService;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.fiap.oficina.interfaces.dtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemServicoControllerTest {

    @Mock
    private OrdemServicoService ordemServicoService;

    @InjectMocks
    private OrdemServicoController ordemServicoController;

    private OrdemServicoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new OrdemServicoResponseDTO(
                1L,
                10L,
                "BRA2E19",
                1L,
                "Maria Silva",
                "Problema na suspensão",
                StatusOrdemServico.RECEBIDA,
                List.of(),
                List.of(),
                BigDecimal.ZERO,
                LocalDateTime.now(),
                null,
                null
        );
    }

    @Test
    @DisplayName("Deve retornar 201 Created ao criar ordem de serviço")
    void deveRetornar201AoCriarOS() {
        OrdemServicoRequestDTO request = new OrdemServicoRequestDTO(10L, "Problema na suspensão");
        when(ordemServicoService.criar(any())).thenReturn(responseDTO);

        ResponseEntity<OrdemServicoResponseDTO> response = ordemServicoController.criar(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao listar todas as ordens de serviço")
    void deveRetornar200AoListarTodas() {
        when(ordemServicoService.listarTodas()).thenReturn(List.of(responseDTO));

        ResponseEntity<List<OrdemServicoResponseDTO>> response = ordemServicoController.listarTodas();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao buscar ordem de serviço por ID")
    void deveRetornar200AoBuscarPorId() {
        when(ordemServicoService.buscarPorId(1L)).thenReturn(responseDTO);

        ResponseEntity<OrdemServicoResponseDTO> response = ordemServicoController.buscarPorId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().id());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao buscar ordens por veículo")
    void deveRetornar200AoBuscarPorVeiculo() {
        when(ordemServicoService.buscarPorVeiculo(10L)).thenReturn(List.of(responseDTO));

        ResponseEntity<List<OrdemServicoResponseDTO>> response = ordemServicoController.buscarPorVeiculo(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao adicionar serviço à OS")
    void deveRetornar200AoAdicionarServico() {
        AdicionarServicoOSDTO request = new AdicionarServicoOSDTO(5L);
        when(ordemServicoService.adicionarServico(eq(1L), any())).thenReturn(responseDTO);

        ResponseEntity<OrdemServicoResponseDTO> response = ordemServicoController.adicionarServico(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ordemServicoService).adicionarServico(1L, request);
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao adicionar peça à OS")
    void deveRetornar200AoAdicionarPeca() {
        AdicionarPecaOSDTO request = new AdicionarPecaOSDTO(3L, 2);
        when(ordemServicoService.adicionarPeca(eq(1L), any())).thenReturn(responseDTO);

        ResponseEntity<OrdemServicoResponseDTO> response = ordemServicoController.adicionarPeca(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ordemServicoService).adicionarPeca(1L, request);
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao atualizar status da OS")
    void deveRetornar200AoAtualizarStatus() {
        AtualizarStatusOSDTO request = new AtualizarStatusOSDTO(StatusOrdemServico.EM_DIAGNOSTICO);
        when(ordemServicoService.atualizarStatus(eq(1L), any())).thenReturn(responseDTO);

        ResponseEntity<OrdemServicoResponseDTO> response = ordemServicoController.atualizarStatus(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ordemServicoService).atualizarStatus(1L, request);
    }

    @Test
    @DisplayName("Deve retornar 204 No Content ao excluir ordem de serviço")
    void deveRetornar204AoExcluir() {
        ResponseEntity<Void> response = ordemServicoController.excluir(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(ordemServicoService).excluir(1L);
    }

    @Test
    @DisplayName("Deve retornar 200 OK com tempo médio de execução")
    void deveRetornar200ComTempoMedioExecucao() {
        TempoMedioExecucaoDTO tempoDTO = new TempoMedioExecucaoDTO(5, 120.0, "2 hora(s) e 0 minuto(s)");
        when(ordemServicoService.obterTempoMedioExecucao()).thenReturn(tempoDTO);

        ResponseEntity<TempoMedioExecucaoDTO> response = ordemServicoController.obterTempoMedioExecucao();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody().totalOrdensFinalizadas());
        assertEquals("2 hora(s) e 0 minuto(s)", response.getBody().tempoMedioFormatado());
    }
}
