package co.edu.uq.sga.servicio;

import co.edu.uq.sga.servicio.resultados.Calendario;
import co.edu.uq.sga.servicio.resultados.Estadisticas;
import co.edu.uq.sga.servicio.resultados.Tablero;

import java.time.LocalDate;
import java.time.YearMonth;

/** Calendario de ocupación, estadísticas y tablero de inicio. */
public interface ServicioReportes {
    Calendario calendario(YearMonth mes);

    Estadisticas estadisticas(LocalDate desde, LocalDate hasta);

    Tablero tablero();
}
