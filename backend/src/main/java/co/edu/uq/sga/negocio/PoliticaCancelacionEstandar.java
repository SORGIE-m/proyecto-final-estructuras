package co.edu.uq.sga.negocio;

import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Reserva;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Política del alojamiento:
 * - Reserva aún Pendiente: se devuelve el 100 % de lo abonado.
 * - 7 o más días antes de la entrada: 100 %.
 * - Entre 2 y 6 días: 50 %.
 * - Menos de 2 días: no hay devolución.
 */
@Component
public class PoliticaCancelacionEstandar implements PoliticaCancelacion {

    @Override
    public Reembolso calcular(Reserva reserva, LocalDate fecha) {
        long pagado = Math.max(0, reserva.getFolio().totalPagado());
        int porcentaje;
        String regla;
        if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
            porcentaje = 100;
            regla = "Reserva sin confirmar: se devuelve todo lo abonado";
        } else {
            long dias = ChronoUnit.DAYS.between(fecha, reserva.getEstancia().entrada());
            if (dias >= 7) {
                porcentaje = 100;
                regla = "Cancelación con 7 o más días de anticipación";
            } else if (dias >= 2) {
                porcentaje = 50;
                regla = "Cancelación entre 2 y 6 días antes de la entrada";
            } else {
                porcentaje = 0;
                regla = "Cancelación con menos de 2 días de anticipación";
            }
        }
        long reembolso = pagado * porcentaje / 100;
        return new Reembolso(pagado, reembolso, pagado - reembolso, porcentaje, regla);
    }
}
