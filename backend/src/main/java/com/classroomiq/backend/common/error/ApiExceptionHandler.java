package com.classroomiq.backend.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Traduce excepciones a respuestas {@link ProblemDetail} (RFC 7807) con el código HTTP correcto.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "No tiene permisos para esta operación");
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail handleConflicto(ConflictoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegridad(DataIntegrityViolationException ex) {
        // Red de seguridad ante violaciones de restricciones de BD (ej. email único).
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operación viola una restricción de datos");
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleCuerpoIlegible(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no se pudo leer o está mal formado");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleArchivoMuyGrande(MaxUploadSizeExceededException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo o la solicitud superan el tamaño máximo permitido");
    }

    /**
     * El cliente cerró la conexión (típico en el stream SSE de {@code /api/lotes/{id}/eventos}:
     * el navegador cierra el EventSource al navegar, recargar o al terminar el lote). El socket ya
     * está roto ("Tubería rota") y la respuesta comprometida, así que no hay a quién responderle.
     * Devolvemos {@code void} (sin cuerpo) para evitar que el handler genérico intente serializar un
     * {@link ProblemDetail} sobre {@code text/event-stream} — lo que provocaba un ERROR + WARN falsos.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClienteDesconectado(AsyncRequestNotUsableException ex) {
        log.debug("Cliente desconectado durante respuesta async/SSE: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleInesperado(Exception ex) {
        // No filtramos detalles internos al cliente; los registramos para diagnóstico.
        log.error("Error inesperado", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado");
    }
}
