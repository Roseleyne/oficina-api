package br.com.fiap.oficina.interfaces.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClienteNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarClienteNaoEncontrado(
            ClienteNaoEncontradoException exception) {
        return resposta(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ClienteDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> tratarClienteDuplicado(
            ClienteDuplicadoException exception) {
        return resposta(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(VeiculoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarVeiculoNaoEncontrado(
            VeiculoNaoEncontradoException exception) {
        return resposta(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(VeiculoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> tratarVeiculoDuplicado(
            VeiculoDuplicadoException exception) {
        return resposta(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ServicoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarServicoNaoEncontrado(
            ServicoNaoEncontradoException exception) {
        return resposta(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ServicoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> tratarServicoDuplicado(
            ServicoDuplicadoException exception) {
        return resposta(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(PecaNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> tratarPecaNaoEncontrada(
            PecaNaoEncontradaException exception) {
        return resposta(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PecaDuplicadaException.class)
    public ResponseEntity<Map<String, Object>> tratarPecaDuplicada(
            PecaDuplicadaException exception) {
        return resposta(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> tratarEstoqueInsuficiente(
            EstoqueInsuficienteException exception) {
        return resposta(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(OrdemServicoNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> tratarOrdemServicoNaoEncontrada(
            OrdemServicoNaoEncontradaException exception) {
        return resposta(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(WebhookNaoAutorizadoException.class)
    public ResponseEntity<Map<String, Object>> tratarWebhookNaoAutorizado(
            WebhookNaoAutorizadoException exception) {
        return resposta(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(StatusOrdemServicoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> tratarStatusInvalido(
            StatusOrdemServicoInvalidoException exception) {
        return resposta(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, Object>> tratarCredenciaisInvalidas(
            CredenciaisInvalidasException exception) {
        return resposta(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(
            MethodArgumentNotValidException exception) {

        String mensagem = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(erro -> erro.getDefaultMessage())
                .orElse("Dados inválidos");

        return resposta(
                HttpStatus.BAD_REQUEST,
                mensagem
        );
    }

    private ResponseEntity<Map<String, Object>> resposta(
            HttpStatus status,
            String mensagem) {

        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "status", status.value(),
                        "error", status.getReasonPhrase(),
                        "message", mensagem
                ));
    }
}