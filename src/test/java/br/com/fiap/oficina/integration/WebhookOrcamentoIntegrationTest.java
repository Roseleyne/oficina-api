package br.com.fiap.oficina.integration;

import br.com.fiap.oficina.domain.cliente.Cliente;
import br.com.fiap.oficina.domain.ordemservico.OrdemServico;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.fiap.oficina.domain.veiculo.Veiculo;
import br.com.fiap.oficina.infrastructure.repositories.ClienteRepository;
import br.com.fiap.oficina.infrastructure.repositories.OrdemServicoRepository;
import br.com.fiap.oficina.infrastructure.repositories.VeiculoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /webhooks/orcamento: aprovacao/recusa externa do orcamento.
 * O token valido em testes vem de src/test/resources/application.properties.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebhookOrcamentoIntegrationTest {

    private static final String TOKEN_VALIDO = "token-teste-webhook";
    private static final String HEADER = "X-Webhook-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private OrdemServicoRepository ordemServicoRepository;

    private Long criarOrdem(StatusOrdemServico statusOrdem) {
        String sufixo = String.valueOf(System.nanoTime());
        sufixo = sufixo.substring(sufixo.length() - 8);

        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Webhook");
        cliente.setCpfCnpj("999" + sufixo);
        cliente.setEmail("webhook" + sufixo + "@email.com");
        cliente.setTelefone("11977770000");
        cliente = clienteRepository.save(cliente);

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca("WBH" + sufixo.substring(4));
        veiculo.setMarca("Ford");
        veiculo.setModelo("Ka");
        veiculo.setAno(2021);
        veiculo.setCliente(cliente);
        veiculo = veiculoRepository.save(veiculo);

        OrdemServico os = new OrdemServico();
        os.setVeiculo(veiculo);
        os.setDescricaoProblema("Teste webhook de orçamento");
        os.setStatus(statusOrdem);
        os.setValorTotal(BigDecimal.ZERO);
        os.setDataAbertura(LocalDateTime.now());
        return ordemServicoRepository.save(os).getId();
    }

    private String corpo(Long id, boolean aprovado) {
        return "{\"ordemServicoId\": " + id + ", \"aprovado\": " + aprovado
                + ", \"observacao\": \"Resposta do cliente\"}";
    }

    @Test
    @DisplayName("Aprovado com token válido: 200 e OS vai para APROVADA")
    void deveAprovarOrcamento() throws Exception {
        Long id = criarOrdem(StatusOrdemServico.AGUARDANDO_APROVACAO);

        mockMvc.perform(post("/webhooks/orcamento")
                        .header(HEADER, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(id, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"));
    }

    @Test
    @DisplayName("Recusado com token válido: 200 e OS vai para CANCELADA")
    void deveRecusarOrcamento() throws Exception {
        Long id = criarOrdem(StatusOrdemServico.AGUARDANDO_APROVACAO);

        mockMvc.perform(post("/webhooks/orcamento")
                        .header(HEADER, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(id, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    @DisplayName("Token inválido: 401")
    void deveRetornar401ComTokenInvalido() throws Exception {
        Long id = criarOrdem(StatusOrdemServico.AGUARDANDO_APROVACAO);

        mockMvc.perform(post("/webhooks/orcamento")
                        .header(HEADER, "token-errado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(id, true)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token do webhook inválido ou ausente"));
    }

    @Test
    @DisplayName("Sem token: 401")
    void deveRetornar401SemToken() throws Exception {
        Long id = criarOrdem(StatusOrdemServico.AGUARDANDO_APROVACAO);

        mockMvc.perform(post("/webhooks/orcamento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(id, true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("OS fora de AGUARDANDO_APROVACAO: 409")
    void deveRetornar409QuandoOrdemNaoAguardaAprovacao() throws Exception {
        Long id = criarOrdem(StatusOrdemServico.RECEBIDA);

        mockMvc.perform(post("/webhooks/orcamento")
                        .header(HEADER, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(id, true)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("OS inexistente: 404")
    void deveRetornar404ParaOrdemInexistente() throws Exception {
        mockMvc.perform(post("/webhooks/orcamento")
                        .header(HEADER, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(999999L, true)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Corpo inválido (sem ordemServicoId): 400")
    void deveRetornar400ComCorpoInvalido() throws Exception {
        mockMvc.perform(post("/webhooks/orcamento")
                        .header(HEADER, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprovado\": true}"))
                .andExpect(status().isBadRequest());
    }
}
