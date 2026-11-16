package co.edu.uq.sga.escenario;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Canal;
import co.edu.uq.sga.dominio.Empleado;
import co.edu.uq.sga.dominio.EstadoApartamento;
import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Estancia;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.dominio.ProcesamientoExterno;
import co.edu.uq.sga.dominio.RegistroPreparacion;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.ResultadoSolicitud;
import co.edu.uq.sga.dominio.Rol;
import co.edu.uq.sga.dominio.SolicitudExterna;
import co.edu.uq.sga.dominio.Tarifas;
import co.edu.uq.sga.dominio.Temporada;
import co.edu.uq.sga.dominio.TipoMovimiento;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.excepciones.ReservaSolapadaException;
import co.edu.uq.sga.excepciones.SgaException;
import co.edu.uq.sga.negocio.CodificadorClaves;
import co.edu.uq.sga.negocio.DetectorSolapamientos;
import co.edu.uq.sga.negocio.Liquidacion;
import co.edu.uq.sga.negocio.LiquidadorEstancia;
import co.edu.uq.sga.persistencia.AlmacenEstado;
import co.edu.uq.sga.persistencia.EstadoSistema;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.repositorio.RepositorioCanalExterno;
import co.edu.uq.sga.repositorio.RepositorioEmpleados;
import co.edu.uq.sga.repositorio.RepositorioHuespedes;
import co.edu.uq.sga.repositorio.RepositorioMapa;
import co.edu.uq.sga.repositorio.RepositorioPreparacion;
import co.edu.uq.sga.repositorio.RepositorioReservas;
import co.edu.uq.sga.repositorio.RepositorioTemporadas;
import co.edu.uq.sga.repositorio.RepositorioTurnos;
import co.edu.uq.sga.servicio.ServicioEscenario;
import co.edu.uq.sga.soporte.Reloj;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;

/**
 * Construye un estado completamente nuevo a partir del archivo plano. Todo ocurre en una sola
 * escritura: si el archivo tiene un error, se conservan los datos anteriores.
 */
@Service
public class CargadorEscenario implements ServicioEscenario {

    public static final String ESCENARIO_PREDETERMINADO = "escenarios/escenario-inicial.json";

    private final ObjectMapper json;
    private final AlmacenEstado almacen;
    private final UnidadDeTrabajo uow;
    private final RepositorioApartamentos apartamentos;
    private final RepositorioHuespedes huespedes;
    private final RepositorioEmpleados empleados;
    private final RepositorioReservas reservas;
    private final RepositorioTemporadas temporadas;
    private final RepositorioMapa mapa;
    private final RepositorioCanalExterno canalExterno;
    private final RepositorioPreparacion preparacion;
    private final RepositorioTurnos turnos;
    private final LiquidadorEstancia liquidador;
    private final DetectorSolapamientos detector;
    private final CodificadorClaves codificador;
    private final Reloj reloj;

    public CargadorEscenario(ObjectMapper json, AlmacenEstado almacen, UnidadDeTrabajo uow,
                             RepositorioApartamentos apartamentos, RepositorioHuespedes huespedes,
                             RepositorioEmpleados empleados, RepositorioReservas reservas,
                             RepositorioTemporadas temporadas, RepositorioMapa mapa,
                             RepositorioCanalExterno canalExterno, RepositorioPreparacion preparacion,
                             RepositorioTurnos turnos, LiquidadorEstancia liquidador, DetectorSolapamientos detector,
                             CodificadorClaves codificador, Reloj reloj) {
        this.json = json.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.almacen = almacen;
        this.uow = uow;
        this.apartamentos = apartamentos;
        this.huespedes = huespedes;
        this.empleados = empleados;
        this.reservas = reservas;
        this.temporadas = temporadas;
        this.mapa = mapa;
        this.canalExterno = canalExterno;
        this.preparacion = preparacion;
        this.turnos = turnos;
        this.liquidador = liquidador;
        this.detector = detector;
        this.codificador = codificador;
        this.reloj = reloj;
    }

    @Override
    public String cargarPredeterminado() {
        try (InputStream in = new ClassPathResource(ESCENARIO_PREDETERMINADO).getInputStream()) {
            return cargar(in);
        } catch (IOException e) {
            throw new SgaException("No se encontró el escenario predeterminado");
        }
    }

