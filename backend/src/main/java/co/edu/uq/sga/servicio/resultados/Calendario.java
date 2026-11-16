package co.edu.uq.sga.servicio.resultados;

import java.time.LocalDate;

/** Cuadrícula de ocupación: una fila por apartamento y una celda por noche del mes. */
public record Calendario(String mes, LocalDate[] noches, Fila[] filas) {

    public enum EstadoNoche { LIBRE, OCUPADA, PENDIENTE, BLOQUEADA }

    public record Celda(LocalDate fecha, EstadoNoche estado, String reserva, String huesped) {
    }

    public record Fila(String apartamento, String nombre, String bloque, String estadoApartamento,
                       int nochesOcupadas, Celda[] celdas) {
    }
}
