package co.edu.uq.sga.estructuras;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;

/** Conjunto genérico sin repetidos (según equals). Favoritos de cada huésped. */
public class Conjunto<T> implements Coleccion<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ListaDoble<T> elementos = new ListaDoble<>();

    /** @return true si el elemento no estaba y fue agregado. */
    public boolean agregar(T dato) {
        if (elementos.contiene(dato)) {
            return false;
        }
        elementos.agregar(dato);
        return true;
    }

    public boolean eliminar(T dato) {
        return elementos.eliminar(dato);
    }

    public boolean contiene(T dato) {
        return elementos.contiene(dato);
    }

    public Conjunto<T> union(Conjunto<T> otro) {
        Conjunto<T> r = new Conjunto<>();
        for (T e : this) {
            r.agregar(e);
        }
        for (T e : otro) {
            r.agregar(e);
        }
        return r;
    }

    public Conjunto<T> interseccion(Conjunto<T> otro) {
        Conjunto<T> r = new Conjunto<>();
        for (T e : this) {
            if (otro.contiene(e)) {
                r.agregar(e);
            }
        }
        return r;
    }

    public Conjunto<T> diferencia(Conjunto<T> otro) {
        Conjunto<T> r = new Conjunto<>();
        for (T e : this) {
            if (!otro.contiene(e)) {
                r.agregar(e);
            }
        }
        return r;
    }

    @Override
    public int tamanio() {
        return elementos.tamanio();
    }

    @Override
    public Iterator<T> iterator() {
        return elementos.iterator();
    }
}
