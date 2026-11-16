package co.edu.uq.sga.negocio;

import co.edu.uq.sga.dominio.TipoTemporada;

import java.time.LocalDate;

/** Indica la temporada vigente en una fecha (BAJA si ninguna temporada la cubre). */
public interface ProveedorTemporadas {
    TipoTemporada tipoPara(LocalDate fecha);
}