    @Override
    public String cargar(InputStream entrada) {
        EscenarioDatos datos;
        try {
            datos = json.readValue(entrada, EscenarioDatos.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ReglaNegocioException("El archivo de escenario no es válido: " + e.getOriginalMessage());
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer el archivo de escenario");
        }
        if (datos == null) {
            throw new ReglaNegocioException("El archivo de escenario está vacío");
        }
        return uow.escribir(() -> {
            almacen.reemplazar(new EstadoSistema());
            cargarTemporadas(datos);
            cargarApartamentos(datos);
            cargarMapa(datos);
            cargarPersonas(datos);
            int nReservas = cargarReservas(datos);
            aplicarEstadosApartamento(datos);
            cargarCanalExterno(datos);
            cargarPreparaciones(datos);
            return "Escenario \"" + (datos.nombre() == null ? "sin nombre" : datos.nombre()) + "\" cargado: "
                    + apartamentos.arbol().tamanio() + " apartamentos, " + huespedes.arbol().tamanio() + " huéspedes, "
                    + empleados.arbol().tamanio() + " empleados, " + nReservas + " reservas, "
                    + mapa.grafo().tamanio() + " puntos y " + mapa.grafo().cantidadConexiones() + " pasillos";
        });
    }

    private static <T> T[] arr(T[] a, T[] vacio) {
        return a == null ? vacio : a;
    }

    private void cargarTemporadas(EscenarioDatos d) {
        int anio = reloj.hoy().getYear();
        for (int y = anio - 1; y <= anio + 1; y++) {
            for (EscenarioDatos.TemporadaE t : arr(d.temporadas(), new EscenarioDatos.TemporadaE[0])) {
                MonthDay mi = MonthDay.parse("--" + t.inicio());
                MonthDay mf = MonthDay.parse("--" + t.fin());
                LocalDate inicio = mi.atYear(y);
                LocalDate fin = mf.isBefore(mi) ? mf.atYear(y + 1) : mf.atYear(y);
                Temporada nueva = new Temporada(temporadas.siguienteId(), t.nombre() + " " + y, t.tipo(), inicio, fin);
                for (Temporada otra : temporadas.todas()) {
                    if (otra.solapaCon(nueva)) {
                        throw new ReglaNegocioException("Temporadas superpuestas en el escenario: " + otra.nombre()
                                + " y " + nueva.nombre());
                    }
                }
                temporadas.todas().agregar(nueva);
            }
        }
    }

    private void cargarApartamentos(EscenarioDatos d) {
        for (EscenarioDatos.ApartamentoE a : arr(d.apartamentos(), new EscenarioDatos.ApartamentoE[0])) {
            Apartamento apto = new Apartamento(a.codigo(), a.nombre(), a.bloque(), a.piso(), a.dormitorios(),
                    a.capacidad(), new Tarifas(a.tarifaBaja(), a.tarifaMedia(), a.tarifaAlta()));
            if (apartamentos.buscar(apto.getCodigo()) != null) {
                throw new ReglaNegocioException("Apartamento repetido en el escenario: " + apto.getCodigo());
            }
            apto.setCaracteristicas(ListaDoble.de(arr(a.caracteristicas(), new String[0])));
            apto.setDescripcion(a.descripcion());
            apto.setImagenUrl(a.imagenUrl());
            apartamentos.guardar(apto);
        }
    }

    private void cargarMapa(EscenarioDatos d) {
        for (EscenarioDatos.NodoE n : arr(d.nodos(), new EscenarioDatos.NodoE[0])) {
            mapa.grafo().agregarVertice(new NodoEdificio(n.id(), n.nombre(), n.tipo(), n.bloque(), n.piso(), n.x(), n.y()));
        }
        for (EscenarioDatos.PasilloE p : arr(d.pasillos(), new EscenarioDatos.PasilloE[0])) {
            NodoEdificio a = NodoEdificio.referencia(p.origen());
            NodoEdificio b = NodoEdificio.referencia(p.destino());
            mapa.grafo().conectar(a, b, p.metros());
            if (Boolean.FALSE.equals(p.habilitado())) {
                mapa.grafo().cambiarEstadoConexion(a, b, false);
            }
        }
    }

    private void cargarPersonas(EscenarioDatos d) {
        for (EscenarioDatos.HuespedE h : arr(d.huespedes(), new EscenarioDatos.HuespedE[0])) {
            Huesped huesped = new Huesped(h.documento(), h.nombre(), h.fechaNacimiento(), h.telefono(), h.correo());
            if (h.clave() != null) {
                huesped.setClaveHash(codificador.codificar(h.clave()));
            }
            for (String f : arr(h.favoritos(), new String[0])) {
                huesped.getFavoritos().agregar(f.toUpperCase());
            }
            huespedes.guardar(huesped);
        }
        for (EscenarioDatos.EmpleadoE e : arr(d.empleados(), new EscenarioDatos.EmpleadoE[0])) {
            Empleado emp = new Empleado(e.documento(), e.nombre(), e.rol(), e.usuario(), e.correo(), e.telefono());
            emp.setClaveHash(codificador.codificar(e.clave()));
            empleados.guardar(emp);
            if (emp.getRol() == Rol.RECEPCIONISTA) {
                turnos.turnos().agregar(emp.getDocumento());
            }
        }
    }

    private int cargarReservas(EscenarioDatos d) {
        LocalDate hoy = reloj.hoy();
        LocalDateTime ahora = reloj.ahora();
        int n = 0;
        for (EscenarioDatos.ReservaE e : arr(d.reservas(), new EscenarioDatos.ReservaE[0])) {
            Apartamento a = apartamentos.buscar(e.apartamento());
            Huesped titular = huespedes.buscar(e.titular());
            if (a == null || titular == null) {
                throw new ReglaNegocioException("Reserva del escenario con apartamento o titular inexistente: "
                        + e.apartamento() + " / " + e.titular());
            }
            LocalDate entrada = hoy.plusDays(e.entrada());
            Estancia estancia = new Estancia(entrada, entrada.plusDays(e.noches()));
            ListaDoble<Ocupante> ocupantes = new ListaDoble<>();
            for (EscenarioDatos.OcupanteE o : arr(e.ocupantes(), new EscenarioDatos.OcupanteE[0])) {
                ocupantes.agregar(new Ocupante(o.nombre(), o.documento(), o.fechaNacimiento()));
            }
            if (ocupantes.tamanio() > a.getCapacidad()) {
                throw new ReglaNegocioException("Reserva del escenario excede la capacidad de " + a.getCodigo());
            }
            EstadoReserva estado = e.estado() == null ? EstadoReserva.CONFIRMADA : e.estado();
            if (estado.esActiva() && detector.primerConflicto(a.getReservas(), estancia, null) != null) {
                throw new ReservaSolapadaException("El escenario contiene reservas solapadas en " + a.getCodigo());
            }
            LocalDateTime creada = e.creadaHorasAtras() != null ? ahora.minusHours(e.creadaHorasAtras())
                    : entrada.minusDays(e.creadaDiasAntes() == null ? 15 : e.creadaDiasAntes()).atTime(10, 30);
            if (creada.isAfter(ahora)) {
                creada = ahora.minusHours(1);
            }
            Liquidacion liq = liquidador.liquidar(a, estancia, ocupantes);
            boolean nacePendiente = estado == EstadoReserva.PENDIENTE
                    || (estado == EstadoReserva.CANCELADA && e.canal() == Canal.PORTAL);
            Reserva r = new Reserva(reservas.siguienteCodigo(), a.getCodigo(), titular.getDocumento(),
                    titular.getNombre(), ocupantes, estancia, e.canal(), creada, e.horaLlegada(), liq.desglose(),
                    liq.total(), nacePendiente ? EstadoReserva.PENDIENTE : EstadoReserva.CONFIRMADA);
            if (e.canal() == Canal.EXTERNA) {
                r.setReferenciaExterna("BKS-" + (480000 + n * 137));
            }
            r.setObservaciones("");
            r.getFolio().registrar(TipoMovimiento.CARGO, "Alojamiento " + liq.noches() + " noche(s) · " + a.getNombre(),
                    liq.total(), creada, "sistema", false);
            String pago = e.pago() == null ? "NINGUNO" : e.pago().toUpperCase();
            if (pago.equals("ANTICIPO") || pago.equals("TOTAL")) {
                r.getFolio().registrar(TipoMovimiento.PAGO, "Anticipo 30 %", r.anticipoRequerido(), creada.plusMinutes(20),
                        "sistema", false);
            }
            for (EscenarioDatos.CargoE c : arr(e.cargos(), new EscenarioDatos.CargoE[0])) {
                r.getFolio().registrar(TipoMovimiento.CARGO, c.concepto(), c.monto(),
                        LocalDateTime.of(entrada.plusDays(Math.min(1, e.noches() - 1)), LocalTime.of(20, 15)), "recepcion", true);
            }
            if (pago.equals("TOTAL")) {
                long saldo = r.getFolio().saldo();
                LocalDateTime cuando = estado == EstadoReserva.FINALIZADA ? estancia.salida().atTime(11, 0)
                        : entrada.atTime(15, 30);
                if (cuando.isAfter(ahora)) {
                    cuando = ahora.minusMinutes(30);
                }
                if (saldo > 0) {
                    r.getFolio().registrar(TipoMovimiento.PAGO, "Pago del saldo", saldo, cuando, "recepcion", false);
                }
            }
            aplicarEstado(r, estado, titular, ahora);
            a.getReservas().insertar(r);
            reservas.guardar(r);
            n++;
        }
        return n;
    }

    private void aplicarEstado(Reserva r, EstadoReserva estado, Huesped titular, LocalDateTime ahora) {
        switch (estado) {
            case PENDIENTE, CONFIRMADA -> {
                // ya está en su estado inicial
            }
            case EN_CURSO -> {
                r.transitar(EstadoReserva.EN_CURSO);
                r.setCheckInEn(r.getEstancia().entrada().atTime(r.getHoraLlegada()));
            }
            case FINALIZADA -> {
                r.transitar(EstadoReserva.EN_CURSO);
                r.transitar(EstadoReserva.FINALIZADA);
                r.setCheckInEn(r.getEstancia().entrada().atTime(r.getHoraLlegada()));
                r.setCheckOutEn(r.getEstancia().salida().atTime(10, 45));
                titular.registrarEstanciaFinalizada(r.getCodigo());
            }
            case CANCELADA -> {
                long anular = r.getFolio().totalCargos() - r.getFolio().totalPagado();
                if (r.getFolio().totalPagado() > 0) {
                    r.getFolio().registrar(TipoMovimiento.REVERSO_PAGO, "Devolución por cancelación (100 %)",
                            r.getFolio().totalPagado(), r.getCreadaEn().plusDays(1), "sistema", false);
                    anular = r.getFolio().totalCargos();
                }
                r.getFolio().registrar(TipoMovimiento.REVERSO_CARGO, "Anulación de cargos por cancelación", anular,
                        r.getCreadaEn().plusDays(1), "sistema", false);
                r.transitar(EstadoReserva.CANCELADA);
                r.setMotivoCierre("El huésped canceló su viaje");
            }
            case NO_SHOW -> {
                long anular = r.getFolio().totalCargos() - r.getFolio().totalPagado();
                if (anular > 0) {
                    r.getFolio().registrar(TipoMovimiento.REVERSO_CARGO, "Anulación de cargos (no-show; se retiene lo pagado)",
                            anular, r.getEstancia().entrada().plusDays(1).atTime(9, 0), "sistema", false);
                }
                r.transitar(EstadoReserva.NO_SHOW);
                r.setMotivoCierre("El huésped no se presentó");
            }
        }
    }

    private void aplicarEstadosApartamento(EscenarioDatos d) {
        LocalDateTime ahora = reloj.ahora();
        for (EscenarioDatos.ApartamentoE e : arr(d.apartamentos(), new EscenarioDatos.ApartamentoE[0])) {
            Apartamento a = apartamentos.buscar(e.codigo());
            boolean ocupado = a.getReservas().flujo().anyMatch(r -> r.getEstado() == EstadoReserva.EN_CURSO);
            EstadoApartamento estado = ocupado ? EstadoApartamento.OCUPADO
                    : e.estado() == null || e.estado() == EstadoApartamento.OCUPADO ? EstadoApartamento.PREPARADO : e.estado();
            LocalDateTime pendiente = estado == EstadoApartamento.PENDIENTE_PREPARACION
                    || estado == EstadoApartamento.EN_PREPARACION ? ahora.minusMinutes(95) : null;
            a.restaurarEstado(estado, estado == EstadoApartamento.FUERA_SERVICIO ? e.motivo() : null, pendiente);
            if (estado == EstadoApartamento.EN_PREPARACION) {
                a.restaurarEstado(EstadoApartamento.PENDIENTE_PREPARACION, null, pendiente);
                a.cambiarEstado(EstadoApartamento.EN_PREPARACION, null, ahora.minusMinutes(25));
            }
        }
    }

    private void cargarCanalExterno(EscenarioDatos d) {
        LocalDate hoy = reloj.hoy();
        LocalDateTime ahora = reloj.ahora();
        for (Reserva r : reservas.todas()) {
            if (r.getCanal() == Canal.EXTERNA) {
                SolicitudExterna s = new SolicitudExterna(canalExterno.siguienteId(), "BookStay", r.getReferenciaExterna(),
                        r.getApartamentoCodigo(), r.getTitularDocumento(), r.getTitularNombre(), LocalDate.of(1985, 1, 1),
                        "", "", r.getOcupantes(), r.getEstancia().entrada(), r.getEstancia().salida(), r.getHoraLlegada(),
                        r.getCreadaEn());
                canalExterno.procesamientos().agregarAlInicio(new ProcesamientoExterno(s, ResultadoSolicitud.ACEPTADA,
                        r.getCodigo(), null, "Reserva creada y confirmada", r.getCreadaEn()));
            }
        }
        for (EscenarioDatos.ConflictoE c : arr(d.conflictosHistoricos(), new EscenarioDatos.ConflictoE[0])) {
            LocalDate entrada = hoy.plusDays(c.entrada());
            Estancia estancia = new Estancia(entrada, entrada.plusDays(c.noches()));
            Apartamento a = apartamentos.buscar(c.apartamento());
            Reserva choque = a == null ? null : detector.reservasEnRango(a.getReservas(), estancia, x -> true)
                    .flujo().findFirst().orElse(null);
            LocalDateTime cuando = ahora.minusDays(c.diasAtras());
            SolicitudExterna s = new SolicitudExterna(canalExterno.siguienteId(), c.plataforma(),
                    c.plataforma().substring(0, 3).toUpperCase() + "-" + (700000 + c.diasAtras() * 31),
                    c.apartamento(), "", c.titular(), LocalDate.of(1990, 1, 1), "", "", new ListaDoble<>(),
                    estancia.entrada(), estancia.salida(), LocalTime.of(15, 0), cuando);
            canalExterno.procesamientos().agregarAlInicio(new ProcesamientoExterno(s, ResultadoSolicitud.CONFLICTO, null,
                    choque == null ? null : choque.getCodigo(),
                    "El apartamento " + c.apartamento() + " ya estaba reservado en esas fechas", cuando));
        }
        for (EscenarioDatos.SolicitudE s : arr(d.solicitudesExternas(), new EscenarioDatos.SolicitudE[0])) {
            ListaDoble<Ocupante> ocupantes = new ListaDoble<>();
            for (EscenarioDatos.OcupanteE o : arr(s.ocupantes(), new EscenarioDatos.OcupanteE[0])) {
                ocupantes.agregar(new Ocupante(o.nombre(), o.documento(), o.fechaNacimiento()));
            }
            LocalDate entrada = hoy.plusDays(s.entrada());
            EscenarioDatos.TitularE t = s.titular();
            canalExterno.cola().encolar(new SolicitudExterna(canalExterno.siguienteId(), s.plataforma(), s.referencia(),
                    s.apartamento().toUpperCase(), t.documento(), t.nombre(), t.fechaNacimiento(), t.telefono(),
                    t.correo(), ocupantes, entrada, entrada.plusDays(s.noches()), s.horaLlegada(),
                    ahora.minusMinutes(40L - canalExterno.cola().tamanio() * 7L)));
        }
    }

    private void cargarPreparaciones(EscenarioDatos d) {
        LocalDateTime ahora = reloj.ahora();
        for (EscenarioDatos.PreparacionE p : arr(d.preparaciones(), new EscenarioDatos.PreparacionE[0])) {
            LocalDateTime fin = ahora.minusDays(p.diasAtras()).withHour(13).withMinute(10).withSecond(0);
            LocalDateTime inicio = fin.minusMinutes(p.duracionMinutos());
            preparacion.registros().agregar(new RegistroPreparacion(p.apartamento().toUpperCase(),
                    inicio.minusMinutes(p.esperaMinutos()), inicio, fin, p.responsable()));
        }
    }
}
