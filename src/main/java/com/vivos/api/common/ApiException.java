package com.vivos.api.common;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con el status HTTP que debe responderse. El mensaje se devuelve
 * como {@code detail} del {@code ProblemDetail}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
