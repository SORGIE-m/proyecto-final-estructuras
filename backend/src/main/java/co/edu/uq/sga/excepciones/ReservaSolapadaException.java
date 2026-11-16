package co.edu.uq.sga.excepciones;

/** La estancia comparte al menos una noche con otra reserva activa del mismo apartamento. */
public class ReservaSolapadaException extends SgaException {
    public ReservaSolapadaException(String mensaje) {
        super(mensaje);
    }
}
