package co.edu.uq.sga.web.dto;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Empleado;
import co.edu.uq.sga.dominio.Folio;
import co.edu.uq.sga.dominio.Huesped;
import co.edu.uq.sga.dominio.ItemPreparacion;
import co.edu.uq.sga.dominio.Movimiento;
import co.edu.uq.sga.dominio.NocheLiquidada;
import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.dominio.ProcesamientoExterno;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.dominio.SolicitudExterna;
import co.edu.uq.sga.dominio.TipoNodo;
import co.edu.uq.sga.estructuras.Coleccion;
import co.edu.uq.sga.estructuras.Grafo;
import co.edu.uq.sga.estructuras.Ruta;
import co.edu.uq.sga.negocio.Liquidacion;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.servicio.resultados.GrupoBloque;
import co.edu.uq.sga.servicio.resultados.OpcionDisponible;
import co.edu.uq.sga.servicio.resultados.PropuestaGrupo;
import co.edu.uq.sga.servicio.resultados.RutaPreparacion;
import co.edu.uq.sga.servicio.resultados.Sesion;
import co.edu.uq.sga.servicio.resultados.TurnoRecepcion;
import co.edu.uq.sga.web.dto.Dtos.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.function.Function;
import java.util.function.IntFunction;

/** Traduce el dominio (con estructuras propias) a DTOs serializables con arreglos. */
@Component
public class Mapeador {

    private final RepositorioApartamentos apartamentos;

    public Mapeador(RepositorioApartamentos apartamentos) {
        this.apartamentos = apartamentos;
    }

    public static <T, R> R[] arreglo(Coleccion<T> origen, Function<T, R> f, IntFunction<R[]> generador) {
        return origen.flujo().map(f).toArray(generador);
    }

    public ApartamentoDto apartamento(Apartamento a) {
        int activas = 0;
        for (Reserva r : a.getReservas()) {
            if (r.esActiva()) {
                activas++;
            }
        }
        return new ApartamentoDto(a.getCodigo(), a.getNombre(), a.getBloque(), a.getPiso(), a.getDormitorios(),
                a.getCapacidad(), arreglo(a.getCaracteristicas(), s -> s, String[]::new), a.getTarifas().baja(),
                a.getTarifas().media(), a.getTarifas().alta(), a.getDescripcion(), a.getImagenUrl(),
                a.getEstado().name(), a.getEstado().etiqueta(), a.getMotivoFueraServicio(), a.getPendienteDesde(),
                a.getEnPreparacionDesde(), a.getReservas().tamanio(), activas);
    }

    public ApartamentoDto[] apartamentos(Coleccion<Apartamento> lista) {
        return arreglo(lista, this::apartamento, ApartamentoDto[]::new);
    }

    public HuespedDto huesped(Huesped h) {
        return new HuespedDto(h.getDocumento(), h.getNombre(), h.getFechaNacimiento(), h.getTelefono(), h.getCorreo(),
                h.getFoto(), arreglo(h.getFavoritos(), s -> s, String[]::new), h.getHistorial().tamanio(),
                h.getClaveHash() != null);
    }

    public EmpleadoDto empleado(Empleado e) {
        return e == null ? null : new EmpleadoDto(e.getDocumento(), e.getNombre(), e.getRol(), e.getUsuario(),
                e.getCorreo(), e.getTelefono(), e.isActivo());
    }

    public NocheDto noche(NocheLiquidada n) {
        return new NocheDto(n.fecha(), n.temporada().etiqueta(), n.tarifaPorPersona(), n.personasQuePagan(),
                n.personasExentas(), n.subtotal());
    }

    public MovimientoDto movimiento(Movimiento m) {
        return m == null ? null : new MovimientoDto(m.numero(), m.tipo().name(), m.tipo().etiqueta(), m.concepto(),
                m.monto(), m.efectoEnSaldo(), m.fecha(), m.numeroReferencia(), m.usuario());
    }

    public FolioDto folio(Reserva r) {
        Folio f = r.getFolio();
        return new FolioDto(r.getCodigo(), arreglo(f.getMovimientos(), this::movimiento, MovimientoDto[]::new),
                f.totalCargos(), f.totalPagado(), f.saldo(), movimiento(f.verUltimaOperacion()), f.operacionesDeshacibles());
    }

