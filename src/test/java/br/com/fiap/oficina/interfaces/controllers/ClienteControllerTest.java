package br.com.fiap.oficina.interfaces.controllers;

import br.com.fiap.oficina.application.services.ClienteService;
import br.com.fiap.oficina.interfaces.dtos.ClienteRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.ClienteResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private ClienteRequestDTO requestDTO;
    private ClienteResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new ClienteRequestDTO("Maria Silva", "52998224725", "maria@email.com", "11999999999");
        responseDTO = new ClienteResponseDTO(1L, "Maria Silva", "52998224725", "maria@email.com", "11999999999");
    }

    @Test
    @DisplayName("Deve retornar 201 Created ao cadastrar cliente")
    void deveRetornar201AoCriarCliente() {
        when(clienteService.criar(any())).thenReturn(responseDTO);

        ResponseEntity<ClienteResponseDTO> response = clienteController.criar(requestDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Maria Silva", response.getBody().nome());
        assertEquals("52998224725", response.getBody().cpfCnpj());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao listar clientes")
    void deveRetornar200AoListarClientes() {
        when(clienteService.listarTodos()).thenReturn(List.of(responseDTO));

        ResponseEntity<List<ClienteResponseDTO>> response = clienteController.listarTodos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao buscar cliente por ID")
    void deveRetornar200AoBuscarPorId() {
        when(clienteService.buscarPorId(1L)).thenReturn(responseDTO);

        ResponseEntity<ClienteResponseDTO> response = clienteController.buscarPorId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().id());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao atualizar cliente")
    void deveRetornar200AoAtualizar() {
        when(clienteService.atualizar(eq(1L), any())).thenReturn(responseDTO);

        ResponseEntity<ClienteResponseDTO> response = clienteController.atualizar(1L, requestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().id());
        verify(clienteService).atualizar(1L, requestDTO);
    }

    @Test
    @DisplayName("Deve retornar 204 No Content ao excluir cliente")
    void deveRetornar204AoExcluir() {
        ResponseEntity<Void> response = clienteController.excluir(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(clienteService).excluir(1L);
    }
}
