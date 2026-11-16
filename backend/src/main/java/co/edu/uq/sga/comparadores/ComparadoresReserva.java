package co.edu.uq.sga.comparadores;

import co.edu.uq.sga.dominio.Reserva;

import java.util.Comparator;

/** Órdenes alternativos de reservas. */
public final class ComparadoresReserva {

    private ComparadoresReserva() {
    }

    /** Las pendientes que vencen antes salen primero de la cola de vencimientos. */
    public static Comparator<Reserva> porVencimiento() {
        return Comparator.comparing(Reserva::getVenceEn, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Reserva::getCodigo);
    }

    /** Más recientes primero. */
    public static Comparator<Reserva> porCreacionDescendente() {
        return Comparator.comparing(Reserva::getCreadaEn).reversed().thenComparing(Reserva::getCodigo);
    }
}
