package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Canal;
import co.edu.uq.sga.dominio.EstadoApartamento;
import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Estancia;
import co.edu.uq.sga.dominio.Movimiento;
import co.edu.uq.sga.dominio.NocheLiquidada;
import co.edu.uq.sga.dominio.ProcesamientoExterno;
import co.edu.uq.sga.dominio.RegistroPreparacion;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.ResultadoSolicitud;
import co.edu.uq.sga.dominio.TipoMovimiento;
import co.edu.uq.sga.dominio.TipoTemporada;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.FechasInvalidasException;
import co.edu.uq.sga.negocio.DetectorSolapamientos;
import co.edu.uq.sga.negocio.ProveedorTemporadas;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.repositorio.RepositorioCanalExterno;
import co.edu.uq.sga.repositorio.RepositorioHuespedes;
import co.edu.uq.sga.repositorio.RepositorioMapa;
import co.edu.uq.sga.repositorio.RepositorioPreparacion;
import co.edu.uq.sga.repositorio.RepositorioReservas;
import co.edu.uq.sga.servicio.ServicioReportes;
import co.edu.uq.sga.servicio.resultados.Calendario;
import co.edu.uq.sga.servicio.resultados.Estadisticas;
import co.edu.uq.sga.servicio.resultados.Tablero;
import co.edu.uq.sga.soporte.Reloj;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
public class ServicioReportesImpl implements ServicioReportes {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RepositorioApartamentos apartamentos;
    private final RepositorioReservas reservas;
    private final RepositorioHuespedes huespedes;
    private final RepositorioCanalExterno canalExterno;
    private final RepositorioPreparacion preparacion;
    private final RepositorioMapa mapa;
    private final DetectorSolapamientos detector;
    private final ProveedorTemporadas temporadas;
    private final UnidadDeTrabajo uow;
    private final Reloj reloj;

