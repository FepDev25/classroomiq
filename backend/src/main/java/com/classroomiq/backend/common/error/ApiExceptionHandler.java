package com.classroomiq.backend.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones a respuestas {@link ProblemDetail}. Mínimo para Fase 2 (auth, conflictos,
 * not-found, validación); se ampliará en el Hito 6.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail handleConflicto(ConflictoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail handleReglaNegocio(ReglaNegocioException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacion(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setDetail("La solicitud tiene campos inválidos");
        ex.getBindingResult().getFieldErrors().forEach(error ->
                problem.setProperty(error.getField(), error.getDefaultMessage()));
        return problem;
    }
}
