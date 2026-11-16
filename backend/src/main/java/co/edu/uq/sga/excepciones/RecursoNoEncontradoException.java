package co.edu.uq.sga.excepciones;

/** El recurso solicitado (apartamento, huésped, reserva...) no existe. */
public class RecursoNoEncontradoException extends SgaException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