    public ServicioReportesImpl(RepositorioApartamentos apartamentos, RepositorioReservas reservas,
                                RepositorioHuespedes huespedes, RepositorioCanalExterno canalExterno,
                                RepositorioPreparacion preparacion, RepositorioMapa mapa,
                                DetectorSolapamientos detector, ProveedorTemporadas temporadas,
                                UnidadDeTrabajo uow, Reloj reloj) {
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.huespedes = huespedes;
        this.canalExterno = canalExterno;
        this.preparacion = preparacion;
        this.mapa = mapa;
        this.detector = detector;
        this.temporadas = temporadas;
        this.uow = uow;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ calendario

    @Override
    public Calendario calendario(YearMonth mes) {
        return uow.leer(() -> {
            LocalDate inicio = mes.atDay(1);
            LocalDate finExclusivo = mes.plusMonths(1).atDay(1);
            int dias = mes.lengthOfMonth();
            LocalDate[] noches = new LocalDate[dias];
            for (int i = 0; i < dias; i++) {
                noches[i] = inicio.plusDays(i);
            }
            Estancia rango = new Estancia(inicio, finExclusivo);
            LocalDate hoy = reloj.hoy();
            ListaDoble<Calendario.Fila> filas = new ListaDoble<>();
            for (Apartamento a : apartamentos.arbol()) {
                Calendario.Celda[] celdas = new Calendario.Celda[dias];
                for (int i = 0; i < dias; i++) {
                    boolean bloqueada = a.getEstado() == EstadoApartamento.FUERA_SERVICIO && !noches[i].isBefore(hoy);
                    celdas[i] = new Calendario.Celda(noches[i],
                            bloqueada ? Calendario.EstadoNoche.BLOQUEADA : Calendario.EstadoNoche.LIBRE, null, null);
                }
                int ocupadas = 0;
                for (Reserva r : detector.reservasEnRango(a.getReservas(), rango,
                        x -> x.getEstado().ocupaNoches() || x.getEstado() == EstadoReserva.PENDIENTE)) {
                    Calendario.EstadoNoche estado = r.getEstado() == EstadoReserva.PENDIENTE
                            ? Calendario.EstadoNoche.PENDIENTE : Calendario.EstadoNoche.OCUPADA;
                    for (LocalDate noche : r.getEstancia()) {
                        if (rango.incluyeNoche(noche)) {
                            int i = (int) ChronoUnit.DAYS.between(inicio, noche);
                            celdas[i] = new Calendario.Celda(noche, estado, r.getCodigo(), r.getTitularNombre());
                            if (estado == Calendario.EstadoNoche.OCUPADA) {
                                ocupadas++;
                            }
                        }
                    }
                }
                filas.agregar(new Calendario.Fila(a.getCodigo(), a.getNombre(), a.getBloque(), a.getEstado().name(),
                        ocupadas, celdas));
            }
            return new Calendario(mes.toString(), noches, filas.flujo().toArray(Calendario.Fila[]::new));
        });
    }

    // ------------------------------------------------------------------ estadísticas

    @Override
    public Estadisticas estadisticas(LocalDate desde, LocalDate hasta) {
        return uow.leer(() -> {
            LocalDate d = desde != null ? desde : reloj.hoy().withDayOfMonth(1);
            LocalDate h = hasta != null ? hasta : YearMonth.from(d).atEndOfMonth();
            if (h.isBefore(d)) {
                throw new FechasInvalidasException("La fecha final del reporte no puede ser anterior a la inicial");
            }
            if (ChronoUnit.DAYS.between(d, h) > 730) {
                throw new FechasInvalidasException("El periodo del reporte no puede superar dos años");
            }
            LocalDate finExclusivo = h.plusDays(1);
            int nochesPeriodo = (int) ChronoUnit.DAYS.between(d, finExclusivo);

            // --- ocupación por apartamento
            ListaDoble<Estadisticas.OcupacionApartamento> porApto = new ListaDoble<>();
            int totalOcupadas = 0;
            long facturadoPeriodo = 0;
            int aptos = 0;
            for (Apartamento a : apartamentos.arbol()) {
                aptos++;
                int ocupadas = 0;
                long ingresos = 0;
                for (Reserva r : a.getReservas()) {
                    if (!r.getEstado().ocupaNoches()) {
                        continue;
                    }
                    for (NocheLiquidada n : r.getDesglose()) {
                        if (!n.fecha().isBefore(d) && n.fecha().isBefore(finExclusivo)) {
                            ocupadas++;
                            ingresos += n.subtotal();
                        }
                    }
                }
                totalOcupadas += ocupadas;
                facturadoPeriodo += ingresos;
                porApto.agregar(new Estadisticas.OcupacionApartamento(a.getCodigo(), a.getNombre(), ocupadas,
                        nochesPeriodo, porcentaje(ocupadas, nochesPeriodo), ingresos));
            }
            porApto.ordenar((x, y) -> Double.compare(y.porcentaje(), x.porcentaje()));

            // --- ocupación por temporada (en el periodo)
            int[] disponibles = new int[TipoTemporada.values().length];
            int[] ocupadasTemp = new int[TipoTemporada.values().length];
            for (LocalDate dia = d; dia.isBefore(finExclusivo); dia = dia.plusDays(1)) {
                disponibles[temporadas.tipoPara(dia).ordinal()] += aptos;
            }
            for (Reserva r : reservas.todas()) {
                if (r.getEstado().ocupaNoches()) {
                    for (NocheLiquidada n : r.getDesglose()) {
                        if (!n.fecha().isBefore(d) && n.fecha().isBefore(finExclusivo)) {
                            ocupadasTemp[temporadas.tipoPara(n.fecha()).ordinal()]++;
                        }
                    }
                }
            }
            Estadisticas.OcupacionTemporada[] porTemporada = new Estadisticas.OcupacionTemporada[disponibles.length];
            for (TipoTemporada t : TipoTemporada.values()) {
                int i = t.ordinal();
                porTemporada[i] = new Estadisticas.OcupacionTemporada(t.etiqueta(), disponibles[i], ocupadasTemp[i],
                        porcentaje(ocupadasTemp[i], disponibles[i]));
            }

            // --- ingresos por mes (12 meses que terminan en el mes final)
            YearMonth ultimo = YearMonth.from(h);
            Estadisticas.IngresoMes[] porMes = new Estadisticas.IngresoMes[12];
            long recaudadoPeriodo = 0;
            for (int i = 0; i < 12; i++) {
                YearMonth m = ultimo.minusMonths(11 - i);
                porMes[i] = new Estadisticas.IngresoMes(m.toString(), 0, 0);
            }
            int reservasPeriodo = 0;
            ReservasCanalAcumulador[] canales = new ReservasCanalAcumulador[Canal.values().length];
            for (Canal c : Canal.values()) {
                canales[c.ordinal()] = new ReservasCanalAcumulador(c);
            }
            for (Reserva r : reservas.todas()) {
                LocalDate creada = r.getCreadaEn().toLocalDate();
                if (!creada.isBefore(d) && creada.isBefore(finExclusivo)) {
                    reservasPeriodo++;
                }
                canales[r.getCanal().ordinal()].contar(r.getEstado());
                for (Movimiento mov : r.getFolio().getMovimientos()) {
                    long neto = mov.tipo() == TipoMovimiento.PAGO ? mov.monto()
                            : mov.tipo() == TipoMovimiento.REVERSO_PAGO ? -mov.monto() : 0;
                    if (neto == 0) {
                        continue;
                    }
                    LocalDate f = mov.fecha().toLocalDate();
                    if (!f.isBefore(d) && f.isBefore(finExclusivo)) {
                        recaudadoPeriodo += neto;
                    }
                    int idx = indiceMes(porMes, YearMonth.from(f));
                    if (idx >= 0) {
                        porMes[idx] = new Estadisticas.IngresoMes(porMes[idx].mes(), porMes[idx].recaudado() + neto,
                                porMes[idx].facturado());
                    }
                }
                if (r.getEstado().ocupaNoches()) {
                    for (NocheLiquidada n : r.getDesglose()) {
                        int idx = indiceMes(porMes, YearMonth.from(n.fecha()));
                        if (idx >= 0) {
                            porMes[idx] = new Estadisticas.IngresoMes(porMes[idx].mes(), porMes[idx].recaudado(),
                                    porMes[idx].facturado() + n.subtotal());
                        }
                    }
                }
            }
            Estadisticas.ReservasCanal[] porCanal = new Estadisticas.ReservasCanal[canales.length];
            for (int i = 0; i < canales.length; i++) {
                porCanal[i] = canales[i].resultado();
            }

            // --- conflictos de canal
            int procesadas = 0;
            int aceptadas = 0;
            int conflictos = 0;
            int rechazadas = 0;
            ListaDoble<Estadisticas.ConflictoItem> recientes = new ListaDoble<>();
            for (ProcesamientoExterno p : canalExterno.procesamientos()) {
                procesadas++;
                switch (p.resultado()) {
                    case ACEPTADA -> aceptadas++;
                    case CONFLICTO -> conflictos++;
                    case RECHAZADA -> rechazadas++;
                }
                if (p.resultado() == ResultadoSolicitud.CONFLICTO && recientes.tamanio() < 8) {
                    recientes.agregar(new Estadisticas.ConflictoItem(p.solicitud().id(), p.solicitud().plataforma(),
                            p.solicitud().apartamentoCodigo(), p.reservaEnConflicto(), p.motivo(),
                            p.procesadaEn().format(FECHA_HORA)));
                }
            }

            // --- tiempos de preparación
            int registros = 0;
            long sumaPrep = 0;
            long sumaEspera = 0;
            long minimo = Long.MAX_VALUE;
            long maximo = 0;
            for (RegistroPreparacion rp : preparacion.registros()) {
                registros++;
                long m = rp.minutosPreparacion();
                sumaPrep += m;
                sumaEspera += rp.minutosEspera();
                minimo = Math.min(minimo, m);
                maximo = Math.max(maximo, m);
            }

            double ocupacionPromedio = porcentaje(totalOcupadas, nochesPeriodo * Math.max(1, aptos));
            long adr = totalOcupadas == 0 ? 0 : facturadoPeriodo / totalOcupadas;
            Estadisticas.EstructurasInfo info = new Estadisticas.EstructurasInfo(apartamentos.arbol().tamanio(),
                    apartamentos.arbol().altura(), huespedes.arbol().tamanio(), huespedes.arbol().altura(),
                    mapa.grafo().tamanio(), mapa.grafo().cantidadConexiones(), canalExterno.cola().tamanio(),
                    reservas.todas().tamanio());
            return new Estadisticas(d, h,
                    new Estadisticas.Resumen(reservasPeriodo, recaudadoPeriodo, facturadoPeriodo, ocupacionPromedio,
                            adr, huespedes.arbol().tamanio(), aptos),
                    porApto.flujo().toArray(Estadisticas.OcupacionApartamento[]::new), porTemporada, porMes, porCanal,
                    new Estadisticas.Conflictos(procesadas, aceptadas, conflictos, rechazadas,
                            recientes.flujo().toArray(Estadisticas.ConflictoItem[]::new)),
                    new Estadisticas.Preparacion(registros, registros == 0 ? 0 : (double) sumaPrep / registros,
                            registros == 0 ? 0 : (double) sumaEspera / registros,
                            registros == 0 ? 0 : minimo, maximo),
                    info);
        });
    }

    private static int indiceMes(Estadisticas.IngresoMes[] meses, YearMonth m) {
        String clave = m.toString();
        for (int i = 0; i < meses.length; i++) {
            if (meses[i].mes().equals(clave)) {
                return i;
            }
        }
        return -1;
    }

    private static double porcentaje(int parte, int total) {
        return total == 0 ? 0 : Math.round(parte * 1000.0 / total) / 10.0;
    }

    /** Acumulador mutable local para contar reservas de un canal por estado. */
    private static final class ReservasCanalAcumulador {
        private final Canal canal;
        private final int[] porEstado = new int[EstadoReserva.values().length];

        ReservasCanalAcumulador(Canal canal) {
            this.canal = canal;
        }

        void contar(EstadoReserva e) {
            porEstado[e.ordinal()]++;
        }

        Estadisticas.ReservasCanal resultado() {
            int total = 0;
            for (int n : porEstado) {
                total += n;
            }
            return new Estadisticas.ReservasCanal(canal.etiqueta(), total,
                    porEstado[EstadoReserva.PENDIENTE.ordinal()], porEstado[EstadoReserva.CONFIRMADA.ordinal()],
                    porEstado[EstadoReserva.EN_CURSO.ordinal()], porEstado[EstadoReserva.FINALIZADA.ordinal()],
                    porEstado[EstadoReserva.CANCELADA.ordinal()], porEstado[EstadoReserva.NO_SHOW.ordinal()]);
        }
    }

    // ------------------------------------------------------------------ tablero

    @Override
    public Tablero tablero() {
        return uow.leer(() -> {
            int total = 0;
            int[] porEstado = new int[EstadoApartamento.values().length];
            for (Apartamento a : apartamentos.arbol()) {
                total++;
                porEstado[a.getEstado().ordinal()]++;
            }
            LocalDate hoy = reloj.hoy();
            int llegadas = 0;
            int salidas = 0;
            int pendientes = 0;
            int nochesHoy = 0;
            long ingresosMes = 0;
            YearMonth mes = YearMonth.from(hoy);
            for (Reserva r : reservas.todas()) {
                if (r.getEstancia().entrada().equals(hoy) && (r.getEstado() == EstadoReserva.CONFIRMADA
                        || r.getEstado() == EstadoReserva.PENDIENTE || r.getEstado() == EstadoReserva.EN_CURSO)) {
                    llegadas++;
                }
                if (r.getEstancia().salida().equals(hoy) && r.getEstado() == EstadoReserva.EN_CURSO) {
                    salidas++;
                }
                if (r.getEstado() == EstadoReserva.PENDIENTE) {
                    pendientes++;
                }
                if (r.getEstado().ocupaNoches() && r.getEstancia().incluyeNoche(hoy)) {
                    nochesHoy++;
                }
                for (Movimiento m : r.getFolio().getMovimientos()) {
                    if (YearMonth.from(m.fecha()).equals(mes)) {
                        if (m.tipo() == TipoMovimiento.PAGO) {
                            ingresosMes += m.monto();
                        } else if (m.tipo() == TipoMovimiento.REVERSO_PAGO) {
                            ingresosMes -= m.monto();
                        }
                    }
                }
            }
            return new Tablero(total, porEstado[EstadoApartamento.PREPARADO.ordinal()],
                    porEstado[EstadoApartamento.OCUPADO.ordinal()],
                    porEstado[EstadoApartamento.PENDIENTE_PREPARACION.ordinal()],
                    porEstado[EstadoApartamento.EN_PREPARACION.ordinal()],
                    porEstado[EstadoApartamento.FUERA_SERVICIO.ordinal()], llegadas, salidas, pendientes,
                    canalExterno.cola().tamanio(), porcentaje(nochesHoy, Math.max(1, total)), ingresosMes);
        });
    }
}
