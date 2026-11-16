package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.Movimiento;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.servicio.comandos.DatosMovimiento;

/** Cargos, pagos y deshacer (con movimiento contrario) sobre el folio de una reserva. */
public interface ServicioFolio {
    Movimiento registrarCargo(String codigoReserva, DatosMovimiento datos, String usuario);

    Movimiento registrarPago(String codigoReserva, DatosMovimiento datos, String usuario);

    /** Paga exactamente el anticipo pendiente (30 %) y confirma la reserva. */
    Movimiento pagarAnticipo(String codigoReserva, String usuario);

    /** Registra el contrario de la última operación apilada. */
    Movimiento deshacer(String codigoReserva, String usuario);

    Reserva reserva(String codigoReserva);
}
