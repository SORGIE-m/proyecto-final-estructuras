package co.edu.uq.sga.excepciones;

/** El rol del usuario no tiene permiso para la operación. */
public class AccesoDenegadoException extends SgaException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
