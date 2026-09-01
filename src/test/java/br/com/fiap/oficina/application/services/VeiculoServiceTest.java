package br.com.fiap.oficina.application.services;

import br.com.fiap.oficina.domain.cliente.Cliente;
import br.com.fiap.oficina.domain.veiculo.Veiculo;
import br.com.fiap.oficina.infrastructure.repositories.ClienteRepository;
import br.com.fiap.oficina.infrastructure.repositories.VeiculoRepository;
import br.com.fiap.oficina.interfaces.dtos.VeiculoRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.VeiculoResponseDTO;
import br.com.fiap.oficina.interfaces.exceptions.ClienteNaoEncontradoException;
import br.com.fiap.oficina.interfaces.exceptions.VeiculoDuplicadoException;
import br.com.fiap.oficina.interfaces.exceptions.VeiculoNaoEncontradoException;
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
class VeiculoServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private VeiculoService veiculoService;

    private Cliente cliente;
    private Veiculo veiculo;
    private VeiculoRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Carlos Andrade");

        veiculo = new Veiculo();
        veiculo.setId(10L);
        veiculo.setPlaca("BRA2E19");
        veiculo.setMarca("Chevrolet");
        veiculo.setModelo("Onix");
        veiculo.setAno(2023);
        veiculo.setCliente(cliente);

        requestDTO = new VeiculoRequestDTO("BRA2E19", "Chevrolet", "Onix", 2023, 1L);
    }

    @Test
    @DisplayName("Deve criar veículo com sucesso quando cliente existe e placa é única")
    void deveCriarVeiculoComSucesso() {
        when(veiculoRepository.existsByPlaca("BRA2E19")).thenReturn(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.save(any())).thenReturn(veiculo);

        VeiculoResponseDTO response = veiculoService.criar(requestDTO);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("BRA2E19", response.placa());
        assertEquals("Carlos Andrade", response.clienteNome());
        verify(veiculoRepository).save(any());
    }

    @Test
    @DisplayName("Deve lançar VeiculoDuplicadoException ao tentar cadastrar placa repetida")
    void deveLancarExcecaoAoCriarPlacaDuplicada() {
        when(veiculoRepository.existsByPlaca("BRA2E19")).thenReturn(true);

        assertThrows(VeiculoDuplicadoException.class, () -> veiculoService.criar(requestDTO));
        verify(veiculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ClienteNaoEncontradoException ao cadastrar veículo para cliente inexistente")
    void deveLancarExcecaoAoCriarVeiculoParaClienteInexistente() {
        when(veiculoRepository.existsByPlaca("BRA2E19")).thenReturn(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class, () -> veiculoService.criar(requestDTO));
    }

    @Test
    @DisplayName("Deve listar todos os veículos")
    void deveListarTodosOsVeiculos() {
        when(veiculoRepository.findAll()).thenReturn(List.of(veiculo));

        List<VeiculoResponseDTO> lista = veiculoService.listarTodos();

        assertFalse(lista.isEmpty());
        assertEquals(1, lista.size());
    }

    @Test
    @DisplayName("Deve buscar veículo por ID")
    void deveBuscarVeiculoPorId() {
        when(veiculoRepository.findById(10L)).thenReturn(Optional.of(veiculo));

        VeiculoResponseDTO response = veiculoService.buscarPorId(10L);

        assertNotNull(response);
        assertEquals("BRA2E19", response.placa());
    }

    @Test
    @DisplayName("Deve lançar VeiculoNaoEncontradoException ao buscar por ID inexistente")
    void deveLancarExcecaoAoBuscarVeiculoInexistentePorId() {
        when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VeiculoNaoEncontradoException.class, () -> veiculoService.buscarPorId(99L));
    }

    @Test
    @DisplayName("Deve buscar veículos por cliente ID")
    void deveBuscarVeiculosPorCliente() {
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(veiculoRepository.findByClienteId(1L)).thenReturn(List.of(veiculo));

        List<VeiculoResponseDTO> lista = veiculoService.buscarPorCliente(1L);

        assertEquals(1, lista.size());
    }

    @Test
    @DisplayName("Deve lançar ClienteNaoEncontradoException ao buscar veículos de cliente inexistente")
    void deveLancarExcecaoAoBuscarVeiculosClienteInexistente() {
        when(clienteRepository.existsById(99L)).thenReturn(false);

        assertThrows(ClienteNaoEncontradoException.class, () -> veiculoService.buscarPorCliente(99L));
    }

    @Test
    @DisplayName("Deve atualizar veículo mantendo a mesma placa")
    void deveAtualizarVeiculoMantendoMesmaPlaca() {
        when(veiculoRepository.findById(10L)).thenReturn(Optional.of(veiculo));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.save(veiculo)).thenReturn(veiculo);

        VeiculoResponseDTO response = veiculoService.atualizar(10L, requestDTO);

        assertNotNull(response);
        verify(veiculoRepository).save(veiculo);
    }

    @Test
    @DisplayName("Deve atualizar veículo com nova placa válida")
    void deveAtualizarVeiculoComNovaPlacaValida() {
        VeiculoRequestDTO updateDTO = new VeiculoRequestDTO("NEW1A23", "Chevrolet", "Onix Plus", 2024, 1L);

        when(veiculoRepository.findById(10L)).thenReturn(Optional.of(veiculo));
        when(veiculoRepository.existsByPlaca("NEW1A23")).thenReturn(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.save(veiculo)).thenReturn(veiculo);

        VeiculoResponseDTO response = veiculoService.atualizar(10L, updateDTO);

        assertNotNull(response);
        assertEquals("NEW1A23", veiculo.getPlaca());
        assertEquals("Onix Plus", veiculo.getModelo());
        verify(veiculoRepository).save(veiculo);
    }

    @Test
    @DisplayName("Deve lançar VeiculoDuplicadoException ao atualizar para placa já existente")
    void deveLancarExcecaoAoAtualizarParaPlacaExistente() {
        VeiculoRequestDTO updateDTO = new VeiculoRequestDTO("NEW1A23", "Chevrolet", "Onix Plus", 2024, 1L);

        when(veiculoRepository.findById(10L)).thenReturn(Optional.of(veiculo));
        when(veiculoRepository.existsByPlaca("NEW1A23")).thenReturn(true);

        assertThrows(VeiculoDuplicadoException.class, () -> veiculoService.atualizar(10L, updateDTO));
        verify(veiculoRepository, never()).save(veiculo);
    }

    @Test
    @DisplayName("Deve lançar VeiculoNaoEncontradoException ao atualizar veículo inexistente")
    void deveLancarExcecaoAoAtualizarVeiculoInexistente() {
        when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VeiculoNaoEncontradoException.class, () -> veiculoService.atualizar(99L, requestDTO));
    }

    @Test
    @DisplayName("Deve lançar ClienteNaoEncontradoException ao atualizar veículo com cliente inexistente")
    void deveLancarExcecaoAoAtualizarVeiculoComClienteInexistente() {
        VeiculoRequestDTO updateDTO = new VeiculoRequestDTO("BRA2E19", "Chevrolet", "Onix", 2023, 99L);

        when(veiculoRepository.findById(10L)).thenReturn(Optional.of(veiculo));
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNaoEncontradoException.class, () -> veiculoService.atualizar(10L, updateDTO));
    }

    @Test
    @DisplayName("Deve excluir veículo por ID")
    void deveExcluirVeiculoComSucesso() {
        when(veiculoRepository.findById(10L)).thenReturn(Optional.of(veiculo));

        veiculoService.excluir(10L);

        verify(veiculoRepository).delete(veiculo);
    }

    @Test
    @DisplayName("Deve lançar VeiculoNaoEncontradoException ao excluir ID inexistente")
    void deveLancarExcecaoAoExcluirVeiculoInexistente() {
        when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VeiculoNaoEncontradoException.class, () -> veiculoService.excluir(99L));
    }
}
