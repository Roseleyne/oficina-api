package br.com.fiap.oficina.application.services;

import br.com.fiap.oficina.domain.peca.Peca;
import br.com.fiap.oficina.infrastructure.repositories.PecaRepository;
import br.com.fiap.oficina.interfaces.dtos.MovimentacaoEstoqueDTO;
import br.com.fiap.oficina.interfaces.dtos.PecaRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.PecaResponseDTO;
import br.com.fiap.oficina.interfaces.exceptions.EstoqueInsuficienteException;
import br.com.fiap.oficina.interfaces.exceptions.PecaDuplicadaException;
import br.com.fiap.oficina.interfaces.exceptions.PecaNaoEncontradaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

    @Mock
    private PecaRepository pecaRepository;

    @InjectMocks
    private PecaService pecaService;

    private Peca peca;
    private PecaRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        peca = new Peca();
        peca.setId(1L);
        peca.setCodigo("FLT-001");
        peca.setNome("Filtro de óleo");
        peca.setDescricao("Filtro de óleo do motor");
        peca.setPreco(new BigDecimal("49.90"));
        peca.setQuantidadeEstoque(20);

        requestDTO = new PecaRequestDTO("FLT-001", "Filtro de óleo", "Filtro de óleo do motor", new BigDecimal("49.90"), 20);
    }

    @Test
    @DisplayName("Deve criar peça com sucesso")
    void deveCriarPecaComSucesso() {
        when(pecaRepository.existsByCodigoIgnoreCase("FLT-001")).thenReturn(false);
        when(pecaRepository.save(any(Peca.class))).thenReturn(peca);

        PecaResponseDTO response = pecaService.criar(requestDTO);

        assertNotNull(response);
        assertEquals("FLT-001", response.codigo());
        assertEquals("Filtro de óleo", response.nome());
        verify(pecaRepository).save(any(Peca.class));
    }

    @Test
    @DisplayName("Deve lançar PecaDuplicadaException ao criar peça com código existente")
    void deveLancarExcecaoAoCriarPecaDuplicada() {
        when(pecaRepository.existsByCodigoIgnoreCase("FLT-001")).thenReturn(true);

        assertThrows(PecaDuplicadaException.class, () -> pecaService.criar(requestDTO));
        verify(pecaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar todas as peças")
    void deveListarTodasAsPecas() {
        when(pecaRepository.findAll()).thenReturn(List.of(peca));

        List<PecaResponseDTO> lista = pecaService.listarTodos();

        assertEquals(1, lista.size());
        assertEquals("FLT-001", lista.get(0).codigo());
    }

    @Test
    @DisplayName("Deve buscar peça por ID com sucesso")
    void deveBuscarPecaPorIdComSucesso() {
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));

        PecaResponseDTO response = pecaService.buscarPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("FLT-001", response.codigo());
    }

    @Test
    @DisplayName("Deve atualizar peça mantendo o mesmo código")
    void deveAtualizarPecaMantendoMesmoCodigo() {
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));
        when(pecaRepository.save(peca)).thenReturn(peca);

        PecaResponseDTO response = pecaService.atualizar(1L, requestDTO);

        assertNotNull(response);
        verify(pecaRepository).save(peca);
    }

    @Test
    @DisplayName("Deve atualizar peça com novo código disponível")
    void deveAtualizarPecaComNovoCodigoDisponivel() {
        PecaRequestDTO updateDTO = new PecaRequestDTO("FLT-002", "Novo Filtro", "Desc", new BigDecimal("55.00"), 15);

        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));
        when(pecaRepository.existsByCodigoIgnoreCase("FLT-002")).thenReturn(false);
        when(pecaRepository.save(peca)).thenReturn(peca);

        PecaResponseDTO response = pecaService.atualizar(1L, updateDTO);

        assertNotNull(response);
        assertEquals("FLT-002", peca.getCodigo());
        verify(pecaRepository).save(peca);
    }

    @Test
    @DisplayName("Deve lançar PecaDuplicadaException ao atualizar com código já usado por outra peça")
    void deveLancarExcecaoAoAtualizarComCodigoDuplicado() {
        PecaRequestDTO updateDTO = new PecaRequestDTO("FLT-002", "Novo Filtro", "Desc", new BigDecimal("55.00"), 15);

        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));
        when(pecaRepository.existsByCodigoIgnoreCase("FLT-002")).thenReturn(true);

        assertThrows(PecaDuplicadaException.class, () -> pecaService.atualizar(1L, updateDTO));
        verify(pecaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar atualizar peça inexistente")
    void deveLancarExcecaoAoAtualizarPecaInexistente() {
        when(pecaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PecaNaoEncontradaException.class, () -> pecaService.atualizar(99L, requestDTO));
    }

    @Test
    @DisplayName("Deve excluir peça por ID com sucesso")
    void deveExcluirPecaPorId() {
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));

        pecaService.excluir(1L);

        verify(pecaRepository).delete(peca);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar excluir peça inexistente")
    void deveLancarExcecaoAoExcluirPecaInexistente() {
        when(pecaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PecaNaoEncontradaException.class, () -> pecaService.excluir(99L));
        verify(pecaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve adicionar estoque com sucesso")
    void deveAdicionarEstoque() {
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));
        when(pecaRepository.save(peca)).thenReturn(peca);

        MovimentacaoEstoqueDTO request = new MovimentacaoEstoqueDTO(10);

        PecaResponseDTO response = pecaService.adicionarEstoque(1L, request);

        assertEquals(30, response.quantidadeEstoque());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar adicionar estoque em peça inexistente")
    void deveLancarExcecaoAoAdicionarEstoquePecaInexistente() {
        when(pecaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PecaNaoEncontradaException.class, () -> pecaService.adicionarEstoque(99L, new MovimentacaoEstoqueDTO(10)));
    }

    @Test
    @DisplayName("Deve baixar estoque com sucesso")
    void deveBaixarEstoque() {
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));
        when(pecaRepository.save(peca)).thenReturn(peca);

        MovimentacaoEstoqueDTO request = new MovimentacaoEstoqueDTO(5);

        PecaResponseDTO response = pecaService.baixarEstoque(1L, request);

        assertEquals(15, response.quantidadeEstoque());
    }

    @Test
    @DisplayName("Deve impedir baixa com estoque insuficiente")
    void deveImpedirBaixaComEstoqueInsuficiente() {
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));

        MovimentacaoEstoqueDTO request = new MovimentacaoEstoqueDTO(100);

        EstoqueInsuficienteException exception = assertThrows(
                EstoqueInsuficienteException.class,
                () -> pecaService.baixarEstoque(1L, request)
        );

        assertEquals("Estoque insuficiente para realizar a baixa", exception.getMessage());
        verify(pecaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar baixar estoque de peça inexistente")
    void deveLancarExcecaoAoBaixarEstoquePecaInexistente() {
        when(pecaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PecaNaoEncontradaException.class, () -> pecaService.baixarEstoque(99L, new MovimentacaoEstoqueDTO(5)));
    }

    @Test
    @DisplayName("Deve retornar erro quando peça não existe ao buscar por ID")
    void deveRetornarErroQuandoPecaNaoExiste() {
        when(pecaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PecaNaoEncontradaException.class, () -> pecaService.buscarPorId(999L));
    }
}