package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Movimiento;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.TipoMovimiento;
import co.edu.uq.sga.excepciones.RecursoNoEncontradoException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioReservas;
import co.edu.uq.sga.servicio.ServicioFolio;
import co.edu.uq.sga.servicio.comandos.DatosMovimiento;
import co.edu.uq.sga.soporte.Reloj;
import org.springframework.stereotype.Service;

@Service
public class ServicioFolioImpl implements ServicioFolio {

    private final RepositorioReservas reservas;
    private final GestorVencimientos vencimientos;
    private final UnidadDeTrabajo uow;
    private final Reloj reloj;

    public ServicioFolioImpl(RepositorioReservas reservas, GestorVencimientos vencimientos, UnidadDeTrabajo uow,
                             Reloj reloj) {
        this.reservas = reservas;
        this.vencimientos = vencimientos;
        this.uow = uow;
        this.reloj = reloj;
    }

    @Override
    public Movimiento registrarCargo(String codigo, DatosMovimiento d, String usuario) {
        return uow.escribir(() -> {
            Reserva r = requerir(codigo);
            if (!r.esActiva()) {
                throw new ReglaNegocioException("Solo se registran cargos en reservas activas (estado: "
                        + r.getEstado().etiqueta() + ")");
            }
            return r.getFolio().registrar(TipoMovimiento.CARGO, d.concepto(), d.monto(), reloj.ahora(), usuario, true);
        });
    }

    @Override
    public Movimiento registrarPago(String codigo, DatosMovimiento d, String usuario) {
        return uow.escribir(() -> pagar(requerir(codigo), d.concepto(), d.monto(), usuario));
    }

    @Override
    public Movimiento pagarAnticipo(String codigo, String usuario) {
        return uow.escribir(() -> {
            Reserva r = requerir(codigo);
            if (r.getEstado() != EstadoReserva.PENDIENTE) {
                throw new ReglaNegocioException("La reserva no está pendiente de anticipo");
            }
            long monto = r.anticipoPendiente();
            if (monto <= 0) {
                throw new ReglaNegocioException("El anticipo ya fue pagado");
            }
            return pagar(r, "Anticipo " + Reserva.PORCENTAJE_ANTICIPO + " %", monto, usuario);
        });
    }

    private Movimiento pagar(Reserva r, String concepto, long monto, String usuario) {
        if (r.getEstado() == EstadoReserva.CANCELADA || r.getEstado() == EstadoReserva.NO_SHOW) {
            throw new ReglaNegocioException("No se reciben pagos en una reserva " + r.getEstado().etiqueta().toLowerCase());
        }
        long saldo = r.getFolio().saldo();
        if (monto > saldo) {
            throw new ReglaNegocioException("El pago ($" + monto + ") supera el saldo pendiente ($" + Math.max(0, saldo) + ")");
        }
        Movimiento m = r.getFolio().registrar(TipoMovimiento.PAGO, concepto, monto, reloj.ahora(), usuario, true);
        if (r.getEstado() == EstadoReserva.PENDIENTE && r.anticipoPendiente() == 0) {
            r.transitar(EstadoReserva.CONFIRMADA);
            vencimientos.retirar(r);
        }
        return m;
    }

    @Override
    public Movimiento deshacer(String codigo, String usuario) {
        return uow.escribir(() -> {
            Reserva r = requerir(codigo);
            if (!r.esActiva()) {
                throw new ReglaNegocioException("Solo se deshacen operaciones de reservas activas");
            }
            return r.getFolio().deshacerUltima(reloj.ahora(), usuario);
        });
    }

    @Override
    public Reserva reserva(String codigo) {
        return uow.leer(() -> requerir(codigo));
    }

    private Reserva requerir(String codigo) {
        Reserva r = reservas.buscar(codigo);
        if (r == null) {
            throw new RecursoNoEncontradoException("No existe la reserva " + codigo);
        }
        return r;
    }
}
