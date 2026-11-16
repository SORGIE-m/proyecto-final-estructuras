package co.edu.uq.sga.soporte;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Abstracción del tiempo (inversión de dependencias): permite fijar "hoy" en las pruebas. */
public interface Reloj {
    LocalDateTime ahora();

    default LocalDate hoy() {
        return ahora().toLocalDate();
    }
}
