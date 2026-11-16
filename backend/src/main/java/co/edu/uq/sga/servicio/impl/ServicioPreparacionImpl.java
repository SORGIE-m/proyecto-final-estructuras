package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.comparadores.ComparadoresPreparacion;
import co.edu.uq.sga.dominio.Apartamento;
import co.edu.uq.sga.dominio.EstadoApartamento;
import co.edu.uq.sga.dominio.EstadoReserva;
import co.edu.uq.sga.dominio.Estancia;
import co.edu.uq.sga.dominio.ItemPreparacion;
import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.dominio.RegistroPreparacion;
import co.edu.uq.sga.dominio.Reserva;
import co.edu.uq.sga.estructuras.ColaPrioridad;
import co.edu.uq.sga.estructuras.Grafo;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.estructuras.Ruta;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import co.edu.uq.sga.excepciones.RecursoNoEncontradoException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.negocio.DetectorSolapamientos;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.repositorio.RepositorioMapa;
import co.edu.uq.sga.repositorio.RepositorioPreparacion;
import co.edu.uq.sga.servicio.ServicioPreparacion;
import co.edu.uq.sga.servicio.resultados.RutaPreparacion;
import co.edu.uq.sga.soporte.Reloj;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ServicioPreparacionImpl implements ServicioPreparacion {

    public static final String ORIGEN_POR_DEFECTO = "RECEPCION";

    private final RepositorioPreparacion repositorio;
    private final RepositorioApartamentos apartamentos;
    private final RepositorioMapa mapa;
    private final DetectorSolapamientos detector;
    private final UnidadDeTrabajo uow;
    private final Reloj reloj;

    public ServicioPreparacionImpl(RepositorioPreparacion repositorio, RepositorioApartamentos apartamentos,
                                   RepositorioMapa mapa, DetectorSolapamientos detector, UnidadDeTrabajo uow,
                                   Reloj reloj) {
        this.repositorio = repositorio;
        this.apartamentos = apartamentos;
        this.mapa = mapa;
        this.detector = detector;
        this.uow = uow;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ cola

    private ColaPrioridad<ItemPreparacion> colaInterna() {
        return repositorio.cola(() -> {
            ColaPrioridad<ItemPreparacion> c = new ColaPrioridad<>(ComparadoresPreparacion.prioridad());
            for (Apartamento a : apartamentos.arbol()) {
                if (a.getEstado() == EstadoApartamento.PENDIENTE_PREPARACION) {
                    c.insertar(item(a));
                }
            }
            return c;
        });
    }

    /** Calcula la prioridad actual: ¿hay un grupo que entra hoy? ¿a qué hora llega? */
    private ItemPreparacion item(Apartamento a) {
        LocalDate hoy = reloj.hoy();
        ListaDoble<Reserva> entrantes = detector.reservasEnRango(a.getReservas(), new Estancia(hoy, hoy.plusDays(1)),
                r -> (r.getEstado() == EstadoReserva.CONFIRMADA || r.getEstado() == EstadoReserva.PENDIENTE)
                        && r.getEstancia().entrada().equals(hoy));
        Reserva proxima = null;
        for (Reserva r : entrantes) {
            if (proxima == null || r.getHoraLlegada().isBefore(proxima.getHoraLlegada())) {
                proxima = r;
            }
        }
        return new ItemPreparacion(a.getCodigo(), a.getNombre(), a.getBloque(), a.getPiso(), a.getPendienteDesde(),
                proxima != null, proxima == null ? null : proxima.getHoraLlegada(),
                proxima == null ? null : proxima.getCodigo(), proxima == null ? null : proxima.getTitularNombre());
    }

    /** Recalcula prioridades (cambian con el día y con nuevas reservas) y reordena el montículo. */
    private ColaPrioridad<ItemPreparacion> refrescada() {
        ColaPrioridad<ItemPreparacion> cola = colaInterna();
        ListaDoble<String> codigos = new ListaDoble<>();
        while (!cola.estaVacia()) {
            codigos.agregar(cola.extraer().apartamentoCodigo());
        }
        for (String codigo : codigos) {
            Apartamento a = apartamentos.buscar(codigo);
            if (a != null && a.getEstado() == EstadoApartamento.PENDIENTE_PREPARACION) {
                cola.insertar(item(a));
            }
        }
        return cola;
    }

    @Override
    public void encolar(String codigo) {
        uow.ejecutar(() -> {
            Apartamento a = requerir(codigo);
            if (a.getEstado() != EstadoApartamento.PENDIENTE_PREPARACION) {
                throw new ReglaNegocioException("Solo se encolan apartamentos pendientes de preparación");
            }
            ColaPrioridad<ItemPreparacion> cola = colaInterna();
            cola.eliminarSi(i -> i.apartamentoCodigo().equals(a.getCodigo()));
            cola.insertar(item(a));
        });
    }

    @Override
    public void retirar(String codigo) {
        uow.exclusivo(() -> colaInterna().eliminarSi(i -> i.apartamentoCodigo().equalsIgnoreCase(codigo)));
    }

    @Override
    public ListaDoble<ItemPreparacion> cola() {
        return uow.exclusivo(() -> ListaDoble.desde(refrescada()));
    }

    @Override
    public ItemPreparacion despacharSiguiente(String responsable) {
        return uow.escribir(() -> {
            ColaPrioridad<ItemPreparacion> cola = refrescada();
            if (cola.estaVacia()) {
                throw new EstructuraVaciaException("Cola de preparación (no hay apartamentos pendientes)");
            }
            ItemPreparacion siguiente = cola.extraer();
            requerir(siguiente.apartamentoCodigo())
                    .cambiarEstado(EstadoApartamento.EN_PREPARACION, null, reloj.ahora());
            return siguiente;
        });
    }

    @Override
    public Apartamento iniciar(String codigo, String responsable) {
        return uow.escribir(() -> {
            Apartamento a = requerir(codigo);
            a.cambiarEstado(EstadoApartamento.EN_PREPARACION, null, reloj.ahora());
            colaInterna().eliminarSi(i -> i.apartamentoCodigo().equals(a.getCodigo()));
            return a;
        });
    }

    @Override
    public Apartamento finalizar(String codigo, String responsable) {
        return uow.escribir(() -> {
            Apartamento a = requerir(codigo);
            if (a.getEstado() != EstadoApartamento.EN_PREPARACION) {
                throw new ReglaNegocioException("El apartamento " + a.getCodigo() + " no está en preparación");
            }
            LocalDateTime pendiente = a.getPendienteDesde();
            LocalDateTime inicio = a.getEnPreparacionDesde();
            LocalDateTime fin = reloj.ahora();
            a.cambiarEstado(EstadoApartamento.PREPARADO, null, fin);
            repositorio.registros().agregar(new RegistroPreparacion(a.getCodigo(), pendiente,
                    inicio == null ? fin : inicio, fin, responsable));
            return a;
        });
    }

    @Override
    public ListaDoble<Apartamento> enPreparacion() {
        return uow.leer(() -> apartamentos.arbol().enInorden()
                .filtrar(a -> a.getEstado() == EstadoApartamento.EN_PREPARACION));
    }

    // ------------------------------------------------------------------ ruta

    @Override
    public RutaPreparacion ruta(String algoritmo, String origenId) {
        return uow.exclusivo(() -> {
            boolean bfs = "BFS".equalsIgnoreCase(algoritmo);
            Grafo<NodoEdificio> grafo = mapa.grafo();
            String idOrigen = (origenId == null || origenId.isBlank()) ? ORIGEN_POR_DEFECTO : origenId.trim().toUpperCase();
            NodoEdificio origen = grafo.buscarVertice(n -> n.id().equals(idOrigen));
            if (origen == null) {
                throw new RecursoNoEncontradoException("El punto de partida " + idOrigen + " no existe en el mapa");
            }
            ListaDoble<ItemPreparacion> pendientes = ListaDoble.desde(refrescada());
            ListaDoble<String> paradas = new ListaDoble<>();
            ListaDoble<String> inalcanzables = new ListaDoble<>();
            Ruta<NodoEdificio> total = new Ruta<>(ListaDoble.de(origen), 0);
            NodoEdificio actual = origen;

            // 1) Prioridad 1 en el orden de la cola (hora de llegada del siguiente grupo)
            ListaDoble<NodoEdificio> resto = new ListaDoble<>();
            for (ItemPreparacion item : pendientes) {
                NodoEdificio nodo = grafo.buscarVertice(n -> n.id().equals(item.apartamentoCodigo()));
                if (nodo == null) {
                    inalcanzables.agregar(item.apartamentoCodigo());
                } else if (item.prioridad() == 1) {
                    Ruta<NodoEdificio> tramo = tramo(grafo, actual, nodo, bfs);
                    if (tramo.existe()) {
                        total = total.concatenar(tramo);
                        paradas.agregar(nodo.id());
                        actual = nodo;
                    } else {
                        inalcanzables.agregar(nodo.id());
                    }
                } else {
                    resto.agregar(nodo);
                }
            }
            // 2) Prioridad 2: vecino más cercano según el algoritmo elegido
            while (!resto.estaVacia()) {
                NodoEdificio mejor = null;
                Ruta<NodoEdificio> mejorTramo = null;
                for (NodoEdificio candidato : resto) {
                    Ruta<NodoEdificio> t = tramo(grafo, actual, candidato, bfs);
                    if (t.existe() && (mejorTramo == null || mejorQue(t, mejorTramo, bfs))) {
                        mejor = candidato;
                        mejorTramo = t;
                    }
                }
                if (mejor == null) {
                    for (NodoEdificio n : resto) {
                        inalcanzables.agregar(n.id());
                    }
                    break;
                }
                total = total.concatenar(mejorTramo);
                paradas.agregar(mejor.id());
                actual = mejor;
                resto.eliminar(mejor);
            }
            return new RutaPreparacion(bfs ? "BFS" : "DIJKSTRA", origen, total.camino(), total.costo(),
                    paradas, inalcanzables);
        });
    }

    private Ruta<NodoEdificio> tramo(Grafo<NodoEdificio> g, NodoEdificio a, NodoEdificio b, boolean bfs) {
        if (a.equals(b)) {
            return new Ruta<>(ListaDoble.de(a), 0);
        }
        return bfs ? g.rutaBfs(a, b) : g.dijkstra(a, b);
    }

    private boolean mejorQue(Ruta<NodoEdificio> a, Ruta<NodoEdificio> b, boolean bfs) {
        if (bfs) {
            int ta = a.camino().tamanio();
            int tb = b.camino().tamanio();
            return ta < tb || (ta == tb && a.costo() < b.costo());
        }
        return a.costo() < b.costo();
    }

    private Apartamento requerir(String codigo) {
        Apartamento a = apartamentos.buscar(codigo);
        if (a == null) {
            throw new RecursoNoEncontradoException("No existe el apartamento " + codigo);
        }
        return a;
    }
}
