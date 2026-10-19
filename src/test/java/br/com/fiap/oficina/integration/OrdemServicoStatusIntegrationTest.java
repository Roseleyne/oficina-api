package br.com.fiap.oficina.integration;

import br.com.fiap.oficina.config.JwtService;
import br.com.fiap.oficina.domain.cliente.Cliente;
import br.com.fiap.oficina.domain.ordemservico.OrdemServico;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.fiap.oficina.domain.usuario.PerfilUsuario;
import br.com.fiap.oficina.domain.usuario.Usuario;
import br.com.fiap.oficina.domain.veiculo.Veiculo;
import br.com.fiap.oficina.infrastructure.repositories.ClienteRepository;
import br.com.fiap.oficina.infrastructure.repositories.OrdemServicoRepository;
import br.com.fiap.oficina.infrastructure.repositories.UsuarioRepository;
import br.com.fiap.oficina.infrastructure.repositories.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /ordens-servico/{id}/status: consulta da situacao atual da OS.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrdemServicoStatusIntegrationTest {

    private static final String EMAIL = "status.integracao@oficina.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private OrdemServicoRepository ordemServicoRepository;

    private String token;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.findByEmail(EMAIL).orElseGet(() -> {
            Usuario novo = new Usuario();
            novo.setNome("Usuário Status Integração");
            novo.setEmail(EMAIL);
            novo.setSenha(passwordEncoder.encode("Teste@123"));
            novo.setPerfil(PerfilUsuario.ADMIN);
            novo.setAtivo(true);
            return usuarioRepository.save(novo);
        });

        token = jwtService.gerarToken(usuario.getEmail(), usuario.getPerfil().name());
    }

    private Long criarOrdem(StatusOrdemServico statusOrdem) {
        String sufixo = String.valueOf(System.nanoTime());
        sufixo = sufixo.substring(sufixo.length() - 8);

        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Status");
        cliente.setCpfCnpj("888" + sufixo);
        cliente.setEmail("status" + sufixo + "@email.com");
        cliente.setTelefone("11988880000");
        cliente = clienteRepository.save(cliente);

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca("STS" + sufixo.substring(4));
        veiculo.setMarca("VW");
        veiculo.setModelo("Gol");
        veiculo.setAno(2019);
        veiculo.setCliente(cliente);
        veiculo = veiculoRepository.save(veiculo);

        OrdemServico os = new OrdemServico();
        os.setVeiculo(veiculo);
        os.setDescricaoProblema("Teste consulta de status");
        os.setStatus(statusOrdem);
        os.setValorTotal(BigDecimal.ZERO);
        os.setDataAbertura(LocalDateTime.now());
        return ordemServicoRepository.save(os).getId();
    }

    @Test
    @DisplayName("Deve retornar 200 com o status atual da OS")
    void deveRetornarStatusDaOrdemServico() throws Exception {
        Long id = criarOrdem(StatusOrdemServico.EM_DIAGNOSTICO);

        mockMvc.perform(get("/ordens-servico/" + id + "/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"))
                .andExpect(jsonPath("$.descricaoStatus").value("Em diagnóstico"));
    }

    @Test
    @DisplayName("Deve retornar 404 ao consultar status de OS inexistente")
    void deveRetornar404AoConsultarStatusDeOrdemInexistente() throws Exception {
        mockMvc.perform(get("/ordens-servico/999999/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ordem de serviço não encontrada"));
    }

    @Test
    @DisplayName("Deve retornar 401 ao consultar status sem token")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/ordens-servico/1/status"))
                .andExpect(status().isUnauthorized());
    }
}
