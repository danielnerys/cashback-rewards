package com.cashbackrewards.compras.api;

import com.cashbackrewards.compras.domain.EstadoInvalidoException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> tratarValidacao(MethodArgumentNotValidException ex) {
        return corpo(HttpStatus.BAD_REQUEST, "Requisição inválida: " + ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> tratarNaoEncontrado(NoSuchElementException ex) {
        return corpo(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<Object> tratarEstadoInvalido(EstadoInvalidoException ex) {
        return corpo(HttpStatus.CONFLICT, ex.getMessage());
    }

    private ResponseEntity<Object> corpo(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "mensagem", mensagem,
                "timestamp", OffsetDateTime.now().toString()));
    }
}
