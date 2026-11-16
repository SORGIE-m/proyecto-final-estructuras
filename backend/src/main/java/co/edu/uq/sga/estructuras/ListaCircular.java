package co.edu.uq.sga.estructuras;

import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Lista circular simplemente enlazada con un cursor "actual". Modela la rotación de turnos
 * de recepción: avanzar() pasa el turno al siguiente y, tras el último, vuelve al primero.
 */
public class ListaCircular<T> implements Coleccion<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final class Nodo<T> {
        T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    /** Último nodo; su siguiente es el primero. */
    private transient Nodo<T> ultimo;
    private transient Nodo<T> actual;
    private transient int tamanio;

    public void agregar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (ultimo == null) {
            nuevo.siguiente = nuevo;
            actual = nuevo;
        } else {
            nuevo.siguiente = ultimo.siguiente;
            ultimo.siguiente = nuevo;
        }
        ultimo = nuevo;
        tamanio++;
    }

    public boolean eliminar(T dato) {
        if (ultimo == null) {
            return false;
        }
        Nodo<T> previo = ultimo;
        for (int i = 0; i < tamanio; i++) {
            Nodo<T> n = previo.siguiente;
            if (Objects.equals(n.dato, dato)) {
                if (tamanio == 1) {
                    ultimo = actual = null;
                } else {
                    previo.siguiente = n.siguiente;
                    if (n == ultimo) {
                        ultimo = previo;
                    }
                    if (n == actual) {
                        actual = n.siguiente;
                    }
                }
                tamanio--;
                return true;
            }
            previo = n;
        }
        return false;
    }

    public T actual() {
        if (actual == null) {
            throw new EstructuraVaciaException("ListaCircular");
        }
        return actual.dato;
    }

    /** Avanza el cursor y devuelve el nuevo elemento actual. */
    public T avanzar() {
        if (actual == null) {
            throw new EstructuraVaciaException("ListaCircular");
        }
        actual = actual.siguiente;
        return actual.dato;
    }

    /** Devuelve el elemento que estará de turno dentro de {@code pasos} rotaciones. */
    public T verSiguiente(int pasos) {
        if (actual == null) {
            throw new EstructuraVaciaException("ListaCircular");
        }
        Nodo<T> n = actual;
        for (int i = 0; i < pasos; i++) {
            n = n.siguiente;
        }
        return n.dato;
    }

    public boolean contiene(T dato) {
        for (T e : this) {
            if (Objects.equals(e, dato)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    /** Recorre una vuelta completa empezando en el elemento actual. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> n = actual;
            private int vistos = 0;

            @Override
            public boolean hasNext() {
                return vistos < tamanio;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T d = n.dato;
                n = n.siguiente;
                vistos++;
                return d;
            }
        };
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(tamanio);
        // desde el primero, y luego cuántos pasos hay hasta el actual
        int posActual = 0;
        if (ultimo != null) {
            Nodo<T> n = ultimo.siguiente;
            for (int i = 0; i < tamanio; i++, n = n.siguiente) {
                out.writeObject(n.dato);
                if (n == actual) {
                    posActual = i;
                }
            }
        }
        out.writeInt(posActual);
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            agregar((T) in.readObject());
        }
        int posActual = in.readInt();
        if (ultimo != null) {
            actual = ultimo.siguiente;
            for (int i = 0; i < posActual; i++) {
                actual = actual.siguiente;
            }
        }
    }
}
