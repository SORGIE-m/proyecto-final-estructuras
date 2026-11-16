package co.edu.uq.sga.dominio;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

/** Historial de una preparación terminada (para el tiempo promedio de preparación). */
public record RegistroPreparacion(String apartamentoCodigo, LocalDateTime pendienteDesde,
                                  LocalDateTime inicio, LocalDateTime fin, String responsable) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public long minutosPreparacion() {
        return Duration.between(inicio, fin).toMinutes();
    }

    public long minutosEspera() {
        return pendienteDesde == null ? 0 : Math.max(0, Duration.between(pendienteDesde, inicio).toMinutes());
    }
}
