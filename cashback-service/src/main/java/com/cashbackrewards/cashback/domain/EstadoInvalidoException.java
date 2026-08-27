package com.cashbackrewards.cashback.domain;

/**
 * Sinaliza uma tentativa de transição de estado inválida sobre uma Compra
 * (ex.: cancelar uma compra já totalmente devolvida, ou devolução cuja soma
 * de percentuais excederia 100%). Mapeada para HTTP 409 pelo
 * GlobalExceptionHandler.
 */
public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(String message) {
        super(message);
    }
}
