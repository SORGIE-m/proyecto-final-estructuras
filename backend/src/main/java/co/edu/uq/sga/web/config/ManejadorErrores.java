package co.edu.uq.sga.web.config;

import co.edu.uq.sga.excepciones.AccesoDenegadoException;
import co.edu.uq.sga.excepciones.AutenticacionException;
import co.edu.uq.sga.excepciones.CapacidadExcedidaException;
import co.edu.uq.sga.excepciones.DatoDuplicadoException;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import co.edu.uq.sga.excepciones.FechasInvalidasException;
import co.edu.uq.sga.excepciones.RecursoNoEncontradoException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.excepciones.ReservaSolapadaException;
import co.edu.uq.sga.excepciones.SgaException;
import co.edu.uq.sga.excepciones.TransicionEstadoInvalidaException;
import co.edu.uq.sga.soporte.Reloj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

/** Traduce las excepciones a respuestas JSON uniformes con mensajes claros en español. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    public record RespuestaError(int estado, String error, String mensaje, String[] detalles, LocalDateTime fecha) {
    }

    private final Reloj reloj;

    public ManejadorErrores(Reloj reloj) {
        this.reloj = reloj;
    }

    private ResponseEntity<RespuestaError> respuesta(HttpStatus estado, String error, String mensaje,
                                                     String... detalles) {
        return ResponseEntity.status(estado)
                .body(new RespuestaError(estado.value(), error, mensaje, detalles, reloj.ahora()));
    }

    @ExceptionHandler(ReservaSolapadaException.class)
    ResponseEntity<RespuestaError> solapada(ReservaSolapadaException e) {
        return respuesta(HttpStatus.CONFLICT, "RESERVA_SOLAPADA", e.getMessage());
    }

    @ExceptionHandler(CapacidadExcedidaException.class)
    ResponseEntity<RespuestaError> capacidad(CapacidadExcedidaException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "CAPACIDAD_EXCEDIDA", e.getMessage());
    }

    @ExceptionHandler(FechasInvalidasException.class)
    ResponseEntity<RespuestaError> fechas(FechasInvalidasException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "FECHAS_INVALIDAS", e.getMessage());
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    ResponseEntity<RespuestaError> transicion(TransicionEstadoInvalidaException e) {
        return respuesta(HttpStatus.CONFLICT, "TRANSICION_INVALIDA", e.getMessage());
    }

    @ExceptionHandler(EstructuraVaciaException.class)
    ResponseEntity<RespuestaError> vacia(EstructuraVaciaException e) {
        return respuesta(HttpStatus.CONFLICT, "ESTRUCTURA_VACIA", e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage());
    }

    @ExceptionHandler(DatoDuplicadoException.class)
    ResponseEntity<RespuestaError> duplicado(DatoDuplicadoException e) {
        return respuesta(HttpStatus.CONFLICT, "DUPLICADO", e.getMessage());
    }

    @ExceptionHandler(AutenticacionException.class)
    ResponseEntity<RespuestaError> autenticacion(AutenticacionException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", e.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    ResponseEntity<RespuestaError> acceso(AccesoDenegadoException e) {
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<RespuestaError> regla(ReglaNegocioException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "REGLA_NEGOCIO", e.getMessage());
    }

    @ExceptionHandler(SgaException.class)
    ResponseEntity<RespuestaError> sga(SgaException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "ERROR_OPERACION", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException e) {
        String[] detalles = e.getBindingResult().getAllErrors().stream()
                .map(err -> err instanceof FieldError f ? f.getField() + ": " + f.getDefaultMessage() : err.getDefaultMessage())
                .toArray(String[]::new);
        String mensaje = detalles.length > 0 ? detalles[0].substring(detalles[0].indexOf(':') + 1).trim()
                : "Datos inválidos";
        return respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", mensaje, detalles);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<RespuestaError> validacionParametros(HandlerMethodValidationException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", "Parámetros inválidos");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<RespuestaError> ilegible(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "FORMATO_INVALIDO",
                "El cuerpo de la solicitud no tiene un formato válido (revise fechas AAAA-MM-DD y números)");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    ResponseEntity<RespuestaError> faltante(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "PARAMETRO_FALTANTE", "Falta un parámetro obligatorio: " + e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<RespuestaError> tipo(MethodArgumentTypeMismatchException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "El parámetro '" + e.getName() + "' no es válido");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<RespuestaError> tamano(MaxUploadSizeExceededException e) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "ARCHIVO_GRANDE", "El archivo supera el tamaño permitido");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<RespuestaError> metodo(HttpRequestMethodNotSupportedException e) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO", e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<RespuestaError> sinRecurso(NoResourceFoundException e) {
        return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Recurso no encontrado");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<RespuestaError> inesperado(Exception e) {
        LOG.error("Error no controlado", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error inesperado. Intente de nuevo.");
    }
}
