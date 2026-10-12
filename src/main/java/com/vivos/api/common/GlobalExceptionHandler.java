package com.vivos.api.common;

import com.vivos.api.auth.DarmozAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(DarmozAuthException.class)
    ProblemDetail handleDarmozAuth(DarmozAuthException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }

    /**
     * Igual que laryon: cada campo invalido va como propiedad del ProblemDetail.
     * El {@code detail} es el primer mensaje, porque el front muestra {@code detail}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        return validationProblem(ex);
    }

    /** Tambien cubre query params agrupados en un record (p. ej. {@code LiveFilter}). */
    @ExceptionHandler(BindException.class)
    ProblemDetail handleBind(BindException ex) {
        return validationProblem(ex);
    }

    private static ProblemDetail validationProblem(BindException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(GlobalExceptionHandler::messageOf)
                .orElse("Datos invalidos");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        ex.getBindingResult().getFieldErrors().forEach(error ->
                problem.setProperty(error.getField(), messageOf(error)));
        return problem;
    }

    /** Un valor que no se pudo convertir (fecha, numero o enum invalido) es un parametro invalido. */
    private static String messageOf(FieldError error) {
        return error.isBindingFailure()
                ? "El parametro " + error.getField() + " no es valido"
                : error.getDefaultMessage();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleNotReadable(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es valido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El parametro " + ex.getName() + " no es valido");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad de datos", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operacion entra en conflicto con datos existentes");
    }
}
