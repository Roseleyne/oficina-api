package br.com.fiap.oficina.application.services;

import br.com.fiap.oficina.config.JwtService;
import br.com.fiap.oficina.domain.usuario.PerfilUsuario;
import br.com.fiap.oficina.domain.usuario.Usuario;
import br.com.fiap.oficina.infrastructure.repositories.UsuarioRepository;
import br.com.fiap.oficina.interfaces.dtos.LoginRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.LoginResponseDTO;
import br.com.fiap.oficina.interfaces.exceptions.CredenciaisInvalidasException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AutenticacaoService autenticacaoService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Admin");
        usuario.setEmail("admin@oficina.com");
        usuario.setSenha("encodedPassword");
        usuario.setPerfil(PerfilUsuario.ADMIN);
        usuario.setAtivo(true);

        ReflectionTestUtils.setField(autenticacaoService, "expiration", 86400000L);
    }

    @Test
    @DisplayName("Deve realizar login com sucesso e retornar token Bearer")
    void deveRealizarLoginComSucesso() {
        LoginRequestDTO request = new LoginRequestDTO("admin@oficina.com", "senha123");

        when(usuarioRepository.findByEmail("admin@oficina.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "encodedPassword")).thenReturn(true);
        when(jwtService.gerarToken("admin@oficina.com", "ADMIN")).thenReturn("jwt-token-valido");

        LoginResponseDTO response = autenticacaoService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token-valido", response.token());
        assertEquals("Bearer", response.tipo());
        assertEquals(86400L, response.expiresIn());
    }

    @Test
    @DisplayName("Deve lançar CredenciaisInvalidasException quando email não for encontrado")
    void deveLancarExcecaoQuandoEmailNaoEncontrado() {
        LoginRequestDTO request = new LoginRequestDTO("naoexiste@oficina.com", "senha123");

        when(usuarioRepository.findByEmail("naoexiste@oficina.com")).thenReturn(Optional.empty());

        CredenciaisInvalidasException ex = assertThrows(
                CredenciaisInvalidasException.class,
                () -> autenticacaoService.login(request)
        );

        assertEquals("E-mail ou senha inválidos", ex.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Deve lançar CredenciaisInvalidasException quando usuário estiver inativo")
    void deveLancarExcecaoQuandoUsuarioInativo() {
        usuario.setAtivo(false);
        LoginRequestDTO request = new LoginRequestDTO("admin@oficina.com", "senha123");

        when(usuarioRepository.findByEmail("admin@oficina.com")).thenReturn(Optional.of(usuario));

        CredenciaisInvalidasException ex = assertThrows(
                CredenciaisInvalidasException.class,
                () -> autenticacaoService.login(request)
        );

        assertEquals("Usuário inativo", ex.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Deve lançar CredenciaisInvalidasException quando senha estiver incorreta")
    void deveLancarExcecaoQuandoSenhaIncorreta() {
        LoginRequestDTO request = new LoginRequestDTO("admin@oficina.com", "senhaErrada");

        when(usuarioRepository.findByEmail("admin@oficina.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "encodedPassword")).thenReturn(false);

        CredenciaisInvalidasException ex = assertThrows(
                CredenciaisInvalidasException.class,
                () -> autenticacaoService.login(request)
        );

        assertEquals("E-mail ou senha inválidos", ex.getMessage());
        verify(jwtService, never()).gerarToken(any(), any());
    }
}
