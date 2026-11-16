package co.edu.uq.sga.excepciones;

/** Se lanza al consultar o extraer elementos de una estructura vacía (p. ej. desencolar una cola vacía). */
public class EstructuraVaciaException extends SgaException {
    public EstructuraVaciaException(String estructura) {
        super("La estructura " + estructura + " está vacía");
    }
}
