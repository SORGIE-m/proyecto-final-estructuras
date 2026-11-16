package co.edu.uq.sga.estructuras;

import java.io.Serial;
import java.io.Serializable;

/** Resultado de un cálculo de camino en el grafo: secuencia de vértices y costo total. */
public final class Ruta<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ListaDoble<T> camino;
    private final double costo;

    public Ruta(ListaDoble<T> camino, double costo) {
        this.camino = camino;
        this.costo = costo;
    }

    public static <T> Ruta<T> inexistente() {
        return new Ruta<>(new ListaDoble<>(), Double.POSITIVE_INFINITY);
    }

    public ListaDoble<T> camino() {
        return camino;
    }

    public double costo() {
        return costo;
    }

    public boolean existe() {
        return !camino.estaVacia() && !Double.isInfinite(costo);
    }

    /** Concatena dos rutas consecutivas (el destino de esta es el origen de la otra). */
    public Ruta<T> concatenar(Ruta<T> siguiente) {
        ListaDoble<T> unido = ListaDoble.desde(camino);
        boolean primero = true;
        for (T v : siguiente.camino) {
            if (primero && !unido.estaVacia()) {
                primero = false;
                continue; // evita repetir el vértice de unión
            }
            primero = false;
            unido.agregar(v);
        }
        return new Ruta<>(unido, costo + siguiente.costo);
    }
}
