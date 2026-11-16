package co.edu.uq.sga.negocio;

import co.edu.uq.sga.dominio.Reserva;

import java.time.LocalDate;

/** Estrategia de reembolso (abierta a extensión: otras políticas sin tocar el servicio). */
public interface PoliticaCancelacion {
    Reembolso calcular(Reserva reserva, LocalDate fechaCancelacion);
}
