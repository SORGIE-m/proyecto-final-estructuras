package co.edu.uq.sga.servicio.impl;

import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.dominio.TipoNodo;
import co.edu.uq.sga.estructuras.Grafo;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.estructuras.Ruta;
import co.edu.uq.sga.excepciones.DatoDuplicadoException;
import co.edu.uq.sga.excepciones.RecursoNoEncontradoException;
import co.edu.uq.sga.excepciones.ReglaNegocioException;
import co.edu.uq.sga.persistencia.UnidadDeTrabajo;
import co.edu.uq.sga.repositorio.RepositorioApartamentos;
import co.edu.uq.sga.repositorio.RepositorioMapa;
import co.edu.uq.sga.servicio.ServicioMapa;
import co.edu.uq.sga.servicio.comandos.DatosNodo;
import co.edu.uq.sga.servicio.comandos.DatosPasillo;
import co.edu.uq.sga.servicio.resultados.GrupoBloque;
import org.springframework.stereotype.Service;

@Service
public class ServicioMapaImpl implements ServicioMapa {

    private final RepositorioMapa mapa;
    private final RepositorioApartamentos apartamentos;
    private final UnidadDeTrabajo uow;

    public ServicioMapaImpl(RepositorioMapa mapa, RepositorioApartamentos apartamentos, UnidadDeTrabajo uow) {
        this.mapa = mapa;
        this.apartamentos = apartamentos;
        this.uow = uow;
    }

    private Grafo<NodoEdificio> g() {
        return mapa.grafo();
    }

    private NodoEdificio requerir(String id) {
        if (id == null || id.isBlank()) {
            throw new ReglaNegocioException("Debe indicar el punto del mapa");
        }
        String clave = id.trim().toUpperCase();
        NodoEdificio n = g().buscarVertice(x -> x.id().equals(clave));
        if (n == null) {
            throw new RecursoNoEncontradoException("El punto " + clave + " no existe en el mapa");
        }
        return n;
    }

    @Override
    public ListaDoble<NodoEdificio> nodos() {
        return uow.leer(() -> ListaDoble.desde(g()));
    }

    @Override
    public ListaDoble<Grafo.Conexion<NodoEdificio>> pasillos() {
        return uow.leer(() -> g().conexiones());
    }

    private NodoEdificio construir(DatosNodo d) {
        NodoEdificio n = new NodoEdificio(d.id(), d.nombre(), d.tipo(), d.bloque(), d.piso(), d.x(), d.y());
        if (n.tipo() == TipoNodo.APARTAMENTO && apartamentos.buscar(n.id()) == null) {
            throw new ReglaNegocioException("Un punto de tipo apartamento debe usar el código de un apartamento existente");
        }
        return n;
    }

    @Override
    public NodoEdificio crearNodo(DatosNodo d) {
        return uow.escribir(() -> {
            NodoEdificio n = construir(d);
            if (!g().agregarVertice(n)) {
                throw new DatoDuplicadoException("Ya existe el punto " + n.id());
            }
            return n;
        });
    }

    @Override
    public NodoEdificio actualizarNodo(String id, DatosNodo d) {
        return uow.escribir(() -> {
            NodoEdificio actual = requerir(id);
            NodoEdificio nuevo = construir(new DatosNodo(actual.id(), d.nombre(), d.tipo(), d.bloque(), d.piso(), d.x(), d.y()));
            g().actualizarVertice(nuevo);
            return nuevo;
        });
    }

    @Override
    public void eliminarNodo(String id) {
        uow.ejecutar(() -> g().eliminarVertice(requerir(id)));
    }

    @Override
    public void conectar(DatosPasillo d) {
        uow.ejecutar(() -> g().conectar(requerir(d.origen()), requerir(d.destino()), d.metros()));
    }

    @Override
    public void desconectar(String origen, String destino) {
        uow.ejecutar(() -> {
            if (!g().desconectar(requerir(origen), requerir(destino))) {
                throw new RecursoNoEncontradoException("No existe un pasillo entre " + origen + " y " + destino);
            }
        });
    }

    @Override
    public void cambiarEstadoPasillo(String origen, String destino, boolean habilitado) {
        uow.ejecutar(() -> g().cambiarEstadoConexion(requerir(origen), requerir(destino), habilitado));
    }

    @Override
    public Ruta<NodoEdificio> ruta(String origen, String destino, String algoritmo) {
        return uow.leer(() -> {
            NodoEdificio a = requerir(origen);
            NodoEdificio b = requerir(destino);
            if (a.equals(b)) {
                return new Ruta<>(ListaDoble.de(a), 0);
            }
            Ruta<NodoEdificio> r = "BFS".equalsIgnoreCase(algoritmo) ? g().rutaBfs(a, b) : g().dijkstra(a, b);
            if (!r.existe()) {
                throw new ReglaNegocioException("No hay un camino abierto entre " + a.id() + " y " + b.id());
            }
            return r;
        });
    }

    @Override
    public ListaDoble<NodoEdificio> recorrido(String origen, String tipo) {
        return uow.leer(() -> "DFS".equalsIgnoreCase(tipo) ? g().dfs(requerir(origen)) : g().bfs(requerir(origen)));
    }

    @Override
    public ListaDoble<NodoEdificio> simularCierre(String origen, String a, String b) {
        return uow.leer(() -> {
            NodoEdificio o = requerir(origen);
            NodoEdificio na = requerir(a);
            NodoEdificio nb = requerir(b);
            if (!g().existeConexion(na, nb)) {
                throw new RecursoNoEncontradoException("No existe un pasillo entre " + na.id() + " y " + nb.id());
            }
            return g().inalcanzablesDesde(o, (x, y) -> !((x.equals(na) && y.equals(nb)) || (x.equals(nb) && y.equals(na))));
        });
    }

    @Override
    public ListaDoble<NodoEdificio> inalcanzables(String origen) {
        return uow.leer(() -> g().inalcanzablesDesde(requerir(origen), (x, y) -> true));
    }

    /** DFS restringido a pasillos entre puntos del mismo bloque: cada componente es un grupo. */
    @Override
    public ListaDoble<GrupoBloque> agruparPorBloque() {
        return uow.leer(() -> {
            ListaDoble<GrupoBloque> grupos = new ListaDoble<>();
            for (ListaDoble<NodoEdificio> comp : g().componentesConexas((x, y) -> !x.bloque().isEmpty() && x.bloque().equals(y.bloque()))) {
                String bloque = comp.primero().bloque();
                if (!bloque.isEmpty()) {
                    grupos.agregar(new GrupoBloque(bloque, comp));
                }
            }
            grupos.ordenar((x, y) -> x.bloque().compareTo(y.bloque()));
            return grupos;
        });
    }

    @Override
    public ListaDoble<ListaDoble<NodoEdificio>> componentes() {
        return uow.leer(() -> g().componentesConexas((x, y) -> true));
    }
}
