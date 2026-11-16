package co.edu.uq.sga.excepciones;

/** Se intentó un cambio de estado no permitido por el ciclo de vida. */
public class TransicionEstadoInvalidaException extends SgaException {
    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
