package com.vivos.api.auth;

import org.springframework.http.HttpStatus;

/**
 * Error al llamar a darmoz-auth. Se responde con el mismo status que devolvio darmoz-auth
 * (502 si no se pudo contactar) y con su {@code message} como {@code detail}.
 */
public class DarmozAuthException extends RuntimeException {

    private final HttpStatus status;

    public DarmozAuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public DarmozAuthException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
