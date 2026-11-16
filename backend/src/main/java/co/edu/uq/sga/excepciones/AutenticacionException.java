package co.edu.uq.sga.excepciones;

/** Credenciales inválidas o sesión inexistente. */
public class AutenticacionException extends SgaException {
    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}
