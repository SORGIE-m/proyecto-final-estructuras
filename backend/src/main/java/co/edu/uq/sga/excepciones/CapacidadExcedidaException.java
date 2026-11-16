package co.edu.uq.sga.excepciones;

/** El número de ocupantes supera la capacidad máxima del apartamento. */
public class CapacidadExcedidaException extends SgaException {
    public CapacidadExcedidaException(String mensaje) {
        super(mensaje);
    }
}
