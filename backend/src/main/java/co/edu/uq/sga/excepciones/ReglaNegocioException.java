package co.edu.uq.sga.excepciones;

/** Se violó una regla operativa del alojamiento. */
public class ReglaNegocioException extends SgaException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
