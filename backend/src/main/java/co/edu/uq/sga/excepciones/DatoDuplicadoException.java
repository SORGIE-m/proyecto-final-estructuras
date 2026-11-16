package co.edu.uq.sga.excepciones;

/** Ya existe un registro con la misma clave. */
public class DatoDuplicadoException extends SgaException {
    public DatoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
