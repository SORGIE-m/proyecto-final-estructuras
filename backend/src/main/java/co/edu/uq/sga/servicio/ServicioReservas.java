package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.Canal;
import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.negocio.Reembolso;
import co.edu.uq.sga.servicio.comandos.SolicitudReserva;
import co.edu.uq.sga.servicio.comandos.SolicitudReservaDirecta;

import java.time.LocalDate;
import java.time.LocalTime;

/** Ciclo de vida completo de las reservas por los tres canales. */
public interface ServicioReservas {

    /** Portal: queda Pendiente hasta pagar el 30 % (vence a las 24 h). */
    Reserva crearPortal(String documentoHuesped, SolicitudReserva solicitud);

    /** Directa (recepción): queda Confirmada. */
    Reserva crearDirecta(SolicitudReservaDirecta solicitud);

    /** Registro común usado por los canales (externo, grupos). */
    Reserva registrar(Huesped titular, String apartamento, LocalDate entrada, LocalDate salida, LocalTime horaLlegada,
                      ListaDoble<Ocupante> ocupantes, Canal canal, String observaciones, String referenciaExterna);

    Reserva obtener(String codigo);

    ListaDoble<Reserva> listar(EstadoReserva estado, Canal canal, String texto);

    ListaDoble<Reserva> llegadasDe(LocalDate fecha);

    ListaDoble<Reserva> salidasDe(LocalDate fecha);

    Reserva confirmar(String codigo);

    Reembolso previsualizarCancelacion(String codigo);

    Reserva cancelar(String codigo, String motivo, String usuario);

    Reserva checkIn(String codigo);

    Reserva checkOut(String codigo);

    Reserva marcarNoShow(String codigo);

    /** Cancela las pendientes cuyo plazo de 24 h ya venció; devuelve las canceladas. */
    ListaDoble<Reserva> vencerPendientes();

    ListaDoble<Reserva> pendientesPorVencer();
}
