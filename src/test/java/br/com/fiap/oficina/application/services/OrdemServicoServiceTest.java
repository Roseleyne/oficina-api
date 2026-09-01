package br.com.fiap.oficina.application.services;

import br.com.fiap.oficina.domain.cliente.Cliente;
import br.com.fiap.oficina.domain.ordemservico.OrdemServico;
import br.com.fiap.oficina.domain.ordemservico.OrdemServicoPeca;
import br.com.fiap.oficina.domain.ordemservico.OrdemServicoServico;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.fiap.oficina.domain.peca.Peca;
import br.com.fiap.oficina.domain.servico.Servico;
import br.com.fiap.oficina.domain.veiculo.Veiculo;
import br.com.fiap.oficina.infrastructure.repositories.*;
import br.com.fiap.oficina.interfaces.dtos.*;
import br.com.fiap.oficina.interfaces.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemServicoServiceTest {

    @Mock
    private OrdemServicoRepository ordemServicoRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private PecaRepository pecaRepository;

    @Mock
    private OrdemServicoServicoRepository ordemServicoServicoRepository;

    @Mock
    private OrdemServicoPecaRepository ordemServicoPecaRepository;

    @InjectMocks
    private OrdemServicoService ordemServicoService;

    private Cliente cliente;
    private Veiculo veiculo;
    private OrdemServico ordemServico;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Maria Silva");
        cliente.setCpfCnpj("52998224725");
        cliente.setEmail("maria@email.com");
        cliente.setTelefone("11999999999");

        veiculo = new Veiculo();
        veiculo.setId(1L);
        veiculo.setPlaca("ABC1D23");
        veiculo.setMarca("Toyota");
        veiculo.setModelo("Corolla");
        veiculo.setAno(2023);
        veiculo.setCliente(cliente);

        ordemServico = new OrdemServico();
        ordemServico.setId(1L);
        ordemServico.setVeiculo(veiculo);
        ordemServico.setDescricaoProblema("Ruído ao frear");
        ordemServico.setStatus(StatusOrdemServico.RECEBIDA);
        ordemServico.setValorTotal(BigDecimal.ZERO);
        ordemServico.setDataAbertura(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar ordem de serviço com sucesso")
    void deveCriarOrdemServico() {
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        when(ordemServicoRepository.save(any(OrdemServico.class))).thenAnswer(invocation -> {
            OrdemServico os = invocation.getArgument(0);
            os.setId(1L);
            return os;
        });
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of());
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of());

        OrdemServicoRequestDTO request = new OrdemServicoRequestDTO(1L, "Ruído ao frear");

        OrdemServicoResponseDTO response = ordemServicoService.criar(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(StatusOrdemServico.RECEBIDA, response.status());
        assertEquals(BigDecimal.ZERO, response.valorTotal());
        assertEquals("ABC1D23", response.placaVeiculo());
        assertEquals("Maria Silva", response.clienteNome());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar ordem com veículo inexistente")
    void deveLancarExcecaoAoCriarOrdemComVeiculoInexistente() {
        when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());

        OrdemServicoRequestDTO request = new OrdemServicoRequestDTO(99L, "Ruído ao frear");

        assertThrows(VeiculoNaoEncontradoException.class, () -> ordemServicoService.criar(request));
        verify(ordemServicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar todas as ordens de serviço")
    void deveListarTodasAsOrdensServico() {
        when(ordemServicoRepository.findAll()).thenReturn(List.of(ordemServico));
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of());
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of());

        List<OrdemServicoResponseDTO> lista = ordemServicoService.listarTodas();

        assertEquals(1, lista.size());
        assertEquals(1L, lista.get(0).id());
    }

    @Test
    @DisplayName("Deve buscar ordem de serviço por ID com itens e serviços")
    void deveBuscarOrdemServicoPorId() {
        Servico servico = new Servico();
        servico.setId(10L);
        servico.setNome("Troca de Pastilha");
        servico.setPreco(new BigDecimal("120.00"));

        OrdemServicoServico itemServico = new OrdemServicoServico();
        itemServico.setId(100L);
        itemServico.setOrdemServico(ordemServico);
        itemServico.setServico(servico);
        itemServico.setPreco(servico.getPreco());

        Peca peca = new Peca();
        peca.setId(20L);
        peca.setNome("Pastilha de Freio");
        peca.setPreco(new BigDecimal("80.00"));

        OrdemServicoPeca itemPeca = new OrdemServicoPeca();
        itemPeca.setId(200L);
        itemPeca.setOrdemServico(ordemServico);
        itemPeca.setPeca(peca);
        itemPeca.setQuantidade(2);
        itemPeca.setPrecoUnitario(peca.getPreco());
        itemPeca.setSubtotal(new BigDecimal("160.00"));

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of(itemServico));
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of(itemPeca));

        OrdemServicoResponseDTO response = ordemServicoService.buscarPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(1, response.servicos().size());
        assertEquals("Troca de Pastilha", response.servicos().get(0).nome());
        assertEquals(1, response.pecas().size());
        assertEquals("Pastilha de Freio", response.pecas().get(0).nome());
        assertEquals(new BigDecimal("160.00"), response.pecas().get(0).subtotal());
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar ordem por ID inexistente")
    void deveLancarExcecaoAoBuscarOrdemInexistente() {
        when(ordemServicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrdemServicoNaoEncontradaException.class, () -> ordemServicoService.buscarPorId(99L));
    }

    @Test
    @DisplayName("Deve buscar ordens de serviço por veículo")
    void deveBuscarOrdensPorVeiculo() {
        when(veiculoRepository.existsById(1L)).thenReturn(true);
        when(ordemServicoRepository.findByVeiculoId(1L)).thenReturn(List.of(ordemServico));
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of());
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of());

        List<OrdemServicoResponseDTO> lista = ordemServicoService.buscarPorVeiculo(1L);

        assertEquals(1, lista.size());
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar ordens para veículo inexistente")
    void deveLancarExcecaoAoBuscarOrdensParaVeiculoInexistente() {
        when(veiculoRepository.existsById(99L)).thenReturn(false);

        assertThrows(VeiculoNaoEncontradoException.class, () -> ordemServicoService.buscarPorVeiculo(99L));
    }

    @Test
    @DisplayName("Deve adicionar serviço à OS em diagnóstico com sucesso")
    void deveAdicionarServicoComSucesso() {
        ordemServico.setStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        ordemServico.setValorTotal(new BigDecimal("50.00"));

        Servico servico = new Servico();
        servico.setId(10L);
        servico.setNome("Alinhamento");
        servico.setPreco(new BigDecimal("100.00"));

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(servicoRepository.findById(10L)).thenReturn(Optional.of(servico));
        when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of());
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of());

        AdicionarServicoOSDTO request = new AdicionarServicoOSDTO(10L);

        OrdemServicoResponseDTO response = ordemServicoService.adicionarServico(1L, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("150.00"), ordemServico.getValorTotal());
        verify(ordemServicoServicoRepository).save(any(OrdemServicoServico.class));
        verify(ordemServicoRepository).save(ordemServico);
    }

    @Test
    @DisplayName("Deve impedir adicionar serviço quando status da OS não é EM_DIAGNOSTICO")
    void deveImpedirAdicionarServicoQuandoStatusNaoForDiagnostico() {
        ordemServico.setStatus(StatusOrdemServico.RECEBIDA);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));

        AdicionarServicoOSDTO request = new AdicionarServicoOSDTO(10L);

        assertThrows(StatusOrdemServicoInvalidoException.class, () -> ordemServicoService.adicionarServico(1L, request));
        verify(ordemServicoServicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando serviço não for encontrado ao adicionar à OS")
    void deveLancarExcecaoQuandoServicoNaoEncontradoAoAdicionar() {
        ordemServico.setStatus(StatusOrdemServico.EM_DIAGNOSTICO);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        AdicionarServicoOSDTO request = new AdicionarServicoOSDTO(99L);

        assertThrows(ServicoNaoEncontradoException.class, () -> ordemServicoService.adicionarServico(1L, request));
    }

    @Test
    @DisplayName("Deve adicionar peça à OS com sucesso atualizando estoque e valor total")
    void deveAdicionarPecaComSucesso() {
        ordemServico.setStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        ordemServico.setValorTotal(new BigDecimal("100.00"));

        Peca peca = new Peca();
        peca.setId(5L);
        peca.setNome("Vela de Ignição");
        peca.setPreco(new BigDecimal("30.00"));
        peca.setQuantidadeEstoque(10);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(pecaRepository.findById(5L)).thenReturn(Optional.of(peca));
        when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of());
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of());

        AdicionarPecaOSDTO request = new AdicionarPecaOSDTO(5L, 4);

        OrdemServicoResponseDTO response = ordemServicoService.adicionarPeca(1L, request);

        assertNotNull(response);
        assertEquals(6, peca.getQuantidadeEstoque());
        assertEquals(new BigDecimal("220.00"), ordemServico.getValorTotal());
        verify(pecaRepository).save(peca);
        verify(ordemServicoPecaRepository).save(any(OrdemServicoPeca.class));
        verify(ordemServicoRepository).save(ordemServico);
    }

    @Test
    @DisplayName("Deve impedir adicionar peça quando status não for EM_DIAGNOSTICO")
    void deveImpedirAdicionarPecaQuandoStatusNaoForDiagnostico() {
        ordemServico.setStatus(StatusOrdemServico.RECEBIDA);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));

        AdicionarPecaOSDTO request = new AdicionarPecaOSDTO(1L, 2);

        assertThrows(StatusOrdemServicoInvalidoException.class, () -> ordemServicoService.adicionarPeca(1L, request));
        verify(pecaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando peça não for encontrada ao adicionar à OS")
    void deveLancarExcecaoQuandoPecaNaoEncontradaAoAdicionar() {
        ordemServico.setStatus(StatusOrdemServico.EM_DIAGNOSTICO);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(pecaRepository.findById(99L)).thenReturn(Optional.empty());

        AdicionarPecaOSDTO request = new AdicionarPecaOSDTO(99L, 2);

        assertThrows(PecaNaoEncontradaException.class, () -> ordemServicoService.adicionarPeca(1L, request));
    }

    @Test
    @DisplayName("Deve impedir adicionar peça quando estoque insuficiente")
    void deveImpedirPecaQuandoEstoqueInsuficiente() {
        ordemServico.setStatus(StatusOrdemServico.EM_DIAGNOSTICO);

        Peca peca = new Peca();
        peca.setId(1L);
        peca.setCodigo("FLT-001");
        peca.setNome("Filtro");
        peca.setDescricao("Filtro de óleo");
        peca.setPreco(new BigDecimal("49.90"));
        peca.setQuantidadeEstoque(2);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));

        AdicionarPecaOSDTO request = new AdicionarPecaOSDTO(1L, 10);

        assertThrows(EstoqueInsuficienteException.class, () -> ordemServicoService.adicionarPeca(1L, request));

        verify(ordemServicoPecaRepository, never()).save(any());
        verify(pecaRepository, never()).save(any());
    }

    @ParameterizedTest(name = "Transicao valida de {0} para {1}")
    @CsvSource({
            "RECEBIDA, EM_DIAGNOSTICO",
            "RECEBIDA, CANCELADA",
            "EM_DIAGNOSTICO, AGUARDANDO_APROVACAO",
            "EM_DIAGNOSTICO, CANCELADA",
            "AGUARDANDO_APROVACAO, APROVADA",
            "AGUARDANDO_APROVACAO, CANCELADA",
            "APROVADA, EM_EXECUCAO",
            "APROVADA, CANCELADA",
            "EM_EXECUCAO, FINALIZADA",
            "FINALIZADA, ENTREGUE"
    })
    @DisplayName("Deve permitir transições de status válidas")
    void devePermitirTransicoesValidas(StatusOrdemServico statusAtual, StatusOrdemServico novoStatus) {
        ordemServico.setStatus(statusAtual);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of());
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of());

        AtualizarStatusOSDTO request = new AtualizarStatusOSDTO(novoStatus);

        OrdemServicoResponseDTO response = ordemServicoService.atualizarStatus(1L, request);

        assertNotNull(response);
        assertEquals(novoStatus, ordemServico.getStatus());
        verify(ordemServicoRepository).save(ordemServico);
    }

    @Test
    @DisplayName("Deve definir dataInicioExecucao ao mudar para EM_EXECUCAO se ainda nao definida")
    void deveDefinirDataInicioExecucaoAoMudarParaEmExecucao() {
        ordemServico.setStatus(StatusOrdemServico.APROVADA);
        ordemServico.setDataInicioExecucao(null);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of());
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of());

        ordemServicoService.atualizarStatus(1L, new AtualizarStatusOSDTO(StatusOrdemServico.EM_EXECUCAO));

        assertNotNull(ordemServico.getDataInicioExecucao());
    }

    @Test
    @DisplayName("Deve manter dataInicioExecucao se ja existente ao mudar para EM_EXECUCAO")
    void deveManterDataInicioExecucaoSeJaExistente() {
        LocalDateTime jaDefinida = LocalDateTime.now().minusDays(1);
        ordemServico.setStatus(StatusOrdemServico.APROVADA);
        ordemServico.setDataInicioExecucao(jaDefinida);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of());
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of());

        ordemServicoService.atualizarStatus(1L, new AtualizarStatusOSDTO(StatusOrdemServico.EM_EXECUCAO));

        assertEquals(jaDefinida, ordemServico.getDataInicioExecucao());
    }

    @Test
    @DisplayName("Deve definir dataFinalizacao ao mudar para FINALIZADA se ainda nao definida")
    void deveDefinirDataFinalizacaoAoMudarParaFinalizada() {
        ordemServico.setStatus(StatusOrdemServico.EM_EXECUCAO);
        ordemServico.setDataFinalizacao(null);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of());
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of());

        ordemServicoService.atualizarStatus(1L, new AtualizarStatusOSDTO(StatusOrdemServico.FINALIZADA));

        assertNotNull(ordemServico.getDataFinalizacao());
    }

    @Test
    @DisplayName("Deve estornar pecas para o estoque ao cancelar ordem de servico")
    void deveEstornarEstoqueAoCancelarOrdem() {
        ordemServico.setStatus(StatusOrdemServico.EM_DIAGNOSTICO);

        Peca peca = new Peca();
        peca.setId(10L);
        peca.setQuantidadeEstoque(5);

        OrdemServicoPeca itemPeca = new OrdemServicoPeca();
        itemPeca.setPeca(peca);
        itemPeca.setQuantidade(3);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));
        when(ordemServicoPecaRepository.findByOrdemServicoId(1L)).thenReturn(List.of(itemPeca));
        when(ordemServicoRepository.save(ordemServico)).thenReturn(ordemServico);
        when(ordemServicoServicoRepository.findByOrdemServicoId(1L)).thenReturn(List.of());

        ordemServicoService.atualizarStatus(1L, new AtualizarStatusOSDTO(StatusOrdemServico.CANCELADA));

        assertEquals(8, peca.getQuantidadeEstoque());
        verify(pecaRepository).save(peca);
        verify(ordemServicoRepository).save(ordemServico);
    }

    @ParameterizedTest(name = "Transicao invalida de {0} para {1}")
    @CsvSource({
            "RECEBIDA, FINALIZADA",
            "RECEBIDA, EM_EXECUCAO",
            "EM_EXECUCAO, CANCELADA",
            "FINALIZADA, CANCELADA",
            "ENTREGUE, CANCELADA",
            "CANCELADA, RECEBIDA",
            "ENTREGUE, EM_EXECUCAO"
    })
    @DisplayName("Deve lançar exceção em transições de status inválidas")
    void deveLancarExcecaoEmTransicoesInvalidas(StatusOrdemServico statusAtual, StatusOrdemServico novoStatus) {
        ordemServico.setStatus(statusAtual);

        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));

        AtualizarStatusOSDTO request = new AtualizarStatusOSDTO(novoStatus);

        assertThrows(StatusOrdemServicoInvalidoException.class, () -> ordemServicoService.atualizarStatus(1L, request));
        verify(ordemServicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve excluir ordem de serviço por ID")
    void deveExcluirOrdemServico() {
        when(ordemServicoRepository.findById(1L)).thenReturn(Optional.of(ordemServico));

        ordemServicoService.excluir(1L);

        verify(ordemServicoRepository).delete(ordemServico);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar excluir ordem inexistente")
    void deveLancarExcecaoAoExcluirOrdemInexistente() {
        when(ordemServicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrdemServicoNaoEncontradaException.class, () -> ordemServicoService.excluir(99L));
        verify(ordemServicoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve retornar tempo médio zerado quando não houver ordens finalizadas")
    void deveRetornarTempoMedioZeradoQuandoSemOrdens() {
        when(ordemServicoRepository.findAll()).thenReturn(List.of());

        TempoMedioExecucaoDTO dto = ordemServicoService.obterTempoMedioExecucao();

        assertNotNull(dto);
        assertEquals(0, dto.totalOrdensFinalizadas());
        assertEquals(0.0, dto.tempoMedioEmMinutos());
        assertEquals("0 minutos", dto.tempoMedioFormatado());
    }

    @Test
    @DisplayName("Deve formatar tempo médio apenas em minutos quando inferior a 1 hora")
    void deveFormatarTempoMedioApenasMinutos() {
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(45);
        LocalDateTime fim = LocalDateTime.now();

        OrdemServico os = new OrdemServico();
        os.setStatus(StatusOrdemServico.FINALIZADA);
        os.setDataInicioExecucao(inicio);
        os.setDataFinalizacao(fim);

        when(ordemServicoRepository.findAll()).thenReturn(List.of(os));

        TempoMedioExecucaoDTO dto = ordemServicoService.obterTempoMedioExecucao();

        assertEquals(1, dto.totalOrdensFinalizadas());
        assertTrue(dto.tempoMedioFormatado().contains("minuto(s)"));
        assertFalse(dto.tempoMedioFormatado().contains("hora(s)"));
    }

    @Test
    @DisplayName("Deve formatar tempo médio em horas e minutos quando superior a 1 hora e considerar ENTREGUE")
    void deveFormatarTempoMedioHorasEMinutosEStatusEntregue() {
        LocalDateTime inicio = LocalDateTime.now().minusHours(2).minusMinutes(30);
        LocalDateTime fim = LocalDateTime.now();

        OrdemServico osEntregue = new OrdemServico();
        osEntregue.setStatus(StatusOrdemServico.ENTREGUE);
        osEntregue.setDataInicioExecucao(inicio);
        osEntregue.setDataFinalizacao(fim);

        when(ordemServicoRepository.findAll()).thenReturn(List.of(osEntregue));

        TempoMedioExecucaoDTO dto = ordemServicoService.obterTempoMedioExecucao();

        assertEquals(1, dto.totalOrdensFinalizadas());
        assertTrue(dto.tempoMedioFormatado().contains("hora(s)"));
        assertTrue(dto.tempoMedioFormatado().contains("minuto(s)"));
    }
}