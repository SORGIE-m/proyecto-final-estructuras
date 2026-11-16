package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.SgaException;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Grafo no dirigido y ponderado, representado con listas de adyacencia propias.
 * Vértices = puntos del edificio; aristas = pasillos con peso (metros). Cada pasillo puede
 * cerrarse (deshabilitarse) sin borrarlo, para analizar alcanzabilidad.
 */
public class Grafo<T> implements Coleccion<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final class Vertice<T> implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private final T dato;
        private int indice;
        private final ListaDoble<Arista<T>> adyacentes = new ListaDoble<>();

        Vertice(T dato, int indice) {
            this.dato = dato;
            this.indice = indice;
        }
    }

    /** Arista dirigida interna; un pasillo se guarda como dos aristas espejo. */
    private static final class Arista<T> implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private final Vertice<T> destino;
        private double peso;
        private boolean habilitada = true;

        Arista(Vertice<T> destino, double peso) {
            this.destino = destino;
            this.peso = peso;
        }
    }

    /** Vista pública e inmutable de un pasillo. */
    public record Conexion<T>(T origen, T destino, double peso, boolean habilitada) implements Serializable {
    }

    private final ListaDoble<Vertice<T>> vertices = new ListaDoble<>();

    // ------------------------------------------------------------------ vértices

    public boolean agregarVertice(T dato) {
        if (vertice(dato) != null) {
            return false;
        }
        vertices.agregar(new Vertice<>(dato, vertices.tamanio()));
        return true;
    }

    public boolean contieneVertice(T dato) {
        return vertice(dato) != null;
    }

    /** Elimina el vértice y todas sus aristas; reasigna índices. */
    public boolean eliminarVertice(T dato) {
        Vertice<T> v = vertice(dato);
        if (v == null) {
            return false;
        }
        for (Vertice<T> otro : vertices) {
            otro.adyacentes.eliminarSi(a -> a.destino == v);
        }
        vertices.eliminar(v);
        int i = 0;
        for (Vertice<T> otro : vertices) {
            otro.indice = i++;
        }
        return true;
    }

    /** Reemplaza el dato de un vértice (por ejemplo al editar sus coordenadas). */
    public void actualizarVertice(T dato) {
        Vertice<T> v = verticeObligatorio(dato);
        int idx = vertices.indiceDe(v);
        Vertice<T> nuevo = new Vertice<>(dato, v.indice);
        nuevo.adyacentes.agregarTodos(v.adyacentes);
        vertices.reemplazar(idx, nuevo);
        for (Vertice<T> otro : vertices) {
            ListaDoble<Arista<T>> ady = otro.adyacentes;
            for (int i = 0; i < ady.tamanio(); i++) {
                Arista<T> a = ady.obtener(i);
                if (a.destino == v) {
                    Arista<T> reemplazo = new Arista<>(nuevo, a.peso);
                    reemplazo.habilitada = a.habilitada;
                    ady.reemplazar(i, reemplazo);
                }
            }
        }
    }

    public T buscarVertice(java.util.function.Predicate<T> condicion) {
        for (Vertice<T> v : vertices) {
            if (condicion.test(v.dato)) {
                return v.dato;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ aristas

    public void conectar(T a, T b, double peso) {
        if (peso <= 0) {
            throw new SgaException("El peso de un pasillo debe ser positivo");
        }
        Vertice<T> va = verticeObligatorio(a);
        Vertice<T> vb = verticeObligatorio(b);
        if (va == vb) {
            throw new SgaException("Un pasillo no puede conectar un punto consigo mismo");
        }
        Arista<T> existente = arista(va, vb);
        if (existente != null) {
            existente.peso = peso;
            arista(vb, va).peso = peso;
            return;
        }
        va.adyacentes.agregar(new Arista<>(vb, peso));
        vb.adyacentes.agregar(new Arista<>(va, peso));
    }

    public boolean desconectar(T a, T b) {
        Vertice<T> va = vertice(a);
        Vertice<T> vb = vertice(b);
        if (va == null || vb == null) {
            return false;
        }
        boolean r = va.adyacentes.eliminarSi(x -> x.destino == vb) > 0;
        vb.adyacentes.eliminarSi(x -> x.destino == va);
        return r;
    }

    /** Abre o cierra un pasillo sin eliminarlo. */
    public void cambiarEstadoConexion(T a, T b, boolean habilitada) {
        Vertice<T> va = verticeObligatorio(a);
        Vertice<T> vb = verticeObligatorio(b);
        Arista<T> ida = arista(va, vb);
        if (ida == null) {
            throw new SgaException("No existe un pasillo entre los puntos indicados");
        }
        ida.habilitada = habilitada;
        arista(vb, va).habilitada = habilitada;
    }

    public boolean existeConexion(T a, T b) {
        Vertice<T> va = vertice(a);
        Vertice<T> vb = vertice(b);
        return va != null && vb != null && arista(va, vb) != null;
    }

    /** Lista cada pasillo una sola vez. */
    public ListaDoble<Conexion<T>> conexiones() {
        ListaDoble<Conexion<T>> r = new ListaDoble<>();
        for (Vertice<T> v : vertices) {
            for (Arista<T> a : v.adyacentes) {
                if (v.indice < a.destino.indice) {
                    r.agregar(new Conexion<>(v.dato, a.destino.dato, a.peso, a.habilitada));
                }
            }
        }
        return r;
    }

    public ListaDoble<T> vecinos(T dato) {
        ListaDoble<T> r = new ListaDoble<>();
        for (Arista<T> a : verticeObligatorio(dato).adyacentes) {
            if (a.habilitada) {
                r.agregar(a.destino.dato);
            }
        }
        return r;
    }

    // ------------------------------------------------------------------ recorridos

    /** Recorrido en anchura desde el origen por pasillos habilitados. */
    public ListaDoble<T> bfs(T origen) {
        return bfs(origen, (x, y) -> true);
    }

    public ListaDoble<T> bfs(T origen, BiPredicate<T, T> pasilloPermitido) {
        Vertice<T> inicio = verticeObligatorio(origen);
        boolean[] visitado = new boolean[vertices.tamanio()];
        ListaDoble<T> orden = new ListaDoble<>();
        Cola<Vertice<T>> cola = new Cola<>();
        visitado[inicio.indice] = true;
        cola.encolar(inicio);
        while (!cola.estaVacia()) {
            Vertice<T> v = cola.desencolar();
            orden.agregar(v.dato);
            for (Arista<T> a : v.adyacentes) {
                if (a.habilitada && !visitado[a.destino.indice] && pasilloPermitido.test(v.dato, a.destino.dato)) {
                    visitado[a.destino.indice] = true;
                    cola.encolar(a.destino);
                }
            }
        }
        return orden;
    }

    /** Recorrido en profundidad recursivo por pasillos habilitados. */
    public ListaDoble<T> dfs(T origen) {
        return dfs(origen, (x, y) -> true);
    }

    public ListaDoble<T> dfs(T origen, BiPredicate<T, T> pasilloPermitido) {
        ListaDoble<T> orden = new ListaDoble<>();
        dfs(verticeObligatorio(origen), new boolean[vertices.tamanio()], orden, pasilloPermitido);
        return orden;
    }

    private void dfs(Vertice<T> v, boolean[] visitado, ListaDoble<T> orden, BiPredicate<T, T> permitido) {
        visitado[v.indice] = true;
        orden.agregar(v.dato);
        for (Arista<T> a : v.adyacentes) {
            if (a.habilitada && !visitado[a.destino.indice] && permitido.test(v.dato, a.destino.dato)) {
                dfs(a.destino, visitado, orden, permitido);
            }
        }
    }

    /** Componentes conexas (DFS) considerando solo los pasillos permitidos. */
    public ListaDoble<ListaDoble<T>> componentesConexas(BiPredicate<T, T> pasilloPermitido) {
        boolean[] visitado = new boolean[vertices.tamanio()];
        ListaDoble<ListaDoble<T>> componentes = new ListaDoble<>();
        for (Vertice<T> v : vertices) {
            if (!visitado[v.indice]) {
                ListaDoble<T> comp = new ListaDoble<>();
                dfs(v, visitado, comp, pasilloPermitido);
                componentes.agregar(comp);
            }
        }
        return componentes;
    }

    /** Vértices a los que NO se puede llegar desde el origen. */
    public ListaDoble<T> inalcanzablesDesde(T origen, BiPredicate<T, T> pasilloPermitido) {
        ListaDoble<T> alcanzados = bfs(origen, pasilloPermitido);
        ListaDoble<T> r = new ListaDoble<>();
        for (Vertice<T> v : vertices) {
            if (!alcanzados.contiene(v.dato)) {
                r.agregar(v.dato);
            }
        }
        return r;
    }

    // ------------------------------------------------------------------ caminos mínimos

    private record Etiqueta(int indice, double distancia) {
    }

    /** Dijkstra con la ColaPrioridad propia. Pesos positivos. */
    public Ruta<T> dijkstra(T origen, T destino) {
        Vertice<T> vo = verticeObligatorio(origen);
        Vertice<T> vd = verticeObligatorio(destino);
        int n = vertices.tamanio();
        double[] dist = new double[n];
        int[] previo = new int[n];
        boolean[] cerrado = new boolean[n];
        Object[] porIndice = indexar();
        for (int i = 0; i < n; i++) {
            dist[i] = Double.POSITIVE_INFINITY;
            previo[i] = -1;
        }
        dist[vo.indice] = 0;
        ColaPrioridad<Etiqueta> cola = new ColaPrioridad<>(
                java.util.Comparator.comparingDouble(Etiqueta::distancia));
        cola.insertar(new Etiqueta(vo.indice, 0));
        while (!cola.estaVacia()) {
            Etiqueta e = cola.extraer();
            if (cerrado[e.indice()]) {
                continue;
            }
            cerrado[e.indice()] = true;
            if (e.indice() == vd.indice) {
                break;
            }
            @SuppressWarnings("unchecked")
            Vertice<T> v = (Vertice<T>) porIndice[e.indice()];
            for (Arista<T> a : v.adyacentes) {
                if (!a.habilitada) {
                    continue;
                }
                int j = a.destino.indice;
                double nd = dist[v.indice] + a.peso;
                if (nd < dist[j]) {
                    dist[j] = nd;
                    previo[j] = v.indice;
                    cola.insertar(new Etiqueta(j, nd));
                }
            }
        }
        return reconstruir(vd.indice, previo, dist[vd.indice], porIndice);
    }

    /** Camino con el menor número de pasillos (BFS). El costo reportado es la suma de pesos. */
    public Ruta<T> rutaBfs(T origen, T destino) {
        Vertice<T> vo = verticeObligatorio(origen);
        Vertice<T> vd = verticeObligatorio(destino);
        int n = vertices.tamanio();
        int[] previo = new int[n];
        double[] costo = new double[n];
        boolean[] visitado = new boolean[n];
        Object[] porIndice = indexar();
        for (int i = 0; i < n; i++) {
            previo[i] = -1;
            costo[i] = Double.POSITIVE_INFINITY;
        }
        Cola<Vertice<T>> cola = new Cola<>();
        visitado[vo.indice] = true;
        costo[vo.indice] = 0;
        cola.encolar(vo);
        while (!cola.estaVacia()) {
            Vertice<T> v = cola.desencolar();
            if (v == vd) {
                break;
            }
            for (Arista<T> a : v.adyacentes) {
                int j = a.destino.indice;
                if (a.habilitada && !visitado[j]) {
                    visitado[j] = true;
                    previo[j] = v.indice;
                    costo[j] = costo[v.indice] + a.peso;
                    cola.encolar(a.destino);
                }
            }
        }
        return reconstruir(vd.indice, previo, costo[vd.indice], porIndice);
    }

    @SuppressWarnings("unchecked")
    private Ruta<T> reconstruir(int destino, int[] previo, double costo, Object[] porIndice) {
        if (Double.isInfinite(costo)) {
            return Ruta.inexistente();
        }
        ListaDoble<T> camino = new ListaDoble<>();
        for (int i = destino; i != -1; i = previo[i]) {
            camino.agregarAlInicio(((Vertice<T>) porIndice[i]).dato);
        }
        return new Ruta<>(camino, costo);
    }

    // ------------------------------------------------------------------ utilidades

    private Object[] indexar() {
        Object[] arr = new Object[vertices.tamanio()];
        for (Vertice<T> v : vertices) {
            arr[v.indice] = v;
        }
        return arr;
    }

    private Vertice<T> vertice(T dato) {
        for (Vertice<T> v : vertices) {
            if (Objects.equals(v.dato, dato)) {
                return v;
            }
        }
        return null;
    }

    private Vertice<T> verticeObligatorio(T dato) {
        Vertice<T> v = vertice(dato);
        if (v == null) {
            throw new SgaException("El punto " + dato + " no existe en el mapa del edificio");
        }
        return v;
    }

    private Arista<T> arista(Vertice<T> a, Vertice<T> b) {
        return a.adyacentes.buscar(x -> x.destino == b);
    }

    public int cantidadConexiones() {
        return conexiones().tamanio();
    }

    @Override
    public int tamanio() {
        return vertices.tamanio();
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<Vertice<T>> it = vertices.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return it.next().dato;
            }
        };
    }
}
