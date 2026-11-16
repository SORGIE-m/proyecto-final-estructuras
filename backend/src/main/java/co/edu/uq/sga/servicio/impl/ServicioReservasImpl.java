package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.comparadores.ComparadoresReserva;
import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Canal;
import co.edu.uq.sga.dominio.EstadoApartamento;
import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Estancia;
import co.edu.uq.sga.dominio.Folio;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.TipoMovimiento;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.CapacidadExcedidaException;
import co.edu.uq.sga.excepciones.FechasInvalidasException;
import co.edu.uq.sga.excepciones.RecursoNoEncontradoException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.excepciones.ReservaSolapadaException;
import co.edu.uq.sga.excepciones.TransicionEstadoInvalidaException;
import co.edu.uq.sga.negocio.DetectorSolapamientos;
import co.edu.uq.sga.negocio.Liquidacion;
import co.edu.uq.sga.negocio.LiquidadorEstancia;
import co.edu.uq.sga.negocio.PoliticaCancelacion;
import co.edu.uq.sga.negocio.Reembolso;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.repositorio.RepositorioHuespedes;
import co.edu.uq.sga.repositorio.RepositorioReservas;
import co.edu.uq.sga.servicio.ServicioHuespedes;
import co.edu.uq.sga.servicio.ServicioPreparacion;
import co.edu.uq.sga.servicio.ServicioReservas;
import co.edu.uq.sga.servicio.comandos.DatosOcupante;
import co.edu.uq.sga.servicio.comandos.SolicitudReserva;
import co.edu.uq.sga.servicio.comandos.SolicitudReservaDirecta;
import co.edu.uq.sga.soporte.Reloj;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class ServicioReservasImpl implements ServicioReservas {

    public static final int EDAD_MINIMA_TITULAR = 18;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RepositorioReservas reservas;
    private final RepositorioApartamentos apartamentos;
    private final RepositorioHuespedes huespedes;
    private final ServicioHuespedes servicioHuespedes;
    private final ServicioPreparacion preparacion;
    private final DetectorSolapamientos detector;
    private final LiquidadorEstancia liquidador;
    private final PoliticaCancelacion politica;
    private final GestorVencimientos vencimientos;
    private final UnidadDeTrabajo uow;
    private final Reloj reloj;

    public ServicioReservasImpl(RepositorioReservas reservas, RepositorioApartamentos apartamentos,
                                RepositorioHuespedes huespedes, ServicioHuespedes servicioHuespedes,
                                ServicioPreparacion preparacion, DetectorSolapamientos detector,
                                LiquidadorEstancia liquidador, PoliticaCancelacion politica,
                                GestorVencimientos vencimientos, UnidadDeTrabajo uow, Reloj reloj) {
        this.reservas = reservas;
        this.apartamentos = apartamentos;
        this.huespedes = huespedes;
        this.servicioHuespedes = servicioHuespedes;
        this.preparacion = preparacion;
        this.detector = detector;
        this.liquidador = liquidador;
        this.politica = politica;
        this.vencimientos = vencimientos;
        this.uow = uow;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ creación

    @Override
    public Reserva crearPortal(String documentoHuesped, SolicitudReserva s) {
        return uow.escribir(() -> {
            Huesped titular = huespedes.buscar(documentoHuesped);
            if (titular == null) {
                throw new RecursoNoEncontradoException("No existe el huésped " + documentoHuesped);
            }
            return registrarInterno(titular, s.apartamentoCodigo(), s.entrada(), s.salida(), s.horaLlegada(),
                    ocupantes(s.ocupantes()), Canal.PORTAL, s.observaciones(), null);
        });
    }

    @Override
    public Reserva crearDirecta(SolicitudReservaDirecta s) {
        return uow.escribir(() -> {
            ListaDoble<Ocupante> ocupantes = ocupantes(s.ocupantes());
            validarPrevio(s.apartamentoCodigo(), s.entrada(), s.salida(), ocupantes);
            Huesped titular = servicioHuespedes.obtenerOCrear(s.titular());
            return registrarInterno(titular, s.apartamentoCodigo(), s.entrada(), s.salida(), s.horaLlegada(),
                    ocupantes, Canal.DIRECTA, s.observaciones(), null);
        });
    }

    @Override
    public Reserva registrar(Huesped titular, String apartamento, LocalDate entrada, LocalDate salida,
                             LocalTime horaLlegada, ListaDoble<Ocupante> ocupantes, Canal canal,
                             String observaciones, String referenciaExterna) {
        return uow.escribir(() -> registrarInterno(titular, apartamento, entrada, salida, horaLlegada, ocupantes,
                canal, observaciones, referenciaExterna));
    }

    private static ListaDoble<Ocupante> ocupantes(DatosOcupante[] datos) {
        ListaDoble<Ocupante> lista = new ListaDoble<>();
        if (datos != null) {
            for (DatosOcupante d : datos) {
                lista.agregar(d.aDominio());
            }
        }
        return lista;
    }

    /**
     * Valida todas las reglas sin modificar nada (así una solicitud rechazada no deja rastros).
     * @return el apartamento validado.
     */
    private Apartamento validarPrevio(String codigoApto, LocalDate entrada, LocalDate salida,
                                      ListaDoble<Ocupante> ocupantes) {
        Estancia estancia = new Estancia(entrada, salida);
        if (entrada.isBefore(reloj.hoy())) {
            throw new FechasInvalidasException("La fecha de entrada no puede estar en el pasado");
        }
        Apartamento a = apartamentos.buscar(codigoApto);
        if (a == null) {
            throw new RecursoNoEncontradoException("No existe el apartamento " + codigoApto);
        }
        if (!a.admiteReservas()) {
            throw new ReglaNegocioException("El apartamento " + a.getCodigo() + " está fuera de servicio: "
                    + a.getMotivoFueraServicio());
        }
        if (ocupantes.estaVacia()) {
            throw new ReglaNegocioException("Debe registrar al menos un ocupante");
        }
        if (ocupantes.tamanio() > a.getCapacidad()) {
            throw new CapacidadExcedidaException("El apartamento " + a.getCodigo() + " admite máximo "
                    + a.getCapacidad() + " personas y la reserva tiene " + ocupantes.tamanio());
        }
        for (Ocupante o : ocupantes) {
            if (o.fechaNacimiento().isAfter(entrada)) {
                throw new FechasInvalidasException("La fecha de nacimiento de " + o.nombre() + " no es válida");
            }
        }
        boolean algunoPaga = false;
        for (Ocupante o : ocupantes) {
            algunoPaga |= o.pagaTarifa(entrada);
        }
        if (!algunoPaga) {
            throw new ReglaNegocioException("Los menores de 6 años deben estar acompañados por al menos un adulto");
        }
        Reserva conflicto = detector.primerConflicto(a.getReservas(), estancia, null);
        if (conflicto != null) {
            throw new ReservaSolapadaException("El apartamento " + a.getCodigo() + " ya está reservado del "
                    + conflicto.getEstancia().entrada().format(FECHA) + " al "
                    + conflicto.getEstancia().salida().format(FECHA) + " (reserva " + conflicto.getCodigo() + ")");
        }
        return a;
    }

    private Reserva registrarInterno(Huesped titular, String codigoApto, LocalDate entrada, LocalDate salida,
                                     LocalTime horaLlegada, ListaDoble<Ocupante> ocupantes, Canal canal,
                                     String observaciones, String referenciaExterna) {
        Apartamento a = validarPrevio(codigoApto, entrada, salida, ocupantes);
        if (titular.edadEn(reloj.hoy()) < EDAD_MINIMA_TITULAR) {
            throw new ReglaNegocioException("El titular de la reserva debe ser mayor de edad");
        }
        Estancia estancia = new Estancia(entrada, salida);
        Liquidacion liq = liquidador.liquidar(a, estancia, ocupantes);
        EstadoReserva inicial = canal == Canal.PORTAL ? EstadoReserva.PENDIENTE : EstadoReserva.CONFIRMADA;
        Reserva r = new Reserva(reservas.siguienteCodigo(), a.getCodigo(), titular.getDocumento(), titular.getNombre(),
                ocupantes, estancia, canal, reloj.ahora(), horaLlegada, liq.desglose(), liq.total(), inicial);
        r.setObservaciones(observaciones == null ? "" : observaciones.trim());
        r.setReferenciaExterna(referenciaExterna);
        r.getFolio().registrar(TipoMovimiento.CARGO, "Alojamiento " + liq.noches() + " noche(s) · " + a.getNombre(),
                liq.total(), reloj.ahora(), "sistema", false);
        a.getReservas().insertar(r);
        reservas.guardar(r);
        if (inicial == EstadoReserva.PENDIENTE) {
            vencimientos.registrar(r);
        }
        return r;
    }

    // ------------------------------------------------------------------ consultas

    @Override
    public Reserva obtener(String codigo) {
        return uow.leer(() -> requerir(codigo));
    }

    private Reserva requerir(String codigo) {
        Reserva r = reservas.buscar(codigo);
        if (r == null) {
            throw new RecursoNoEncontradoException("No existe la reserva " + codigo);
        }
        return r;
    }

    @Override
    public ListaDoble<Reserva> listar(EstadoReserva estado, Canal canal, String texto) {
        return uow.leer(() -> {
            String t = texto == null ? "" : texto.trim().toLowerCase();
            ListaDoble<Reserva> r = new ListaDoble<>();
            for (Reserva x : reservas.todas()) {
                if ((estado == null || x.getEstado() == estado) && (canal == null || x.getCanal() == canal)
                        && (t.isEmpty() || x.getCodigo().toLowerCase().contains(t)
                        || x.getTitularNombre().toLowerCase().contains(t)
                        || x.getTitularDocumento().contains(t)
                        || x.getApartamentoCodigo().toLowerCase().contains(t)
                        || (x.getReferenciaExterna() != null && x.getReferenciaExterna().toLowerCase().contains(t)))) {
                    r.agregar(x);
                }
            }
            r.ordenar(ComparadoresReserva.porCreacionDescendente());
            return r;
        });
    }

    @Override
    public ListaDoble<Reserva> llegadasDe(LocalDate fecha) {
        return uow.leer(() -> reservas.todas().flujo()
                .filter(r -> r.getEstancia().entrada().equals(fecha)
                        && (r.esActiva() || r.getEstado() == EstadoReserva.FINALIZADA))
                .collect(ListaDoble::new, ListaDoble::agregar, ListaDoble::agregarTodos));
    }

    @Override
    public ListaDoble<Reserva> salidasDe(LocalDate fecha) {
        return uow.leer(() -> reservas.todas().flujo()
                .filter(r -> r.getEstancia().salida().equals(fecha)
                        && (r.getEstado() == EstadoReserva.EN_CURSO || r.getEstado() == EstadoReserva.FINALIZADA))
                .collect(ListaDoble::new, ListaDoble::agregar, ListaDoble::agregarTodos));
    }

    @Override
    public ListaDoble<Reserva> pendientesPorVencer() {
        return uow.exclusivo(vencimientos::pendientesPorVencer);
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    public Reserva confirmar(String codigo) {
        return uow.escribir(() -> {
            Reserva r = requerir(codigo);
            if (r.getEstado() == EstadoReserva.PENDIENTE && r.anticipoPendiente() > 0) {
                throw new ReglaNegocioException("Falta pagar el anticipo del 30 %: $" + r.anticipoPendiente());
            }
            r.transitar(EstadoReserva.CONFIRMADA);
            vencimientos.retirar(r);
            return r;
        });
    }

    @Override
    public Reembolso previsualizarCancelacion(String codigo) {
        return uow.leer(() -> {
            Reserva r = requerir(codigo);
            if (!r.getEstado().puedePasarA(EstadoReserva.CANCELADA)) {
                throw new TransicionEstadoInvalidaException("La reserva " + r.getCodigo() + " ("
                        + r.getEstado().etiqueta() + ") ya no se puede cancelar");
            }
            return politica.calcular(r, reloj.hoy());
        });
    }

    @Override
    public Reserva cancelar(String codigo, String motivo, String usuario) {
        return uow.escribir(() -> cancelarInterno(requerir(codigo), motivo, usuario));
    }

    private Reserva cancelarInterno(Reserva r, String motivo, String usuario) {
        if (!r.getEstado().puedePasarA(EstadoReserva.CANCELADA)) {
            throw new TransicionEstadoInvalidaException("La reserva " + r.getCodigo() + " ("
                    + r.getEstado().etiqueta() + ") ya no se puede cancelar");
        }
        Reembolso reembolso = politica.calcular(r, reloj.hoy());
        Folio folio = r.getFolio();
        if (reembolso.reembolso() > 0) {
            folio.registrar(TipoMovimiento.REVERSO_PAGO, "Devolución por cancelación (" + reembolso.porcentaje() + " %)",
                    reembolso.reembolso(), reloj.ahora(), usuario, false);
        }
        long cargosAnular = folio.totalCargos() - reembolso.retenido();
        if (cargosAnular > 0) {
            folio.registrar(TipoMovimiento.REVERSO_CARGO, "Anulación de cargos por cancelación", cargosAnular,
                    reloj.ahora(), usuario, false);
        }
        r.transitar(EstadoReserva.CANCELADA);
        r.setMotivoCierre(motivo == null || motivo.isBlank() ? "Cancelada por " + usuario : motivo.trim());
        vencimientos.retirar(r);
        return r;
    }

    @Override
    public Reserva checkIn(String codigo) {
        return uow.escribir(() -> {
            Reserva r = requerir(codigo);
            if (r.getEstado() == EstadoReserva.PENDIENTE) {
                throw new ReglaNegocioException("La reserva aún está pendiente: falta confirmar el anticipo");
            }
            if (r.getEstado() != EstadoReserva.CONFIRMADA) {
                throw new TransicionEstadoInvalidaException("Solo se hace check-in de reservas confirmadas (estado actual: "
                        + r.getEstado().etiqueta() + ")");
            }
            LocalDate hoy = reloj.hoy();
            if (hoy.isBefore(r.getEstancia().entrada())) {
                throw new FechasInvalidasException("El check-in es a partir del " + r.getEstancia().entrada().format(FECHA));
            }
            if (!hoy.isBefore(r.getEstancia().salida())) {
                throw new FechasInvalidasException("La estancia ya terminó; registre un no-show");
            }
            Apartamento a = apartamentos.buscar(r.getApartamentoCodigo());
            if (a.getEstado() != EstadoApartamento.PREPARADO) {
                throw new ReglaNegocioException("El apartamento " + a.getCodigo() + " no está preparado (estado: "
                        + a.getEstado().etiqueta() + ")");
            }
            a.cambiarEstado(EstadoApartamento.OCUPADO, null, reloj.ahora());
            r.transitar(EstadoReserva.EN_CURSO);
            r.setCheckInEn(reloj.ahora());
            return r;
        });
    }

    @Override
    public Reserva checkOut(String codigo) {
        return uow.escribir(() -> {
            Reserva r = requerir(codigo);
            if (r.getEstado() != EstadoReserva.EN_CURSO) {
                throw new TransicionEstadoInvalidaException("Solo se hace check-out de estancias en curso (estado actual: "
                        + r.getEstado().etiqueta() + ")");
            }
            long saldo = r.getFolio().saldo();
            if (saldo > 0) {
                throw new ReglaNegocioException("El folio tiene un saldo pendiente de $" + saldo + "; regístrelo antes del check-out");
            }
            r.transitar(EstadoReserva.FINALIZADA);
            r.setCheckOutEn(reloj.ahora());
            Apartamento a = apartamentos.buscar(r.getApartamentoCodigo());
            a.cambiarEstado(EstadoApartamento.PENDIENTE_PREPARACION, null, reloj.ahora());
            preparacion.encolar(a.getCodigo());
            Huesped h = huespedes.buscar(r.getTitularDocumento());
            if (h != null) {
                h.registrarEstanciaFinalizada(r.getCodigo());
            }
            return r;
        });
    }

    @Override
    public Reserva marcarNoShow(String codigo) {
        return uow.escribir(() -> {
            Reserva r = requerir(codigo);
            if (r.getEstado() != EstadoReserva.CONFIRMADA) {
                throw new TransicionEstadoInvalidaException("Solo una reserva confirmada puede marcarse como no-show");
            }
            if (!reloj.hoy().isAfter(r.getEstancia().entrada())) {
                throw new FechasInvalidasException("El no-show se registra después del día de entrada ("
                        + r.getEstancia().entrada().format(FECHA) + ")");
            }
            Folio folio = r.getFolio();
            long anular = folio.totalCargos() - folio.totalPagado();
            if (anular > 0) {
                folio.registrar(TipoMovimiento.REVERSO_CARGO, "Anulación de cargos (no-show; se retiene lo pagado)",
                        anular, reloj.ahora(), "sistema", false);
            }
            r.transitar(EstadoReserva.NO_SHOW);
            r.setMotivoCierre("El huésped no se presentó");
            return r;
        });
    }

    @Override
    public ListaDoble<Reserva> vencerPendientes() {
        return uow.escribir(() -> {
            ListaDoble<Reserva> vencidas = vencimientos.extraerVencidas(reloj.ahora());
            for (Reserva r : vencidas) {
                cancelarInterno(r, "Vencida: no se pagó el anticipo en " + Reserva.HORAS_VENCIMIENTO + " horas", "sistema");
            }
            return vencidas;
        });
    }
}
