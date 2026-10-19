package br.com.fiap.oficina.integration;

import br.com.fiap.oficina.application.services.OrdemServicoService;
import br.com.fiap.oficina.domain.cliente.Cliente;
import br.com.fiap.oficina.domain.ordemservico.OrdemServico;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.fiap.oficina.domain.veiculo.Veiculo;
import br.com.fiap.oficina.infrastructure.repositories.ClienteRepository;
import br.com.fiap.oficina.infrastructure.repositories.OrdemServicoRepository;
import br.com.fiap.oficina.infrastructure.repositories.VeiculoRepository;
import br.com.fiap.oficina.interfaces.dtos.OrdemServicoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Valida a listagem de OS contra o banco (H2): filtro por status na query
 * e ordenacao por prioridade de status + data de abertura.
 */
@SpringBootTest
@Transactional
class OrdemServicoListagemIntegrationTest {

    @Autowired
    private OrdemServicoService ordemServicoService;

    @Autowired
    private OrdemServicoRepository ordemServicoRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    private Veiculo criarVeiculo() {
        String sufixo = String.valueOf(System.nanoTime());
        sufixo = sufixo.substring(sufixo.length() - 8);

        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Listagem");
        cliente.setCpfCnpj("777" + sufixo);
        cliente.setEmail("listagem" + sufixo + "@email.com");
        cliente.setTelefone("11999990000");
        cliente = clienteRepository.save(cliente);

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca("LST" + sufixo.substring(4));
        veiculo.setMarca("Fiat");
        veiculo.setModelo("Uno");
        veiculo.setAno(2020);
        veiculo.setCliente(cliente);
        return veiculoRepository.save(veiculo);
    }

    private Long salvarOrdem(Veiculo veiculo, StatusOrdemServico status, LocalDateTime abertura) {
        OrdemServico os = new OrdemServico();
        os.setVeiculo(veiculo);
        os.setDescricaoProblema("Teste listagem " + status);
        os.setStatus(status);
        os.setValorTotal(BigDecimal.ZERO);
        os.setDataAbertura(abertura);
        return ordemServicoRepository.save(os).getId();
    }

    @Test
    @DisplayName("GET /ordens-servico: ordena por status e data e exclui Finalizada/Entregue/Cancelada")
    void deveListarOrdensAtivasOrdenadasEExcluirEncerradas() {
        Veiculo veiculo = criarVeiculo();
        LocalDateTime base = LocalDateTime.of(2026, 1, 10, 9, 0);

        Long recebidaNova = salvarOrdem(veiculo, StatusOrdemServico.RECEBIDA, base.plusDays(3));
        Long finalizada = salvarOrdem(veiculo, StatusOrdemServico.FINALIZADA, base);
        Long execucao = salvarOrdem(veiculo, StatusOrdemServico.EM_EXECUCAO, base.plusDays(2));
        Long entregue = salvarOrdem(veiculo, StatusOrdemServico.ENTREGUE, base);
        Long aguardando = salvarOrdem(veiculo, StatusOrdemServico.AGUARDANDO_APROVACAO, base.plusDays(1));
        Long cancelada = salvarOrdem(veiculo, StatusOrdemServico.CANCELADA, base);
        Long diagnostico = salvarOrdem(veiculo, StatusOrdemServico.EM_DIAGNOSTICO, base.plusDays(1));
        Long recebidaAntiga = salvarOrdem(veiculo, StatusOrdemServico.RECEBIDA, base);

        Set<Long> criadas = Set.of(recebidaNova, finalizada, execucao, entregue,
                aguardando, cancelada, diagnostico, recebidaAntiga);

        List<Long> idsListados = ordemServicoService.listarTodas()
                .stream()
                .map(OrdemServicoResponseDTO::id)
                .filter(criadas::contains)
                .toList();

        assertEquals(
                List.of(execucao, aguardando, diagnostico, recebidaAntiga, recebidaNova),
                idsListados
        );
    }
}
