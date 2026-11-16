package co.edu.uq.sga.servicio;

import co.edu.uq.sga.dominio.NodoEdificio;
import co.edu.uq.sga.estructuras.Grafo;
import co.edu.uq.sga.estructuras.ListaDoble;
import co.edu.uq.sga.estructuras.Ruta;
import co.edu.uq.sga.servicio.comandos.DatosNodo;
import co.edu.uq.sga.servicio.comandos.DatosPasillo;
import co.edu.uq.sga.servicio.resultados.GrupoBloque;

/** Mapa del edificio como grafo: CRUD de puntos y pasillos, rutas y análisis. */
public interface ServicioMapa {
    ListaDoble<NodoEdificio> nodos();

    ListaDoble<Grafo.Conexion<NodoEdificio>> pasillos();

    NodoEdificio crearNodo(DatosNodo datos);

    NodoEdificio actualizarNodo(String id, DatosNodo datos);

    void eliminarNodo(String id);

    void conectar(DatosPasillo datos);

    void desconectar(String origen, String destino);

    /** Abre o cierra (sin borrar) un pasillo. */
    void cambiarEstadoPasillo(String origen, String destino, boolean habilitado);

    /** @param algoritmo DIJKSTRA o BFS */
    Ruta<NodoEdificio> ruta(String origen, String destino, String algoritmo);

    /** @param tipo BFS o DFS */
    ListaDoble<NodoEdificio> recorrido(String origen, String tipo);

    /** Apartamentos/puntos que quedarían inalcanzables desde el origen si se cierra ese pasillo. */
    ListaDoble<NodoEdificio> simularCierre(String origen, String a, String b);

    /** Puntos inalcanzables hoy desde el origen (con los pasillos cerrados actualmente). */
    ListaDoble<NodoEdificio> inalcanzables(String origen);

    ListaDoble<GrupoBloque> agruparPorBloque();

    ListaDoble<ListaDoble<NodoEdificio>> componentes();
}
