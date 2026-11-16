package co.edu.uq.sga.dominio;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Elemento de la cola de preparación. Prioridad 1: el apartamento tiene una entrada programada
 * para hoy (se ordena por hora estimada de llegada). Prioridad 2: no tiene entrada hoy.
 */
public record ItemPreparacion(String apartamentoCodigo, String apartamentoNombre, String bloque, int piso,
                              LocalDateTime pendienteDesde, boolean entradaHoy, LocalTime horaLlegada,
                              String reservaEntrante, String huespedEntrante) {

    public int prioridad() {
        return entradaHoy ? 1 : 2;
    }
}
