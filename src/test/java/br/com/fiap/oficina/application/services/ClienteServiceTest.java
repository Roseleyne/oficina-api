package br.com.fiap.oficina.application.services;

import br.com.fiap.oficina.domain.cliente.Cliente;
import br.com.fiap.oficina.infrastructure.repositories.ClienteRepository;
import br.com.fiap.oficina.interfaces.dtos.ClienteRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.ClienteResponseDTO;
import br.com.fiap.oficina.interfaces.exceptions.ClienteDuplicadoException;
import br.com.fiap.oficina.interfaces.exceptions.ClienteNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;
    private ClienteRequestDTO request;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Maria Silva");
        cliente.setCpfCnpj("52998224725");
        cliente.setEmail("maria@email.com");
        cliente.setTelefone("11999999999");

        request = new ClienteRequestDTO(
                "Maria Silva",
                "52998224725",
                "maria@email.com",
                "11999999999"
        );
    }

    @Test
    @DisplayName("Deve criar cliente com sucesso")
    void deveCriarClienteComSucesso() {
        when(clienteRepository.existsByCpfCnpj(request.cpfCnpj())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        ClienteResponseDTO response = clienteService.criar(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Maria Silva", response.nome());
        assertEquals("52998224725", response.cpfCnpj());

        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve impedir criação de cliente duplicado por CPF/CNPJ")
    void deveImpedirClienteDuplicado() {
        when(clienteRepository.existsByCpfCnpj(request.cpfCnpj())).thenReturn(true);

        ClienteDuplicadoException exception = assertThrows(
                ClienteDuplicadoException.class,
                () -> clienteService.criar(request)
        );

        assertEquals("Cliente já cadastrado com este CPF/CNPJ", exception.getMessage());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar todos os clientes")
    void deveListarTodosOsClientes() {
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));

        List<ClienteResponseDTO> lista = clienteService.listarTodos();

        assertEquals(1, lista.size());
        assertEquals("Maria Silva", lista.get(0).nome());
    }

    @Test
    @DisplayName("Deve buscar cliente por ID existente")
    void deveBuscarClientePorId() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ClienteResponseDTO response = clienteService.buscarPorId(1L);

        assertEquals(1L, response.id());
        assertEquals("Maria Silva", response.nome());
    }

    @Test
    @DisplayName("Deve retornar erro quando cliente não existe ao buscar por ID")
    void deveRetornarErroQuandoClienteNaoExiste() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class, () -> clienteService.buscarPorId(999L));
    }

    @Test
    @DisplayName("Deve atualizar cliente mantendo o mesmo CPF/CNPJ")
    void deveAtualizarClienteMantendoMesmoCpf() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        ClienteResponseDTO response = clienteService.atualizar(1L, request);

        assertNotNull(response);
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("Deve atualizar cliente alterando para novo CPF/CNPJ disponível")
    void deveAtualizarClienteComNovoCpfDisponivel() {
        ClienteRequestDTO updateRequest = new ClienteRequestDTO("Maria Nova", "12345678909", "maria2@email.com", "11988888888");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByCpfCnpj("12345678909")).thenReturn(false);
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        ClienteResponseDTO response = clienteService.atualizar(1L, updateRequest);

        assertNotNull(response);
        assertEquals("12345678909", cliente.getCpfCnpj());
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar cliente para CPF/CNPJ já existente")
    void deveLancarExcecaoAoAtualizarParaCpfDuplicado() {
        ClienteRequestDTO updateRequest = new ClienteRequestDTO("Maria Nova", "12345678909", "maria2@email.com", "11988888888");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByCpfCnpj("12345678909")).thenReturn(true);

        assertThrows(ClienteDuplicadoException.class, () -> clienteService.atualizar(1L, updateRequest));
        verify(clienteRepository, never()).save(cliente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar atualizar cliente inexistente")
    void deveLancarExcecaoAoAtualizarClienteInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class, () -> clienteService.atualizar(99L, request));
    }

    @Test
    @DisplayName("Deve excluir cliente por ID com sucesso")
    void deveExcluirClientePorId() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.excluir(1L);

        verify(clienteRepository).delete(cliente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar excluir cliente inexistente")
    void deveLancarExcecaoAoExcluirClienteInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class, () -> clienteService.excluir(99L));
        verify(clienteRepository, never()).delete(any());
    }
}