package br.com.fiap.oficina.integration;

import br.com.fiap.oficina.config.JwtService;
import br.com.fiap.oficina.domain.peca.Peca;
import br.com.fiap.oficina.domain.servico.Servico;
import br.com.fiap.oficina.domain.usuario.PerfilUsuario;
import br.com.fiap.oficina.domain.usuario.Usuario;
import br.com.fiap.oficina.infrastructure.repositories.PecaRepository;
import br.com.fiap.oficina.infrastructure.repositories.ServicoRepository;
import br.com.fiap.oficina.infrastructure.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /ordens-servico/completa: abertura de OS com cliente, veiculo, servicos e pecas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AberturaOrdemServicoIntegrationTest {

    private static final String EMAIL = "abertura.integracao@oficina.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private PecaRepository pecaRepository;

    private String token;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.findByEmail(EMAIL).orElseGet(() -> {
            Usuario novo = new Usuario();
            novo.setNome("Usuário Abertura Integração");
            novo.setEmail(EMAIL);
            novo.setSenha(passwordEncoder.encode("Teste@123"));
            novo.setPerfil(PerfilUsuario.ADMIN);
            novo.setAtivo(true);
            return usuarioRepository.save(novo);
        });
        token = jwtService.gerarToken(usuario.getEmail(), usuario.getPerfil().name());
    }

    /** Gera um CPF valido (digitos verificadores corretos) e aleatorio. */
    private static String cpfValido() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = ThreadLocalRandom.current().nextInt(10);
            }
        } while (d[0] == d[1] && d[1] == d[2]);
        for (int j = 9; j < 11; j++) {
            int soma = 0;
            for (int i = 0; i < j; i++) {
                soma += d[i] * ((j + 1) - i);
            }
            int resto = (soma * 10) % 11;
            d[j] = resto == 10 ? 0 : resto;
        }
        StringBuilder sb = new StringBuilder();
        for (int n : d) {
            sb.append(n);
        }
        return sb.toString();
    }

    private static String placaValida() {
        return "TST" + (1000 + ThreadLocalRandom.current().nextInt(9000));
    }

    private Servico criarServico() {
        Servico s = new Servico();
        s.setNome("Revisão freios " + System.nanoTime());
        s.setDescricao("Troca de pastilhas");
        s.setPreco(new BigDecimal("100.00"));
        return servicoRepository.save(s);
    }

    private Peca criarPeca(int estoque) {
        Peca p = new Peca();
        p.setCodigo("PC-" + System.nanoTime());
        p.setNome("Pastilha de freio");
        p.setDescricao("Jogo dianteiro");
        p.setPreco(new BigDecimal("25.00"));
        p.setQuantidadeEstoque(estoque);
        return pecaRepository.save(p);
    }

    private String corpo(String cpf, String placa, String itens) {
        return """
                {
                  "cliente": {"nome": "Carlos Andrade", "cpfCnpj": "%s",
                              "email": "carlos@email.com", "telefone": "11977776666"},
                  "veiculo": {"placa": "%s", "marca": "Chevrolet", "modelo": "Onix", "ano": 2023},
                  "descricaoProblema": "Barulho no freio dianteiro"%s
                }
                """.formatted(cpf, placa, itens);
    }

    @Test
    @DisplayName("Com serviços e peças: 201, OS AGUARDANDO_APROVACAO, valor total e estoque corretos")
    void deveAbrirOrdemCompletaComItens() throws Exception {
        Servico servico = criarServico();
        Peca peca = criarPeca(10);
        String itens = """
                ,
                  "servicos": [{"servicoId": %d}],
                  "pecas": [{"pecaId": %d, "quantidade": 2}]
                """.formatted(servico.getId(), peca.getId());

        mockMvc.perform(post("/ordens-servico/completa")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(cpfValido(), placaValida(), itens)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.valorTotal").value(150.0))
                .andExpect(jsonPath("$.servicos.length()").value(1))
                .andExpect(jsonPath("$.pecas.length()").value(1));

        assertEquals(8, pecaRepository.findById(peca.getId()).orElseThrow()
                .getQuantidadeEstoque().intValue());
    }

    @Test
    @DisplayName("Sem itens: 201 e OS RECEBIDA")
    void deveAbrirOrdemSemItens() throws Exception {
        mockMvc.perform(post("/ordens-servico/completa")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(cpfValido(), placaValida(), "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEBIDA"));
    }

    @Test
    @DisplayName("Mesmo CPF em duas aberturas: reaproveita o cliente")
    void deveReaproveitarClientePeloCpf() throws Exception {
        String cpf = cpfValido();

        String primeira = mockMvc.perform(post("/ordens-servico/completa")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(cpf, placaValida(), "")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String segunda = mockMvc.perform(post("/ordens-servico/completa")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(cpf, placaValida(), "")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Object clientePrimeira = com.jayway.jsonpath.JsonPath.read(primeira, "$.clienteId");
        Object clienteSegunda = com.jayway.jsonpath.JsonPath.read(segunda, "$.clienteId");
        assertEquals(clientePrimeira, clienteSegunda);
    }

    @Test
    @DisplayName("Estoque insuficiente: 409 e nada é aberto")
    void deveRecusarQuandoEstoqueInsuficiente() throws Exception {
        Peca peca = criarPeca(1);
        String itens = """
                ,
                  "pecas": [{"pecaId": %d, "quantidade": 5}]
                """.formatted(peca.getId());

        mockMvc.perform(post("/ordens-servico/completa")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(cpfValido(), placaValida(), itens)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CPF inválido: 400")
    void deveRetornar400ComCpfInvalido() throws Exception {
        mockMvc.perform(post("/ordens-servico/completa")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("12345678900", placaValida(), "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sem token: 401")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(post("/ordens-servico/completa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(cpfValido(), placaValida(), "")))
                .andExpect(status().isUnauthorized());
    }
}
