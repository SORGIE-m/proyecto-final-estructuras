package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.Estancia;
import co.edu.uq.sga.dominio.Ocupante;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.excepciones.FechasInvalidasException;
import co.edu.uq.sga.excepciones.RecursoNoEncontradoException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.negocio.AsignadorGrupos;
import co.edu.uq.sga.negocio.DetectorSolapamientos;
import co.edu.uq.sga.negocio.Liquidacion;
import co.edu.uq.sga.negocio.LiquidadorEstancia;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.servicio.ServicioDisponibilidad;
import co.edu.uq.sga.servicio.resultados.OpcionDisponible;
import co.edu.uq.sga.servicio.resultados.PropuestaGrupo;
import co.edu.uq.sga.soporte.Reloj;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;

@Service
public class ServicioDisponibilidadImpl implements ServicioDisponibilidad {

    public static final int MAXIMO_GRUPO = 120;

    private final RepositorioApartamentos apartamentos;
    private final DetectorSolapamientos detector;
    private final LiquidadorEstancia liquidador;
    private final AsignadorGrupos asignador;
    private final UnidadDeTrabajo uow;
    private final Reloj reloj;

    public ServicioDisponibilidadImpl(RepositorioApartamentos apartamentos, DetectorSolapamientos detector,
                                      LiquidadorEstancia liquidador, AsignadorGrupos asignador, UnidadDeTrabajo uow,
                                      Reloj reloj) {
        this.apartamentos = apartamentos;
        this.detector = detector;
        this.liquidador = liquidador;
        this.asignador = asignador;
        this.uow = uow;
        this.reloj = reloj;
    }

    private Estancia validar(LocalDate entrada, LocalDate salida) {
        Estancia e = new Estancia(entrada, salida);
        if (entrada.isBefore(reloj.hoy())) {
            throw new FechasInvalidasException("La fecha de entrada no puede estar en el pasado");
        }
        return e;
    }

    private ListaDoble<Apartamento> libresInterno(Estancia e) {
        ListaDoble<Apartamento> libres = new ListaDoble<>();
        for (Apartamento a : apartamentos.arbol()) {
            if (a.admiteReservas() && detector.primerConflicto(a.getReservas(), e, null) == null) {
                libres.agregar(a);
            }
        }
        return libres;
    }

    @Override
    public ListaDoble<Apartamento> libres(LocalDate entrada, LocalDate salida) {
        return uow.leer(() -> libresInterno(validar(entrada, salida)));
    }

    @Override
    public ListaDoble<OpcionDisponible> buscar(LocalDate entrada, LocalDate salida, int personas, String orden) {
        return uow.leer(() -> {
            if (personas < 1) {
                throw new ReglaNegocioException("El número de personas debe ser al menos 1");
            }
            Estancia e = validar(entrada, salida);
            ListaDoble<OpcionDisponible> opciones = new ListaDoble<>();
            for (Apartamento a : libresInterno(e)) {
                if (a.getCapacidad() >= personas) {
                    opciones.agregar(new OpcionDisponible(a, liquidador.liquidar(a, e, personas, 0)));
                }
            }
            Comparator<OpcionDisponible> c = comparador(orden);
            if (c != null) {
                opciones.ordenar(c);
            }
            return opciones;
        });
    }

    private static Comparator<OpcionDisponible> comparador(String orden) {
        if (orden == null || orden.isBlank() || orden.equalsIgnoreCase("precio")) {
            return Comparator.comparingLong((OpcionDisponible o) -> o.cotizacion().total())
                    .thenComparing(o -> o.apartamento().getCodigo());
        }
        Comparator<Apartamento> porApto = ServicioApartamentosImpl.comparador(orden);
        return porApto == null ? null : Comparator.comparing(OpcionDisponible::apartamento, porApto);
    }

    @Override
    public Liquidacion cotizar(String codigo, LocalDate entrada, LocalDate salida, ListaDoble<Ocupante> ocupantes) {
        return uow.leer(() -> {
            Apartamento a = requerir(codigo);
            if (ocupantes.tamanio() > a.getCapacidad()) {
                throw new co.edu.uq.sga.excepciones.CapacidadExcedidaException("El apartamento admite máximo "
                        + a.getCapacidad() + " personas");
            }
            return liquidador.liquidar(a, new Estancia(entrada, salida), ocupantes);
        });
    }

    @Override
    public Liquidacion cotizar(String codigo, LocalDate entrada, LocalDate salida, int pagan, int exentos) {
        return uow.leer(() -> {
            Apartamento a = requerir(codigo);
            if (pagan < 0 || exentos < 0 || pagan + exentos < 1) {
                throw new ReglaNegocioException("Indique al menos una persona");
            }
            if (pagan + exentos > a.getCapacidad()) {
                throw new co.edu.uq.sga.excepciones.CapacidadExcedidaException("El apartamento admite máximo "
                        + a.getCapacidad() + " personas");
            }
            return liquidador.liquidar(a, new Estancia(entrada, salida), pagan, exentos);
        });
    }

    @Override
    public PropuestaGrupo proponerGrupo(LocalDate entrada, LocalDate salida, int personas) {
        return uow.leer(() -> {
            if (personas < 1 || personas > MAXIMO_GRUPO) {
                throw new ReglaNegocioException("El grupo debe tener entre 1 y " + MAXIMO_GRUPO + " personas");
            }
            Estancia e = validar(entrada, salida);
            ListaDoble<Apartamento> libres = libresInterno(e);
            ListaDoble<Apartamento> elegidos = asignador.asignar(libres, personas);
            if (elegidos.estaVacia()) {
                throw new ReglaNegocioException("No hay combinación de apartamentos libres para " + personas
                        + " personas en esas fechas");
            }
            int capacidad = 0;
            long total = 0;
            int restantes = personas;
            for (Apartamento a : elegidos) {
                capacidad += a.getCapacidad();
                int aqui = Math.min(a.getCapacidad(), restantes);
                restantes -= aqui;
                total += liquidador.liquidar(a, e, aqui, 0).total();
            }
            return new PropuestaGrupo(personas, elegidos, capacidad, total, libres.tamanio());
        });
    }

    private Apartamento requerir(String codigo) {
        Apartamento a = apartamentos.buscar(codigo);
        if (a == null) {
            throw new RecursoNoEncontradoException("No existe el apartamento " + codigo);
        }
        return a;
    }
}
