package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.comparadores.ComparadoresReserva;
import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.ColaPrioridad;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.repositorio.RepositorioReservas;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Cola de prioridad de reservas Pendientes ordenadas por su hora de vencimiento
 * (24 h después de creadas). Debe usarse dentro de una operación de escritura.
 */
@Component
public class GestorVencimientos {

    private final RepositorioReservas reservas;

    public GestorVencimientos(RepositorioReservas reservas) {
        this.reservas = reservas;
    }

    private ColaPrioridad<Reserva> cola() {
        return reservas.colaVencimientos(() -> {
            ColaPrioridad<Reserva> c = new ColaPrioridad<>(ComparadoresReserva.porVencimiento());
            for (Reserva r : reservas.todas()) {
                if (r.getEstado() == EstadoReserva.PENDIENTE && r.getVenceEn() != null) {
                    c.insertar(r);
                }
            }
            return c;
        });
    }

    /** Idempotente: si la cola se construyó con la reserva ya incluida, no la duplica. */
    public void registrar(Reserva r) {
        ColaPrioridad<Reserva> c = cola();
        c.eliminarSi(x -> x.getCodigo().equals(r.getCodigo()));
        c.insertar(r);
    }

    public void retirar(Reserva r) {
        cola().eliminarSi(x -> x.getCodigo().equals(r.getCodigo()));
    }

    /** Extrae, en orden de vencimiento, todas las pendientes cuyo plazo ya pasó. */
    public ListaDoble<Reserva> extraerVencidas(LocalDateTime ahora) {
        ListaDoble<Reserva> vencidas = new ListaDoble<>();
        ColaPrioridad<Reserva> c = cola();
        while (!c.estaVacia() && !c.verPrimero().getVenceEn().isAfter(ahora)) {
            Reserva r = c.extraer();
            if (r.getEstado() == EstadoReserva.PENDIENTE) {
                vencidas.agregar(r);
            }
        }
        return vencidas;
    }

    public ListaDoble<Reserva> pendientesPorVencer() {
        return ListaDoble.desde(cola());
    }
}
