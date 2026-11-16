package co.edu.uq.sga.excepciones;

/** Las fechas de la estancia o de una temporada no son válidas. */
public class FechasInvalidasException extends SgaException {
    public FechasInvalidasException(String mensaje) {
        super(mensaje);
    }
}
