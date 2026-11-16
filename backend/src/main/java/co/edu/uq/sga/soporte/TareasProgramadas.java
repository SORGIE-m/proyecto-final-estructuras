package co.edu.uq.sga.soporte;

import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.servicio.ServicioReservas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Vencimiento automático de reservas Pendientes (cada minuto). */
@Component
@ConditionalOnProperty(name = "sga.tareas.habilitadas", havingValue = "true", matchIfMissing = true)
public class TareasProgramadas {

    private static final Logger LOG = LoggerFactory.getLogger(TareasProgramadas.class);

    private final ServicioReservas reservas;

    public TareasProgramadas(ServicioReservas reservas) {
        this.reservas = reservas;
    }

    @Scheduled(initialDelayString = "${sga.tareas.retraso-inicial-ms:15000}", fixedDelayString = "${sga.tareas.intervalo-ms:60000}")
    public void vencerPendientes() {
        try {
            ListaDoble<Reserva> vencidas = reservas.vencerPendientes();
            for (Reserva r : vencidas) {
                LOG.info("Reserva {} cancelada automáticamente por vencimiento del anticipo", r.getCodigo());
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudo ejecutar el vencimiento automático: {}", e.getMessage());
        }
    }
}
