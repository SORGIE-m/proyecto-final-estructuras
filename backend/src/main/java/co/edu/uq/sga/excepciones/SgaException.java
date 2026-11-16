package co.edu.uq.sga.excepciones;

/** Raíz de las excepciones propias del sistema. Son no verificadas para no contaminar las firmas. */
public class SgaException extends RuntimeException {
    public SgaException(String mensaje) {
        super(mensaje);
    }
}
