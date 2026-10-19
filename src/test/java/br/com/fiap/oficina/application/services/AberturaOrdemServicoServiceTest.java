package br.com.fiap.oficina.application.services;

import br.com.fiap.oficina.domain.cliente.Cliente;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.fiap.oficina.domain.veiculo.Veiculo;
import br.com.fiap.oficina.infrastructure.repositories.ClienteRepository;
import br.com.fiap.oficina.infrastructure.repositories.VeiculoRepository;
import br.com.fiap.oficina.interfaces.dtos.*;
import br.com.fiap.oficina.interfaces.exceptions.VeiculoDuplicadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AberturaOrdemServicoServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private OrdemServicoService ordemServicoService;

    @InjectMocks
    private AberturaOrdemServicoService aberturaService;

    private Cliente cliente;
    private Veiculo veiculo;
    private OrdemServicoResponseDTO osCriada;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Maria Silva");
        cliente.setCpfCnpj("52998224725");

        veiculo = new Veiculo();
        veiculo.setId(10L);
        veiculo.setPlaca("ABC1D23");
        veiculo.setCliente(cliente);

        osCriada = new OrdemServicoResponseDTO(100L, 10L, "ABC1D23", 1L, "Maria Silva",
                "Barulho no freio", StatusOrdemServico.RECEBIDA, List.of(), List.of(),
                BigDecimal.ZERO, LocalDateTime.now(), null, null);
    }

    private AberturaOrdemServicoRequestDTO requisicao(List<AdicionarServicoOSDTO> servicos,
                                                     List<AdicionarPecaOSDTO> pecas) {
        return new AberturaOrdemServicoRequestDTO(
                new ClienteRequestDTO("Maria Silva", "52998224725", "maria@email.com", "11999999999"),
                new VeiculoAberturaDTO("abc1d23", "Toyota", "Corolla", 2023),
                "Barulho no freio",
                servicos,
                pecas
        );
    }

    @Test
    @DisplayName("Cliente e veículo novos, com itens: cria tudo e deixa a OS aguardando aprovação")
    void deveAbrirOrdemCompletaComClienteEVeiculoNovos() {
        when(clienteRepository.findByCpfCnpj("52998224725")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);
        when(veiculoRepository.findByPlaca("ABC1D23")).thenReturn(Optional.empty());
        when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);
        when(ordemServicoService.criar(any())).thenReturn(osCriada);
        when(ordemServicoService.buscarPorId(100L)).thenReturn(osCriada);

        aberturaService.abrir(requisicao(
                List.of(new AdicionarServicoOSDTO(7L)),
                List.of(new AdicionarPecaOSDTO(3L, 2))));

        verify(clienteRepository).save(any(Cliente.class));
        verify(veiculoRepository).save(any(Veiculo.class));

        InOrder ordem = inOrder(ordemServicoService);
        ordem.verify(ordemServicoService).criar(new OrdemServicoRequestDTO(10L, "Barulho no freio"));
        ordem.verify(ordemServicoService).atualizarStatus(100L,
                new AtualizarStatusOSDTO(StatusOrdemServico.EM_DIAGNOSTICO));
        ordem.verify(ordemServicoService).adicionarServico(100L, new AdicionarServicoOSDTO(7L));
        ordem.verify(ordemServicoService).adicionarPeca(100L, new AdicionarPecaOSDTO(3L, 2));
        ordem.verify(ordemServicoService).atualizarStatus(100L,
                new AtualizarStatusOSDTO(StatusOrdemServico.AGUARDANDO_APROVACAO));
        ordem.verify(ordemServicoService).buscarPorId(100L);
    }

    @Test
    @DisplayName("Cliente e veículo já existentes são reaproveitados")
    void deveReaproveitarClienteEVeiculoExistentes() {
        when(clienteRepository.findByCpfCnpj("52998224725")).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findByPlaca("ABC1D23")).thenReturn(Optional.of(veiculo));
        when(ordemServicoService.criar(any())).thenReturn(osCriada);
        when(ordemServicoService.buscarPorId(100L)).thenReturn(osCriada);

        OrdemServicoResponseDTO resposta = aberturaService.abrir(requisicao(List.of(), List.of()));

        assertEquals(100L, resposta.id());
        verify(clienteRepository, never()).save(any());
        verify(veiculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sem serviços e peças a OS permanece RECEBIDA")
    void naoDeveAlterarStatusQuandoNaoHaItens() {
        when(clienteRepository.findByCpfCnpj("52998224725")).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findByPlaca("ABC1D23")).thenReturn(Optional.of(veiculo));
        when(ordemServicoService.criar(any())).thenReturn(osCriada);
        when(ordemServicoService.buscarPorId(100L)).thenReturn(osCriada);

        aberturaService.abrir(requisicao(null, null));

        verify(ordemServicoService, never()).atualizarStatus(anyLong(), any());
        verify(ordemServicoService, never()).adicionarServico(anyLong(), any());
        verify(ordemServicoService, never()).adicionarPeca(anyLong(), any());
    }

    @Test
    @DisplayName("Placa cadastrada para outro cliente: recusa a abertura")
    void deveRecusarVeiculoDeOutroCliente() {
        Cliente outro = new Cliente();
        outro.setId(2L);
        veiculo.setCliente(outro);

        when(clienteRepository.findByCpfCnpj("52998224725")).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findByPlaca("ABC1D23")).thenReturn(Optional.of(veiculo));

        AberturaOrdemServicoRequestDTO request = requisicao(List.of(), List.of());

        assertThrows(VeiculoDuplicadoException.class, () -> aberturaService.abrir(request));
        verify(ordemServicoService, never()).criar(any());
    }
}