    public ReservaDto reserva(Reserva r) {
        Apartamento a = apartamentos.buscar(r.getApartamentoCodigo());
        LocalDate entrada = r.getEstancia().entrada();
        return new ReservaDto(r.getCodigo(), r.getApartamentoCodigo(), a == null ? r.getApartamentoCodigo() : a.getNombre(),
                a == null ? null : a.getImagenUrl(), r.getTitularDocumento(), r.getTitularNombre(),
                arreglo(r.getOcupantes(), (Ocupante o) -> new OcupanteDto(o.nombre(), o.documento(), o.fechaNacimiento(),
                        o.edadEn(entrada), o.pagaTarifa(entrada)), OcupanteDto[]::new),
                entrada, r.getEstancia().salida(), r.getEstancia().noches(), r.getCanal().name(), r.getEstado().name(),
                r.getEstado().etiqueta(), r.getCreadaEn(), r.getVenceEn(), r.getHoraLlegada(), r.getTotal(),
                r.anticipoRequerido(), r.anticipoPendiente(), r.getFolio().totalPagado(), r.getFolio().saldo(),
                r.getCheckInEn(), r.getCheckOutEn(), r.getMotivoCierre(), r.getReferenciaExterna(), r.getObservaciones(),
                arreglo(r.getDesglose(), this::noche, NocheDto[]::new));
    }

    public ReservaDto[] reservas(Coleccion<Reserva> lista) {
        return arreglo(lista, this::reserva, ReservaDto[]::new);
    }

    public CotizacionDto cotizacion(String codigo, Liquidacion l, LocalDate entrada, LocalDate salida) {
        long anticipo = (l.total() * Reserva.PORCENTAJE_ANTICIPO + 99) / 100;
        return new CotizacionDto(codigo, entrada, salida, l.noches(), l.personasQuePagan(), l.personasExentas(),
                l.total(), l.noches() == 0 ? 0 : l.total() / l.noches(), anticipo,
                arreglo(l.desglose(), this::noche, NocheDto[]::new));
    }

    public OpcionDto opcion(OpcionDisponible o) {
        Liquidacion l = o.cotizacion();
        return new OpcionDto(apartamento(o.apartamento()), l.total(), l.noches(),
                l.noches() == 0 ? 0 : l.total() / l.noches());
    }

    public ItemPreparacionDto item(ItemPreparacion i) {
        return new ItemPreparacionDto(i.apartamentoCodigo(), i.apartamentoNombre(), i.bloque(), i.piso(), i.prioridad(),
                i.entradaHoy(), i.horaLlegada(), i.reservaEntrante(), i.huespedEntrante(), i.pendienteDesde());
    }

    public NodoDto nodo(NodoEdificio n) {
        String estado = null;
        if (n.tipo() == TipoNodo.APARTAMENTO) {
            Apartamento a = apartamentos.buscar(n.id());
            estado = a == null ? null : a.getEstado().name();
        }
        return new NodoDto(n.id(), n.nombre(), n.tipo().name(), n.bloque(), n.piso(), n.x(), n.y(), estado);
    }

    public PasilloDto pasillo(Grafo.Conexion<NodoEdificio> c) {
        return new PasilloDto(c.origen().id(), c.destino().id(), c.peso(), c.habilitada());
    }

    public RutaDto ruta(String algoritmo, Ruta<NodoEdificio> r) {
        return new RutaDto(algoritmo, arreglo(r.camino(), NodoEdificio::id, String[]::new), r.costo(), new String[0],
                new String[0]);
    }

    public RutaDto rutaPreparacion(RutaPreparacion r) {
        return new RutaDto(r.algoritmo(), arreglo(r.camino(), NodoEdificio::id, String[]::new), r.metros(),
                arreglo(r.paradas(), s -> s, String[]::new), arreglo(r.inalcanzables(), s -> s, String[]::new));
    }

    public GrupoBloqueDto grupo(GrupoBloque g) {
        return new GrupoBloqueDto(g.bloque(), arreglo(g.nodos(), NodoEdificio::id, String[]::new),
                g.nodos().flujo().filter(n -> n.tipo() == TipoNodo.APARTAMENTO).map(NodoEdificio::id).toArray(String[]::new));
    }

    public SolicitudExternaDto solicitud(SolicitudExterna s) {
        return new SolicitudExternaDto(s.id(), s.plataforma(), s.referenciaExterna(), s.apartamentoCodigo(),
                s.titularNombre(), s.titularDocumento(), s.entrada(), s.salida(), s.ocupantes().tamanio(), s.recibidaEn());
    }

    public ProcesamientoDto procesamiento(ProcesamientoExterno p) {
        return new ProcesamientoDto(solicitud(p.solicitud()), p.resultado().name(), p.reservaCodigo(),
                p.reservaEnConflicto(), p.motivo(), p.procesadaEn());
    }

    public TurnoDto turno(TurnoRecepcion t) {
        return new TurnoDto(empleado(t.actual()), t.franja(), arreglo(t.siguientes(), this::empleado, EmpleadoDto[]::new));
    }

    public PropuestaGrupoDto propuesta(PropuestaGrupo p) {
        return new PropuestaGrupoDto(p.personas(), apartamentos(p.apartamentos()), p.capacidadTotal(), p.totalEstimado(),
                p.candidatosEvaluados());
    }

    public SesionDto sesion(Sesion s) {
        return new SesionDto(s.token(), s.rol(), s.identificador(), s.nombre(), s.usuario(), s.expira());
    }
}
