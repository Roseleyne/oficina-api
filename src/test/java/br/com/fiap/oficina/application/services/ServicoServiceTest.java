package br.com.fiap.oficina.application.services;

import br.com.fiap.oficina.domain.servico.Servico;
import br.com.fiap.oficina.infrastructure.repositories.ServicoRepository;
import br.com.fiap.oficina.interfaces.dtos.ServicoRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.ServicoResponseDTO;
import br.com.fiap.oficina.interfaces.exceptions.ServicoDuplicadoException;
import br.com.fiap.oficina.interfaces.exceptions.ServicoNaoEncontradoException;
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
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;

    @InjectMocks
    private ServicoService servicoService;

    private Servico servico;
    private ServicoRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        servico = new Servico();
        servico.setId(1L);
        servico.setNome("Troca de Óleo");
        servico.setDescricao("Troca de óleo do motor");
        servico.setPreco(new BigDecimal("150.00"));

        requestDTO = new ServicoRequestDTO("Troca de Óleo", "Troca de óleo do motor", new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Deve criar serviço com sucesso quando dados válidos")
    void deveCriarServicoComSucesso() {
        when(servicoRepository.existsByNomeIgnoreCase(any())).thenReturn(false);
        when(servicoRepository.save(any())).thenReturn(servico);

        ServicoResponseDTO response = servicoService.criar(requestDTO);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Troca de Óleo", response.nome());
        verify(servicoRepository).save(any());
    }

    @Test
    @DisplayName("Deve lançar ServicoDuplicadoException ao criar serviço com nome existente")
    void deveLancarExcecaoAoCriarServicoDuplicado() {
        when(servicoRepository.existsByNomeIgnoreCase(any())).thenReturn(true);

        assertThrows(ServicoDuplicadoException.class, () -> servicoService.criar(requestDTO));
        verify(servicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar todos os serviços")
    void deveListarTodosOsServicos() {
        when(servicoRepository.findAll()).thenReturn(List.of(servico));

        List<ServicoResponseDTO> lista = servicoService.listarTodos();

        assertFalse(lista.isEmpty());
        assertEquals(1, lista.size());
    }

    @Test
    @DisplayName("Deve buscar serviço por ID existente")
    void deveBuscarServicoPorId() {
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));

        ServicoResponseDTO response = servicoService.buscarPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve lançar ServicoNaoEncontradoException ao buscar ID inexistente")
    void deveLancarExcecaoAoBuscarServicoInexistente() {
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ServicoNaoEncontradoException.class, () -> servicoService.buscarPorId(99L));
    }

    @Test
    @DisplayName("Deve atualizar serviço mantendo o mesmo nome")
    void deveAtualizarServicoMantendoMesmoNome() {
        Servico s = new Servico();
        s.setId(1L);
        s.setNome("Alinhamento");
        s.setDescricao("Alinhamento 3D");
        s.setPreco(new BigDecimal("120.00"));

        ServicoRequestDTO req = new ServicoRequestDTO("Alinhamento", "Alinhamento 3D", new BigDecimal("120.00"));

        when(servicoRepository.findById(1L)).thenReturn(Optional.of(s));
        when(servicoRepository.save(any())).thenReturn(s);

        ServicoResponseDTO response = servicoService.atualizar(1L, req);

        assertNotNull(response);
        verify(servicoRepository).save(any());
        verify(servicoRepository, never()).existsByNomeIgnoreCase(any());
    }

    @Test
    @DisplayName("Deve atualizar serviço com novo nome disponível")
    void deveAtualizarServicoComSucesso() {
        ServicoRequestDTO updateRequest = new ServicoRequestDTO("Alinhamento", "Alinhamento 3D", new BigDecimal("120.00"));
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.existsByNomeIgnoreCase("Alinhamento")).thenReturn(false);
        when(servicoRepository.save(any())).thenReturn(servico);

        ServicoResponseDTO response = servicoService.atualizar(1L, updateRequest);

        assertNotNull(response);
        verify(servicoRepository).save(any());
    }

    @Test
    @DisplayName("Deve lançar ServicoDuplicadoException ao atualizar para nome existente")
    void deveLancarExcecaoAoAtualizarServicoComNomeDuplicado() {
        ServicoRequestDTO updateRequest = new ServicoRequestDTO("Balanceamento", "Desc", new BigDecimal("80.00"));
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.existsByNomeIgnoreCase("Balanceamento")).thenReturn(true);

        assertThrows(ServicoDuplicadoException.class, () -> servicoService.atualizar(1L, updateRequest));
        verify(servicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ServicoNaoEncontradoException ao atualizar serviço inexistente")
    void deveLancarExcecaoAoAtualizarServicoInexistente() {
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ServicoNaoEncontradoException.class, () -> servicoService.atualizar(99L, requestDTO));
    }

    @Test
    @DisplayName("Deve excluir serviço com sucesso")
    void deveExcluirServicoComSucesso() {
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));

        servicoService.excluir(1L);

        verify(servicoRepository).delete(servico);
    }

    @Test
    @DisplayName("Deve lançar ServicoNaoEncontradoException ao excluir serviço inexistente")
    void deveLancarExcecaoAoExcluirServicoInexistente() {
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ServicoNaoEncontradoException.class, () -> servicoService.excluir(99L));
        verify(servicoRepository, never()).delete(any());
    }
}
