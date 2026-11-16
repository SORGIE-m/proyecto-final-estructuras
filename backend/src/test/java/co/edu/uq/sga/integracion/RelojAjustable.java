package co.edu.uq.sga.integracion;

import co.edu.uq.sga.soporte.Reloj;

import java.time.Duration;
import java.time.LocalDateTime;

/** Reloj controlable para pruebas: "hoy" es fijo y se puede adelantar. */
public class RelojAjustable implements Reloj {

    public static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 16, 9, 0);

    private LocalDateTime ahora = INICIO;

    @Override
    public synchronized LocalDateTime ahora() {
        return ahora;
    }

    public synchronized void reiniciar() {
        ahora = INICIO;
    }

    public synchronized void adelantar(Duration d) {
        ahora = ahora.plus(d);
    }
}
