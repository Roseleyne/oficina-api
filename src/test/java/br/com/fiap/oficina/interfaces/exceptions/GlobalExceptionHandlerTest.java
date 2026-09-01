package br.com.fiap.oficina.interfaces.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Deve tratar ClienteNaoEncontradoException retornando 404")
    void deveTratarClienteNaoEncontrado() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarClienteNaoEncontrado(new ClienteNaoEncontradoException("Cliente não encontrado"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Cliente não encontrado", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar ClienteDuplicadoException retornando 409")
    void deveTratarClienteDuplicado() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarClienteDuplicado(new ClienteDuplicadoException("Cliente duplicado"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Cliente duplicado", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar VeiculoNaoEncontradoException retornando 404")
    void deveTratarVeiculoNaoEncontrado() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarVeiculoNaoEncontrado(new VeiculoNaoEncontradoException("Veículo não encontrado"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Veículo não encontrado", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar VeiculoDuplicadoException retornando 409")
    void deveTratarVeiculoDuplicado() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarVeiculoDuplicado(new VeiculoDuplicadoException("Veículo duplicado"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Veículo duplicado", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar ServicoNaoEncontradoException retornando 404")
    void deveTratarServicoNaoEncontrado() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarServicoNaoEncontrado(new ServicoNaoEncontradoException("Serviço não encontrado"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Serviço não encontrado", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar ServicoDuplicadoException retornando 409")
    void deveTratarServicoDuplicado() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarServicoDuplicado(new ServicoDuplicadoException("Serviço duplicado"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Serviço duplicado", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar PecaNaoEncontradaException retornando 404")
    void deveTratarPecaNaoEncontrada() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarPecaNaoEncontrada(new PecaNaoEncontradaException("Peça não encontrada"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Peça não encontrada", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar PecaDuplicadaException retornando 409")
    void deveTratarPecaDuplicada() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarPecaDuplicada(new PecaDuplicadaException("Peça duplicada"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Peça duplicada", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar EstoqueInsuficienteException retornando 409")
    void deveTratarEstoqueInsuficiente() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarEstoqueInsuficiente(new EstoqueInsuficienteException("Estoque insuficiente"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Estoque insuficiente", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar OrdemServicoNaoEncontradaException retornando 404")
    void deveTratarOrdemServicoNaoEncontrada() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarOrdemServicoNaoEncontrada(new OrdemServicoNaoEncontradaException("OS não encontrada"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("OS não encontrada", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar StatusOrdemServicoInvalidoException retornando 409")
    void deveTratarStatusInvalido() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarStatusInvalido(new StatusOrdemServicoInvalidoException("Status inválido"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Status inválido", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar CredenciaisInvalidasException retornando 401")
    void deveTratarCredenciaisInvalidas() {
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.tratarCredenciaisInvalidas(new CredenciaisInvalidasException("Credenciais inválidas"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().get("status"));
        assertEquals("Credenciais inválidas", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException retornando 400 com mensagem do campo")
    void deveTratarMethodArgumentNotValidExceptionComCampo() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "campo", "Campo obrigatório");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = exceptionHandler.tratarValidacao(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Campo obrigatório", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException com mensagem padrão quando lista vazia")
    void deveTratarMethodArgumentNotValidExceptionSemCampo() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = exceptionHandler.tratarValidacao(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Dados inválidos", response.getBody().get("message"));
    }
}
